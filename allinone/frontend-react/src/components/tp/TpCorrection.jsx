import React, { useState } from 'react'
import { CheckCircle, MessageSquare, Download } from 'lucide-react'
import { corrigerTp, downloadSoumissionFile } from '../../services/TpRessourceService'

function StatutBadge({ statut }) {
  const s = String(statut || '').toUpperCase()
  if (s === 'CORRIGE') {
    return (
      <span className="badge border-emerald-400/30 bg-emerald-500/15 text-emerald-200">
        Corrigé
      </span>
    )
  }
  return (
    <span className="badge border-amber-400/30 bg-amber-500/15 text-amber-200">
      Soumis
    </span>
  )
}

export default function TpCorrection({ tp, soumissions, onSuccess, onCancel }) {
  const [editingId, setEditingId] = useState(null)
  const [note, setNote] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [downloadingId, setDownloadingId] = useState(null)

  async function handleDownload(soumission) {
    if (!soumission?.id) return
    setDownloadingId(soumission.id)
    try {
      await downloadSoumissionFile(soumission.id)
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Erreur téléchargement.')
    } finally {
      setDownloadingId(null)
    }
  }

  async function handleCorrect(soumission) {
    setError('')
    const n = note !== '' && note != null ? Number(note) : null
    if (n != null && (n < 0 || n > 20)) {
      setError('La note doit être entre 0 et 20.')
      return
    }
    setSaving(true)
    try {
      await corrigerTp(soumission.id, {
        statut: 'CORRIGE',
        note: n,
        commentaire: (commentaire || '').trim() || null,
      })
      setEditingId(null)
      setNote('')
      setCommentaire('')
      onSuccess?.()
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Erreur lors de la correction.')
    } finally {
      setSaving(false)
    }
  }

  function startEdit(s) {
    setEditingId(s.id)
    setNote(s.note != null ? String(s.note) : '')
    setCommentaire(s.feedback || '')
    setError('')
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-slate-100">
        <CheckCircle className="w-5 h-5" />
        <span className="font-semibold">Corriger les rendus — {tp?.titre || 'TP'}</span>
      </div>
      {error && <div className="alert-error">{error}</div>}
      <div className="overflow-auto">
        <table className="table">
          <thead>
            <tr>
              <th>Stagiaire</th>
              <th>Rendu</th>
              <th>Statut</th>
              <th>Note</th>
              <th>Feedback</th>
              <th className="text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            {(soumissions || []).map((s) => (
              <tr key={s.id}>
                <td>
                  <div className="font-medium text-slate-100">
                    {s?.stagiaire ? `${s.stagiaire.prenom} ${s.stagiaire.nom}` : '-'}
                  </div>
                  <div className="text-xs text-slate-400">{s?.stagiaire?.email}</div>
                </td>
                <td>
                  {s?.fichierSoumisUrl ? (
                    <button
                      type="button"
                      className="btn text-sm inline-flex items-center gap-1"
                      onClick={() => handleDownload(s)}
                      disabled={downloadingId === s.id}
                    >
                      <Download className="w-4 h-4" />
                      {downloadingId === s.id ? '…' : 'Télécharger'}
                    </button>
                  ) : (
                    <span className="text-slate-500">—</span>
                  )}
                </td>
                <td><StatutBadge statut={s.statut} /></td>
                <td>{s.note != null ? `${Number(s.note)}/20` : '—'}</td>
                <td className="max-w-[200px]">
                  {s.feedback ? (
                    <span className="text-xs text-slate-300 line-clamp-2">{s.feedback}</span>
                  ) : (
                    <span className="text-slate-500">—</span>
                  )}
                </td>
                <td className="text-right">
                  {editingId === s.id ? (
                    <button
                      className="btn"
                      onClick={() => { setEditingId(null); setError('') }}
                      disabled={saving}
                    >
                      Annuler
                    </button>
                  ) : (
                    <button className="btn btn-primary" onClick={() => startEdit(s)}>
                      Corriger
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {(!soumissions || soumissions.length === 0) && (
              <tr><td colSpan={6} className="text-slate-400">Aucune soumission.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {editingId && (
        <div className="card p-4 space-y-3 border border-indigo-400/20">
          <div className="flex items-center gap-2 text-slate-100">
            <MessageSquare className="w-4 h-4" />
            <span className="font-medium">Saisir la note et le retour</span>
          </div>
          <div>
            <label className="label">Note /20</label>
            <input
              className="fc-field w-24"
              type="number"
              min={0}
              max={20}
              step={0.5}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="—"
            />
          </div>
          <div>
            <label className="label">Feedback formateur (commentaire)</label>
            <textarea
              className="fc-field w-full min-h-[80px]"
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              placeholder="Retour pour le stagiaire..."
            />
          </div>
          <div className="flex items-center justify-end gap-2">
            <button className="btn" onClick={() => setEditingId(null)} disabled={saving}>
              Annuler
            </button>
            <button
              className="btn btn-primary"
              onClick={() => handleCorrect(soumissions.find((x) => x.id === editingId))}
              disabled={saving}
            >
              {saving ? 'Enregistrement…' : 'Enregistrer la correction'}
            </button>
          </div>
        </div>
      )}

      <div className="flex justify-end">
        <button className="btn" onClick={onCancel}>Fermer</button>
      </div>
    </div>
  )
}
