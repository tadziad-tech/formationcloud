import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useParams, useLocation, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'
import TpList from '../components/tp/TpList'

// Avoid hard crashes when backend returns an unexpected shape
function asArray(v) {
  if (Array.isArray(v)) return v
  // Spring Data pages sometimes look like { content: [...] }
  if (v && typeof v === 'object' && Array.isArray(v.content)) return v.content
  return []
}

function priceLabel(p) {
  const v = Number(p || 0)
  return v <= 0 ? 'Gratuit' : `${v} DH`
}

function daysBetweenInclusive(a, b) {
  try {
    if (!a || !b) return 0
    const d1 = new Date(a)
    const d2 = new Date(b)
    // normalize (UTC noon) to avoid DST issues
    const t1 = Date.UTC(d1.getFullYear(), d1.getMonth(), d1.getDate(), 12)
    const t2 = Date.UTC(d2.getFullYear(), d2.getMonth(), d2.getDate(), 12)
    const diff = Math.round((t2 - t1) / (1000 * 60 * 60 * 24))
    return diff >= 0 ? diff + 1 : 0
  } catch {
    return 0
  }
}

function dureeLabel(f) {
  const n = daysBetweenInclusive(f?.dateDebut, f?.dateFin)
  if (!n) return '-'
  if (n === 1) return '1 jour'
  return `${n} jours`
}



function fmtDT(v) {
  try {
    if (!v) return '-'
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    return d.toLocaleString()
  } catch {
    return String(v || '-')
  }
}

function toInputDT(v) {
  // Accepts ISO-ish string; returns value compatible with <input type="datetime-local">
  try {
    if (!v) return ''
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) {
      // fallback: already like 2026-02-03T10:00
      return String(v).slice(0,16)
    }
    const pad = (n) => String(n).padStart(2,'0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return ''
  }
}

function normalizeDTLocal(v) {
  // input datetime-local gives 'YYYY-MM-DDTHH:mm' -> backend expects LocalDateTime (seconds optional)
  if (!v) return null
  const s = String(v)
  return s.length === 16 ? `${s}:00` : s
}

function statutDisplay(f) {
  const s = String(f?.statut || '').toUpperCase()
  if (s === 'TERMINEE') return { label: 'Terminée', tone: 'success' }
  if (s === 'ANNULEE') return { label: 'Annulée', tone: 'danger' }

  // ACTIVE : on détecte si la formation est en cours selon les dates
  const today = new Date()
  const dd = f?.dateDebut ? new Date(f.dateDebut) : null
  const df = f?.dateFin ? new Date(f.dateFin) : null
  if (dd && df && today >= dd && today <= df) {
    return { label: 'En cours', tone: 'warning' }
  }

  return { label: 'Active', tone: 'neutral' }
}

function badgeClass(tone) {
  if (tone === 'success') return 'badge border-emerald-400/30 bg-emerald-500/15 text-emerald-200'
  if (tone === 'danger') return 'badge border-red-400/30 bg-red-500/15 text-red-200'
  if (tone === 'warning') return 'badge border-amber-400/30 bg-amber-500/15 text-amber-200'
  return 'badge'
}

function progressTone(pct) {
  const v = Number(pct)
  if (Number.isNaN(v)) return 'neutral'
  if (v >= 80) return 'success'
  if (v >= 50) return 'warning'
  return 'danger'
}


function inscriptionBadge(statut) {
  const s = String(statut || '').toUpperCase()
  if (s === 'CONFIRMEE') return { label: 'Inscrit', cls: badgeClass('success') }
  if (s === 'EN_COURS') return { label: 'En cours', cls: badgeClass('warning') }
  if (s === 'TERMINEE') return { label: 'Terminée', cls: badgeClass('success') }
  if (s === 'EN_ATTENTE') return { label: 'En attente', cls: 'badge border-indigo-400/30 bg-indigo-500/15 text-indigo-200' }
  if (s === 'REFUSEE') return { label: 'Refusée', cls: badgeClass('danger') }
  if (s === 'ABANDONNEE') return { label: 'Quitée', cls: 'badge border-slate-400/30 bg-slate-500/10 text-slate-200' }
  return { label: s || '-', cls: 'badge' }
}

