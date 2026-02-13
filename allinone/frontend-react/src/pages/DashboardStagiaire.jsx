import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { getCurrentUser } from '../auth/auth'

function fmtStatut(s) {
  if (!s) return ''
  return s.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())
}

function fmtDate(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleDateString()
  } catch {
    return ''
  }
}

export default function DashboardStagiaire() {
  const user = getCurrentUser()
  const id = user?.id
  const navigate = useNavigate()

  const [data, setData] = useState(null)
  const [todoItems, setTodoItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  const totals = data?.totals || {}

  const todo = useMemo(() => {
    const evals = (data?.evaluations || []).slice(0, 6)
    return { evals }
  }, [data])

  async function load() {
    if (!id) return
    setLoading(true)
    setErr('')
    try {
      const res = await api.get(`/api/dashboard/stagiaire/${id}/overview`, { params: { days: 14 } })
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
      setErr(e?.response?.data?.message || 'Impossible de charger le dashboard')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  if (!id) {
    return (
      <div className="card p-4">
        <div className="title">Accès refusé</div>
        <div className="muted text-sm mt-1">Utilisateur non détecté.</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="title">Votre espace</div>
          <div className="muted text-sm">Progression et prochaines évaluations.</div>
        </div>
        <button className="btn" onClick={load} disabled={loading}>
          {loading ? 'Chargement…' : 'Rafraîchir'}
        </button>
      </div>

      {err ? <div className="alert-error">{err}</div> : null}

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
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="kpi">
          <div>
            <h3>Formations actives</h3>
            <div className="v">{totals.inscriptionsActives ?? 0}</div>
          </div>
          <div className="badge">🎓</div>
        </div>
        <div className="kpi">
          <div>
            <h3>Demandes en attente</h3>
            <div className="v">{totals.inscriptionsEnAttente ?? 0}</div>
          </div>
          <div className="badge">⏳</div>
        </div>
        <div className="kpi">
          <div>
            <h3>Évaluations à passer</h3>
            <div className="v">{totals.evaluationsACompleter ?? 0}</div>
          </div>
          <div className="badge">📝</div>
        </div>
        <div className="kpi">
          <div>
            <h3>Certificats</h3>
            <div className="v">{totals.certificatsObtenus ?? 0}</div>
          </div>
          <div className="badge">🏅</div>
        </div>
      </div>

      {/* Prochaines évaluations */}
      <div className="grid grid-cols-1 gap-4">
        <div className="card p-4">
          <div className="flex items-center justify-between">
            <div className="font-semibold">Prochaines évaluations</div>
            <div className="muted text-sm">à préparer</div>
          </div>

          {todo.evals.length === 0 ? (
            <div className="muted text-sm mt-3">Aucune évaluation à venir.</div>
          ) : (
            <div className="space-y-3 mt-4">
            {todo.evals.map((e) => (
              <div
                key={e.id}
                className="card p-3 cursor-pointer hover:bg-white/5 transition"
                onClick={() => e?.formationId && navigate(`/formations?formationId=${e.formationId}`)}
              >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="font-medium">{e.titre}</div>
                      <div className="muted text-xs mt-1">
                        {e.formationTitre ? `${e.formationTitre} • ` : ''}
                        Date: {fmtDate(e.dateEvaluation)}
                      </div>
                    </div>
                    <div className="badge">{e.dureeMinutes ? `${e.dureeMinutes} min` : '—'}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="card p-4 lg:col-span-2">
          <div className="flex items-center justify-between">
            <div className="font-semibold">Dernières inscriptions</div>
            <div className="muted text-sm">vos activités</div>
          </div>

          <div className="overflow-auto mt-3">
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Formation</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                {(data?.inscriptions || []).slice(0, 8).map((i) => (
                  <tr
                    key={i.id}
                    className="cursor-pointer hover:bg-white/5"
                    onClick={() => i?.formationId && navigate(`/formations?formationId=${i.formationId}`)}
                  >
                    <td className="muted">{fmtDate(i.dateInscription)}</td>
                    <td>{i.formationTitre || '—'}</td>
                    <td>
                      <span className="badge">{fmtStatut(i.statut)}</span>
                    </td>
                  </tr>
                ))}
                {(data?.inscriptions || []).length === 0 ? (
                  <tr>
                    <td colSpan={3} className="muted py-6">
                      Aucune inscription.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card p-4">
          <div className="flex items-center justify-between">
            <div className="font-semibold">Notifications</div>
            <div className="badge">{totals.notificationsNonLues ?? 0}</div>
          </div>
          <div className="mt-3 space-y-3">
            {(data?.notifications || []).slice(0, 6).map((n) => (
              <div key={n.id} className="card p-3">
                <div className="font-medium">{n.titre}</div>
                <div className="muted text-xs mt-1">{fmtDate(n.dateCreation)}</div>
                <div className="text-sm mt-2">{n.message}</div>
              </div>
            ))}
            {(data?.notifications || []).length === 0 ? (
              <div className="muted text-sm">Aucune notification non lue.</div>
            ) : null}
          </div>
        </div>
      </div>

      {/* Certificats */}
      <div className="card p-4">
        <div className="flex items-center justify-between">
          <div className="font-semibold">Certificats récents</div>
          <div className="muted text-sm">vos réussites</div>
        </div>

        <div className="overflow-auto mt-3">
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Formation</th>
                <th>Numéro</th>
                <th>Note</th>
              </tr>
            </thead>
            <tbody>
              {(data?.certificats || []).slice(0, 8).map((c) => (
                <tr key={c.id}>
                  <td className="muted">{fmtDate(c.dateObtention)}</td>
                  <td>{c.formationTitre || '—'}</td>
                  <td className="muted">{c.numeroUnique || '—'}</td>
                  <td>{c.noteFinale != null ? c.noteFinale : '—'}</td>
                </tr>
              ))}
              {(data?.certificats || []).length === 0 ? (
                <tr>
                  <td colSpan={4} className="muted py-6">
                    Aucun certificat.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
