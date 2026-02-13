import React, { useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'
import UserAvatar from '../components/UserAvatar'

function initials(nom = '', prenom = '') {
  const a = (prenom || '').trim()[0] || ''
  const b = (nom || '').trim()[0] || ''
  return (a + b).toUpperCase() || '?'
}

function fmtDate(iso) {
  if (!iso) return '—'
  try {
    const d = new Date(iso)
    return d.toLocaleDateString()
  } catch {
    return '—'
  }
}

function Badge({ children, tone = 'default' }) {
  const tones = {
    default: 'badge',
    ok: 'badge border-emerald-400/30 bg-emerald-500/10 text-emerald-200',
    warn: 'badge border-amber-400/30 bg-amber-500/10 text-amber-200',
    danger: 'badge border-rose-400/30 bg-rose-500/10 text-rose-200',
    info: 'badge border-sky-400/30 bg-sky-500/10 text-sky-200',
  }
  return <span className={tones[tone] || tones.default}>{children}</span>
}

function ActionIconButton({ title, onClick, disabled = false, tone = 'default', children }) {
  const tones = {
    default: 'border-white/15 bg-white/5 text-slate-200 hover:bg-white/10',
    primary: 'border-indigo-400/35 bg-indigo-500/15 text-indigo-100 hover:bg-indigo-500/25',
    danger: 'border-rose-400/35 bg-rose-500/15 text-rose-100 hover:bg-rose-500/25',
  }
  return (
    <button
      type="button"
      className={`inline-flex h-8 w-8 items-center justify-center rounded-lg border transition disabled:opacity-50 disabled:cursor-not-allowed ${tones[tone] || tones.default}`}
      title={title}
      aria-label={title}
      disabled={disabled}
      onClick={onClick}
    >
      <span className="text-sm leading-none">{children}</span>
    </button>
  )
}

// Modal simple inspiré de la page Formations (évite les styles manquants)

export default function Users() {
  const me = useMemo(() => getCurrentUser(), [])
  const myRole = (me?.role || '')
  const isAdmin = myRole === 'ADMIN'
  const isFormateur = myRole === 'FORMATEUR'
  const isStagiaire = myRole === 'STAGIAIRE'

  const [rows, setRows] = useState([])
  const [q, setQ] = useState('')
  const [err, setErr] = useState('')
  const [onlyPending, setOnlyPending] = useState(false)
  const [roleFilter, setRoleFilter] = useState('')
  const [busyId, setBusyId] = useState(null)

  // Détails utilisateur
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState(null) // UtilisateurDTO
  const [profile, setProfile] = useState(null) // ProfilDTO
  const [tab, setTab] = useState('infos')

  // Edition
  const [editMode, setEditMode] = useState(false)
  const [form, setForm] = useState({
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    adresse: '',
    role: '',
    typeFormateur: '',
    motDePasse: '',
  })

  useEffect(() => {
    let ok = true
    ;(async () => {
      try {
        setErr('')
        const res = await api.get('/api/utilisateurs')
        if (ok) setRows(res.data || [])
      } catch (e) {
        if (ok) setErr(e?.response?.data?.message || e?.message || 'Erreur utilisateurs')
      }
    })()
    return () => { ok = false }
  }, [])

  const pendingCount = useMemo(() => (rows || []).filter(u => !u.valide).length, [rows])

  const countsByRole = useMemo(() => {
    const c = { ADMIN: 0, FORMATEUR: 0, STAGIAIRE: 0 }
    ;(rows || []).forEach(u => {
      if (u?.role && c[u.role] !== undefined) c[u.role] += 1
    })
    return c
  }, [rows])

  const filtered = useMemo(() => {
    const qq = q.trim().toLowerCase()
    return (rows || []).filter(u => {
      const s = `${u.nom || ''} ${u.prenom || ''} ${u.email || ''} ${u.role || ''}`.toLowerCase()
      if (onlyPending && u.valide) return false
      if (roleFilter && String(u.role) !== roleFilter) return false
      return !qq || s.includes(qq)
    })
  }, [rows, q, onlyPending, roleFilter])

  async function refresh() {
    const res = await api.get('/api/utilisateurs')
    setRows(res.data || [])
  }

  function canInteract(targetUser, currentUserRole, currentUserId) {
    if (!targetUser) return false
    if (currentUserRole === 'STAGIAIRE') return false
    if (currentUserRole === 'FORMATEUR') {
      if (String(targetUser.role) === 'ADMIN') return false
      if (String(targetUser.role) === 'FORMATEUR' && Number(targetUser.id) !== Number(currentUserId)) return false
      return true
    }
    if (currentUserRole === 'ADMIN') return true
    return false
  }

  async function openDetails(u) {
    if (!canInteract(u, myRole, me?.id)) return
    setSelected(u)
    setOpen(true)
    setEditMode(false)
    setTab('infos')
    setProfile(null)
    try {
      const res = await api.get(`/api/profile/${u.id}`)
      setProfile(res.data)
      // Prefill edit form
      setForm({
        nom: res.data?.nom || '',
        prenom: res.data?.prenom || '',
        email: res.data?.email || '',
        telephone: res.data?.telephone || '',
        adresse: res.data?.adresse || '',
        role: String(res.data?.role || ''),
        typeFormateur: String(res.data?.typeFormateur || ''),
        motDePasse: '',
      })
    } catch (e) {
      if (e?.response?.status === 403) {
        setOpen(false)
        setSelected(null)
      } else {
        setErr(e?.response?.data?.message || e?.message || 'Erreur profil utilisateur')
      }
    }
  }

  function canEditTarget(target) {
    if (!target) return false
    if (isAdmin) return true
    if (isFormateur) return target.id === me?.id || String(target.role) === 'STAGIAIRE'
    return false
  }

  function canDeleteTarget(target) {
    if (!target) return false
    if (isAdmin) return target.id !== me?.id
    if (isFormateur) return target.id === me?.id || String(target.role) === 'STAGIAIRE'
    return false
  }

  function canChangeRole(target) {
    if (!target) return false
    if (!isAdmin) return false
    // évite de se tirer une balle dans le pied
    if (target.id === me?.id) return false
    return true
  }

  async function valider(u) {
    if (!isAdmin) return
    setBusyId(u.id)
    try {
      await api.put(`/api/utilisateurs/${u.id}/valider`)
      await refresh()
      // recharger profil dans la modal si ouverte
      if (open && selected?.id === u.id) {
        const res = await api.get(`/api/profile/${u.id}`)
        setProfile(res.data)
      }
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur validation')
    } finally {
      setBusyId(null)
    }
  }

  async function removeUser(u) {
    if (!canDeleteTarget(u)) return
    const ok = window.confirm(`Supprimer définitivement ${u.prenom} ${u.nom} ?`)
    if (!ok) return
    setBusyId(u.id)
    try {
      await api.delete(`/api/utilisateurs/${u.id}`)
      await refresh()
      setOpen(false)
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur suppression')
    } finally {
      setBusyId(null)
    }
  }

  async function saveEdit() {
    if (!selected) return
    if (!canEditTarget(selected)) return

    // STAGIAIRE: on n'édite pas depuis ici => profil
    if (isStagiaire) {
      window.location.hash = '#/profil'
      return
    }

    setBusyId(selected.id)
    try {
      const payload = {
        nom: form.nom,
        prenom: form.prenom,
        email: form.email,
        telephone: form.telephone,
        adresse: form.adresse,
      }

      if (isAdmin) {
        payload.role = form.role || null
        payload.typeFormateur = form.role === 'FORMATEUR' ? (form.typeFormateur || null) : null
      }

      if (form.motDePasse?.trim()) {
        payload.motDePasse = form.motDePasse
      }

      const res = await api.put(`/api/utilisateurs/${selected.id}`, payload)
      // update list
      await refresh()
      // refresh profile
      const p = await api.get(`/api/profile/${selected.id}`)
      setProfile(p.data)
      setSelected(res.data)
      setEditMode(false)
      setForm(f => ({ ...f, motDePasse: '' }))
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur mise à jour')
    } finally {
      setBusyId(null)
    }
  }

  const modalFooter = useMemo(() => {
    if (!selected) return null
    const canEdit = canEditTarget(selected)
    const canDelete = canDeleteTarget(selected)
    const isPending = profile?.statutValidation === false

    return (
      <div className="flex items-center justify-between gap-2 flex-wrap">
        <div className="flex items-center gap-2 flex-wrap">
          {isAdmin && isPending && (
            <button
              type="button"
              className="btn btn-primary"
              disabled={busyId === selected.id}
              onClick={() => valider(selected)}
            >
              Valider le compte
            </button>
          )}
          {canEdit && (
            <button
              type="button"
              className="btn"
              onClick={() => {
                if (isStagiaire) {
                  window.location.hash = '#/profil'
                } else {
                  setEditMode(v => !v)
                }
              }}
            >
              {isStagiaire ? 'Modifier mon profil' : (editMode ? 'Annuler' : 'Modifier')}
            </button>
          )}
          {canDelete && (
            <button
              type="button"
              className="btn"
              disabled={busyId === selected.id}
              onClick={() => removeUser(selected)}
            >
              Supprimer
            </button>
          )}
        </div>

        {editMode && (isAdmin || isFormateur) && (
          <button
            type="button"
            className="btn btn-primary"
            disabled={busyId === selected.id}
            onClick={saveEdit}
          >
            Enregistrer
          </button>
        )}
      </div>
    )
  }, [selected, profile, editMode, busyId])

  return (
    <div className="space-y-3">
      <div className="flex items-start sm:items-center justify-between gap-3 flex-wrap">
        <div>
          <div className="title">Utilisateurs</div>
          <div className="text-sm muted">
            {filtered.length} éléments{pendingCount ? ` · ${pendingCount} en attente` : ''}
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <div className="segmented">
            <button
              type="button"
              className={`segmented-btn ${!onlyPending ? 'segmented-btn-active' : ''}`}
              onClick={() => setOnlyPending(false)}
            >
              Tous
            </button>
            <button
              type="button"
              className={`segmented-btn ${onlyPending ? 'segmented-btn-active' : ''}`}
              onClick={() => setOnlyPending(true)}
            >
              En attente
            </button>
          </div>

          <select className="fc-field" value={roleFilter} onChange={e => setRoleFilter(e.target.value)}>
            <option value="">Tous rôles</option>
            <option value="ADMIN">ADMIN ({countsByRole.ADMIN})</option>
            <option value="FORMATEUR">FORMATEUR ({countsByRole.FORMATEUR})</option>
            <option value="STAGIAIRE">STAGIAIRE ({countsByRole.STAGIAIRE})</option>
          </select>

          <input
            className="fc-field"
            placeholder="Recherche..."
            value={q}
            onChange={e => setQ(e.target.value)}
          />
        </div>
      </div>

      {err && <div className="alert-error">{err}</div>}

      <div className="card overflow-auto">
        <table className="table">
          <thead>
            <tr>
              <th>Utilisateur</th>
              <th>Email</th>
              <th>Rôle</th>
              <th>Statut</th>
              {!isStagiaire && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {filtered.map(u => (
              <tr
                key={u.id}
                className={`${canInteract(u, myRole, me?.id) ? 'cursor-pointer hover:bg-white/5' : ''}`}
                onClick={() => { if (canInteract(u, myRole, me?.id)) openDetails(u) }}
              >
                <td className="font-medium text-slate-100">
                  <div className="flex items-center gap-3">
                    <UserAvatar
                      photo={u.photoProfil}
                      nom={u.nom}
                      prenom={u.prenom}
                      size={36}
                      cacheKey={`user_${u.id}`}
                    />
                    <div className="leading-tight">
                      <div>{u.prenom} {u.nom}</div>
                      {u.id === me?.id && <div className="text-xs muted">Vous</div>}
                    </div>
                  </div>
                </td>
                <td>{u.email}</td>
                <td><Badge tone="info">{u.role}</Badge></td>
                <td>
                  {u.valide ? (
                    <Badge tone="ok">Validé</Badge>
                  ) : (
                    <Badge tone="warn">En attente</Badge>
                  )}
                </td>
                {!isStagiaire && (
                  <td onClick={e => e.stopPropagation()}>
                    <div className="flex items-center gap-2">
                      {isAdmin && !u.valide && (
                        <ActionIconButton
                          tone="primary"
                          title="Valider le compte"
                          disabled={busyId === u.id}
                          onClick={() => valider(u)}
                        >
                          ✓
                        </ActionIconButton>
                      )}
                      {canInteract(u, myRole, me?.id) && canEditTarget(u) && (
                        <ActionIconButton
                          title="Modifier"
                          disabled={busyId === u.id}
                          onClick={() => openDetails(u)}
                        >
                          ✎
                        </ActionIconButton>
                      )}
                      {canInteract(u, myRole, me?.id) && canDeleteTarget(u) && (
                        <ActionIconButton
                          tone="danger"
                          title="Supprimer"
                          disabled={busyId === u.id}
                          onClick={() => removeUser(u)}
                        >
                          🗑
                        </ActionIconButton>
                      )}
                      {(!canInteract(u, myRole, me?.id) || (!canEditTarget(u) && !canDeleteTarget(u))) && <span className="muted">—</span>}
                    </div>
                  </td>
                )}
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={isStagiaire ? 4 : 5} className="muted">Aucun utilisateur</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal détails */}
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => { setOpen(false); setEditMode(false) }}
          />

          <div className="relative w-full max-w-3xl card p-5 max-h-[85vh] overflow-auto" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <div className="text-base font-semibold text-slate-100">
                  {selected ? `Profil · ${selected.prenom} ${selected.nom}` : 'Profil'}
                </div>
                <div className="text-sm muted">
                  Cliquez sur un utilisateur pour voir ses détails.
                </div>
              </div>
              <button className="btn" onClick={() => { setOpen(false); setEditMode(false) }}>✕</button>
            </div>

            {!profile && (
              <div className="muted">Chargement...</div>
            )}

            {profile && (
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <UserAvatar
                    photo={profile.photoProfil}
                    nom={profile.nom}
                    prenom={profile.prenom}
                    size={64}
                    cacheKey={`profile_${profile.id}`}
                  />

                  <div className="min-w-0">
                <div className="text-lg font-semibold text-slate-100 truncate">{profile.prenom} {profile.nom}</div>
                <div className="text-sm muted truncate">{profile.email}</div>
                <div className="flex items-center gap-2 mt-1 flex-wrap">
                  <Badge tone="info">{profile.role}</Badge>
                  {profile.statutValidation ? <Badge tone="ok">Compte validé</Badge> : <Badge tone="warn">En attente</Badge>}
                  {profile.typeFormateur && <Badge>{String(profile.typeFormateur)}</Badge>}
                </div>
              </div>
                </div>

            <div className="segmented">
              <button
                type="button"
                className={`segmented-btn ${tab === 'infos' ? 'segmented-btn-active' : ''}`}
                onClick={() => setTab('infos')}
              >
                Infos
              </button>
              <button
                type="button"
                className={`segmented-btn ${tab === 'formations' ? 'segmented-btn-active' : ''}`}
                onClick={() => setTab('formations')}
              >
                Formations
              </button>
              {String(profile.role) === 'STAGIAIRE' && (
                <button
                  type="button"
                  className={`segmented-btn ${tab === 'certifs' ? 'segmented-btn-active' : ''}`}
                  onClick={() => setTab('certifs')}
                >
                  Certificats
                </button>
              )}
            </div>

            {/* Infos */}
            {tab === 'infos' && (
              <div className="card">
                {!editMode && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div>
                      <div className="text-xs muted">Téléphone</div>
                      <div className="text-slate-100">{profile.telephone || '—'}</div>
                    </div>
                    <div>
                      <div className="text-xs muted">Adresse</div>
                      <div className="text-slate-100">{profile.adresse || '—'}</div>
                    </div>
                    <div>
                      <div className="text-xs muted">Date création</div>
                      <div className="text-slate-100">{fmtDate(profile.dateCreation)}</div>
                    </div>
                  </div>
                )}

                {editMode && (isAdmin || isFormateur) && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div>
                      <div className="text-xs muted">Nom</div>
                      <input className="fc-field" value={form.nom} onChange={e => setForm(f => ({ ...f, nom: e.target.value }))} />
                    </div>
                    <div>
                      <div className="text-xs muted">Prénom</div>
                      <input className="fc-field" value={form.prenom} onChange={e => setForm(f => ({ ...f, prenom: e.target.value }))} />
                    </div>
                    <div className="md:col-span-2">
                      <div className="text-xs muted">Email</div>
                      <input className="fc-field" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
                    </div>
                    <div>
                      <div className="text-xs muted">Téléphone</div>
                      <input className="fc-field" value={form.telephone} onChange={e => setForm(f => ({ ...f, telephone: e.target.value }))} />
                    </div>
                    <div>
                      <div className="text-xs muted">Adresse</div>
                      <input className="fc-field" value={form.adresse} onChange={e => setForm(f => ({ ...f, adresse: e.target.value }))} />
                    </div>

                    {canChangeRole(selected) && (
                      <>
                        <div>
                          <div className="text-xs muted">Rôle</div>
                          <select className="fc-field" value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                            <option value="ADMIN">ADMIN</option>
                            <option value="FORMATEUR">FORMATEUR</option>
                            <option value="STAGIAIRE">STAGIAIRE</option>
                          </select>
                        </div>
                        <div>
                          <div className="text-xs muted">Type formateur</div>
                          <select
                            className="fc-field"
                            value={form.typeFormateur}
                            disabled={form.role !== 'FORMATEUR'}
                            onChange={e => setForm(f => ({ ...f, typeFormateur: e.target.value }))}
                          >
                            <option value="">—</option>
                            <option value="INTERNE">INTERNE</option>
                            <option value="EXTERNE">EXTERNE</option>
                          </select>
                        </div>
                      </>
                    )}

                    <div className="md:col-span-2">
                      <div className="text-xs muted">Nouveau mot de passe (optionnel)</div>
                      <input
                        className="fc-field"
                        type="password"
                        value={form.motDePasse}
                        onChange={e => setForm(f => ({ ...f, motDePasse: e.target.value }))}
                      />
                    </div>

                    <div className="md:col-span-2 text-xs muted">
                      Note: seuls les admins peuvent changer les rôles. Le formateur peut gérer uniquement les stagiaires (ou lui-même).
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Formations */}
            {tab === 'formations' && (
              <div className="card">
                {String(profile.role) === 'STAGIAIRE' && (
                  <div>
                    <div className="font-semibold text-slate-100 mb-2">Formations (inscriptions)</div>
                    <div className="space-y-2">
                      {(profile.inscriptions || []).map(i => (
                        <div key={i.id} className="flex items-center justify-between gap-2">
                          <div className="min-w-0">
                            <div className="text-slate-100 truncate">{i.formation?.titre || '—'}</div>
                            <div className="text-xs muted">{fmtDate(i.dateInscription)}</div>
                          </div>
                          <Badge>{String(i.statut || '—')}</Badge>
                        </div>
                      ))}
                      {(profile.inscriptions || []).length === 0 && <div className="muted">Aucune inscription</div>}
                    </div>
                  </div>
                )}

                {String(profile.role) === 'FORMATEUR' && (
                  <div>
                    <div className="font-semibold text-slate-100 mb-2">Formations encadrées</div>
                    <div className="space-y-2">
                      {(profile.formationsFormateur || []).map(f => (
                        <div key={f.id} className="flex items-center justify-between gap-2">
                          <div className="min-w-0">
                            <div className="text-slate-100 truncate">{f.titre || '—'}</div>
                            <div className="text-xs muted">{fmtDate(f.dateDebut)} → {fmtDate(f.dateFin)}</div>
                          </div>
                          <Badge>{String(f.statut || '—')}</Badge>
                        </div>
                      ))}
                      {(profile.formationsFormateur || []).length === 0 && <div className="muted">Aucune formation</div>}
                    </div>
                  </div>
                )}

                {String(profile.role) === 'ADMIN' && (
                  <div className="muted">Un administrateur n'est pas inscrit à des formations.</div>
                )}
              </div>
            )}

            {/* Certificats */}
            {tab === 'certifs' && String(profile.role) === 'STAGIAIRE' && (
              <div className="card">
                <div className="font-semibold text-slate-100 mb-2">Certificats obtenus</div>
                <div className="space-y-2">
                  {(profile.certificats || []).map(c => (
                    <div key={c.id} className="flex items-center justify-between gap-2">
                      <div className="min-w-0">
                        <div className="text-slate-100 truncate">{c.formation?.titre || '—'}</div>
                        <div className="text-xs muted">N° {c.numeroCertificat || '—'} · {fmtDate(c.dateObtention)}</div>
                      </div>
                      <Badge>{c.noteObtenue != null ? `Note ${c.noteObtenue}` : '—'}</Badge>
                    </div>
                  ))}
                  {(profile.certificats || []).length === 0 && <div className="muted">Aucun certificat</div>}
                </div>
              </div>
            )}
              </div>
            )}

            <div className="mt-5">
              {modalFooter}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
