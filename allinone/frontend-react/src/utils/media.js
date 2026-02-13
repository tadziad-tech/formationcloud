// Helpers for serving media (profile photos) reliably.

export function resolveMediaUrl(path) {
  if (!path) return null
  const s = String(path)
  if (s.startsWith('http://') || s.startsWith('https://')) return s
  if (s.startsWith('//')) return `${window.location.protocol}${s}`
  if (s.startsWith('/')) return s
  return `/${s}`
}

export function withCacheBuster(url, v) {
  if (!url) return null
  // Les URLs locales (blob:, data:) ne supportent pas toujours les query params.
  if (url.startsWith('blob:') || url.startsWith('data:')) return url
  const sep = url.includes('?') ? '&' : '?'
  const val = v ?? Date.now()
  return `${url}${sep}v=${encodeURIComponent(val)}`
}
