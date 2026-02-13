import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'

function fmtDateTime(d) {
  if (!d) return '-'
  return d.toString().replace('T', ' ').slice(0, 19)
}

function normalizeLink(lien) {
  if (lien == null || lien === '') return null
  let l = String(lien).trim()
  if (!l) return null
  if (l.startsWith('http')) return l
  if (l.startsWith('/#')) l = l.substring(2)
  if (l.startsWith('#')) l = l.substring(1)
  if (!l.startsWith('/')) l = '/' + l
  return l
}

export default function Activites() {
  const u = getCurrentUser()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [items, setItems] = useState([])
  const [unreadOnly, setUnreadOnly] = useState(false)

  async function load() {
    setLoading(true)
    setErr('')
    try {
      const url = `/api/notifications/me${unreadOnly ? '?unreadOnly=true' : ''}`
      const res = await api.get(url)
      setItems(res.data || [])
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur notifications')
    } finally {
      setLoading(false)
    }
  }

  async function markAsRead(id) {
    try {
      await api.put(`/api/notifications/${id}/read`)
      await load()
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur')
    }
  }

  async function markAllRead() {
    try {
      await api.put('/api/notifications/read-all')
      await load()
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || 'Erreur')
    }
  }

  async function handleNotificationClick(notif) {
    if (!notif.lue) {
      await markAsRead(notif.id)
    }
    const raw = notif.lien || notif.link
    const target = normalizeLink(raw)
    if (!target) {
      navigate('/formations')
      return
    }
    if (target === '/' || target === '/#' || target === '/home') {
      navigate('/formations')
      return
    }
    if (target.startsWith('http')) {
      window.open(target, '_blank')
      return
    }
    navigate(target)
  }

  useEffect(() => {
    load()
  }, [unreadOnly])

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div>
          <div className="title">Notifications</div>
          <div className="text-sm muted">Mes notifications</div>
        </div>
        <div className="flex items-center gap-2">
          <label className="flex items-center gap-2 text-sm text-slate-300 cursor-pointer">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(e) => setUnreadOnly(e.target.checked)}
              className="rounded"
            />
            <span>Non lues uniquement</span>
          </label>
          <button className="btn" onClick={markAllRead}>Tout marquer lu</button>
        </div>
      </div>

      {err && <div className="alert-error">{err}</div>}
      {loading && <div className="alert-info">Chargement…</div>}

      {!loading && !err && (
        <>
          {items.length === 0 ? (
            <div className="card p-8 text-center">
              <div className="text-slate-400">Aucune notification</div>
            </div>
          ) : (
            <div className="space-y-2">
              {items.map(n => (
                <div
                  key={n.id}
                  onClick={() => handleNotificationClick(n)}
                  className={`card p-4 cursor-pointer transition hover:bg-white/5 ${
                    !n.lue ? 'border-l-4 border-l-indigo-500 bg-indigo-500/5' : ''
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-semibold text-slate-100">{n.titre || 'Notification'}</span>
                        {!n.lue && (
                          <span className="text-xs bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded">Non lue</span>
                        )}
                      </div>
                      <div className="text-sm text-slate-300 mb-2">{n.message || '-'}</div>
                      <div className="text-xs text-slate-500 font-mono">{fmtDateTime(n.dateCreation)}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
