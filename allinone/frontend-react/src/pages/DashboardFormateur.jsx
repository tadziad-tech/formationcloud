import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

function fmtStatut(s) {
  if (!s) return ''
  return s.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())
}

function fmtDate(iso) {
  try {
    if (!iso) return '-'
    const d = new Date(iso)
    return d.toLocaleString()
  } catch {
    return iso
  }
}

export default function DashboardFormateur() {
  const navigate = useNavigate()
  const u = getCurrentUser()
  const formateurId = u?.id

  const [days, setDays] = useState(14)
  const [todoItems, setTodoItems] = useState([])
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  const load = async () => {
    try {
      setLoading(true)
      setErr('')
      const res = await api.get(`/api/dashboard/formateur/${formateurId}/overview`, {
        params: { days },
      })
      setData(res.data)
      try {
        const todoRes = await api.get('/api/todo/me')
        const d = todoRes.data || {}
        const items = [...(d.todo || []), ...(d.overdue || [])].slice(0, 5)
        setTodoItems(items)
      } catch {
        setTodoItems([])
      }
    } catch (e) {
      setErr(e?.response?.data?.message || 'Erreur de chargement du dashboard')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!formateurId) return
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formateurId, days])

  const pending = data?.inscriptionsEnAttente || []
  const k = data?.totals || {}

  const series = useMemo(() => {
    const s = data?.inscriptionsParJour || []
    return s.map((p) => ({
      date: p.date,
      count: p.count,
    }))
  }, [data])

  const formations = (data?.formations || []).slice(0, 6)
  const evals = (data?.evaluationsAVenir || []).slice(0, 6)

  const validateInscription = async (id) => {
    await api.put(`/api/inscriptions/${id}/valider`)
    await load()
  }

  const refuseInscription = async (id) => {
    const motif = window.prompt('Motif du refus (optionnel) :') || ''
    await api.put(`/api/inscriptions/${id}/refuser`, null, { params: { motif } })
    await load()
  }

  if (loading) {
    return (
      <div className="card p-6">
        <div className="title">Dashboard Formateur</div>
        <div className="muted mt-2">Chargement…</div>
      </div>
    )
  }

  if (err) {
    return (
      <div className="card p-6">
        <div className="title">Dashboard Formateur</div>
        <div className="alert-error mt-3">{err}</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-3">
        <div>
          <div className="title">Espace Formateur</div>
          <div className="muted text-sm">Validation & suivi opérationnel + un peu de stats (BD réelle)</div>
        </div>

        <div className="card p-2 flex items-center gap-2">
          <span className="muted text-xs px-2">Période</span>
          <div className="segmented">
            {[0, 7, 14, 30].map((d) => (
              <button
                key={d}
                className={`segmented-btn ${days === d ? 'segmented-btn-active' : ''}`}
                onClick={() => setDays(d)}
              >
                {d === 0 ? 'Tout' : `${d}j`}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* À faire maintenant — top 5 */}
      {todoItems.length > 0 && (
        <div className="card p-4">
          <div className="font-semibold mb-3">À faire maintenant</div>
          <div className="space-y-2">
            {todoItems.map((item, i) => (
              <div
                key={i}
                className="flex items-center justify-between gap-3 p-3 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 transition cursor-pointer"
                onClick={() => item.link && navigate(item.link)}
              >
                <div className="min-w-0 flex-1">
                  <div className="font-medium text-slate-100">{item.title}</div>
                  <div className="muted text-xs">{item.message}</div>
                </div>
                {item.link && (
                  <button type="button" className="btn shrink-0" onClick={(e) => { e.stopPropagation(); navigate(item.link) }}>
                    Ouvrir
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="kpi">
          <div>
            <h3>Formations</h3>
            <div className="v">{k.totalFormations ?? 0}</div>
          </div>
          <div className="badge">Actives: {k.formationsActives ?? 0}</div>
        </div>
        <div className="kpi">
          <div>
            <h3>Inscriptions</h3>
            <div className="v">{k.totalInscriptions ?? 0}</div>
          </div>
          <div className="badge">En attente: {k.inscriptionsEnAttente ?? 0}</div>
        </div>
        <div className="kpi">
          <div>
            <h3>Évaluations</h3>
            <div className="v">{k.totalEvaluations ?? 0}</div>
          </div>
          <div className="badge">À venir: {k.evaluationsAVenir ?? 0}</div>
        </div>
        <div className="kpi">
          <div>
            <h3>Certificats</h3>
            <div className="v">{k.certificatsDelivres ?? 0}</div>
          </div>
          <div className="badge">Délivrés</div>
        </div>
        <div className="kpi">
          <div>
            <h3>À traiter</h3>
            <div className="v">{pending.length}</div>
          </div>
          <div className="badge">Demandes</div>
        </div>
      </div>

      {/* OPERATIONNEL */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="card p-4 lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <div className="font-semibold">Demandes d'inscription à valider</div>
              <div className="muted text-sm">Traitez rapidement les demandes en attente</div>
            </div>
            <div className="badge">{pending.length} en attente</div>
          </div>

          {pending.length === 0 ? (
            <div className="alert-info mt-4">Aucune inscription en attente 🎉</div>
          ) : (
            <div className="mt-4 overflow-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>Stagiaire</th>
                    <th>Formation</th>
                    <th>Date</th>
                    <th>Statut</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {pending.slice(0, 10).map((i) => (
                    <tr
                      key={i.id}
                      className="cursor-pointer hover:bg-white/5"
                      onClick={() => i?.formation?.id && navigate(`/formations?formationId=${i.formation.id}&focus=pending`)}
                    >
                      <td>
                        <div className="font-medium">
                          {i.stagiaire?.prenom} {i.stagiaire?.nom}
                        </div>
                        <div className="muted text-xs">{i.stagiaire?.email}</div>
                      </td>
                      <td>
                        <div className="font-medium">{i.formation?.nom || i.formation?.titre}</div>
                        <div className="muted text-xs">{i.formation?.categorie || 'Sans catégorie'}</div>
                      </td>
                      <td className="text-xs">{fmtDate(i.dateInscription)}</td>
                      <td>
                        <span className="badge">{fmtStatut(i.statut)}</span>
                      </td>
                      <td className="text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-2">
                          <button className="btn btn-primary" onClick={() => validateInscription(i.id)}>
                            Valider
                          </button>
                          <button className="btn btn-danger" onClick={() => refuseInscription(i.id)}>
                            Refuser
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card p-4">
          <div className="font-semibold">Statistiques (Inscriptions)</div>
          <div className="muted text-sm">{days} derniers jours</div>
          <div className="h-56 mt-3">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={series} margin={{ left: -8, right: 8 }}>
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} />
                <Tooltip
                  contentStyle={{ background: 'rgba(15,23,42,0.92)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 12 }}
                />
                <Area type="monotone" dataKey="count" strokeWidth={2} fillOpacity={0.2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* ACTIVITY */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="card p-4">
          <div className="font-semibold">Prochaines évaluations</div>
          <div className="muted text-sm">Préparez les sessions à venir</div>
          <div className="mt-3 space-y-2">
            {evals.length === 0 ? (
              <div className="alert-info">Aucune évaluation planifiée.</div>
            ) : (
              evals.map((e) => (
                <div
                  key={e.id}
                  className="card p-3 cursor-pointer hover:bg-white/5 transition"
                  onClick={() => e?.formation?.id && navigate(`/formations?formationId=${e.formation.id}`)}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="font-medium">{e.titre}</div>
                      <div className="muted text-xs">{e.formation?.nom || e.formation?.titre}</div>
                    </div>
                    <span className="badge">{e.dateEvaluation}</span>
                  </div>
                  <div className="muted text-xs mt-2">
                    {e.dureeMinutes ? `${e.dureeMinutes} min` : 'Durée non définie'} • Seuil{' '}
                    {typeof e.seuilReussite === 'number' ? `${e.seuilReussite}%` : '—'}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card p-4 lg:col-span-2">
          <div className="font-semibold">Vos formations</div>
          <div className="muted text-sm">Aperçu des formations récentes / actives</div>

          {formations.length === 0 ? (
            <div className="alert-info mt-4">Aucune formation.</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4">
              {formations.map((f) => (
                <div
                  key={f.id}
                  className="card p-4 cursor-pointer hover:bg-white/5 transition"
                  onClick={() => f?.id && navigate(`/formations?formationId=${f.id}`)}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="font-semibold">{f.nom || f.titre}</div>
                      <div className="muted text-xs">{f.categorie || 'Sans catégorie'}</div>
                    </div>
                    <span className="badge">{fmtStatut(f.statut)}</span>
                  </div>

                  <div className="mt-3 flex items-center justify-between text-sm">
                    <div className="muted">Inscrits</div>
                    <div className="font-semibold">{f.inscrits ?? 0}</div>
                  </div>
                  <div className="mt-2 flex items-center justify-between text-sm">
                    <div className="muted">Capacité</div>
                    <div className="font-semibold">{f.capaciteMax ?? '—'}</div>
                  </div>

                  <div className="mt-3 h-2 rounded-full bg-white/5 overflow-hidden">
                    <div
                      className="h-full bg-white/20"
                      style={{
                        width:
                          f.capaciteMax && f.capaciteMax > 0
                            ? `${Math.min(100, Math.round(((f.inscrits ?? 0) / f.capaciteMax) * 100))}%`
                            : '0%',
                      }}
                    />
                  </div>
                  <div className="muted text-xs mt-2">
                    {f.capaciteMax && f.capaciteMax > 0
                      ? `${Math.min(100, Math.round(((f.inscrits ?? 0) / f.capaciteMax) * 100))}% de remplissage`
                      : 'Capacité non définie'}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
