import { api } from '../api/client'

/**
 * TP & Ressources – appels API
 */

export function getAllByFormation(formationId) {
  return api.get(`/api/formations/${formationId}/tp-ressources`).then((r) => r.data || [])
}

export function create(formationId, data) {
  return api
    .post(`/api/formations/${formationId}/tp-ressources`, {
      titre: data.titre,
      description: data.description || null,
      type: data.type || 'TP',
      fichierUrl: data.fichierUrl || null,
      dateLimite: data.dateLimite || null,
    })
    .then((r) => r.data)
}

export function getOne(tpId) {
  return api.get(`/api/tp-ressources/${tpId}`).then((r) => r.data)
}

export function update(tpId, data) {
  return api
    .put(`/api/tp-ressources/${tpId}`, {
      titre: data.titre,
      description: data.description || null,
      type: data.type || 'TP',
      fichierUrl: data.fichierUrl || null,
      dateLimite: data.dateLimite || null,
    })
    .then((r) => r.data)
}

export function remove(tpId) {
  return api.delete(`/api/tp-ressources/${tpId}`)
}

/** Admin/Formateur : upload fichier ressource TP (multipart, key "file") */
export function uploadTpFile(tpId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/api/tp-ressources/${tpId}/fichier`, formData).then((r) => r.data)
}

/** Télécharger le fichier ressource TP (blob + déclenchement download) */
export async function downloadTpFile(tpId) {
  const res = await api.get(`/api/tp-ressources/${tpId}/fichier`, { responseType: 'blob' })
  const blob = res.data
  const disposition = res.headers?.['content-disposition']
  let fileName = 'document'
  if (disposition && /filename[*]?=(?:UTF-8'')?["']?([^"'\s;]+)["']?/i.test(disposition)) {
    fileName = disposition.match(/filename[*]?=(?:UTF-8'')?["']?([^"'\s;]+)["']?/i)[1].trim()
  }
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  URL.revokeObjectURL(url)
}

/** Stagiaire : soumission par upload (multipart "file" + "commentaire" optionnel) */
export function submitSoumissionUpload(tpId, file, commentaire) {
  const formData = new FormData()
  formData.append('file', file)
  if (commentaire != null && String(commentaire).trim() !== '') {
    formData.append('commentaire', String(commentaire).trim())
  }
  return api.post(`/api/tp-ressources/${tpId}/soumissions/upload`, formData).then((r) => r.data)
}

/** Télécharger le fichier d'une soumission (blob + download) */
export async function downloadSoumissionFile(soumissionId) {
  const res = await api.get(`/api/tp-soumissions/${soumissionId}/fichier`, { responseType: 'blob' })
  const blob = res.data
  const disposition = res.headers?.['content-disposition']
  let fileName = 'rendu'
  if (disposition && /filename[*]?=(?:UTF-8'')?["']?([^"'\s;]+)["']?/i.test(disposition)) {
    fileName = disposition.match(/filename[*]?=(?:UTF-8'')?["']?([^"'\s;]+)["']?/i)[1].trim()
  }
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  URL.revokeObjectURL(url)
}

/** Stagiaire : déposer un rendu (lien du fichier) */
export function soumettreTp(tpId, data) {
  return api
    .post(`/api/tp-ressources/${tpId}/soumissions`, {
      fichierSoumisUrl: data.fichierSoumisUrl,
    })
    .then((r) => r.data)
}

/** Formateur : corriger une soumission (note + feedback) */
export function corrigerTp(soumissionId, payload) {
  return api
    .put(`/api/tp-soumissions/${soumissionId}/corriger`, {
      statut: payload.statut || 'CORRIGE',
      note: payload.note != null ? Number(payload.note) : null,
      commentaire: payload.commentaire || null,
    })
    .then((r) => r.data)
}

/** Formateur : liste des soumissions pour un TP */
export function getSoumissions(tpId) {
  return api.get(`/api/tp-ressources/${tpId}/soumissions`).then((r) => r.data || [])
}

/** Stagiaire : mes soumissions */
export function getSoumissionsByStagiaire(stagiaireId) {
  return api.get(`/api/stagiaires/${stagiaireId}/tp-soumissions`).then((r) => r.data || [])
}

export function getSoumission(soumissionId) {
  return api.get(`/api/tp-soumissions/${soumissionId}`).then((r) => r.data)
}
