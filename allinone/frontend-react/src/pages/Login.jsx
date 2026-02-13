import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { login, requestAccess } from '../auth/auth'

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

function MiniCard({ className = '', title, subtitle, value }) {
  return (
    <div
      className={
        `rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl shadow-lg shadow-black/25 px-4 py-3 ${className}`
      }
    >
      <div className="text-[11px] font-semibold text-slate-200">{title}</div>
      <div className="text-[11px] text-slate-400">{subtitle}</div>
      {value && <div className="mt-2 text-xs text-slate-300">{value}</div>}
    </div>
  )
}

export default function Login() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [email, setEmail] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // modal inscription
  const [open, setOpen] = useState(false)
  const [reqNom, setReqNom] = useState('')
  const [reqPrenom, setReqPrenom] = useState('')
  const [reqEmail, setReqEmail] = useState('')
  const [reqPwd, setReqPwd] = useState('')
  const [reqRole, setReqRole] = useState('STAGIAIRE')
  // photo de profil : ajout/modif depuis la page Profil
  const [reqMsg, setReqMsg] = useState('')
  const [reqOk, setReqOk] = useState('')

  // open signup modal via /login?signup=1
  useEffect(() => {
    const v = (searchParams.get('signup') || '').toLowerCase()
    if (v === '1' || v === 'true' || v === 'yes') {
      setOpen(true)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (!email.trim() || !motDePasse) {
      setError('Email et mot de passe obligatoires')
      return
    }

    try {
      setLoading(true)
      await login(email.trim(), motDePasse)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err?.message || 'Erreur de connexion')
    } finally {
      setLoading(false)
    }
  }

  async function submitRequest() {
    setReqMsg('')
    setReqOk('')
    if (!reqNom.trim() || !reqPrenom.trim() || !reqEmail.trim() || !reqPwd) {
      setReqMsg('Nom, prénom, email et mot de passe sont obligatoires')
      return
    }
    try {
      await requestAccess({
        nom: reqNom.trim(),
        prenom: reqPrenom.trim(),
        email: reqEmail.trim(),
        motDePasse: reqPwd,
        role: reqRole,
      })
      if (reqRole === 'STAGIAIRE') {
        setReqOk("Compte créé. Vous pouvez vous connecter immédiatement.")
      } else {
        setReqOk("Demande envoyée. Votre compte sera activé après validation par un administrateur.")
      }
      setReqNom('')
      setReqPrenom('')
      setReqEmail('')
      setReqPwd('')
      setReqRole('STAGIAIRE')
    } catch (e) {
      setReqMsg(e?.message || "Erreur envoi demande")
    }
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* floating glow */}
      <div className="pointer-events-none absolute -top-32 -left-28 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl fc-float-1" />
      <div className="pointer-events-none absolute -bottom-40 -right-24 h-[520px] w-[520px] rounded-full bg-fuchsia-500/15 blur-3xl fc-float-2" />
      <div className="pointer-events-none absolute top-1/3 right-1/4 h-72 w-72 rounded-full bg-emerald-500/10 blur-3xl fc-float-3" />

      <div className="relative mx-auto max-w-6xl px-4 py-12 flex items-center min-h-screen">
        <div className="w-full grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
          {/* Left hero (client-friendly, non-technical) */}
          <div className="card p-7 sm:p-9 lg:p-10 relative overflow-hidden">
            <div className="absolute inset-0 pointer-events-none">
              <div className="absolute -top-16 -left-16 h-64 w-64 rounded-full bg-indigo-500/15 blur-3xl" />
              <div className="absolute -bottom-20 -right-20 h-72 w-72 rounded-full bg-emerald-500/10 blur-3xl" />
            </div>

            <div className="relative">
              <div className="inline-flex items-center gap-3">
                <div className="h-12 w-12 rounded-2xl bg-gradient-to-br from-indigo-500 via-fuchsia-500 to-emerald-500 text-white grid place-items-center shadow-lg shadow-black/30 ring-1 ring-white/10">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 2C7.6 2 4 5.6 4 10c0 4.1 3 7.4 6.9 7.9V21a1 1 0 0 0 2 0v-3.1C17 17.4 20 14.1 20 10c0-4.4-3.6-8-8-8Z" stroke="rgba(255,255,255,0.9)" strokeWidth="1.5" />
                    <path d="M8.5 10.5c.7-1.7 2.2-2.8 3.5-2.8 1.3 0 2.8 1.1 3.5 2.8" stroke="rgba(255,255,255,0.9)" strokeWidth="1.5" strokeLinecap="round" />
                  </svg>
                </div>
                <div>
                  <div className="text-sm font-semibold text-slate-100">FormationCloud</div>
                  <div className="text-xs text-slate-400">Formations · Suivi · Certifications</div>
                </div>
              </div>

              <h1 className="mt-6 text-3xl sm:text-4xl font-semibold text-slate-100 leading-tight">
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

              {/* Dashboard preview (non-interactive) */}
              <div className="mt-8">
                <div className="card p-5 sm:p-6 relative overflow-hidden">
                  <div className="absolute inset-0 pointer-events-none">
                    <div className="absolute left-6 top-6 h-1.5 w-24 rounded-full bg-white/10" />
                    <div className="absolute left-6 top-10 h-1.5 w-40 rounded-full bg-white/10" />
                  </div>

                  <div className="relative h-44 sm:h-52">
                    <MiniCard
                      className="absolute left-0 top-0 w-52 sm:w-56 fc-float-1"
                      title="Session du jour"
                      subtitle="10:00 · Java Spring Boot"
                      value=""
                    />

                    <MiniCard
                      className="absolute left-1/2 top-12 -translate-x-1/2 w-56 sm:w-64 fc-float-2"
                      title="Progression"
                      subtitle="Suivi des stagiaires"
                      value=""
                    />

                    <MiniCard
                      className="absolute right-0 bottom-2 w-44 sm:w-48 fc-float-3"
                      title="Certificat"
                      subtitle="Prêt à générer"
                      value=""
                    />

                    {/* simple chart bars */}
                    <div className="absolute left-5 bottom-4 w-28 card p-3">
                      <div className="text-[11px] font-semibold text-slate-200">Progression</div>
                      <div className="mt-2 flex items-end gap-1 h-12">
                        <div className="w-3 rounded bg-indigo-400/50" style={{ height: '35%' }} />
                        <div className="w-3 rounded bg-indigo-400/50" style={{ height: '55%' }} />
                        <div className="w-3 rounded bg-indigo-400/50" style={{ height: '45%' }} />
                        <div className="w-3 rounded bg-indigo-400/50" style={{ height: '75%' }} />
                        <div className="w-3 rounded bg-indigo-400/50" style={{ height: '60%' }} />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right login */}
          <div className="card p-6 sm:p-7 max-w-md w-full lg:ml-auto">
            <div>
              <div className="text-xl font-semibold text-slate-100">Connexion</div>
              <div className="text-sm text-slate-400">Accédez à votre espace en quelques secondes.</div>
            </div>

            {error && <div className="alert-error mt-4">{error}</div>}

            <form onSubmit={submit} className="mt-5 space-y-4">
              <div>
                <label className="text-xs font-medium text-slate-300">Email</label>
                <input
                  className="fc-field mt-1"
                  type="email"
                  placeholder="ex: prenom.nom@entreprise.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300">Mot de passe</label>
                <input
                  className="fc-field mt-1"
                  type="password"
                  placeholder="Votre mot de passe"
                  value={motDePasse}
                  onChange={(e) => setMotDePasse(e.target.value)}
                />
              </div>

              <button disabled={loading} className="btn btn-primary w-full" type="submit">
                {loading ? 'Connexion…' : 'Se connecter'}
              </button>

              <div className="flex items-center gap-3 py-2">
                <div className="h-px flex-1 bg-white/10" />
                <div className="text-xs text-slate-400">ou</div>
                <div className="h-px flex-1 bg-white/10" />
              </div>

              <button
                type="button"
                className="btn w-full"
                onClick={() => {
                  setOpen(true)
                  setReqOk('')
                  setReqMsg('')
                }}
              >
                S'inscrire
              </button>

              <div className="mt-2 rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="text-sm font-semibold text-slate-100">Nouveau sur la plateforme ?</div>
                <div className="text-xs text-slate-400 mt-1">
                  Un compte <span className="text-slate-200 font-semibold">Stagiaire</span> est créé immédiatement.
                  Les profils <span className="text-slate-200 font-semibold">Formateur</span> et <span className="text-slate-200 font-semibold">Admin</span>
                  nécessitent une validation.
                </div>
              </div>
            </form>

            <div className="mt-6 text-xs text-slate-500">© {new Date().getFullYear()} FormationCloud · Tous droits réservés</div>
          </div>
        </div>
      </div>

      {/* Modal inscription */}
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setOpen(false)} />
          <div className="relative card w-full max-w-4xl p-0 overflow-hidden">
            <div className="grid grid-cols-1 md:grid-cols-5">
              {/* left: value */}
              <div className="md:col-span-2 p-6 sm:p-7 border-b md:border-b-0 md:border-r border-white/10 bg-white/5">
                <div className="flex items-center gap-3">
                  <div className="h-11 w-11 rounded-2xl bg-gradient-to-br from-indigo-500 via-fuchsia-500 to-emerald-500 text-white grid place-items-center shadow-lg shadow-black/30 ring-1 ring-white/10">
                    <span className="text-sm font-bold">FC</span>
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-slate-100">FormationCloud</div>
                    <div className="text-xs text-slate-400">Créez votre espace en quelques secondes</div>
                  </div>
                </div>

                <div className="mt-5 text-lg font-semibold text-slate-100">Demande d’inscription</div>
                <div className="mt-2 text-sm text-slate-300 leading-relaxed">
                  Remplissez ce formulaire. Le compte <span className="text-slate-100 font-semibold">Stagiaire</span> est activé immédiatement.
                  Les profils <span className="text-slate-100 font-semibold">Formateur</span> et <span className="text-slate-100 font-semibold">Admin</span>
                  seront activés après validation.
                </div>

                <div className="mt-5 space-y-3">
                  <div className="flex items-start gap-3">
                    <div className="h-9 w-9 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl grid place-items-center">
                      <span className="text-indigo-300 text-sm">✓</span>
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-slate-100">Accès sécurisé</div>
                      <div className="text-xs text-slate-400">Vos données et vos formations restent privées.</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <div className="h-9 w-9 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl grid place-items-center">
                      <span className="text-emerald-300 text-sm">✓</span>
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-slate-100">Suivi clair</div>
                      <div className="text-xs text-slate-400">Progression, évaluations et certificats.</div>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <div className="h-9 w-9 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl grid place-items-center">
                      <span className="text-fuchsia-300 text-sm">✓</span>
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-slate-100">Interface premium</div>
                      <div className="text-xs text-slate-400">Une expérience moderne et agréable.</div>
                    </div>
                  </div>
                </div>

                <div className="mt-6 text-xs text-slate-400">
                  Déjà un compte ?{' '}
                  <button className="underline underline-offset-4 text-slate-200" onClick={() => setOpen(false)}>
                    Se connecter
                  </button>
                </div>
              </div>

              {/* right: form */}
              <div className="md:col-span-3 p-6 sm:p-7">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="text-lg font-semibold text-slate-100">Vos informations</div>
                    <div className="text-sm text-slate-400">Merci de compléter les champs ci-dessous.</div>
                  </div>
                  <button className="btn" onClick={() => setOpen(false)}>
                    Fermer
                  </button>
                </div>

                {reqMsg && <div className="alert-error mt-4">{reqMsg}</div>}
                {reqOk && <div className="alert-success mt-4">{reqOk}</div>}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4">
                  <div>
                    <label className="text-xs font-medium text-slate-300">Nom</label>
                    <input className="fc-field mt-1" placeholder="Votre nom" value={reqNom} onChange={(e) => setReqNom(e.target.value)} />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-300">Prénom</label>
                    <input className="fc-field mt-1" placeholder="Votre prénom" value={reqPrenom} onChange={(e) => setReqPrenom(e.target.value)} />
                  </div>
                  <div className="md:col-span-2">
                    <label className="text-xs font-medium text-slate-300">Email</label>
                    <input
                      className="fc-field mt-1"
                      type="email"
                      placeholder="ex: prenom.nom@entreprise.com"
                      value={reqEmail}
                      onChange={(e) => setReqEmail(e.target.value)}
                    />
                  </div>


                  <div>
                    <label className="text-xs font-medium text-slate-300">Mot de passe</label>
                    <input
                      className="fc-field mt-1"
                      type="password"
                      placeholder="Choisissez un mot de passe"
                      value={reqPwd}
                      onChange={(e) => setReqPwd(e.target.value)}
                    />
                    <div className="mt-1 text-[11px] text-slate-500">
                      Astuce : au moins 8 caractères pour un meilleur niveau de sécurité.
                    </div>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-300">Profil</label>
                    <select className="fc-field mt-1" value={reqRole} onChange={(e) => setReqRole(e.target.value)}>
                      <option value="STAGIAIRE">Stagiaire</option>
                      <option value="FORMATEUR">Formateur</option>
                    </select>
                    <div className="mt-1 text-[11px] text-slate-500">
                      Stagiaire : accès immédiat. Formateur : validation par un administrateur.
                    </div>
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row sm:justify-end gap-2 mt-5">
                  <button className="btn" onClick={() => setOpen(false)}>
                    Annuler
                  </button>
                  <button className="btn btn-primary" onClick={submitRequest}>
                    {reqRole === 'STAGIAIRE' ? "Créer mon compte" : "Envoyer demande d'inscription"}
                  </button>
                </div>

                <div className="text-xs text-slate-400 mt-3">
                  {reqRole === 'STAGIAIRE'
                    ? "Vous pouvez vous connecter dès maintenant."
                    : "Après validation, vous pourrez vous connecter et accéder à votre espace."}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
