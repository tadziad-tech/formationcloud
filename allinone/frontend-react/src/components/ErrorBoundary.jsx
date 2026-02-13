import React from 'react'

/**
 * Small ErrorBoundary to avoid the "blank page" syndrome in production.
 * If a render error happens, we show a readable card instead of a white/empty page.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, message: '' }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, message: error?.message || String(error) }
  }

  componentDidCatch(error, info) {
    // eslint-disable-next-line no-console
    console.error('UI crash:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen p-4">
          <div className="max-w-2xl mx-auto card p-6">
            <div className="text-lg font-semibold text-slate-100">Erreur d’affichage</div>
            <div className="text-sm text-slate-300 mt-2">
              Une erreur JavaScript a cassé l’interface. Ce n’est pas normal.
            </div>
            <div className="mt-4 text-xs text-slate-400">
              Détail : <span className="text-slate-200">{this.state.message}</span>
            </div>
            <div className="mt-5 flex gap-2 flex-wrap">
              <button className="btn btn-primary" onClick={() => window.location.reload()}>
                Recharger
              </button>
              <button className="btn" onClick={() => this.setState({ hasError: false, message: '' })}>
                Continuer
              </button>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
