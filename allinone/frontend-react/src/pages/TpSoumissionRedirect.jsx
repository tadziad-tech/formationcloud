import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api/client'

export default function TpSoumissionRedirect() {
  const { soumissionId } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!soumissionId) {
      setLoading(false)
      setError('ID soumission manquant')
      return
    }

    let cancelled = false

    async function redirect() {
      try {
        const soumissionRes = await api.get(`/api/tp-soumissions/${soumissionId}`)
        const tpId = soumissionRes.data?.tpId
        if (!tpId) {
          if (!cancelled) setError('Soumission ou TP introuvable')
          return
        }
        const tpRes = await api.get(`/api/tp-ressources/${tpId}`)
        const formationId = tpRes.data?.formationId
        if (!formationId) {
          if (!cancelled) setError('Formation introuvable')
          return
        }
        if (!cancelled) {
          navigate(`/formations/${formationId}/tp`, { replace: true })
        }
      } catch (e) {
        if (!cancelled) {
          setError(e?.response?.data?.message || e?.message || 'Erreur de chargement')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    redirect()
    return () => { cancelled = true }
  }, [soumissionId, navigate])

  if (loading) {
    return (
      <div className="p-6 text-center text-slate-300">
        Redirection…
      </div>
    )
  }

  if (error) {
    return (
      <div className="p-6 space-y-3 max-w-md mx-auto">
        <div className="alert-error">{error}</div>
        <button
          type="button"
          className="btn w-full"
          onClick={() => navigate('/activites')}
        >
          Retour aux notifications
        </button>
      </div>
    )
  }

  return null
}
