import React from 'react'
import { Navigate } from 'react-router-dom'
import { isAuthed, getCurrentUser } from '../auth/auth'

/**
 * Route guard.
 * - If not authenticated => /login
 * - If roles is provided, user role must be included.
 *
 * Usage:
 *   <RequireAuth>...</RequireAuth>
 *   <RequireAuth roles={["ADMIN","FORMATEUR"]}>...</RequireAuth>
 */
export default function RequireAuth({ children, roles }) {
  if (!isAuthed()) return <Navigate to="/login" replace />

  const u = getCurrentUser()
  if (Array.isArray(roles) && roles.length > 0) {
    if (!u?.role || !roles.includes(u.role)) {
      // authenticated but not authorized: send to a safe home
      return <Navigate to="/dashboard" replace />
    }
  }

  return children
}
