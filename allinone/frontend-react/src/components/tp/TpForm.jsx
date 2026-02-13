import React, { useState } from 'react'
import { FileText } from 'lucide-react'
import { create, uploadTpFile } from '../../services/TpRessourceService'

export default function TpForm({ formationId, formationTitre, onSuccess, onCancel }) {
  const [titre, setTitre] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState('TP')
  const [fichierUrl, setFichierUrl] = useState('')
  const [dateLimite, setDateLimite] = useState('')
  const [file, setFile] = useState(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (!titre.trim()) {
      setError('Le titre est obligatoire.')
      return
    }
    setSaving(true)
    try {
      const created = await create(formationId, {
        titre: titre.trim(),
        description: description.trim() || null,
        type,
        fichierUrl: fichierUrl.trim() || null,
        dateLimite: dateLimite.trim() || null,
      })
      if (file && created?.id) {
        await uploadTpFile(created.id, file)
      }
      onSuccess?.()
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Erreur lors de la création.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-slate-100">
        <FileText className="w-5 h-5" />
        <span className="font-semibold">Ajouter un TP ou une ressource</span>
      </div>
      {formationTitre && (
        <p className="text-sm text-slate-400">{formationTitre}</p>
      )}
      {error && <div className="alert-error">{error}</div>}
      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label className="label">Titre</label>
          <input
            className="fc-field w-full"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            placeholder="Ex: TP1 - Introduction"
          />
        </div>
        <div>
          <label className="label">Type</label>
          <select
            className="fc-field w-full"
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            <option value="TP">TP</option>
            <option value="COURS">Cours</option>
          </select>
        </div>
        <div>
          <label className="label">Fichier document (optionnel)</label>
          <input
            className="fc-field w-full"
            type="file"
            accept=".pdf,.doc,.docx,.zip"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
        </div>
        <div>
          <label className="label">Date limite (optionnel)</label>
          <input
            className="fc-field w-full"
            type="datetime-local"
            value={dateLimite}
            onChange={(e) => setDateLimite(e.target.value)}
          />
        </div>
        <div>
          <label className="label">Description (optionnel)</label>
          <textarea
            className="fc-field w-full min-h-[80px]"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Consignes ou contenu..."
          />
        </div>
        <div className="flex items-center justify-end gap-2 pt-2">
          <button type="button" className="btn" onClick={onCancel} disabled={saving}>
            Annuler
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Création…' : 'Créer'}
          </button>
        </div>
      </form>
    </div>
  )
}
