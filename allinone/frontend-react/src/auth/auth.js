import { api } from '../api/client'

export function isAuthed() {
  return !!sessionStorage.getItem('authToken')
}

export function getCurrentUser() {
  try { return JSON.parse(sessionStorage.getItem('currentUser') || 'null') } catch { return null }
}

export async function login(email, password) {
  try {
    const res = await api.post('/api/auth/login', { email, motDePasse: password })
    // Backend returns a flat object: { token, id, email, nom, prenom, role, statutValidation }
    const token = res.data?.token
    const user = {
      id: res.data?.id,
      email: res.data?.email,
      nom: res.data?.nom,
      prenom: res.data?.prenom,
      role: res.data?.role,
      statutValidation: res.data?.statutValidation,
    }
    if (!token || !user?.role) throw new Error('Réponse login invalide')
    sessionStorage.setItem('authToken', token)
    sessionStorage.setItem('currentUser', JSON.stringify(user))
    return user
  } catch (e) {
    const status = e?.response?.status
    if (status === 401 || status === 403) {
      const msg = e?.response?.data?.message
      if (typeof msg === 'string' && msg.trim() && /attente|désactivé/i.test(msg)) {
        throw new Error(msg)
      }
      throw new Error('Identifiants invalides')
    }
    if (!status) throw new Error('Serveur indisponible')
    if (status >= 500) throw new Error('Erreur serveur')
    throw new Error(e?.response?.data?.message || 'Connexion impossible')
  }
}

export async function requestAccess(payload) {
  // payload: { nom, prenom, email, motDePasse, role, typeFormateur?, telephone?, adresse? }
  try {
    const res = await api.post('/api/auth/register', payload)
    return res.data
  } catch (e) {
    const status = e?.response?.status
    if (!status) throw new Error('Serveur indisponible')
    const msg = e?.response?.data?.message
    if (typeof msg === 'string' && msg.trim()) throw new Error(msg)
    // Bean validation may return a map
    const data = e?.response?.data
    if (data && typeof data === 'object') {
      const first = Object.values(data)[0]
      if (typeof first === 'string') throw new Error(first)
    }
    throw new Error('Inscription impossible')
  }
}

export function logout() {
  sessionStorage.removeItem('authToken')
  sessionStorage.removeItem('currentUser')
}
