import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
} from 'recharts'

function fmtStatut(s) {
  if (!s) return ''
  return s
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase())
}

function fmtDate(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleString()
  } catch {
    return iso
  }
}

export default function DashboardAdmin() {
  const navigate = useNavigate()
  const [days, setDays] = useState(14)
  const [loading, setLoading] = useState(true)
  const [overview, setOverview] = useState(null)
  const [pending, setPending] = useState([])
  const [todoItems, setTodoItems] = useState([])
  const [err, setErr] = useState('')
  const [busyId, setBusyId] = useState(null)

  async function load() {
    setLoading(true)
    setErr('')
    try {
      const [o, ins] = await Promise.all([
        api.get('/api/dashboard/admin/overview', { params: { days } }),
        api.get('/api/inscriptions'),
      ])
      setOverview(o.data)
      try {
        const todoRes = await api.get('/api/todo/me')
        const d = todoRes.data || {}
        const items = [...(d.todo || []), ...(d.overdue || [])].slice(0, 5)
        setTodoItems(items)
      } catch {
        setTodoItems([])
      }
      const all = Array.isArray(ins.data) ? ins.data : []
      const pend = all
        .filter((i) => i?.statut === 'EN_ATTENTE')
        .sort((a, b) => (b?.dateInscription || '').localeCompare(a?.dateInscription || ''))
        .slice(0, 12)
      setPending(pend)
    } catch (e) {
      setErr(e?.response?.data?.message || 'Erreur de chargement')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [days])

  const seriesIns = useMemo(() => {
    const arr = overview?.inscriptionsParJour || []
    return arr.map((p) => ({
      date: p?.date || '',
      count: Number(p?.count || 0),
    }))
  }, [overview])

  const usersByRole = useMemo(() => {
    const arr = overview?.utilisateursParRole || []
    return arr.map((x) => ({ name: x?.label || '-', value: Number(x?.count || 0) }))
  }, [overview])

  const insByStatut = useMemo(() => {
    const arr = overview?.inscriptionsParStatut || []
    return arr.map((x) => ({ name: fmtStatut(x?.label || '-'), count: Number(x?.count || 0) }))
  }, [overview])

  const formationsByCat = useMemo(() => {
    const arr = overview?.formationsActivesParCategorie || []
    return arr.slice(0, 6).map((x) => ({ name: x?.label || '-', count: Number(x?.count || 0) }))
  }, [overview])

  async function validateInscription(id) {
    try {
      setBusyId(id)
      await api.put(`/api/inscriptions/${id}/valider`)
      await load()
    } catch (e) {
      alert(e?.response?.data?.message || 'Erreur validation')
    } finally {
      setBusyId(null)
    }
  }

  async function refuseInscription(id) {
    const motif = window.prompt('Motif du refus (optionnel) :', '') || ''
    try {
      setBusyId(id)
      await api.put(`/api/inscriptions/${id}/refuser`, null, { params: { motif } })
      await load()
    } catch (e) {
      alert(e?.response?.data?.message || 'Erreur refus')
    } finally {
      setBusyId(null)
    }
  }

  const t = overview?.totals || {}
  const es = overview?.evaluationSummary || {}

  const periodLabel = days <= 0 ? 'Tout' : `${days}j`

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="title">Dashboard Admin</div>
          <div className="muted text-sm">Validation & suivi opérationnel + statistiques générales</div>
        </div>

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

      {err ? <div className="alert-error">{err}</div> : null}

      {/* À faire maintenant — top 5 (API /api/todo/me) */}
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
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-3">
        <div className="kpi">
          <div>
            <h3>Utilisateurs</h3>
            <div className="v">{t?.totalUtilisateurs ?? '—'}</div>
          </div>
          <div className="badge">{t?.totalFormateurs ?? 0} formateurs</div>
        </div>

        <div className="kpi">
          <div>
            <h3>Formations actives</h3>
            <div className="v">{t?.formationsActives ?? '—'}</div>
          </div>
          <div className="badge">/ {t?.totalFormations ?? 0}</div>
        </div>

        <div className="kpi">
          <div>
            <h3>Inscriptions ({periodLabel})</h3>
            <div className="v">{t?.inscriptionsSurPeriode ?? '—'}</div>
          </div>
          <div className="badge">Total: {t?.totalInscriptions ?? 0}</div>
        </div>

        <div className="kpi">
          <div>
            <h3>Certificats ({periodLabel})</h3>
            <div className="v">{t?.certificatsSurPeriode ?? '—'}</div>
          </div>
          <div className="badge">Total: {t?.totalCertificats ?? 0}</div>
        </div>

        <div className="kpi">
          <div>
            <h3>Évaluations à venir</h3>
            <div className="v">{es?.evaluationsAVenir ?? '—'}</div>
          </div>
          <div className="badge">Réussites: {es?.participantsReussis ?? 0}</div>
        </div>
      </div>

      {/* Operational */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="card p-4 lg:col-span-2">
          <div className="flex items-center justify-between gap-3 mb-3">
            <div>
              <div className="font-semibold">Validation — inscriptions en attente</div>
              <div className="muted text-sm">Traitement rapide des demandes d’accès aux formations</div>
            </div>
            <button className="btn" onClick={load} disabled={loading}>
              Rafraîchir
            </button>
          </div>

          {pending.length === 0 ? (
            <div className="alert-info">Aucune inscription en attente.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>Stagiaire</th>
                    <th>Formation</th>
                    <th>Date</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {pending.map((i) => (
                    <tr
                      key={i.id}
                      className="cursor-pointer hover:bg-white/5"
                      onClick={() => i?.formation?.id && navigate(`/formations?formationId=${i.formation.id}&focus=pending`)}
                    >
                      <td>
                        <div className="font-medium text-slate-100">
                          {i?.stagiaire?.prenom} {i?.stagiaire?.nom}
                        </div>
                        <div className="muted text-xs">{i?.stagiaire?.email}</div>
                      </td>
                      <td>
                        <div className="font-medium text-slate-100">{i?.formation?.nom || i?.formation?.titre}</div>
                        <div className="muted text-xs">{i?.formation?.categorie || 'Sans catégorie'}</div>
                      </td>
                      <td>
                        <div className="badge">{fmtDate(i?.dateInscription)}</div>
                      </td>
                      <td className="text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="inline-flex gap-2">
                          <button
                            className="btn btn-primary"
                            onClick={() => validateInscription(i.id)}
                            disabled={busyId === i.id}
                          >
                            Valider
                          </button>
                          <button
                            className="btn btn-danger"
                            onClick={() => refuseInscription(i.id)}
                            disabled={busyId === i.id}
                          >
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
          <div className="font-semibold mb-3">Activité récente</div>
          <div className="space-y-3">
            <div className="rounded-xl border border-white/10 bg-white/5 p-3">
              <div className="text-sm font-semibold">Dernières inscriptions</div>
              <div className="muted text-xs">Les 6 plus récentes</div>
              <div className="mt-2 space-y-2">
                {(overview?.dernieresInscriptions || []).slice(0, 6).map((x) => (
                  <div
                    key={x.id}
                    className="flex items-center justify-between gap-2 cursor-pointer hover:bg-white/5 p-2 rounded-lg -mx-2"
                    onClick={() => x?.formation?.id && navigate(`/formations?formationId=${x.formation.id}`)}
                  >
                    <div className="min-w-0">
                      <div className="text-sm truncate">
                        {x?.stagiaire?.prenom} {x?.stagiaire?.nom}
                      </div>
                      <div className="muted text-xs truncate">{x?.formation?.nom || x?.formation?.titre}</div>
                    </div>
                    <div className="badge">{fmtStatut(x?.statut)}</div>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-xl border border-white/10 bg-white/5 p-3">
              <div className="text-sm font-semibold">Certificats récents</div>
              <div className="muted text-xs">Les 5 plus récents</div>
              <div className="mt-2 space-y-2">
                {(overview?.derniersCertificats || []).slice(0, 5).map((c) => (
                  <div key={c.id} className="flex items-center justify-between gap-2">
                    <div className="min-w-0">
                      <div className="text-sm truncate">{c?.formation?.titre}</div>
                      <div className="muted text-xs truncate">
                        {c?.stagiaire?.prenom} {c?.stagiaire?.nom}
                      </div>
                    </div>
                    <div className="badge">{c?.noteFinale ?? '—'}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="card p-4 lg:col-span-2">
          <div className="flex items-center justify-between">
            <div>
              <div className="font-semibold">Inscriptions — tendance</div>
              <div className="muted text-sm">Sur {days} jours</div>
            </div>
            <div className="badge">{seriesIns.reduce((a, b) => a + b.count, 0)} total</div>
          </div>
          <div className="h-56 mt-3">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={seriesIns}>
                <XAxis dataKey="date" hide />
                <YAxis hide />
                <Tooltip
                  contentStyle={{ background: 'rgba(15,23,42,0.92)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 12 }}
                  labelFormatter={(v) => `Date: ${v}`}
                />
                <Area type="monotone" dataKey="count" strokeWidth={2} fillOpacity={0.25} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-4">
          <div className="font-semibold">Utilisateurs par rôle</div>
          <div className="muted text-sm">Répartition</div>
          <div className="h-56 mt-3">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={usersByRole} dataKey="value" nameKey="name" innerRadius={48} outerRadius={75} paddingAngle={3}>
                  {usersByRole.map((_, idx) => (
                    <Cell key={idx} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ background: 'rgba(15,23,42,0.92)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 12 }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-2 space-y-1">
            {usersByRole.map((x) => (
              <div key={x.name} className="flex items-center justify-between text-sm">
                <span className="muted">{x.name}</span>
                <span className="font-semibold">{x.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="card p-4">
          <div className="font-semibold">Inscriptions par statut</div>
          <div className="muted text-sm">Instantané</div>
          <div className="h-56 mt-3">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={insByStatut}>
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} />
                <Tooltip
                  contentStyle={{ background: 'rgba(15,23,42,0.92)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 12 }}
                />
                <Bar dataKey="count" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-4">
          <div className="font-semibold">Formations actives — catégories</div>
          <div className="muted text-sm">Top catégories (6)</div>
          <div className="h-56 mt-3">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={formationsByCat} layout="vertical">
                <XAxis type="number" allowDecimals={false} />
                <YAxis type="category" dataKey="name" width={120} tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{ background: 'rgba(15,23,42,0.92)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 12 }}
                />
                <Bar dataKey="count" radius={[0, 10, 10, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  )
}
