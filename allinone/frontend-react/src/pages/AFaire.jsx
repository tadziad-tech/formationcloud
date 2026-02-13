import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'

const NOTIFICATIONS_ROUTE = '/activites'

function fmtDateTime(d) {
  if (!d) return '-'
  return d.toString().replace('T', ' ').slice(0, 16)
}

function fmtDateTimeLong(d) {
  if (!d) return '-'
  return d.toString().replace('T', ' ').slice(0, 19)
}

/**
 * Normalise un lien: pas de fallback, retourne null si vide.
 * - http => retourné tel quel (ouvert en nouvel onglet par openLink)
 * - /# ou # => enlevé
 * - ne commence pas par / => préfixe /
 */
function normalizeLink(link) {
  if (link == null || (typeof link === 'string' && link.trim() === '')) return null
  let l = String(link).trim()
  if (!l) return null
  if (l.startsWith('http')) return l
  if (l.startsWith('/#')) l = l.substring(2)
  if (l.startsWith('#')) l = l.substring(1)
  if (!l.startsWith('/')) l = '/' + l
  return l
}

/**
 * Ouverture d’un lien: fallback unique /formations si target null.
 * Interdit: window.location.href, <a href>, reload.
 */
function openLink(link, navigate) {
  const target = normalizeLink(link)
  if (target == null) {
    navigate('/formations')
    return
  }
  if (target.startsWith('http')) {
    window.open(target, '_blank')
    return
  }
  navigate(target)
}

