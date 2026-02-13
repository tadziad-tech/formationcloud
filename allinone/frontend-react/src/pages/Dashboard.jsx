import React from 'react'
import { getCurrentUser } from '../auth/auth'
import DashboardAdmin from './DashboardAdmin'
import DashboardFormateur from './DashboardFormateur'
import DashboardStagiaire from './DashboardStagiaire'

export default function Dashboard() {
  const u = getCurrentUser()
  const role = u?.role

  if (role === 'ADMIN') return <DashboardAdmin />
  if (role === 'FORMATEUR') return <DashboardFormateur />
  if (role === 'STAGIAIRE') return <DashboardStagiaire />

  return (
    <div className="card p-4 text-sm muted">
      Rôle inconnu. Veuillez vous reconnecter.
    </div>
  )
}
