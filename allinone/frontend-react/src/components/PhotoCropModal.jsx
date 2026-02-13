import React, { useEffect, useRef, useState } from 'react'

/**
 * Modal de recadrage + resize (client-side).
 *
 * Objectif: produire une image finale optimisée pour photo de profil.
 * - Recadrage visuel (aspect 1:1)
 * - Zoom + déplacement
 * - Export JPEG 320x320 compressé (qualité 0.85)
 */

function clamp(v, min, max) {
  return Math.min(max, Math.max(min, v))
}

function getPointer(e) {
  if (e.touches?.[0]) return { x: e.touches[0].clientX, y: e.touches[0].clientY }
  return { x: e.clientX, y: e.clientY }
}

export default function PhotoCropModal({
  open,
  file,
  onClose,
  onConfirm,
  outputSize = 320,
}) {
  const [src, setSrc] = useState(null)
  const [zoom, setZoom] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [baseScale, setBaseScale] = useState(1)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const imgRef = useRef(null)
  const dragRef = useRef({ dragging: false, startX: 0, startY: 0, baseX: 0, baseY: 0 })

  const containerSize = outputSize // viewport crop carré

  // Crée / libère l'URL locale
  useEffect(() => {
    if (!open || !file) {
      setSrc(null)
      return
    }
    const url = URL.createObjectURL(file)
    setSrc(url)
    return () => {
      try { URL.revokeObjectURL(url) } catch {}
    }
  }, [open, file])

  // Reset état à l'ouverture
  useEffect(() => {
    if (open) {
      setZoom(1)
      setOffset({ x: 0, y: 0 })
      setBaseScale(1)
      setBusy(false)
      setErr('')
    }
  }, [open])

  function computeBaseScale() {
    const img = imgRef.current
    if (!img) return 1
    const w = img.naturalWidth || 1
    const h = img.naturalHeight || 1
    // couvrir tout le cadre (pas de bandes)
    return Math.max(containerSize / w, containerSize / h)
  }

  function clampOffset(nextOffset, nextZoom) {
    const img = imgRef.current
    if (!img) return nextOffset
    const w = img.naturalWidth || 1
    const h = img.naturalHeight || 1
    const base = baseScale || computeBaseScale()
    const dispW = w * base * nextZoom
    const dispH = h * base * nextZoom

    const maxX = Math.max(0, (dispW - containerSize) / 2)
    const maxY = Math.max(0, (dispH - containerSize) / 2)

    return {
      x: clamp(nextOffset.x, -maxX, maxX),
      y: clamp(nextOffset.y, -maxY, maxY),
    }
  }

  function onZoomChange(v) {
    const z = Number(v)
    const zClamped = clamp(z, 1, 3)
    setZoom(zClamped)
    setOffset((o) => clampOffset(o, zClamped))
  }

  function onPointerDown(e) {
    e.preventDefault()
    if (!open) return
    const p = getPointer(e)
    dragRef.current.dragging = true
    dragRef.current.startX = p.x
    dragRef.current.startY = p.y
    dragRef.current.baseX = offset.x
    dragRef.current.baseY = offset.y
  }

  function onPointerMove(e) {
    if (!dragRef.current.dragging) return
    e.preventDefault()
    const p = getPointer(e)
    const dx = p.x - dragRef.current.startX
    const dy = p.y - dragRef.current.startY
    const next = { x: dragRef.current.baseX + dx, y: dragRef.current.baseY + dy }
    setOffset(clampOffset(next, zoom))
  }

  function onPointerUp() {
    dragRef.current.dragging = false
  }

  useEffect(() => {
    if (!open) return
    window.addEventListener('mouseup', onPointerUp)
    window.addEventListener('touchend', onPointerUp)
    window.addEventListener('mousemove', onPointerMove, { passive: false })
    window.addEventListener('touchmove', onPointerMove, { passive: false })
    return () => {
      window.removeEventListener('mouseup', onPointerUp)
      window.removeEventListener('touchend', onPointerUp)
      window.removeEventListener('mousemove', onPointerMove)
      window.removeEventListener('touchmove', onPointerMove)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, zoom, offset])

  async function buildCroppedBlob() {
    const img = imgRef.current
    if (!img) throw new Error('Image non chargée')

    const w = img.naturalWidth || 1
    const h = img.naturalHeight || 1
    const base = baseScale || computeBaseScale()
    const dispW = w * base * zoom
    const dispH = h * base * zoom

    // Position top-left de l'image dans le cadre
    const left = (containerSize - dispW) / 2 + offset.x
    const top = (containerSize - dispH) / 2 + offset.y

    const canvas = document.createElement('canvas')
    canvas.width = outputSize
    canvas.height = outputSize
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas indisponible')

    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'

    // Fond blanc (utile si PNG avec alpha)
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, outputSize, outputSize)

    // Dessine l'image transformée dans le canvas final
    // Ici containerSize == outputSize (cadre = sortie). Si tu changes, adapte.
    ctx.drawImage(img, left, top, dispW, dispH)

    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob(
        (b) => (b ? resolve(b) : reject(new Error('Échec conversion image'))),
        'image/jpeg',
        0.85
      )
    })

    return blob
  }

  async function confirm() {
    setBusy(true)
    setErr('')
    try {
      const blob = await buildCroppedBlob()
      await onConfirm(blob)
      onClose()
    } catch (e) {
      setErr(e?.message || 'Erreur recadrage')
    } finally {
      setBusy(false)
    }
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 z-[999] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="card w-full max-w-[560px] p-4">
        <div className="flex items-start justify-between gap-2">
          <div>
            <div className="text-lg font-semibold text-slate-100">Recadrer la photo</div>
            <div className="text-sm text-slate-400">Déplace l'image et ajuste le zoom. Sortie: 320×320 compressée.</div>
          </div>
          <button className="btn" onClick={onClose} disabled={busy}>✕</button>
        </div>

        {err && <div className="alert-error mt-3">{err}</div>}

        <div className="mt-4 flex flex-col items-center gap-4">
          <div
            className="relative rounded-2xl border border-white/10 bg-slate-950/40 overflow-hidden"
            style={{ width: containerSize, height: containerSize }}
            onMouseDown={onPointerDown}
            onTouchStart={onPointerDown}
          >
            {src ? (
              <img
                ref={imgRef}
                src={src}
                alt="crop"
                draggable={false}
                className="absolute left-1/2 top-1/2 select-none"
                style={{
                  width: `${(imgRef.current?.naturalWidth || 1) * (baseScale || 1)}px`,
                  height: `${(imgRef.current?.naturalHeight || 1) * (baseScale || 1)}px`,
                  transform: `translate(-50%, -50%) translate(${offset.x}px, ${offset.y}px) scale(${zoom})`,
                  transformOrigin: 'center center',
                }}
                onLoad={() => {
                  const b = computeBaseScale()
                  setBaseScale(b)
                  // Re-clamp après chargement pour éviter bandes
                  setOffset((o) => clampOffset(o, zoom))
                }}
              />
            ) : (
              <div className="w-full h-full grid place-items-center text-slate-400">Chargement...</div>
            )}

            {/* Overlay cercle pour guider (photo finale ronde) */}
            <div className="pointer-events-none absolute inset-0 grid place-items-center">
              <div className="h-[72%] w-[72%] rounded-full border-2 border-white/30 shadow-[0_0_0_9999px_rgba(0,0,0,0.35)]" />
            </div>
          </div>

          <div className="w-full">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Zoom</span>
              <span>{zoom.toFixed(2)}×</span>
            </div>
            <input
              type="range"
              min="1"
              max="3"
              step="0.01"
              value={zoom}
              onChange={(e) => onZoomChange(e.target.value)}
              className="w-full mt-2"
              disabled={busy}
            />
          </div>

          <div className="flex items-center justify-end gap-2 w-full">
            <button className="btn" onClick={onClose} disabled={busy}>Annuler</button>
            <button className="btn btn-primary" onClick={confirm} disabled={busy}>
              {busy ? 'Traitement...' : 'Valider la photo'}
            </button>
          </div>

          <div className="text-[11px] text-slate-500 w-full">
            Astuce: garde le visage au centre. L'image est automatiquement redimensionnée et compressée.
          </div>
        </div>
      </div>
    </div>
  )
}
