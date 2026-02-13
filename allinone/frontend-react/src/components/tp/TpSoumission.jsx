import React, { useState } from 'react'
import { Upload, CheckCircle, Download } from 'lucide-react'
import { submitSoumissionUpload, downloadSoumissionFile } from '../../services/TpRessourceService'

function formatDate(d) {
  if (!d) return null
  const x = new Date(d)
  if (Number.isNaN(x.getTime())) return null
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(x)
}

export default function TpSoumission({ tp, mySoumission, onSuccess, onCancel }) {
  const [file, setFile] = useState(null)
  const [commentaire, setCommentaire] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [downloading, setDownloading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (!file) {
      setError(mySoumission ? 'Sélectionnez un fichier pour remplacer votre rendu.' : 'Sélectionnez un fichier pour votre rendu.')
      return
    }
    setSaving(true)
    try {
      await submitSoumissionUpload(tp.id, file, commentaire || null)
      onSuccess?.()
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Erreur lors de l'envoi.")
    } finally {
      setSaving(false)
    }
  }

  async function handleDownload() {
    if (!mySoumission?.id) return
    setDownloading(true)
    try {
      await downloadSoumissionFile(mySoumission.id)
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Erreur téléchargement.')
    } finally {
      setDownloading(false)
    }
  }

  const isCorrige = String(mySoumission?.statut || '').toUpperCase() === 'CORRIGE'

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-slate-100">
        <Upload className="w-5 h-5" />
        <span className="font-semibold">{tp?.titre || 'TP'}</span>
      </div>
      {tp?.description && (
        <p className="text-sm text-slate-400">{tp.description}</p>
      )}

      {mySoumission && (
        <div className="space-y-2 rounded-lg border border-white/10 bg-white/5 p-3 text-sm">
          <div className="flex items-center gap-2 text-slate-200">
            <span className="text-slate-400">Statut :</span>
            <span className={isCorrige ? 'text-emerald-300' : 'text-amber-300'}>
              {isCorrige ? 'Corrigé' : 'Soumis'}
            </span>
            {mySoumission.dateSoumission && (
              <span className="text-slate-500">
                — {formatDate(mySoumission.dateSoumission)}
              </span>
            )}
          </div>
          {mySoumission.fichierSoumisUrl && (
            <div>
              <button
                type="button"
                className="btn inline-flex items-center gap-1"
                onClick={handleDownload}
                disabled={downloading}
              >
                <Download className="w-4 h-4" />
                {downloading ? 'Téléchargement…' : 'Télécharger mon dépôt'}
              </button>
            </div>
          )}
          {mySoumission.commentaire != null && mySoumission.commentaire !== '' && (
            <div>
              <span className="text-slate-400">Votre commentaire : </span>
              <span className="text-slate-200">{mySoumission.commentaire}</span>
            </div>
          )}
          {mySoumission.note != null && (
            <div className="flex items-center gap-2 text-emerald-300">
              <CheckCircle className="w-5 h-5" />
              <span>Note : {Number(mySoumission.note)}/20</span>
            </div>
          )}
          {mySoumission.feedback != null && mySoumission.feedback !== '' && (
            <div className="text-slate-300 bg-white/5 rounded p-2">
              <span className="text-slate-400">Feedback formateur : </span>
              {mySoumission.feedback}
            </div>
          )}
        </div>
      )}

      {error && <div className="alert-error">{error}</div>}
      <form onSubmit={handleSubmit} className="space-y-3">
        {!mySoumission && (
          <div>
            <label className="label">Fichier rendu (obligatoire)</label>
            <input
              className="fc-field w-full"
              type="file"
              accept=".pdf,.doc,.docx,.zip"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </div>
        )}
        {mySoumission && (
          <div>
            <label className="label">Nouveau rendu (remplace l’ancien)</label>
            <input
              className="fc-field w-full"
              type="file"
              accept=".pdf,.doc,.docx,.zip"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </div>
        )}
        <div>
          <label className="label">Commentaire (optionnel)</label>
          <textarea
            className="fc-field w-full min-h-[80px]"
            value={commentaire}
            onChange={(e) => setCommentaire(e.target.value)}
            placeholder="Votre commentaire..."
          />
        </div>
        <div className="flex items-center justify-end gap-2 pt-2">
          <button type="button" className="btn" onClick={onCancel} disabled={saving}>
            Fermer
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Envoi…' : mySoumission ? 'Remplacer le rendu' : 'Soumettre'}
          </button>
        </div>
      </form>
    </div>
  )
}
