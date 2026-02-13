import React, { useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'
import UserAvatar from '../components/UserAvatar'

function badgeClass(statut) {
  if (statut === 'VALIDE') return 'bg-emerald-500/15 text-emerald-200 border-emerald-400/20'
  if (statut === 'REVOQUE') return 'bg-rose-500/15 text-rose-200 border-rose-400/20'
  return 'bg-white/5 text-slate-200 border-white/10'
}

function formatDate(d) {
  if (!d) return '-'
  try {
    return new Date(d).toLocaleDateString()
  } catch {
    return d
  }
}

function groupByFormation(certs = []) {
  const map = new Map()
  certs.forEach((c) => {
    const fid = c?.formation?.id
    if (!fid) return
    if (!map.has(fid)) {
      map.set(fid, {
        formation: c.formation,
        items: [],
      })
    }
    map.get(fid).items.push(c)
  })
  return Array.from(map.values())
    .sort((a, b) => String(a?.formation?.titre || '').localeCompare(String(b?.formation?.titre || '')))
}

async function downloadPdf(id, numeroCertificat) {
  try {
    const pdf = await api.get(`/api/certificats/${id}/pdf`, { responseType: 'blob' })
    const blob = new Blob([pdf.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `certificat_${numeroCertificat || id}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
    alert(e?.response?.data?.message || 'Téléchargement PDF impossible')
  }
}

export default function Certificats() {
  const user = getCurrentUser()
  const role = user?.role

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [certificats, setCertificats] = useState([])

  const [query, setQuery] = useState('')
  const [only, setOnly] = useState('TOUS') // TOUS | VALIDE | REVOQUE

  const [openFormation, setOpenFormation] = useState(null) // {formation, items}

  async function load() {
    setLoading(true)
    setError('')
    try {
      if (!user) throw new Error('Non connecté')

      // Admin: tout
      // Formateur: uniquement ses formations
      // Stagiaire: uniquement ses certificats
      let res
      if (role === 'ADMIN') {
        res = await api.get('/api/certificats')
      } else if (role === 'FORMATEUR') {
        res = await api.get('/api/certificats/formateur/me')
      } else {
        res = await api.get(`/api/certificats/utilisateur/${user.id}`)
      }
      setCertificats(res?.data || [])
    } catch (e) {
      setError(e?.response?.data?.message || e.message || 'Erreur de chargement')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (certificats || []).filter((c) => {
      if (only !== 'TOUS' && c?.statut !== only) return false
      if (!q) return true
      const f = (c?.formation?.titre || '').toLowerCase()
      const nom = `${c?.stagiaire?.prenom || ''} ${c?.stagiaire?.nom || ''}`.toLowerCase()
      const code = (c?.numeroCertificat || '').toLowerCase()
      return f.includes(q) || nom.includes(q) || code.includes(q)
    })
  }, [certificats, query, only])

  const groups = useMemo(() => groupByFormation(filtered), [filtered])

  const myList = useMemo(() => {
    // stagiaire: on garde liste plate triée
    return [...(filtered || [])].sort((a, b) => {
      const da = a?.dateObtention ? String(a.dateObtention) : ''
      const db = b?.dateObtention ? String(b.dateObtention) : ''
      return db.localeCompare(da)
    })
  }, [filtered])

  async function revoke(id) {
    if (!window.confirm('Révoquer ce certificat ?')) return
    try {
      await api.post(`/api/certificats/${id}/revoke`)
      await load()
    } catch (e) {
      alert(e?.response?.data?.message || e.message || 'Erreur')
    }
  }

  async function restore(id) {
    if (!window.confirm('Restaurer ce certificat ?')) return
    try {
      await api.post(`/api/certificats/${id}/restore`)
      await load()
    } catch (e) {
      alert(e?.response?.data?.message || e.message || 'Erreur')
    }
  }

  async function del(id) {
    if (!window.confirm('Supprimer définitivement ce certificat ?')) return
    try {
      await api.delete(`/api/certificats/${id}`)
      await load()
    } catch (e) {
      alert(e?.response?.data?.message || e.message || 'Erreur')
    }
  }

  const isStaff = role === 'ADMIN' || role === 'FORMATEUR'

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="text-lg font-semibold text-slate-100">Certificats</div>
          <div className="text-sm text-slate-400">
            Les certificats sont <span className="text-slate-200 font-semibold">générés automatiquement</span> après réussite d’une évaluation.
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-2 sm:items-center">
          <input
            className="input"
            placeholder="Rechercher (formation, stagiaire, code...)"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />

          <select className="input" value={only} onChange={(e) => setOnly(e.target.value)}>
            <option value="TOUS">Tous</option>
            <option value="VALIDE">Valides</option>
            <option value="REVOQUE">Révoqués</option>
          </select>

          <button className="btn" onClick={load} disabled={loading}>
            Actualiser
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3 rounded-xl border border-rose-400/20 bg-rose-500/10 text-rose-200 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-sm text-slate-400">Chargement...</div>
      ) : role === 'STAGIAIRE' ? (
        <div className="overflow-x-auto">
          <table className="table w-full">
            <thead>
              <tr>
                <th>Formation</th>
                <th>Code</th>
                <th>Note</th>
                <th>Statut</th>
                <th>Obtenu le</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {myList.map((c) => (
                <tr key={c.id}>
                  <td className="font-medium">{c?.formation?.titre || '-'}</td>
                  <td className="font-mono text-xs">{c?.numeroCertificat || '-'}</td>
                  <td>{c?.noteObtenue ?? '-'}</td>
                  <td>
                    <span className={`inline-flex items-center px-2 py-1 rounded-lg border text-xs ${badgeClass(c?.statut)}`}>
                      {c?.statut || '-'}
                    </span>
                  </td>
                  <td>{formatDate(c?.dateObtention)}</td>
                  <td className="text-right space-x-2">
                    <button className="btn btn-sm" onClick={() => downloadPdf(c.id, c?.numeroCertificat)}>
                      PDF
                    </button>
                    <a className="btn btn-sm" href={`/certificats/verify/${c?.numeroCertificat || ''}`}>
                      Vérifier
                    </a>
                  </td>
                </tr>
              ))}
              {myList.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-sm text-slate-400">Aucun certificat.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="space-y-3">
          {groups.map((g) => {
            const validCount = g.items.filter((x) => x?.statut === 'VALIDE').length
            const revokedCount = g.items.filter((x) => x?.statut === 'REVOQUE').length
            return (
              <div key={g.formation.id} className="p-4 rounded-2xl border border-white/10 bg-white/5">
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
                  <div>
                    <div className="text-slate-100 font-semibold">{g?.formation?.titre || 'Formation'}</div>
                    <div className="text-xs text-slate-400">
                      {formatDate(g?.formation?.dateDebut)} → {formatDate(g?.formation?.dateFin)} · {g?.formation?.statut || ''}
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-xs px-2 py-1 rounded-lg border border-white/10 bg-white/5 text-slate-200">
                      Total: {g.items.length}
                    </span>
                    <span className="text-xs px-2 py-1 rounded-lg border border-emerald-400/20 bg-emerald-500/10 text-emerald-200">
                      Valides: {validCount}
                    </span>
                    <span className="text-xs px-2 py-1 rounded-lg border border-rose-400/20 bg-rose-500/10 text-rose-200">
                      Révoqués: {revokedCount}
                    </span>

                    <button className="btn" onClick={() => setOpenFormation(g)}>
                      Détails
                    </button>
                  </div>
                </div>
              </div>
            )
          })}

          {groups.length === 0 && (
            <div className="text-sm text-slate-400">Aucun certificat.</div>
          )}
        </div>
      )}

      {/* Modal details */}
      {openFormation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/60"
            onClick={() => setOpenFormation(null)}
          />

          <div className="relative w-[min(1100px,95vw)] max-h-[85vh] overflow-hidden rounded-2xl border border-white/10 bg-slate-950 shadow-xl">
            <div className="p-4 border-b border-white/10 flex items-center justify-between">
              <div>
                <div className="text-slate-100 font-semibold">{openFormation?.formation?.titre || 'Détails'}</div>
                <div className="text-xs text-slate-400">Liste des certificats obtenus par les stagiaires</div>
              </div>
              <button className="btn" onClick={() => setOpenFormation(null)}>Fermer</button>
            </div>

            <div className="p-4 overflow-auto max-h-[calc(85vh-80px)]">
              <div className="overflow-x-auto">
                <table className="table w-full">
                  <thead>
                    <tr>
                      <th>Stagiaire</th>
                      <th>Code</th>
                      <th>Note</th>
                      <th>Statut</th>
                      <th>Obtenu</th>
                      <th>Révoqué</th>
                      <th className="text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {openFormation.items
                      .slice()
                      .sort((a, b) => {
                        const na = `${a?.stagiaire?.prenom || ''} ${a?.stagiaire?.nom || ''}`
                        const nb = `${b?.stagiaire?.prenom || ''} ${b?.stagiaire?.nom || ''}`
                        return na.localeCompare(nb)
                      })
                      .map((c) => (
                        <tr key={c.id}>
                          <td>
                            <div className="flex items-center gap-3">
                              <UserAvatar
                                photo={c?.stagiaire?.photoProfil}
                                nom={c?.stagiaire?.nom}
                                prenom={c?.stagiaire?.prenom}
                                size={32}
                                cacheKey={c?.stagiaire?.photoProfil}
                              />
                              <div className="min-w-0">
                                <div className="text-slate-100 text-sm font-medium truncate">
                                  {c?.stagiaire ? `${c?.stagiaire?.prenom || ''} ${c?.stagiaire?.nom || ''}` : '-'}
                                </div>
                                <div className="text-xs text-slate-400 truncate">{c?.stagiaire?.email || ''}</div>
                              </div>
                            </div>
                          </td>
                          <td className="font-mono text-xs">{c?.numeroCertificat || '-'}</td>
                          <td>{c?.noteObtenue ?? '-'}</td>
                          <td>
                            <span className={`inline-flex items-center px-2 py-1 rounded-lg border text-xs ${badgeClass(c?.statut)}`}>
                              {c?.statut || '-'}
                            </span>
                          </td>
                          <td>{formatDate(c?.dateObtention)}</td>
                          <td>{formatDate(c?.dateRevocation)}</td>
                          <td className="text-right space-x-2">
                            <button className="btn btn-sm" onClick={() => downloadPdf(c.id, c?.numeroCertificat)}>
                              PDF
                            </button>
                            <a className="btn btn-sm" href={`/certificats/verify/${c?.numeroCertificat || ''}`}>
                              Vérifier
                            </a>

                            {/* Revoke / Restore */}
                            {c?.statut === 'VALIDE' ? (
                              <button className="btn btn-sm" onClick={() => revoke(c.id)}>
                                Révoquer
                              </button>
                            ) : role === 'ADMIN' ? (
                              <button className="btn btn-sm" onClick={() => restore(c.id)}>
                                Restaurer
                              </button>
                            ) : null}

                            {/* Delete */}
                            {isStaff && (
                              <button className="btn btn-sm" onClick={() => del(c.id)}>
                                Supprimer
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}

                    {openFormation.items.length === 0 && (
                      <tr>
                        <td colSpan={7} className="text-sm text-slate-400">Aucun certificat.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
