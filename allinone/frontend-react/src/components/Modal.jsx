import React, { useEffect } from "react";

/**
 * Modal premium (glass / dark).
 * Props:
 *  - title
 *  - children
 *  - onClose
 *  - size: "md" | "lg" | "xl"
 */
export default function Modal({ title, children, onClose, size = "md" }) {
  const maxW = size === "xl" ? "max-w-5xl" : size === "lg" ? "max-w-3xl" : "max-w-xl";

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === "Escape") onClose?.();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/60"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose?.();
      }}
    >
      <div className={`card w-full ${maxW} overflow-hidden`}
        style={{ boxShadow: "0 18px 60px rgba(0,0,0,0.45)" }}
      >
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
          <div className="font-semibold text-slate-100">{title}</div>
          <button className="btn" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </div>

        <div className="p-4 max-h-[78vh] overflow-auto">
          {children}
        </div>
      </div>
    </div>
  );
}