export default function Formations() {
  const u = getCurrentUser()
  const me = u
  const role = (me?.role ?? '').toUpperCase()
  const isAdmin = role.includes('ADMIN')
  const isFormateur = role.includes('FORMATEUR')
  const isStagiaire = role.includes('STAGIAIRE')
  const { id: paramId } = useParams()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const fidParam = paramId || searchParams.get('formationId')

  const [rows, setRows] = useState([])
  const [q, setQ] = useState('')
  const [viewMode, setViewMode] = useState('ALL') // ALL | MINE
  const [myInscriptions, setMyInscriptions] = useState([]) // STAGIAIRE only
  const [err, setErr] = useState('')

  // Create modal (ADMIN)
  const [showCreate, setShowCreate] = useState(false)
  const [formateurs, setFormateurs] = useState([])
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState('')

  // Details modal
  const [showDetails, setShowDetails] = useState(false)
  const [selected, setSelected] = useState(null)
  const [inscriptions, setInscriptions] = useState([])
  const [formationAccess, setFormationAccess] = useState(null) // { role, isAdmin, isAssignedFormateur, inscriptionStatus, enrolledConfirmed } from /api/formations/{id}/access
  const [stagiairesActifs, setStagiairesActifs] = useState([])
  const [addStagiaireId, setAddStagiaireId] = useState('')
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [detailsErr, setDetailsErr] = useState('')
  const [actionMsg, setActionMsg] = useState('')
  const [working, setWorking] = useState(false)

  // Progression (dans détails formation)
  const [progressMap, setProgressMap] = useState({})
  const [progressLoading, setProgressLoading] = useState(false)
  const [progressErr, setProgressErr] = useState('')

  // Séances (dans détails formation)
  const [seances, setSeances] = useState([])
  const [seancesLoading, setSeancesLoading] = useState(false)
  const [seancesErr, setSeancesErr] = useState('')
  const [showSeanceModal, setShowSeanceModal] = useState(false)
  const [editingSeance, setEditingSeance] = useState(null)
  const [seTitre, setSeTitre] = useState('')
  const [seDescription, setSeDescription] = useState('')
  const [seMode, setSeMode] = useState('PRESENTIEL')
  const [seDateDebut, setSeDateDebut] = useState('')
  const [seDateFin, setSeDateFin] = useState('')
  const [seZoomLink, setSeZoomLink] = useState('')
  const [seLieu, setSeLieu] = useState('')
  const [seStatut, setSeStatut] = useState('PLANIFIEE')
  const [seSaving, setSeSaving] = useState(false)

  // Présences (dans détails formation)
  const [showPresenceModal, setShowPresenceModal] = useState(false)
  const [presenceSeance, setPresenceSeance] = useState(null)
  const [presences, setPresences] = useState([])
  const [presenceLoading, setPresenceLoading] = useState(false)
  const [presenceSaving, setPresenceSaving] = useState(false)
  const [presenceErr, setPresenceErr] = useState('')
  const [myPresence, setMyPresence] = useState(null)

  // Onglet détails formation : participants | programme | ressources
  const [detailsTab, setDetailsTab] = useState('participants')
  // Filtre séances (Programme) : ALL | UPCOMING | PAST
  const [seancesFilter, setSeancesFilter] = useState('ALL')


  // Form create fields
  const [titre, setTitre] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState('A_DISTANCE')
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')
  const [capaciteMax, setCapaciteMax] = useState(30)
  const [prix, setPrix] = useState('0')
  const [formateurId, setFormateurId] = useState('')

  async function loadData() {
    setErr('')
    setSuccessMsg('')

    // Tous les utilisateurs voient toutes les formations.
    // La gestion dépend ensuite du rôle (stagiaire = demande, formateur = uniquement ses formations, admin = tout).
    let formationsUrl = '/api/formations'

    const r1 = await api.get(formationsUrl)
    setRows(asArray(r1.data))

    // STAGIAIRE: on récupère mes inscriptions pour afficher 'Mon statut' + filtre 'Mes formations'
    if (role === 'STAGIAIRE' && u?.id) {
      try {
        const r2 = await api.get(`/api/inscriptions/utilisateur/${u.id}`)
        setMyInscriptions(asArray(r2.data))
      } catch (e) {
        setMyInscriptions([])
      }
    } else {
      setMyInscriptions([])
    }
  }

  useEffect(() => {
    let ok = true
    ;(async () => {
      try {
        await loadData()
        if (!ok) return
      } catch (e) {
        if (ok) setErr(e?.response?.data?.message || e?.message || 'Erreur formations')
      }
    })()
    return () => { ok = false }
  }, [role])

  // Deep-link: ouvrir modal formation (URL = /formations?formationId=5&tab=ressources&tpId=…&seanceId=…)
  const lastDeepLink = useRef(null)
  useEffect(() => {
    if (!fidParam) return
    const fid = Number(fidParam)
    if (!fid || Number.isNaN(fid)) return
    const rawTab = searchParams.get('tab') || ''
    const tabFromQuery = (rawTab === 'seances') ? 'programme' : rawTab
    const initialTab = (tabFromQuery && ['participants', 'programme', 'ressources'].includes(tabFromQuery))
      ? tabFromQuery
      : (location.pathname.endsWith('/tp') ? 'ressources' : 'programme')
    const key = `${fid}-${initialTab}`
    if (lastDeepLink.current === key) return
    lastDeepLink.current = key
    openDetails(fid, initialTab)
  }, [fidParam, location.pathname, searchParams])

  useEffect(() => {
    let ok = true
    ;(async () => {
      try {
        if (role !== 'ADMIN') return
        const res = await api.get('/api/formateurs/valides')
        if (!ok) return
        setFormateurs(asArray(res.data))
      } catch {
        // silencieux
      }
    })()
    return () => { ok = false }
  }, [role])


  const myStatusByFormation = useMemo(() => {
    const map = {}
    ;(myInscriptions || []).forEach(insc => {
      const fid = insc?.formation?.id
      if (!fid) return
      const t = insc?.dateInscription ? new Date(insc.dateInscription).getTime() : (Number(insc?.id) || 0)
      const prev = map[fid]
      const tPrev = prev?.dateInscription ? new Date(prev.dateInscription).getTime() : (Number(prev?.id) || 0)
      if (!prev || t > tPrev) map[fid] = insc
    })
    const out = {}
    Object.keys(map).forEach(fid => { out[fid] = map[fid]?.statut })
    return out
  }, [myInscriptions])

  const filtered = useMemo(() => {
    const qq = q.trim().toLowerCase()
    return (rows || []).filter(f => {
      const okQ = !qq
        || (f.titre || '').toLowerCase().includes(qq)
        || (f.description || '').toLowerCase().includes(qq)

      // Filtre "Mes formations"
      if (viewMode === 'MINE') {
        if (role === 'FORMATEUR') {
          return okQ && Number(f?.formateur?.id) === Number(u?.id)
        }
        if (role === 'STAGIAIRE') {
          const st = String(myStatusByFormation?.[f?.id] || '').toUpperCase()
          return okQ && (st === 'CONFIRMEE' || st === 'EN_COURS' || st === 'TERMINEE')
        }
      }

      return okQ
    })
  }, [rows, q, viewMode, role, u?.id, myStatusByFormation])

  function resetCreateForm() {
    setTitre('')
    setDescription('')
    setType('A_DISTANCE')
    setDateDebut('')
    setDateFin('')
    setCapaciteMax(30)
    setPrix('0')
    setFormateurId('')
  }

  async function createFormation() {
    try {
      setErr('')
      setSuccessMsg('')
      if (role !== 'ADMIN') return
      if (!titre.trim()) throw new Error('Titre requis')
      if (!type) throw new Error('Type requis')
      if (!dateDebut) throw new Error('Date début requise')
      if (!dateFin) throw new Error('Date fin requise')
      if (!formateurId) throw new Error('Veuillez sélectionner un formateur')
      if (new Date(dateFin) < new Date(dateDebut)) throw new Error('La date de fin doit être après la date de début')

      const cap = Number(capaciteMax)
      if (!cap || cap < 1) throw new Error('Capacité invalide')

      setSaving(true)
      await api.post('/api/formations', {
        titre: titre.trim(),
        description: description?.trim() || '',
        type,
        dateDebut,
        dateFin,
        capaciteMax: cap,
        prix: Number(prix || 0),
        formateurId: Number(formateurId),
      })

      setShowCreate(false)
      resetCreateForm()
      setSuccessMsg('Formation créée avec succès')
      await loadData()
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur création formation')
    } finally {
      setSaving(false)
    }
  }

  async function openDetails(formationId, initialTab) {
    try {
      setShowDetails(true)
      setDetailsErr('')
      setActionMsg('')
      setDetailsLoading(true)
      setSelected(null)
      setInscriptions([])
      setFormationAccess(null)
      setProgressMap({})
      setProgressErr('')
      setProgressLoading(false)
      setSeances([])
      setSeancesErr('')
      setMyPresence(null)
      setPresences([])
      setPresenceErr('')
      setAddStagiaireId('')
      setDetailsTab(initialTab || 'programme')

      const formationIdNum = Number(formationId)
      if (!formationIdNum || Number.isNaN(formationIdNum)) {
        setDetailsErr('Identifiant de formation invalide')
        setDetailsLoading(false)
        return
      }

      // 1) Toujours charger les infos publiques + access (ne jamais bloquer l'ouverture à cause d'un 403 interne)
      let formation
      let access = null
      try {
        const [fRes, accessRes] = await Promise.all([
          api.get(`/api/formations/${formationId}`),
          api.get(`/api/formations/${formationId}/access`).catch(() => ({ data: null })),
        ])
        formation = fRes.data
        access = accessRes?.data ?? null
        setSelected(formation)
        setFormationAccess(access)
      } catch (e) {
        const status = e?.response?.status
        const msg = e?.response?.data?.message ?? e?.message ?? 'Erreur chargement détails'
        if (status === 403) setDetailsErr('Vous n\'avez pas accès à cette formation.')
        else setDetailsErr(msg)
        setDetailsLoading(false)
        return
      }

      const formateurId = formation?.formateur?.id != null ? Number(formation.formateur.id) : null
      const canManage = isAdmin || (isFormateur && formateurId === Number(me?.id))
      const inscriptionStatus = String(access?.inscriptionStatus || '').toUpperCase()
      const inscriptionConfirmed = access?.enrolledConfirmed === true || ['CONFIRMEE', 'EN_COURS', 'TERMINEE'].includes(inscriptionStatus)
      const canSeeInternals = canManage || (isStagiaire && inscriptionConfirmed)

      // 2) Uniquement si canManage : charger participants / inscriptions
      if (canManage) {
        try {
          const iRes = await api.get(`/api/inscriptions/formation/${formationId}`)
          setInscriptions(asArray(iRes.data))
        } catch (e) {
          setInscriptions([])
          if (e?.response?.status !== 403 && e?.response?.status !== 401) {
            const msg = e?.response?.data?.message || e?.message
            if (msg) setDetailsErr(msg)
          }
        }

        try {
          setProgressLoading(true)
          const pRes = await api.get(`/api/formations/${formationId}/progression/participants`)
          const arr = asArray(pRes.data)
          const map = {}
          for (const it of arr) {
            const id = it?.utilisateur?.id
            if (id != null) map[Number(id)] = it?.progression
          }
          setProgressMap(map)
        } catch (e) {
          setProgressMap({})
          if (e?.response?.status !== 403 && e?.response?.status !== 401) {
            setProgressErr(e?.response?.data?.message || e?.message || 'Erreur chargement progression')
          }
        } finally {
          setProgressLoading(false)
        }

        try {
          const sRes = await api.get('/api/stagiaires/actifs')
          setStagiairesActifs(asArray(sRes.data))
        } catch (e) {
          setStagiairesActifs([])
        }
      } else {
        setStagiairesActifs([])
      }

      // 3) Uniquement si canSeeInternals : charger séances
      if (canSeeInternals) {
        try {
          setSeancesLoading(true)
          setSeancesErr('')
          const res = await api.get(`/api/formations/${formationId}/seances`)
          setSeances(asArray(res.data))
        } catch (e) {
          setSeances([])
          if (e?.response?.status === 403 || e?.response?.status === 401) {
            setSeancesErr('')
          } else {
            setSeancesErr(e?.response?.data?.message || e?.message || 'Erreur chargement séances')
          }
        } finally {
          setSeancesLoading(false)
        }
      } else {
        setSeances([])
        setSeancesErr('')
      }
    } catch (e) {
      const status = e?.response?.status
      const msg = e?.response?.data?.message ?? e?.message ?? 'Erreur chargement détails'
      if (status === 403) setDetailsErr('Vous n\'avez pas accès à cette formation.')
      else setDetailsErr(msg)
    } finally {
      setDetailsLoading(false)
    }
  }

  async function refreshDetails() {
    if (!selected?.id) return
    await openDetails(selected.id)
  }

  
  async function loadSeances(formationId) {
    if (!formationId) return
    try {
      setSeancesLoading(true)
      setSeancesErr('')
      const res = await api.get(`/api/formations/${formationId}/seances`)
      setSeances(asArray(res.data))
    } catch (e) {
      const status = e?.response?.status
      setSeances([])
      if (status === 403 || status === 401) {
        setSeancesErr('')
        return
      }
      setSeancesErr(e?.response?.data?.message || e?.message || 'Erreur chargement séances')
    } finally {
      setSeancesLoading(false)
    }
  }

  function openCreateSeance() {
    setEditingSeance(null)
    setSeTitre('')
    setSeDescription('')
    setSeMode('PRESENTIEL')
    setSeDateDebut('')
    setSeDateFin('')
    setSeZoomLink('')
    setSeLieu('')
    setSeStatut('PLANIFIEE')
    setShowSeanceModal(true)
  }

  function openEditSeance(s) {
    if (!s) return
    setEditingSeance(s)
    setSeTitre(s.titre || '')
    setSeDescription(s.description || '')
    setSeMode(String(s.mode || 'PRESENTIEL'))
    setSeDateDebut(toInputDT(s.dateDebut))
    setSeDateFin(toInputDT(s.dateFin))
    setSeZoomLink(s.zoomLink || '')
    setSeLieu(s.lieu || '')
    setSeStatut(String(s.statut || 'PLANIFIEE'))
    setShowSeanceModal(true)
  }

  async function saveSeance() {
    if (!selected?.id) return
    try {
      setSeSaving(true)
      setSeancesErr('')
      const payload = {
        titre: seTitre,
        description: seDescription,
        mode: seMode,
        dateDebut: normalizeDTLocal(seDateDebut),
        dateFin: normalizeDTLocal(seDateFin),
        zoomLink: seMode === 'DISTANCIEL' ? seZoomLink : null,
        lieu: seMode === 'PRESENTIEL' ? seLieu : null,
        statut: seStatut,
      }
      if (editingSeance?.id) {
        await api.put(`/api/seances/${editingSeance.id}`, payload)
      } else {
        await api.post(`/api/formations/${selected.id}/seances`, payload)
      }
      setShowSeanceModal(false)
      await loadSeances(selected.id)
      await refreshDetails()
    } catch (e) {
      setSeancesErr(e?.response?.data?.message || e?.message || 'Erreur enregistrement séance')
    } finally {
      setSeSaving(false)
    }
  }

  async function deleteSeance(seanceId) {
    if (!seanceId) return
    if (!window.confirm('Supprimer cette séance ?')) return
    try {
      setWorking(true)
      setSeancesErr('')
      await api.delete(`/api/seances/${seanceId}`)
      await loadSeances(selected?.id)
    } catch (e) {
      setSeancesErr(e?.response?.data?.message || e?.message || 'Erreur suppression séance')
    } finally {
      setWorking(false)
    }
  }

  async function openPresence(seance) {
    if (!seance?.id) return
    setPresenceSeance(seance)
    setPresences([])
    setPresenceErr('')
    setMyPresence(null)
    setShowPresenceModal(true)
    await loadPresenceData(seance.id)
  }

  async function loadPresenceData(seanceId) {
    try {
      setPresenceLoading(true)
      setPresenceErr('')
      if (role === 'STAGIAIRE') {
        const res = await api.get(`/api/seances/${seanceId}/presences/me`)
        setMyPresence(res.data || null)
      } else {
        const res = await api.get(`/api/seances/${seanceId}/presences`)
        setPresences(asArray(res.data))
      }
    } catch (e) {
      setPresenceErr(e?.response?.data?.message || e?.message || 'Erreur chargement présences')
    } finally {
      setPresenceLoading(false)
    }
  }

  function updatePresence(idx, patch) {
    setPresences(prev => {
      const next = [...prev]
      next[idx] = { ...next[idx], ...patch }
      return next
    })
  }

  async function savePresences() {
    if (!presenceSeance?.id) return
    try {
      setPresenceSaving(true)
      setPresenceErr('')
      const payload = presences.map(p => ({
        stagiaireId: p?.stagiaire?.id,
        statut: p?.statut || 'NON_MARQUE',
        remarque: p?.remarque || '',
      }))
      const res = await api.put(`/api/seances/${presenceSeance.id}/presences/bulk`, payload)
      setPresences(asArray(res.data))
      setShowPresenceModal(false)
    } catch (e) {
      setPresenceErr(e?.response?.data?.message || e?.message || 'Erreur enregistrement présences')
    } finally {
      setPresenceSaving(false)
    }
  }

async function inscrireMoi() {
    if (!selected?.id) return
    try {
      setWorking(true)
      setActionMsg('')
      await api.post('/api/inscriptions', {
        // utilisateurId ignoré côté back (sécurité) mais on le garde pour compat
        utilisateurId: u.id,
        formationId: selected.id,
      })
      setActionMsg('Demande envoyée ✅')
      await refreshDetails()
      await loadData()
    } catch (e) {
      setDetailsErr(e?.response?.data?.message || e?.message || 'Erreur inscription')
    } finally {
      setWorking(false)
    }
  }

  async function annulerOuQuitter(inscriptionId) {
    if (!inscriptionId) return
    try {
      setWorking(true)
      setActionMsg('')
      await api.delete(`/api/inscriptions/${inscriptionId}`)
      setActionMsg('Action effectuée ✅')
      await refreshDetails()
      await loadData()
    } catch (e) {
      setDetailsErr(e?.response?.data?.message || e?.message || 'Erreur action')
    } finally {
      setWorking(false)
    }
  }

  async function validerInscription(inscriptionId) {
    try {
      setWorking(true)
      setActionMsg('')
      await api.put(`/api/inscriptions/${inscriptionId}/valider`)
      setActionMsg('Inscription validée ✅')
      await refreshDetails()
      await loadData()
    } catch (e) {
      setDetailsErr(e?.response?.data?.message || e?.message || 'Erreur validation')
    } finally {
      setWorking(false)
    }
  }

  async function refuserInscription(inscriptionId) {
    try {
      setWorking(true)
      setActionMsg('')
      await api.put(`/api/inscriptions/${inscriptionId}/refuser`)
      setActionMsg('Inscription refusée ✅')
      await refreshDetails()
      await loadData()
    } catch (e) {
      setDetailsErr(e?.response?.data?.message || e?.message || 'Erreur refus')
    } finally {
      setWorking(false)
    }
  }

  async function addParticipant() {
    try {
      setDetailsErr('')
      setActionMsg('')
      if (!addStagiaireId) throw new Error('Choisissez un stagiaire')
      setWorking(true)
      await api.post(`/api/formations/${selected.id}/participants`, {
        stagiaireId: Number(addStagiaireId),
      })
      setAddStagiaireId('')
      setActionMsg('Stagiaire ajouté ✅')
      await refreshDetails()
      await loadData()
    } catch (e) {
      setDetailsErr(e?.response?.data?.message || e?.message || 'Erreur ajout')
    } finally {
      setWorking(false)
    }
  }

  async function removeParticipant(stagiaireId) {
    try {
      setDetailsErr('')
      setActionMsg('')
      setWorking(true)
      await api.delete(`/api/formations/${selected.id}/participants/${stagiaireId}`)
      setActionMsg('Stagiaire retiré ✅')
      await refreshDetails()
      await loadData()
    } catch (e) {
      setDetailsErr(e?.response?.data?.message || e?.message || 'Erreur suppression')
    } finally {
      setWorking(false)
    }
  }

  // Liste visible "Stagiaires inscrits" : toutes les inscriptions sauf EN_ATTENTE.
  const participantsConfirmes = useMemo(() => {
    return (inscriptions || []).filter(i => {
      const s = String(i?.statut || '').toUpperCase()
      return s !== 'EN_ATTENTE'
    })
  }, [inscriptions])

  // Liste "Demandes en attente" pour validation (admin/formateur assigné).
  const pendingRequests = useMemo(() => {
    return (inscriptions || []).filter(i => String(i?.statut || '').toUpperCase() === 'EN_ATTENTE')
  }, [inscriptions])

  const myInscription = useMemo(() => {
    if (!u?.id) return null
    return (inscriptions || []).find(i => i?.utilisateur?.id === u.id)
  }, [inscriptions, u?.id])
  const isAssignedFormateur = useMemo(() => {
    return isFormateur && selected != null && Number(selected?.formateur?.id) === Number(me?.id)
  }, [isFormateur, selected, me?.id])

  const canManage = useMemo(() => isAdmin || isAssignedFormateur, [isAdmin, isAssignedFormateur])

  const currentInscriptionStatus = useMemo(() => {
    const st = formationAccess?.inscriptionStatus ?? myInscription?.statut
    return st ? String(st).toUpperCase() : ''
  }, [formationAccess, myInscription])

  const isStagiaireConfirmed = useMemo(() => {
    if (!isStagiaire) return false
    if (formationAccess?.enrolledConfirmed === true) return true
    return ['CONFIRMEE', 'EN_COURS', 'TERMINEE'].includes(currentInscriptionStatus)
  }, [isStagiaire, formationAccess, currentInscriptionStatus])

  const canAccessAdvancedSections = useMemo(() => canManage || isStagiaireConfirmed, [canManage, isStagiaireConfirmed])

  const canSeeSeances = useMemo(() => canManage || isStagiaireConfirmed, [canManage, isStagiaireConfirmed])

  const formateurNotAssigned = isFormateur && !isAssignedFormateur

  // Séance passée = dateFin < now
  function isSeancePast(s) {
    const fin = s?.dateFin ? new Date(s.dateFin) : null
    return fin != null && fin < new Date()
  }

  // À venir: dateFin > now (ou si dateFin null: dateDebut > now)
  function isSeanceUpcoming(s) {
    const now = new Date()
    const fin = s?.dateFin ? new Date(s.dateFin) : null
    const ref = fin || (s?.dateDebut ? new Date(s.dateDebut) : null)
    return ref != null && ref > now
  }

  const filteredSeances = useMemo(() => {
    const list = seances || []
    if (seancesFilter === 'UPCOMING') return list.filter(isSeanceUpcoming)
    if (seancesFilter === 'PAST') return list.filter(isSeancePast)
    return list
  }, [seances, seancesFilter])

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div>
          <div className="title">Formations</div>
          <div className="text-sm muted">{filtered.length} éléments</div>
        </div>
        <div className="flex gap-2 flex-wrap">
          <input
            className="fc-field"
            placeholder="Recherche..."
            value={q}
            onChange={e => setQ(e.target.value)}
          />

          {(role === 'STAGIAIRE' || role === 'FORMATEUR') && (
            <div className="flex gap-2">
              <button
                className={`btn ${viewMode === 'ALL' ? 'btn-primary' : ''}`}
                onClick={() => setViewMode('ALL')}
              >
                Toutes
              </button>
              <button
                className={`btn ${viewMode === 'MINE' ? 'btn-primary' : ''}`}
                onClick={() => setViewMode('MINE')}
              >
                Mes formations
              </button>
            </div>
          )}

          {role === 'ADMIN' && (
            <button
              className="btn btn-primary"
              onClick={() => { setShowCreate(true); setErr(''); setSuccessMsg('') }}
            >
              + Créer une formation
            </button>
          )}
        </div>
      </div>

      {err && <div className="alert-error">{err}</div>}
      {successMsg && <div className="alert-success">{successMsg}</div>}

      <div className="card overflow-auto">
        <table className="table">
          <thead>
            <tr>
              <th>Titre</th>
              <th>Durée</th>
              <th>Type</th>
              <th>Statut</th>
              <th>Mon statut</th>
              <th>Prix</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(f => (
              <tr
                key={f.id}
                className="cursor-pointer hover:bg-white/5 transition"
                onClick={() => openDetails(f.id)}
              >
                <td className="font-medium text-slate-100">{f.titre}</td>
                <td>{dureeLabel(f)}</td>
                <td>{f.type || '-'}</td>
                <td>
                  {(() => {
                    const st = statutDisplay(f)
                    return <span className={badgeClass(st.tone)}>{st.label}</span>
                  })()}
                </td>
                <td>
                  {(() => {
                    if (role === 'STAGIAIRE') {
                      const st = myStatusByFormation?.[f.id]
                      if (!st) return <span className="muted">—</span>
                      const b = inscriptionBadge(st)
                      return <span className={b.cls}>{b.label}</span>
                    }
                    if (role === 'FORMATEUR' && Number(f?.formateur?.id) === Number(u?.id)) {
                      return <span className="badge border-indigo-400/30 bg-indigo-500/15 text-indigo-200">Formateur</span>
                    }
                    return <span className="muted">—</span>
                  })()}
                </td>
                <td>{priceLabel(f.prix)}</td>
              </tr>
            ))}
            {filtered.length === 0 && <tr><td colSpan="6" className="muted">Aucune formation</td></tr>}
          </tbody>
        </table>
      </div>

      {/* Modal détails formation (tous rôles) */}
      {showDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => { setShowDetails(false); setDetailsErr(''); setActionMsg('') }}
          />

          <div className="relative w-full max-w-5xl card p-5 max-h-[85vh] overflow-auto">
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <div className="text-base font-semibold text-slate-100">Détails de la formation</div>
                <div className="text-sm muted">Clique sur une formation pour voir participants et inscriptions.</div>
              </div>
              <div className="flex items-center gap-2">
                <button className="btn" onClick={refreshDetails} disabled={detailsLoading || working}>⟳</button>
                <button className="btn" onClick={() => { setShowDetails(false); setDetailsErr(''); setActionMsg('') }}>✕</button>
              </div>
            </div>

            {detailsErr && <div className="alert-error mb-3">{detailsErr}</div>}
            {actionMsg && <div className="alert-success mb-3">{actionMsg}</div>}

            {detailsLoading && (
              <div className="muted">Chargement…</div>
            )}

            {!detailsLoading && selected && (
              <div className="space-y-4">
                <div className={formateurNotAssigned ? '' : 'grid grid-cols-1 md:grid-cols-3 gap-3'}>
                  <div className={formateurNotAssigned ? 'card p-4' : 'md:col-span-2 card p-4'}>
                    <div className="text-lg font-semibold text-slate-100">{selected.titre}</div>
                    <div className="text-sm muted mt-1">{selected.description || 'Aucune description.'}</div>

                    <div className="flex flex-wrap gap-2 mt-3">
                      <span className="badge">{selected.type || '-'}</span>
                      <span className="badge">{dureeLabel(selected)}</span>
                      {(() => {
                        const st = statutDisplay(selected)
                        return <span className={badgeClass(st.tone)}>{st.label}</span>
                      })()}
                      <span className="badge">{priceLabel(selected.prix)}</span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-4 text-sm">
                      <div className="muted">Date début: <span className="text-slate-100">{selected.dateDebut || '-'}</span></div>
                      <div className="muted">Date fin: <span className="text-slate-100">{selected.dateFin || '-'}</span></div>
                      <div className="muted">Formateur: <span className="text-slate-100">{selected?.formateur ? `${selected.formateur.prenom} ${selected.formateur.nom}` : '-'}</span></div>
                    </div>
                  </div>

                  {!formateurNotAssigned && (
                  <div className="card p-4">
                    <div className="text-sm font-semibold text-slate-100">Votre action</div>
                    <div className="mt-2">
                      {role === 'STAGIAIRE' ? (
                        <div className="space-y-2">
                          {(() => {
                            const status = (formationAccess?.inscriptionStatus ?? myInscription?.statut) ? String(formationAccess?.inscriptionStatus ?? myInscription?.statut).toUpperCase() : null
                            if (status === 'EN_ATTENTE') {
                              return (
                                <>
                                  <div className="text-xs muted">Statut de votre inscription</div>
                                  <div className="mt-1">
                                    <span className="badge border-indigo-400/30 bg-indigo-500/15 text-indigo-200">En attente</span>
                                  </div>
                                  <div className="text-xs muted mt-2">L'inscription sera validée par l'admin ou le formateur.</div>
                                </>
                              )
                            }
                            if (status === 'REFUSEE' || status === 'ABANDONNEE') {
                              return (
                                <>
                                  <div className="text-xs muted">Statut de votre inscription</div>
                                  <div className="mt-1">
                                    {(() => { const b = inscriptionBadge(formationAccess?.inscriptionStatus || myInscription?.statut); return <span className={b.cls}>{b.label}</span> })()}
                                  </div>
                                  <button
                                    className="btn btn-primary w-full mt-3"
                                    onClick={inscrireMoi}
                                    disabled={working}
                                  >
                                    {working ? 'Envoi…' : "Demander inscription"}
                                  </button>
                                  <div className="text-xs muted">L'inscription sera validée par l'admin ou le formateur.</div>
                                </>
                              )
                            }
                            if (status === 'CONFIRMEE' || status === 'EN_COURS' || status === 'TERMINEE') {
                              return (
                                <>
                                  <div className="text-xs muted">Statut de votre inscription</div>
                                  <div className="mt-1">
                                    {(() => { const b = inscriptionBadge(formationAccess?.inscriptionStatus || myInscription?.statut); return <span className={b.cls}>{b.label}</span> })()}
                                  </div>
                                  {myInscription?.id != null && (
                                    <button
                                      className="btn w-full mt-3"
                                      onClick={() => annulerOuQuitter(myInscription.id)}
                                      disabled={working}
                                    >
                                      {working ? 'Traitement…' : 'Quitter la formation'}
                                    </button>
                                  )}
                                  <div className="text-xs muted">L'inscription sera validée par l'admin ou le formateur.</div>
                                </>
                              )
                            }
                            return (
                              <>
                                <button
                                  className="btn btn-primary w-full"
                                  onClick={inscrireMoi}
                                  disabled={working}
                                >
                                  {working ? 'Envoi…' : "Demander inscription"}
                                </button>
                                <div className="text-xs muted">L'inscription sera validée par l'admin ou le formateur.</div>
                              </>
                            )
                          })()}
                        </div>
                      ) : (
                        canManage ? (
                          <div className="space-y-2">
                            <div className="text-xs muted">Ajouter un stagiaire (direct)</div>
                            <select
                              className="fc-field"
                              value={addStagiaireId}
                              onChange={e => setAddStagiaireId(e.target.value)}
                            >
                              <option value="">Choisir un stagiaire…</option>
                              {stagiairesActifs.map(s => (
                                <option key={s.id} value={String(s.id)}>
                                  {s.prenom} {s.nom} ({s.email})
                                </option>
                              ))}
                            </select>
                            <button
                              className="btn btn-primary w-full"
                              onClick={addParticipant}
                              disabled={working}
                            >
                              {working ? 'Ajout…' : 'Ajouter'}
                            </button>
                            <div className="text-xs muted">Ajoute directement le stagiaire à la liste (inscription confirmée).</div>
                          </div>
                        ) : (
                          <div className="text-sm muted">
                            Vous pouvez consulter cette formation, mais vous ne pouvez pas gérer ses inscriptions.
                          </div>
                        )
                      )}
                    </div>
                  </div>
                  )}
                </div>

                {!formateurNotAssigned && canAccessAdvancedSections && canManage && (
                <div className={`grid gap-3 ${pendingRequests.length > 0 ? 'grid-cols-1 lg:grid-cols-2' : 'grid-cols-1'}`}>
                  <div className="card p-4 overflow-auto">
                    <div className="flex items-center justify-between">
                      <div className="text-sm font-semibold text-slate-100">Stagiaires inscrits</div>
                      <div className="text-xs muted">{participantsConfirmes.length} participant(s)</div>
                    </div>

                    <div className="mt-3">
                      <table className="table">
                        <thead>
                          <tr>
                            <th>Stagiaire</th>
                            <th>Statut</th>
                            {canManage && <th>Progression</th>}
                            {canManage && <th className="text-right">Action</th>}
                          </tr>
                        </thead>
                        <tbody>
                          {participantsConfirmes.map(i => (
                            <tr key={i.id}>
                              <td>
                                <div className="font-medium text-slate-100">{i?.utilisateur ? `${i.utilisateur.prenom} ${i.utilisateur.nom}` : '-'}</div>
                                <div className="text-xs muted">{i?.utilisateur?.email || ''}</div>
                              </td>
                              <td>
                                {(() => {
                                  const b = inscriptionBadge(i.statut)
                                  return <span className={b.cls}>{b.label}</span>
                                })()}
                              </td>
                              {canManage && (
                                <td>
                                  {(() => {
                                    const sid = Number(i?.utilisateur?.id)
                                    const st = String(i?.statut || '').toUpperCase()
                                    if (st === 'EN_ATTENTE') return <span className="muted">-</span>
                                    if (progressLoading) return <span className="muted">…</span>
                                    const pct = progressMap?.[sid]
                                    if (typeof pct === 'number') {
                                      const tone = progressTone(pct)
                                      return <span className={badgeClass(tone)}>{pct}%</span>
                                    }
                                    return <span className="muted">-</span>
                                  })()}
                                </td>
                              )}
                              {canManage && (
                                <td className="text-right">
                                  <button
                                    className="btn"
                                    onClick={() => removeParticipant(i?.utilisateur?.id)}
                                    disabled={working}
                                  >
                                    Retirer
                                  </button>
                                </td>
                              )}
                            </tr>
                          ))}
                          {participantsConfirmes.length === 0 && (
                            <tr><td colSpan={canManage ? 4 : 2} className="muted">Aucun participant</td></tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  {canManage && pendingRequests.length > 0 && (
                    <div className="card p-4 overflow-auto">
                      <div className="flex items-center justify-between mb-3">
                        <div className="text-sm font-semibold text-slate-100">Demandes d'inscription</div>
                        <div className="text-xs muted">{pendingRequests.length}</div>
                      </div>

                      <table className="table">
                        <thead>
                          <tr>
                            <th>Stagiaire</th>
                            <th>Date</th>
                            <th className="text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {pendingRequests.map((i) => (
                            <tr key={i.id}>
                              <td>
                                <div className="font-medium text-slate-100">
                                  {i?.utilisateur ? `${i.utilisateur.prenom} ${i.utilisateur.nom}` : '-'}
                                </div>
                                <div className="text-xs muted">{i?.utilisateur?.email || ''}</div>
                              </td>
                              <td className="text-xs muted">{fmtDT(i?.dateInscription)}</td>
                              <td className="text-right">
                                <div className="flex justify-end gap-2 flex-wrap">
                                  <button
                                    className="btn btn-primary"
                                    onClick={() => validerInscription(i.id)}
                                    disabled={working}
                                  >
                                    Accepter
                                  </button>
                                  <button
                                    className="btn"
                                    onClick={() => refuserInscription(i.id)}
                                    disabled={working}
                                  >
                                    Refuser
                                  </button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
                )}

                {!formateurNotAssigned && !canAccessAdvancedSections && isStagiaire && (
                  <div className="text-sm muted px-1">
                    Contenu disponible après inscription confirmée.
                  </div>
                )}

                {!formateurNotAssigned && canAccessAdvancedSections && (
                <>
                {/* Onglets Programme (Séances) | Ressources (TP) */}
                <div className="flex gap-2 border-b border-white/10 pb-2 mb-2">
                  <button
                    className={`px-3 py-2 text-sm font-medium rounded-xl transition ${detailsTab === 'programme' ? 'bg-indigo-500/20 text-indigo-50 border border-indigo-400/30' : 'bg-white/5 text-slate-300 border border-white/10 hover:bg-white/10'}`}
                    onClick={() => setDetailsTab('programme')}
                  >
                    Programme
                  </button>
                  <button
                    className={`px-3 py-2 text-sm font-medium rounded-xl transition ${detailsTab === 'ressources' ? 'bg-indigo-500/20 text-indigo-50 border border-indigo-400/30' : 'bg-white/5 text-slate-300 border border-white/10 hover:bg-white/10'}`}
                    onClick={() => setDetailsTab('ressources')}
                  >
                    Ressources
                  </button>
                </div>

                {detailsTab === 'ressources' && (
                  <div className="card p-4">
                    <TpList
                      formationId={selected?.id}
                      formationTitre={selected?.titre || selected?.nom}
                      canManage={canManage}
                    />
                  </div>
                )}

                {detailsTab === 'programme' && (
                <div className="card p-4">
                  <div className="flex items-center justify-between gap-3 flex-wrap">
                    <div>
                      <div className="text-sm font-semibold text-slate-100">Séances</div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        className="btn"
                        onClick={() => loadSeances(selected?.id)}
                        disabled={seancesLoading}
                      >
                        {seancesLoading ? 'Chargement…' : 'Rafraîchir'}
                      </button>

                      {canManage && (
                        <button
                          className="btn btn-primary"
                          onClick={openCreateSeance}
                          disabled={working || seancesLoading}
                        >
                          + Nouvelle séance
                        </button>
                      )}
                    </div>
                  </div>

                  {seancesErr && <div className="alert-error mt-3">{seancesErr}</div>}

                  {!canSeeSeances && (
                      <div className="text-sm muted mt-3">
                        Séances visibles après validation de l'inscription.
                      </div>
                    )}

                  {canSeeSeances && (
                    <>
                      <div className="flex items-center gap-2 mt-3 flex-wrap">
                        <span className="text-xs muted">Séances :</span>
                        <button
                          className={`btn ${seancesFilter === 'ALL' ? 'btn-primary' : ''}`}
                          onClick={() => setSeancesFilter('ALL')}
                        >
                          Toutes
                        </button>
                        <button
                          className={`btn ${seancesFilter === 'UPCOMING' ? 'btn-primary' : ''}`}
                          onClick={() => setSeancesFilter('UPCOMING')}
                        >
                          À venir
                        </button>
                        <button
                          className={`btn ${seancesFilter === 'PAST' ? 'btn-primary' : ''}`}
                          onClick={() => setSeancesFilter('PAST')}
                        >
                          Passées
                        </button>
                      </div>
                    <div className="mt-3 overflow-auto">
                      <table className="table">
                        <thead>
                          <tr>
                            <th>Début</th>
                            <th>Titre</th>
                            <th>Mode</th>
                            <th>Statut</th>
                            <th className="text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {filteredSeances.map((s) => {
                            const past = isSeancePast(s)
                            return (
                            <tr key={s.id}>
                              <td className="text-xs">{fmtDT(s.dateDebut)}</td>
                              <td>
                                <div className="font-medium">{s.titre || '—'}</div>
                                {s.description && <div className="text-xs muted mt-1 line-clamp-2">{s.description}</div>}
                              </td>
                              <td className="text-xs">
                                <div className="muted">{String(s.mode || '—')}</div>
                                {String(s.mode || '') === 'DISTANCIEL' && s.zoomLink ? (
                                  <a className="link" href={s.zoomLink} target="_blank" rel="noreferrer">
                                    Ouvrir le lien
                                  </a>
                                ) : (
                                  s.lieu && <div className="muted">{s.lieu}</div>
                                )}
                              </td>
                              <td className="text-xs muted">{String(s.statut || '—')}</td>
                              <td className="text-right">
                                <div className="flex items-center justify-end gap-2 flex-wrap">
                                  {past ? (
                                    <button className="btn" onClick={() => openPresence(s)}>
                                      Présence
                                    </button>
                                  ) : (
                                    <span className="inline-flex flex-col items-end" title="Disponible après la séance">
                                      <button className="btn" disabled>
                                        Présence
                                      </button>
                                      <span className="text-xs muted">Disponible après la séance</span>
                                    </span>
                                  )}

                                  {canManage && (
                                    <>
                                      <button className="btn" onClick={() => openEditSeance(s)} disabled={working}>
                                        Modifier
                                      </button>
                                      <button className="btn" onClick={() => deleteSeance(s.id)} disabled={working}>
                                        Supprimer
                                      </button>
                                    </>
                                  )}
                                </div>
                              </td>
                            </tr>
                          )})}
                          {filteredSeances.length === 0 && (
                            <tr>
                              <td colSpan={5} className="muted">
                                {seances.length === 0 ? 'Aucune séance planifiée.' : 'Aucune séance pour ce filtre.'}
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                    </>
                  )}
                </div>
                )}
                </>
                )}

              </div>
            )}
          </div>
        </div>
      )}


      {/* Modal séance (création / édition) */}
      {showSeanceModal && selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setShowSeanceModal(false)}
          />

          <div className="relative w-full max-w-2xl card p-5 max-h-[85vh] overflow-auto">
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <div className="text-base font-semibold text-slate-100">
                  {editingSeance?.id ? 'Modifier la séance' : 'Nouvelle séance'}
                </div>
                <div className="text-sm muted">{selected?.nom || selected?.titre || 'Formation'}</div>
              </div>
              <button className="btn" onClick={() => setShowSeanceModal(false)} disabled={seSaving}>
                Fermer
              </button>
            </div>

            {seancesErr && <div className="alert-error mb-3">{seancesErr}</div>}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <div className="label">Titre</div>
                <input className="input" value={seTitre} onChange={e => setSeTitre(e.target.value)} />
              </div>

              <div>
                <div className="label">Mode</div>
                <select className="input" value={seMode} onChange={e => setSeMode(e.target.value)}>
                  <option value="PRESENTIEL">Présentiel</option>
                  <option value="DISTANCIEL">Distanciel</option>
                </select>
              </div>

              <div>
                <div className="label">Début</div>
                <input className="input" type="datetime-local" value={seDateDebut} onChange={e => setSeDateDebut(e.target.value)} />
              </div>

              <div>
                <div className="label">Fin</div>
                <input className="input" type="datetime-local" value={seDateFin} onChange={e => setSeDateFin(e.target.value)} />
              </div>

              {seMode === 'DISTANCIEL' ? (
                <div className="md:col-span-2">
                  <div className="label">Lien Zoom / Meet</div>
                  <input className="input" value={seZoomLink} onChange={e => setSeZoomLink(e.target.value)} placeholder="https://..." />
                </div>
              ) : (
                <div className="md:col-span-2">
                  <div className="label">Lieu</div>
                  <input className="input" value={seLieu} onChange={e => setSeLieu(e.target.value)} placeholder="Salle, bâtiment..." />
                </div>
              )}

              <div className="md:col-span-2">
                <div className="label">Description (optionnel)</div>
                <textarea className="input min-h-[90px]" value={seDescription} onChange={e => setSeDescription(e.target.value)} />
              </div>

              <div>
                <div className="label">Statut</div>
                <select className="input" value={seStatut} onChange={e => setSeStatut(e.target.value)}>
                  <option value="PLANIFIEE">Planifiée</option>
                  <option value="REALISEE">Réalisée</option>
                  <option value="ANNULEE">Annulée</option>
                </select>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 mt-4">
              <button className="btn" onClick={() => setShowSeanceModal(false)} disabled={seSaving}>
                Annuler
              </button>
              <button className="btn btn-primary" onClick={saveSeance} disabled={seSaving}>
                {seSaving ? 'Enregistrement…' : 'Enregistrer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal présence */}
      {showPresenceModal && presenceSeance && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => setShowPresenceModal(false)}
          />

          <div className="relative w-full max-w-4xl card p-5 max-h-[85vh] overflow-auto">
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <div className="text-base font-semibold text-slate-100">Présence</div>
                <div className="text-sm muted">
                  {presenceSeance?.titre || 'Séance'} — {fmtDT(presenceSeance?.dateDebut)}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button className="btn" onClick={() => loadPresenceData(presenceSeance.id)} disabled={presenceLoading}>
                  {presenceLoading ? 'Chargement…' : 'Rafraîchir'}
                </button>
                <button className="btn" onClick={() => setShowPresenceModal(false)} disabled={presenceSaving}>
                  Fermer
                </button>
              </div>
            </div>

            {presenceErr && <div className="alert-error mb-3">{presenceErr}</div>}

            {presenceLoading ? (
              <div className="muted">Chargement…</div>
            ) : role === 'STAGIAIRE' ? (
              <div className="card p-4">
                <div className="text-sm muted">
                  Statut : <span className="text-slate-100 font-semibold">{String(myPresence?.statut || 'NON_MARQUE')}</span>
                </div>
                {myPresence?.remarque && <div className="text-sm muted mt-2">Remarque : {myPresence.remarque}</div>}
                <div className="text-xs muted mt-3">
                  La présence est validée par le formateur (ou l'admin).
                </div>
              </div>
            ) : (
              <>
                <div className="overflow-auto">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Stagiaire</th>
                        <th>Statut</th>
                        <th>Remarque</th>
                      </tr>
                    </thead>
                    <tbody>
                      {presences.map((p, idx) => (
                        <tr key={p?.stagiaire?.id || idx}>
                          <td>
                            <div className="font-medium">
                              {p?.stagiaire?.prenom} {p?.stagiaire?.nom}
                            </div>
                            <div className="text-xs muted">{p?.stagiaire?.email}</div>
                          </td>
                          <td>
                            <select
                              className="input"
                              value={p?.statut || 'NON_MARQUE'}
                              onChange={(e) => updatePresence(idx, { statut: e.target.value })}
                            >
                              <option value="NON_MARQUE">Non marqué</option>
                              <option value="PRESENT">Présent</option>
                              <option value="ABSENT">Absent</option>
                              <option value="RETARD">Retard</option>
                            </select>
                          </td>
                          <td>
                            <input
                              className="input"
                              value={p?.remarque || ''}
                              onChange={(e) => updatePresence(idx, { remarque: e.target.value })}
                              placeholder="Optionnel…"
                            />
                          </td>
                        </tr>
                      ))}
                      {presences.length === 0 && (
                        <tr><td colSpan={3} className="muted">Aucune donnée.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="flex items-center justify-end gap-2 mt-4">
                  <button className="btn" onClick={() => setShowPresenceModal(false)} disabled={presenceSaving}>
                    Annuler
                  </button>
                  <button className="btn btn-primary" onClick={savePresences} disabled={presenceSaving}>
                    {presenceSaving ? 'Enregistrement…' : 'Enregistrer'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Modal création formation (ADMIN) */}
      {showCreate && role === 'ADMIN' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => { setShowCreate(false); setErr('') }}
          />

          <div className="relative w-full max-w-3xl card p-5 max-h-[85vh] overflow-auto">
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <div className="text-base font-semibold text-slate-100">Créer une formation</div>
                <div className="text-sm muted">Assigner un formateur est obligatoire.</div>
              </div>
              <button className="btn" onClick={() => { setShowCreate(false); setErr('') }}>✕</button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="md:col-span-2">
                <label className="text-xs muted">Titre</label>
                <input className="fc-field mt-1" value={titre} onChange={e=>setTitre(e.target.value)} placeholder="Ex: Formation Spring Boot" />
              </div>

              <div className="md:col-span-2">
                <label className="text-xs muted">Description</label>
                <textarea className="fc-field mt-1 min-h-[90px]" value={description} onChange={e=>setDescription(e.target.value)} placeholder="Décrivez brièvement la formation..." />
              </div>

              <div>
                <label className="text-xs muted">Type</label>
                <select className="fc-field mt-1" value={type} onChange={e=>setType(e.target.value)}>
                  <option value="A_DISTANCE">À distance</option>
                  <option value="PRESENTIELLE">Présentielle</option>
                </select>
              </div>

              <div>
                <label className="text-xs muted">Date début</label>
                <input type="date" className="fc-field mt-1" value={dateDebut} onChange={e=>setDateDebut(e.target.value)} />
              </div>
              <div>
                <label className="text-xs muted">Date fin</label>
                <input type="date" className="fc-field mt-1" value={dateFin} onChange={e=>setDateFin(e.target.value)} />
              </div>

              <div>
                <label className="text-xs muted">Capacité</label>
                <input className="fc-field mt-1" value={String(capaciteMax)} onChange={e=>setCapaciteMax(e.target.value)} placeholder="30" />
              </div>

              <div>
                <label className="text-xs muted">Prix (DH)</label>
                <input className="fc-field mt-1" value={String(prix)} onChange={e=>setPrix(e.target.value)} placeholder="0" />
              </div>

              <div className="md:col-span-2">
                <label className="text-xs muted">Formateur (obligatoire)</label>
                <select className="fc-field mt-1" value={formateurId} onChange={e=>setFormateurId(e.target.value)}>
                  <option value="">Choisir un formateur</option>
                  {formateurs
                    .filter(x => String(x.role || '').toUpperCase() === 'FORMATEUR')
                    .map(fr => (
                      <option key={fr.id} value={String(fr.id)}>
                        {fr.prenom} {fr.nom} ({fr.email})
                      </option>
                    ))}
                </select>
                <div className="text-xs muted mt-1">Seuls les formateurs validés sont listés.</div>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 mt-5">
              <button
                className="btn"
                onClick={() => { setShowCreate(false); setErr(''); resetCreateForm() }}
                disabled={saving}
              >
                Annuler
              </button>
              <button
                className="btn btn-primary"
                onClick={createFormation}
                disabled={saving}
              >
                {saving ? 'Création…' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
