import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { isAuthed } from '../auth/auth'

function Feature({ title, desc }) {
  return (
    <div className="flex items-start gap-3">
      <div className="h-10 w-10 shrink-0 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl grid place-items-center shadow-lg shadow-black/25">
        <div className="h-5 w-5 rounded-full bg-indigo-500/25 border border-indigo-400/25" />
      </div>
      <div>
        <div className="text-sm font-semibold text-slate-100">{title}</div>
        <div className="text-xs text-slate-400 leading-relaxed">{desc}</div>
      </div>
    </div>
  )
}

function MiniCard({ className = '', title, subtitle }) {
  return (
    <div className={`rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl shadow-lg shadow-black/25 px-4 py-3 ${className}`}>
      <div className="text-[11px] font-semibold text-slate-200">{title}</div>
      <div className="text-[11px] text-slate-400">{subtitle}</div>
    </div>
  )
}

export default function HomePublic() {
  const navigate = useNavigate()
  const authed = isAuthed()
  const [verifyCode, setVerifyCode] = useState('')

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Premium glows */}
      <div className="pointer-events-none absolute -top-40 -left-32 h-[520px] w-[520px] rounded-full bg-indigo-500/18 blur-3xl fc-float-1" />
      <div className="pointer-events-none absolute -bottom-40 -right-32 h-[560px] w-[560px] rounded-full bg-fuchsia-500/14 blur-3xl fc-float-2" />
      <div className="pointer-events-none absolute top-1/3 right-1/3 h-72 w-72 rounded-full bg-emerald-500/10 blur-3xl fc-float-3" />

      <div className="relative mx-auto max-w-6xl px-4 py-8">
        {/* Top bar */}
        <div className="flex items-center justify-between">
          <div className="inline-flex items-center gap-3">
            <div className="h-11 w-11 rounded-2xl bg-gradient-to-br from-indigo-500 via-fuchsia-500 to-emerald-500 text-white grid place-items-center shadow-lg shadow-black/30 ring-1 ring-white/10">
              <span className="text-sm font-bold">FC</span>
            </div>
            <div>
              <div className="text-sm font-semibold text-slate-100">FormationCloud</div>
              <div className="text-xs text-slate-400">Formations · Suivi · Certifications</div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {authed ? (
              <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>
                Aller à mon espace
              </button>
            ) : (
              <>
                <button className="btn" onClick={() => navigate('/login')}>
                  Se connecter
                </button>
                <button className="btn btn-primary" onClick={() => navigate('/login?signup=1')}>
                  S'inscrire
                </button>
              </>
            )}
          </div>
        </div>

        {/* Verify certificate */}
        <div className="mt-8 grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="card p-6 lg:col-span-2">
            <div className="text-sm font-semibold text-slate-100">Vérifier un certificat</div>
            <div className="text-xs text-slate-400 mt-1">
              Saisissez le code du certificat (ex: FC-2026-000123) pour vérifier sa validité.
            </div>

            <div className="mt-4 flex flex-col sm:flex-row gap-2">
              <input
                className="fc-field"
                placeholder="Code du certificat"
                value={verifyCode}
                onChange={e => setVerifyCode(e.target.value)}
              />
              <button
                className="btn btn-primary"
                onClick={() => {
                  const c = verifyCode.trim()
                  if (c) navigate(`/verify/${encodeURIComponent(c)}`)
                }}
              >
                Vérifier
              </button>
            </div>
          </div>

          <div className="card p-6">
            <div className="text-sm font-semibold text-slate-100">Anti-falsification</div>
            <div className="text-xs text-slate-400 mt-1 leading-relaxed">
              Chaque certificat contient un QR code lié à une vérification serveur.
              Si le statut est <span className="font-semibold text-slate-200">REVOQUE</span>, il n’est plus valable.
            </div>
          </div>
        </div>

        {/* Hero */}
        <div className="mt-10 grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
          <div className="card p-7 sm:p-9 lg:p-10 relative overflow-hidden">
            <div className="absolute inset-0 pointer-events-none">
              <div className="absolute -top-16 -left-16 h-64 w-64 rounded-full bg-indigo-500/15 blur-3xl" />
              <div className="absolute -bottom-24 -right-24 h-72 w-72 rounded-full bg-emerald-500/10 blur-3xl" />
            </div>

            <div className="relative">
              <h1 className="text-3xl sm:text-4xl font-semibold text-slate-100 leading-tight">
                Gérez vos <span className="text-indigo-300">formations</span> sans chaos.
              </h1>

              <p className="mt-4 text-slate-300 max-w-2xl">
                Planifiez le catalogue, suivez la progression, organisez les évaluations et générez des certificats —
                le tout dans une interface claire et rapide.
              </p>

              <div className="mt-7 grid grid-cols-1 sm:grid-cols-2 gap-5">
                <Feature
                  title="Catalogue & sessions"
                  desc="Créez et planifiez vos formations par catégorie, niveau et type."
                />
                <Feature
                  title="Suivi & évaluations"
                  desc="Mesurez l’avancement et les résultats, sans Excel ni confusion."
                />
                <Feature
                  title="Certificats & reporting"
                  desc="Délivrez des certificats et suivez vos indicateurs en temps réel."
                />
                <Feature
                  title="Espace personnel sécurisé"
                  desc="Chaque utilisateur retrouve uniquement ses formations et évaluations."
                />
              </div>

              <div className="mt-8 flex flex-wrap gap-2">
                <div className="badge">Pilotage simple</div>
                <div className="badge">Suivi en temps réel</div>
                <div className="badge">Certificats automatiques</div>
                <div className="badge">Interface premium</div>
              </div>

              {!authed && (
                <div className="mt-7 flex flex-col sm:flex-row gap-3">
                  <button className="btn btn-primary" onClick={() => navigate('/login')}>
                    Se connecter
                  </button>
                  <button className="btn" onClick={() => navigate('/login?signup=1')}>
                    Créer un compte
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Preview */}
          <div className="card p-6 sm:p-7 relative overflow-hidden">
            <div className="absolute inset-0 pointer-events-none">
              <div className="absolute left-6 top-6 h-1.5 w-24 rounded-full bg-white/10" />
              <div className="absolute left-6 top-10 h-1.5 w-40 rounded-full bg-white/10" />
            </div>

            <div className="relative h-[320px] sm:h-[360px]">
              <MiniCard
                className="absolute left-0 top-0 w-56 sm:w-60 fc-float-1"
                title="Session du jour"
                subtitle="10:00 · Java Spring Boot"
              />

              <MiniCard
                className="absolute left-1/2 top-16 -translate-x-1/2 w-64 sm:w-72 fc-float-2"
                title="Suivi"
                subtitle="Progression des stagiaires"
              />

              <MiniCard
                className="absolute right-0 bottom-6 w-52 sm:w-56 fc-float-3"
                title="Certificat"
                subtitle="Prêt à générer"
              />

              <div className="absolute left-6 bottom-6 w-40 card p-4">
                <div className="text-[11px] font-semibold text-slate-200">Progression</div>
                <div className="mt-2 flex items-end gap-1 h-16">
                  <div className="w-4 rounded bg-indigo-400/50" style={{ height: '35%' }} />
                  <div className="w-4 rounded bg-indigo-400/50" style={{ height: '55%' }} />
                  <div className="w-4 rounded bg-indigo-400/50" style={{ height: '45%' }} />
                  <div className="w-4 rounded bg-indigo-400/50" style={{ height: '75%' }} />
                  <div className="w-4 rounded bg-indigo-400/50" style={{ height: '60%' }} />
                </div>
              </div>
            </div>

            <div className="mt-4 text-xs text-slate-400">
              Une interface moderne, pensée pour un suivi clair et une gestion rapide.
            </div>
          </div>
        </div>

        {/* Verify */}
        <div className="mt-8 card p-6 sm:p-7 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            <div className="text-sm font-semibold text-slate-100">Vérifier un certificat</div>
            <div className="text-xs text-slate-400">Entrez le code (FC-YYYY-000001) ou scannez le QR du PDF.</div>
          </div>
          <div className="flex w-full sm:w-auto gap-2">
            <input
              className="fc-field"
              placeholder="FC-2026-000123"
              value={verifyCode}
              onChange={e => setVerifyCode(e.target.value)}
            />
            <button
              className="btn btn-primary"
              onClick={() => {
                const c = verifyCode.trim()
                if (!c) return
                navigate(`/verify/${encodeURIComponent(c)}`)
              }}
            >
              Vérifier
            </button>
          </div>
        </div>

        <div className="mt-10 text-center text-xs text-slate-500">
          © {new Date().getFullYear()} FormationCloud · Tous droits réservés
        </div>
      </div>
    </div>
  )
}
