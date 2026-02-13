import React, { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'
import PhotoCropModal from '../components/PhotoCropModal'
import { resolveMediaUrl, withCacheBuster } from '../utils/media'

function roleLabel(role) {
  if (role === 'ADMIN') return 'Administrateur'
  if (role === 'FORMATEUR') return 'Formateur'
  if (role === 'STAGIAIRE') return 'Stagiaire'
  return role || 'Utilisateur'
}

function badgeForStatus(s) {
  if (!s) return 'badge'
  const v = String(s).toUpperCase()
  if (v.includes('ATTENTE')) return 'badge badge-warn'
  if (v.includes('CONFIR') || v.includes('EN_COURS')) return 'badge badge-ok'
  if (v.includes('TERM')) return 'badge badge-neutral'
  if (v.includes('REFUS')) return 'badge badge-bad'
  return 'badge'
}

function initials(nom, prenom) {
  const a = (prenom || '').trim().slice(0, 1).toUpperCase()
  const b = (nom || '').trim().slice(0, 1).toUpperCase()
  return `${a}${b}` || 'U'
}

function fmtDate(d) {
  if (!d) return '-'
  try {
    return String(d).slice(0, 10)
  } catch {
    return '-'
  }
}

export default function Profil() {
  const me = useMemo(() => getCurrentUser(), [])
  const [p, setP] = useState(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  const [edit, setEdit] = useState(false)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')

  const [form, setForm] = useState({
    nom: '',
    prenom: '',
    telephone: '',
    adresse: '',
    nouveauMotDePasse: '',
  })

  const fileRef = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [localPreview, setLocalPreview] = useState(null)

  // Photo: si l'image serveur a été en 404 une fois, certains navigateurs peuvent la "cacher".
  // On ajoute un cache-buster et un fallback propre.
  const [photoBroken, setPhotoBroken] = useState(false)
  const [photoV, setPhotoV] = useState(Date.now())

  // Recadrage
  const [cropOpen, setCropOpen] = useState(false)
  const [cropFile, setCropFile] = useState(null)

  async function uploadPhotoBlob(blob) {
    setUploading(true)
    setMsg('')
    try {
      const fd = new FormData()
      const file = new File([blob], 'profile.jpg', { type: 'image/jpeg' })
      fd.append('file', file)
      await api.post('/api/profile/me/photo', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      await load()
      setMsg('Photo mise à jour ✅')
    } catch (err) {
      setMsg(err?.response?.data || err?.message || 'Erreur upload photo')
    } finally {
      setUploading(false)
    }
  }

  async function load() {
    setLoading(true)
    setErr('')
    try {
      const { data } = await api.get('/api/profile/me')
      setP(data)
      setPhotoBroken(false)
      setPhotoV(Date.now())
      setForm({
        nom: data?.nom || '',
        prenom: data?.prenom || '',
        telephone: data?.telephone || '',
        adresse: data?.adresse || '',
        nouveauMotDePasse: '',
      })
    } catch (e) {
      setErr(e?.message || 'Erreur chargement profil')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const isStagiaire = (p?.role || me?.role) === 'STAGIAIRE'
  const isFormateur = (p?.role || me?.role) === 'FORMATEUR'
  const rawPhoto = localPreview || (p?.photoProfil ? p.photoProfil : null)
  const photoSrc = rawPhoto ? withCacheBuster(resolveMediaUrl(rawPhoto), photoV) : null

  async function saveProfile() {
    setSaving(true)
    setMsg('')
    try {
      await api.put('/api/profile/me', {
        nom: form.nom,
        prenom: form.prenom,
        telephone: form.telephone,
        adresse: form.adresse,
        nouveauMotDePasse: form.nouveauMotDePasse || null,
      })
      setMsg('Profil mis à jour ✅')
      setEdit(false)
      setLocalPreview(null)
      await load()
    } catch (e) {
      setMsg(e?.response?.data || e?.message || 'Erreur mise à jour')
    } finally {
      setSaving(false)
    }
  }

  function cancelEdit() {
    setEdit(false)
    setLocalPreview(null)
    setMsg('')
    if (p) {
      setForm({
        nom: p.nom || '',
        prenom: p.prenom || '',
        telephone: p.telephone || '',
        adresse: p.adresse || '',
        nouveauMotDePasse: '',
      })
    }
  }

  async function onPickFile(e) {
    const file = e.target.files?.[0]
    if (!file) return
    if (!file.type?.startsWith('image/')) {
      setMsg("Veuillez choisir une image (jpg/png/webp)")
      return
    }

    // Ouvre la modal de recadrage (obligatoire)
    setCropFile(file)
    setCropOpen(true)
    // reset value pour pouvoir re-sélectionner le même fichier
    try { e.target.value = '' } catch {}
  }

  async function removePhoto() {
    setUploading(true)
    setMsg('')
    try {
      await api.delete('/api/profile/me/photo')
      setLocalPreview(null)
      await load()
      setMsg('Photo supprimée ✅')
    } catch (e) {
      setMsg(e?.message || 'Erreur suppression photo')
    } finally {
      setUploading(false)
    }
  }

  if (loading) {
    return (
      <div className="p-6">
        <div className="text-slate-300">Chargement du profil...</div>
      </div>
    )
  }

  if (err) {
    return (
      <div className="p-6">
        <div className="alert-error">{err}</div>
      </div>
    )
  }

  return (
    <div className="p-6 space-y-6">
      <PhotoCropModal
        open={cropOpen}
        file={cropFile}
        onClose={() => {
          setCropOpen(false)
          setCropFile(null)
        }}
        onConfirm={async (blob) => {
          const previewUrl = URL.createObjectURL(blob)
          setLocalPreview(previewUrl)
          await uploadPhotoBlob(blob)
        }}
        outputSize={320}
      />

      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <div className="text-2xl font-semibold text-slate-100">Mon profil</div>
          <div className="text-slate-400 text-sm">Gérez vos informations et votre photo.</div>
        </div>

        <div className="flex items-center gap-2">
          {!edit ? (
            <button className="btn btn-primary" onClick={() => setEdit(true)}>
              Modifier
            </button>
          ) : (
            <>
              <button className="btn" disabled={saving} onClick={cancelEdit}>
                Annuler
              </button>
              <button className="btn btn-primary" disabled={saving} onClick={saveProfile}>
                {saving ? 'Enregistrement...' : 'Enregistrer'}
              </button>
            </>
          )}
        </div>
      </div>

      {msg && <div className="alert-info">{msg}</div>}

      {/* Header card */}
      <div className="card p-5">
        <div className="flex flex-col md:flex-row items-start md:items-center gap-5">
          <div className="relative">
            {photoSrc && !photoBroken ? (
              <img
                src={photoSrc}
                alt="avatar"
                className="h-24 w-24 rounded-full object-cover border border-slate-700"
                onError={() => setPhotoBroken(true)}
              />
            ) : (
              <div className="h-24 w-24 rounded-full grid place-items-center bg-slate-800 border border-slate-700 text-slate-100 text-2xl font-semibold">
                {initials(p?.nom, p?.prenom)}
              </div>
            )}

            <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={onPickFile} />
          </div>

          <div className="flex-1">
            <div className="text-xl font-semibold text-slate-100">
              {p?.prenom} {p?.nom}
            </div>
            <div className="text-slate-400 text-sm">{p?.email}</div>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="badge">{roleLabel(p?.role)}</span>
              {p?.role === 'FORMATEUR' && p?.typeFormateur && <span className="badge badge-neutral">{p.typeFormateur}</span>}
              {p?.role !== 'STAGIAIRE' && (
                <span className={p?.statutValidation ? 'badge badge-ok' : 'badge badge-warn'}>
                  {p?.statutValidation ? 'Compte validé' : 'En attente validation'}
                </span>
              )}
              <span className="badge badge-neutral">Créé le {fmtDate(p?.dateCreation)}</span>
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <button
              className="btn"
              disabled={uploading}
              onClick={() => fileRef.current?.click()}
            >
              {uploading ? 'Upload...' : 'Télécharger une photo'}
            </button>
            <button className="btn" disabled={uploading || !p?.photoProfil} onClick={removePhoto}>
              Supprimer la photo
            </button>
            <div className="text-[11px] text-slate-500 max-w-[220px]">
              Formats: JPG/PNG/WebP • Max 3MB
            </div>
          </div>
        </div>
      </div>

      {/* Infos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card p-5">
          <div className="text-lg font-semibold text-slate-100">Informations personnelles</div>
          <div className="text-sm text-slate-400">Mettez à jour vos informations.</div>

          <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-medium text-slate-300">Nom</label>
              <input
                className="fc-field mt-1"
                disabled={!edit}
                value={form.nom}
                onChange={(e) => setForm((x) => ({ ...x, nom: e.target.value }))}
              />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-300">Prénom</label>
              <input
                className="fc-field mt-1"
                disabled={!edit}
                value={form.prenom}
                onChange={(e) => setForm((x) => ({ ...x, prenom: e.target.value }))}
              />
            </div>

            <div>
              <label className="text-xs font-medium text-slate-300">Téléphone</label>
              <input
                className="fc-field mt-1"
                disabled={!edit}
                placeholder="Ex: +212 6xx xx xx xx"
                value={form.telephone}
                onChange={(e) => setForm((x) => ({ ...x, telephone: e.target.value }))}
              />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-300">Adresse</label>
              <input
                className="fc-field mt-1"
                disabled={!edit}
                placeholder="Ex: Casablanca, Maroc"
                value={form.adresse}
                onChange={(e) => setForm((x) => ({ ...x, adresse: e.target.value }))}
              />
            </div>

            <div className="md:col-span-2">
              <label className="text-xs font-medium text-slate-300">Nouveau mot de passe (optionnel)</label>
              <input
                className="fc-field mt-1"
                type="password"
                disabled={!edit}
                placeholder="••••••"
                value={form.nouveauMotDePasse}
                onChange={(e) => setForm((x) => ({ ...x, nouveauMotDePasse: e.target.value }))}
              />
              <div className="mt-1 text-[11px] text-slate-500">Laissez vide pour ne pas changer.</div>
            </div>
          </div>
        </div>

        {/* Right side: role specific */}
        <div className="card p-5">
          <div className="text-lg font-semibold text-slate-100">
            {isStagiaire ? 'Mes formations' : isFormateur ? 'Mes formations encadrées' : 'Aperçu'}
          </div>
          <div className="text-sm text-slate-400">
            {isStagiaire
              ? "Formations où vous êtes inscrit"
              : isFormateur
              ? "Formations que vous encadrez"
              : "Résumé de votre compte"}
          </div>

          <div className="mt-4 space-y-3">
            {isStagiaire ? (
              (p?.inscriptions || []).length ? (
                p.inscriptions.map((i) => (
                  <div key={i.id} className="p-3 rounded-xl border border-slate-800 bg-slate-900/30">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="font-semibold text-slate-100">{i?.formation?.titre}</div>
                        <div className="text-xs text-slate-500">{fmtDate(i?.formation?.dateDebut)} → {fmtDate(i?.formation?.dateFin)}</div>
                      </div>
                      <span className={badgeForStatus(i?.statut)}>{i?.statut}</span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-slate-500 text-sm">Aucune inscription pour le moment.</div>
              )
            ) : isFormateur ? (
              (p?.formationsFormateur || []).length ? (
                p.formationsFormateur.map((f) => (
                  <div key={f.id} className="p-3 rounded-xl border border-slate-800 bg-slate-900/30">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="font-semibold text-slate-100">{f?.titre}</div>
                        <div className="text-xs text-slate-500">{fmtDate(f?.dateDebut)} → {fmtDate(f?.dateFin)}</div>
                      </div>
                      <span className={badgeForStatus(f?.statut)}>{f?.statut}</span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-slate-500 text-sm">Aucune formation assignée pour le moment.</div>
              )
            ) : (
              <div className="text-slate-500 text-sm">
                Compte {p?.statutValidation ? 'actif' : 'en attente de validation'}.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Certificats (STAGIAIRE seulement) */}
      {isStagiaire && (
        <div className="card p-5">
          <div className="text-lg font-semibold text-slate-100">Certificats obtenus</div>
          <div className="text-sm text-slate-400">Vos certificats générés après réussite.</div>

          <div className="mt-4 overflow-auto">
            {(p?.certificats || []).length ? (
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Formation</th>
                    <th>Date</th>
                    <th>Note</th>
                  </tr>
                </thead>
                <tbody>
                  {p.certificats.map((c) => (
                    <tr key={c.id}>
                      <td className="text-slate-300">{c.numeroCertificat}</td>
                      <td className="text-slate-200">{c.formation?.titre || '-'}</td>
                      <td className="text-slate-300">{fmtDate(c.dateObtention)}</td>
                      <td className="text-slate-300">{c.noteObtenue ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="text-slate-500 text-sm">Aucun certificat pour le moment.</div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