function SeverityBadge({ severity }) {
  const s = (severity || 'INFO').toUpperCase()
  const classes = {
    INFO: 'bg-slate-500/20 text-slate-300',
    WARN: 'bg-amber-500/20 text-amber-300',
    URGENT: 'bg-red-500/20 text-red-300',
  }
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded ${classes[s] || classes.INFO}`}>
      {s === 'URGENT' ? 'Urgent' : s === 'WARN' ? 'Attention' : 'Info'}
    </span>
  )
}

const cardClass = 'rounded-2xl bg-white/5 border border-white/10 shadow-lg shadow-black/10 transition hover:border-indigo-400/30'

export default function AFaire() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [data, setData] = useState({ upcomingSeances: [], todo: [], overdue: [] })
  const [unreadCount, setUnreadCount] = useState(0)
  const [recentNotifications, setRecentNotifications] = useState([])

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setErr('')
      try {
        const [todoRes, unreadRes, notifRes] = await Promise.all([
          api.get('/api/todo/me'),
          api.get('/api/notifications/unread-count'),
          api.get('/api/notifications/me?unreadOnly=false'),
        ])
        if (cancelled) return
        setData(todoRes.data || { upcomingSeances: [], todo: [], overdue: [] })
        const count = typeof unreadRes.data === 'number' ? unreadRes.data : (unreadRes.data?.count ?? 0)
        setUnreadCount(count)
        const list = Array.isArray(notifRes.data) ? notifRes.data : []
        setRecentNotifications(list.slice(0, 5))
      } catch (e) {
        if (!cancelled) setErr(e?.response?.data?.message || e?.message || 'Erreur chargement')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  const upcoming = data.upcomingSeances || []
  const todo = data.todo || []
  const overdue = data.overdue || []

  const handleOpen = (link) => (e) => {
    e?.stopPropagation?.()
    openLink(link, navigate)
  }

  async function handleNotificationClick(notif) {
    if (!notif.lue) {
      try {
        await api.put(`/api/notifications/${notif.id}/read`)
      } catch (_) {}
    }
    openLink(notif.lien || notif.link, navigate)
  }

  const seanceLink = (s) => {
    if (s.link) return s.link
    return `/formations?formationId=${s.formationId}&tab=seances&seanceId=${s.seanceId}`
  }

  if (loading) {
    return (
      <div className="space-y-4">
        <div>
          <div className="title">Tableau de bord</div>
          <div className="text-sm text-slate-400">Vos prochaines séances</div>
        </div>
        <div className="alert-info rounded-2xl">Chargement…</div>
      </div>
    )
  }

  if (err) {
    return (
      <div className="space-y-4">
        <div className="title">Tableau de bord</div>
        <div className="alert-error rounded-2xl">{err}</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <header>
        <h1 className="text-xl font-semibold text-slate-100">Tableau de bord</h1>
        <p className="text-sm text-slate-400 mt-0.5">Vos prochaines séances</p>
      </header>

      {/* KPI row — tout cliquable */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <button
          type="button"
          onClick={() => openLink(NOTIFICATIONS_ROUTE, navigate)}
          className={`${cardClass} p-5 text-left cursor-pointer`}
        >
          <div className="text-3xl font-bold text-slate-100">{unreadCount}</div>
          <div className="text-sm text-slate-400 mt-1">Notifications non lues</div>
        </button>
        <button
          type="button"
          onClick={() => {
            if (todo.length > 0 && todo[0].link) openLink(todo[0].link, navigate)
            else openLink('/a-faire', navigate)
          }}
          className={`${cardClass} p-5 text-left cursor-pointer`}
        >
          <div className="text-3xl font-bold text-slate-100">{todo.length}</div>
          <div className="text-sm text-slate-400 mt-1">À faire</div>
        </button>
        <button
          type="button"
          onClick={() => {
            if (overdue.length > 0 && overdue[0].link) openLink(overdue[0].link, navigate)
            else openLink('/a-faire#overdue', navigate)
          }}
          className={`${cardClass} p-5 text-left cursor-pointer`}
        >
          <div className="text-3xl font-bold text-slate-100">{overdue.length}</div>
          <div className="text-sm text-slate-400 mt-1">En retard</div>
        </button>
      </div>

      {/* Content grid: notifications | todo | overdue */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Dernières notifications */}
        <section className={`${cardClass} p-4`}>
          <div className="flex items-center justify-between gap-2 mb-3">
            <h2 className="text-sm font-semibold text-slate-300">Dernières notifications</h2>
            <button type="button" className="btn text-sm" onClick={() => openLink(NOTIFICATIONS_ROUTE, navigate)}>
              Voir tout
            </button>
          </div>
          {recentNotifications.length === 0 ? (
            <div className="py-6 text-center text-slate-500 text-sm">
              <p>Aucune notification</p>
              <button type="button" className="btn mt-3" onClick={() => openLink('/formations', navigate)}>
                Voir formations
              </button>
            </div>
          ) : (
            <ul className="space-y-2">
              {recentNotifications.map((n) => (
                <li
                  key={n.id}
                  onClick={() => handleNotificationClick(n)}
                  className={`p-3 rounded-xl cursor-pointer transition hover:bg-white/5 ${!n.lue ? 'border-l-4 border-l-indigo-500 bg-indigo-500/5' : ''}`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-slate-100 text-sm">{n.titre || 'Notification'}</span>
                    {!n.lue && (
                      <span className="text-xs bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded">Non lue</span>
                    )}
                  </div>
                  <span className="text-xs text-slate-500 mt-1 block">{fmtDateTimeLong(n.dateCreation)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* À faire */}
        <section className={`${cardClass} p-4`}>
          <h2 className="text-sm font-semibold text-slate-300 mb-3">À faire</h2>
          {todo.length === 0 ? (
            <div className="py-6 text-center text-slate-500 text-sm">
              <p>Rien à faire</p>
              <button type="button" className="btn mt-3" onClick={() => openLink('/formations', navigate)}>
                Voir formations
              </button>
            </div>
          ) : (
            <ul className="space-y-2">
              {todo.map((item, i) => (
                <li
                  key={i}
                  onClick={() => openLink(item.link, navigate)}
                  className="p-3 rounded-xl cursor-pointer transition hover:bg-white/5 flex items-center justify-between gap-2 flex-wrap"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium text-slate-100 text-sm">{item.title}</span>
                      <SeverityBadge severity={item.severity} />
                    </div>
                    <div className="text-xs text-slate-400 mt-0.5">{item.message}</div>
                  </div>
                  <button type="button" className="btn shrink-0 text-sm" onClick={handleOpen(item.link)}>
                    Ouvrir
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* En retard */}
        <section className={`${cardClass} p-4`}>
          <h2 className="text-sm font-semibold text-slate-300 mb-3">En retard</h2>
          {overdue.length === 0 ? (
            <div className="py-6 text-center text-slate-500 text-sm">
              <p>Aucun retard</p>
              <button type="button" className="btn mt-3" onClick={() => openLink('/formations', navigate)}>
                Voir formations
              </button>
            </div>
          ) : (
            <ul className="space-y-2">
              {overdue.map((item, i) => (
                <li
                  key={i}
                  onClick={() => openLink(item.link, navigate)}
                  className="p-3 rounded-xl cursor-pointer transition hover:bg-white/5 border-l-4 border-l-red-500/50 flex items-center justify-between gap-2 flex-wrap"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium text-slate-100 text-sm">{item.title}</span>
                      <SeverityBadge severity={item.severity} />
                    </div>
                    <div className="text-xs text-slate-400 mt-0.5">{item.message}</div>
                  </div>
                  <button type="button" className="btn shrink-0 text-sm" onClick={handleOpen(item.link)}>
                    Ouvrir
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {/* Prochaines séances — full width */}
      <section className={`${cardClass} p-4`}>
        <h2 className="text-sm font-semibold text-slate-300 mb-3">Prochaines séances</h2>
        {upcoming.length === 0 ? (
          <div className="py-6 text-center text-slate-500 text-sm">
            <p>Aucune séance prévue</p>
            <button type="button" className="btn mt-3" onClick={() => openLink('/formations', navigate)}>
              Voir formations
            </button>
          </div>
        ) : (
          <ul className="space-y-2">
            {upcoming.map((s) => (
              <li
                key={s.seanceId}
                onClick={() => openLink(seanceLink(s), navigate)}
                className="p-4 rounded-xl cursor-pointer transition hover:bg-white/5 flex items-center justify-between gap-3 flex-wrap"
              >
                <div className="min-w-0 flex-1">
                  <div className="font-medium text-slate-100">{s.titre}</div>
                  <div className="text-sm text-slate-400">{s.formationTitre}</div>
                  <div className="text-xs text-slate-500 mt-1">
                    {fmtDateTime(s.dateDebut)} → {fmtDateTime(s.dateFin)}
                    {s.mode && ` · ${s.mode}`}
                    {s.lieu && ` · ${s.lieu}`}
                  </div>
                </div>
                <button type="button" className="btn shrink-0" onClick={handleOpen(seanceLink(s))}>
                  Ouvrir
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
