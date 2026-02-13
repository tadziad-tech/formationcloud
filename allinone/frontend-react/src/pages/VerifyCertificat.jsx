import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api/client'

export default function VerifyCertificat() {
  const { code } = useParams()
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [data, setData] = useState(null)

  useEffect(() => {
    let ok = true
    ;(async () => {
      try {
        setLoading(true)
        setErr('')
        const res = await api.get(`/api/certificats/verify/${encodeURIComponent(code)}`)
        if (!ok) return
        setData(res.data)
      } catch (e) {
        if (!ok) return
        setErr(e?.response?.data?.message || e?.message || 'Certificat introuvable')
      } finally {
        if (ok) setLoading(false)
      }
    })()
    return () => { ok = false }
  }, [code])

  return (
    <div className="max-w-3xl mx-auto space-y-4">
      <div className="card p-6">
        <div className="title">Vérification du certificat</div>
        <div className="text-sm muted">Code : <span className="font-mono">{code}</span></div>
      </div>

      {loading && <div className="alert-info">Vérification…</div>}
      {err && <div className="alert-error">{err}</div>}

      {!loading && !err && data && (
        <div className="card p-6 space-y-3">
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div className="font-mono text-sm">{data.numeroCertificat}</div>
            <span className={`badge ${data.statut === 'REVOQUE' ? 'badge-bad' : 'badge-ok'}`}>
              {data.statut || 'VALIDE'}
            </span>
          </div>

          <div className="grid md:grid-cols-2 gap-3">
            <div className="p-3 rounded-xl border border-white/10 bg-white/5">
              <div className="text-xs muted">Titulaire</div>
              <div className="font-semibold">{data.nomComplet}</div>
            </div>
            <div className="p-3 rounded-xl border border-white/10 bg-white/5">
              <div className="text-xs muted">Formation</div>
              <div className="font-semibold">{data.formation}</div>
            </div>
            <div className="p-3 rounded-xl border border-white/10 bg-white/5">
              <div className="text-xs muted">Formateur</div>
              <div className="font-semibold">{data.formateur}</div>
            </div>
            <div className="p-3 rounded-xl border border-white/10 bg-white/5">
              <div className="text-xs muted">Date d’obtention</div>
              <div className="font-semibold">{data.dateObtention || '-'}</div>
            </div>
          </div>

          <div className="p-3 rounded-xl border border-white/10 bg-white/5">
            <div className="text-xs muted">Note</div>
            <div className="font-semibold">{data.noteObtenue ?? '-'}</div>
          </div>

          {data.statut === 'REVOQUE' && (
            <div className="alert-error">Ce certificat a été révoqué.</div>
          )}
        </div>
      )}
    </div>
  )
}
