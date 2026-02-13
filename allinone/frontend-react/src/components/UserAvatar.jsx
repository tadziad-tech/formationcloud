import React, { useEffect, useState } from 'react'
import { resolveMediaUrl, withCacheBuster } from '../utils/media'

function initials(nom = '', prenom = '') {
  const a = (prenom || '').trim()[0] || ''
  const b = (nom || '').trim()[0] || ''
  return (a + b).toUpperCase() || '?'
}

export default function UserAvatar({ photo, nom, prenom, size = 36, cacheKey }) {
  const [broken, setBroken] = useState(false)
  const url = photo ? withCacheBuster(resolveMediaUrl(photo), cacheKey || photo) : null

  useEffect(() => {
    setBroken(false)
  }, [photo])

  if (!url || broken) {
    return (
      <div
        className="rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-xs"
        style={{ width: size, height: size }}
      >
        {initials(nom, prenom)}
      </div>
    )
  }

  return (
    <img
      src={url}
      alt=""
      style={{ width: size, height: size }}
      className="rounded-full object-cover border border-white/10"
      onError={() => setBroken(true)}
    />
  )
}
