import React, { useEffect, useState } from 'react'
import { FileText, Plus, Upload, CheckCircle, ListChecks } from 'lucide-react'
import { getCurrentUser } from '../../auth/auth'
import {
  getAllByFormation,
  getSoumissions,
  getSoumissionsByStagiaire,
  remove,
  downloadTpFile,
} from '../../services/TpRessourceService'
import TpForm from './TpForm'
import TpSoumission from './TpSoumission'
import TpCorrection from './TpCorrection'

function formatDeadline(dateStr) {
  if (!dateStr) return null
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return null
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(d)
}

function StatutBadge({ statut }) {
  const s = String(statut || '').toUpperCase()
  if (s === 'CORRIGE') {
    return (
      <span className="badge border-emerald-400/30 bg-emerald-500/15 text-emerald-200">
        Corrigé
      </span>
    )
  }
  return (
    <span className="badge border-amber-400/30 bg-amber-500/15 text-amber-200">
      Soumis
    </span>
  )
}

export default function TpList({ formationId, formationTitre, canManage }) {
  const u = getCurrentUser()
  const role = u?.role
  const isStagiaire = role === 'STAGIAIRE'

  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [soumissionTp, setSoumissionTp] = useState(null)
  const [correctionTp, setCorrectionTp] = useState(null)
  const [soumissionsByTp, setSoumissionsByTp] = useState({})
  const [mySoumissionsByTp, setMySoumissionsByTp] = useState({})

  async function loadTpList() {
    if (!formationId) return
    setLoading(true)
    setErr('')
    try {
      const data = await getAllByFormation(formationId)
      setList(Array.isArray(data) ? data : [])
    } catch (e) {
      const status = e?.response?.status
      if (status === 403 || status === 401) {
        setList([])
        setErr('')
        return
      }
      setErr(e?.response?.data?.message || e?.message || 'Erreur chargement TP.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTpList()
  }, [formationId])

  useEffect(() => {
    if (!formationId || !u?.id) return
    if (isStagiaire) {
      getSoumissionsByStagiaire(u.id)
        .then((arr) => {
          const byTp = {}
          ;(Array.isArray(arr) ? arr : []).forEach((s) => {
            const tid = s?.tpId
            if (tid != null) byTp[tid] = s
          })
          setMySoumissionsByTp(byTp)
        })
        .catch(() => setMySoumissionsByTp({}))
    }
  }, [formationId, u?.id, isStagiaire, list.length])

  async function loadSoumissionsForTp(tpId) {
    try {
      const data = await getSoumissions(tpId)
      setSoumissionsByTp((prev) => ({ ...prev, [tpId]: Array.isArray(data) ? data : [] }))
    } catch {
      setSoumissionsByTp((prev) => ({ ...prev, [tpId]: [] }))
    }
  }

  function openCorrection(tp) {
    setCorrectionTp(tp)
    loadSoumissionsForTp(tp.id)
  }

  async function deleteTp(tp) {
    if (!window.confirm(`Supprimer « ${tp.titre } » ?`)) return
    try {
      await remove(tp.id)
      await loadTpList()
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur suppression.')
    }
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div className="flex items-center gap-2 text-slate-100">
          <FileText className="w-5 h-5" />
          <span className="text-sm font-semibold">Ressources & Travaux pratiques</span>
        </div>
        {canManage && (
          <button
            className="btn btn-primary inline-flex items-center gap-2"
            onClick={() => setShowForm(true)}
          >
            <Plus className="w-4 h-4" />
            Ajouter un TP
          </button>
        )}
      </div>

      {err && <div className="alert-error">{err}</div>}

      {loading && <div className="text-sm text-slate-400">Chargement…</div>}

      {!loading && list.length === 0 && (
        <div className="text-sm text-slate-400">Aucun TP ou ressource pour l’instant.</div>
      )}

      {!loading && list.length > 0 && (
        <div className="overflow-auto">
          <table className="table">
            <thead>
              <tr>
                <th>Titre</th>
                <th>Type</th>
                <th>Lien</th>
                <th>Deadline</th>
                {isStagiaire && <th>Mon statut</th>}
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {list.map((tp) => {
                const mySub = mySoumissionsByTp[tp.id]
                return (
                  <tr key={tp.id}>
                    <td>
                      <div className="font-medium text-slate-100">{tp.titre || '—'}</div>
                      {tp.description && (
                        <div className="text-xs text-slate-400 line-clamp-2 mt-0.5">
                          {tp.description}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="badge">{tp.type || 'TP'}</span>
                    </td>
                    <td>
                      {tp.fichierUrl ? (
                        <button
                          type="button"
                          className="btn text-sm"
                          onClick={() => downloadTpFile(tp.id)}
                        >
                          Télécharger
                        </button>
                      ) : (
                        <span className="text-slate-500">Aucun fichier</span>
                      )}
                    </td>
                    <td>
                      {formatDeadline(tp.dateLimite) ?? <span className="text-slate-500">—</span>}
                    </td>
                    {isStagiaire && (
                      <td>
                        {mySub ? (
                          <StatutBadge statut={mySub.statut} />
                        ) : (
                          <span className="text-slate-500">Non rendu</span>
                        )}
                      </td>
                    )}
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-2 flex-wrap">
                        {isStagiaire ? (
                          <button
                            className="btn btn-primary inline-flex items-center gap-1"
                            onClick={() => setSoumissionTp({ tp, mySoumission: mySub })}
                          >
                            <Upload className="w-4 h-4" />
                            Voir / Soumettre
                          </button>
                        ) : canManage ? (
                          <>
                            <button
                              className="btn inline-flex items-center gap-1"
                              onClick={() => openCorrection(tp)}
                            >
                              <ListChecks className="w-4 h-4" />
                              Corriger
                            </button>
                            <button
                              className="btn"
                              onClick={() => deleteTp(tp)}
                            >
                              Supprimer
                            </button>
                          </>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal: Ajouter un TP */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setShowForm(false)}
          />
          <div className="relative w-full max-w-lg card p-5 max-h-[85vh] overflow-auto">
            <TpForm
              formationId={formationId}
              formationTitre={formationTitre}
              onSuccess={() => {
                setShowForm(false)
                loadTpList()
              }}
              onCancel={() => setShowForm(false)}
            />
          </div>
        </div>
      )}

      {/* Modal: Soumettre (stagiaire) */}
      {soumissionTp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setSoumissionTp(null)}
          />
          <div className="relative w-full max-w-lg card p-5 max-h-[85vh] overflow-auto">
            <TpSoumission
              tp={soumissionTp.tp}
              mySoumission={soumissionTp.mySoumission}
              onSuccess={() => {
                setSoumissionTp(null)
                getSoumissionsByStagiaire(u?.id).then((arr) => {
                  const byTp = {}
                  ;(Array.isArray(arr) ? arr : []).forEach((s) => {
                    const tid = s?.tpId
                    if (tid != null) byTp[tid] = s
                  })
                  setMySoumissionsByTp(byTp)
                })
              }}
              onCancel={() => setSoumissionTp(null)}
            />
          </div>
        </div>
      )}

      {/* Modal: Corriger (formateur) */}
      {correctionTp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setCorrectionTp(null)}
          />
          <div className="relative w-full max-w-4xl card p-5 max-h-[85vh] overflow-auto">
            <TpCorrection
              tp={correctionTp}
              soumissions={soumissionsByTp[correctionTp.id] || []}
              onSuccess={() => {
                loadSoumissionsForTp(correctionTp.id)
              }}
              onCancel={() => setCorrectionTp(null)}
            />
          </div>
        </div>
      )}
    </div>
  )
}
