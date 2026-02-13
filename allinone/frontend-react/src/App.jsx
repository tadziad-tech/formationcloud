import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import RequireAuth from './components/RequireAuth'
import Shell from './components/Shell'
import HomePublic from './pages/HomePublic'
import Login from './pages/Login'
import VerifyCertificat from './pages/VerifyCertificat'
import Formations from './pages/Formations'
import Users from './pages/Users'
import Certificats from './pages/Certificats'
import Evaluations from './pages/Evaluations'
import Activites from './pages/Activites'
import Dashboard from './pages/Dashboard'
import Profil from './pages/Profil'
import TpSoumissionRedirect from './pages/TpSoumissionRedirect'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePublic />} />
      <Route path="/login" element={<Login />} />
      <Route path="/verify/:code" element={<VerifyCertificat />} />

      {/* Protected space */}
      <Route
        element={
          <RequireAuth>
            <Shell />
          </RequireAuth>
        }
      >
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="a-faire" element={<Navigate to="/dashboard" replace />} />
        <Route path="profil" element={<Profil />} />
        <Route path="formations" element={<Formations />} />
        <Route path="formations/:id" element={<Formations />} />
        <Route path="formations/:id/tp" element={<Formations />} />
        <Route path="users" element={<Users />} />
        <Route path="certificats" element={<Certificats />} />
        <Route path="certificats/:id" element={<Certificats />} />
        <Route path="evaluations" element={<Evaluations />} />
        <Route path="evaluations/:id" element={<Evaluations />} />
        <Route path="tp-soumissions/:soumissionId" element={<TpSoumissionRedirect />} />
        <Route path="activites" element={<Activites />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
