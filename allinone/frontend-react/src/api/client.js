import axios from 'axios'

function getToken() {
  return sessionStorage.getItem('authToken') || ''
}

export const api = axios.create({
  baseURL: '', // use relative URLs; in dev proxy handles /api
})

api.interceptors.request.use((config) => {
  const t = getToken()
  if (t) config.headers.Authorization = `Bearer ${t}`
  return config
})
