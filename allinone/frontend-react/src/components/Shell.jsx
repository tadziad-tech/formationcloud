import React, { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { getCurrentUser, logout } from '../auth/auth'
import { api } from '../api/client'

function navForRole(role) {
  // Tous les rôles peuvent consulter la liste des utilisateurs.
  // Les actions (modifier/supprimer/changer rôle) sont gérées dans la page selon le rôle.
  return [
    { to: '/dashboard', label: 'Tableau de bord' },
    { to: '/profil', label: 'Profil' },
    { to: '/formations', label: 'Formations' },
    { to: '/users', label: 'Utilisateurs' },
    { to: '/certificats', label: 'Certificats' },
    { to: '/evaluations', label: 'Évaluations' },
    { to: '/activites', label: 'Notifications', isNotifications: true },
  ]
}

function roleLabel(role) {
  if (role === 'ADMIN') return 'Admin'
  if (role === 'FORMATEUR') return 'Formateur'
  if (role === 'STAGIAIRE') return 'Stagiaire'
  return role || 'Utilisateur'
}

export default function Shell() {
  const u = getCurrentUser()
  const navigate = useNavigate()
  const nav = navForRole(u?.role)
  const [unreadCount, setUnreadCount] = useState(0)

  // Polling pour le badge de notifications non lues
  useEffect(() => {
    if (!u?.id) return

    async function fetchUnreadCount() {
      try {
        const res = await api.get('/api/notifications/unread-count')
        setUnreadCount(res.data || 0)
      } catch (e) {
        // Ignore errors silently
      }
    }

    fetchUnreadCount()
    const interval = setInterval(fetchUnreadCount, 30000) // Poll toutes les 30s

    return () => clearInterval(interval)
  }, [u?.id])

  return (
    <div className="h-full flex">
      {/* Sidebar */}
      <aside className="w-72 lg:w-64 shrink-0 p-3">
        <div className="card h-full flex flex-col overflow-hidden">
          <div className="p-4 border-b border-white/10">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-2xl bg-gradient-to-br from-indigo-500 via-fuchsia-500 to-emerald-500 text-white grid place-items-center shadow-lg shadow-black/30 ring-1 ring-white/10">
                <span className="text-sm font-bold">FC</span>
              </div>
              <div className="leading-tight">
                <div className="text-sm font-semibold text-slate-100">FormationCloud</div>
                <div className="text-xs text-slate-400">{u?.prenom} {u?.nom} · {roleLabel(u?.role)}</div>
              </div>
            </div>
          </div>

          <nav className="p-2 space-y-1 flex-1 overflow-auto">
            {nav.map((i) => (
              <NavLink
                key={i.to}
                to={i.to}
                className={({ isActive }) =>
                  `block rounded-xl px-3 py-2 text-sm transition relative ${isActive ? 'bg-indigo-500/20 text-white border border-indigo-400/20' : 'text-slate-200 hover:bg-white/10 border border-transparent'}`
                }
              >
                {i.label}
                {i.isNotifications && unreadCount > 0 && (
                  <span className="absolute top-1 right-1 bg-red-500 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                )}
              </NavLink>
            ))}
          </nav>

          <div className="p-3 border-t border-white/10">
            <button
              className="btn w-full"
              onClick={() => { logout(); navigate('/login') }}
            >
              Se déconnecter
            </button>
          </div>
        </div>
      </aside>

      {/* Content */}
      <main className="flex-1 min-w-0 p-3 pl-0">
        <div className="card h-full flex flex-col overflow-hidden">
          <header className="p-4 border-b border-white/10 bg-white/5 backdrop-blur-xl">
            <div className="text-sm text-slate-300">
              Espace <span className="text-slate-100 font-semibold">{roleLabel(u?.role)}</span>
            </div>
          </header>

          <div className="p-4 flex-1 overflow-auto">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  )
}
