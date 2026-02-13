import React, { useEffect, useMemo, useState } from "react";
import Modal from "../components/Modal";
import { api } from "../api/client";
import { getCurrentUser } from "../auth/auth";

// ---------- utils ----------
function getApiErrorMessage(err, fallback = "Erreur inattendue") {
  if (!err) return fallback;
  // Local throw new Error("…")
  if (!err.response && err.message) return err.message;
  const data = err?.response?.data;
  if (!data) return fallback;
  // Spring: { message: "…" }
  if (typeof data.message === "string" && data.message) return data.message;
  // Spring validation: { field: "msg", field2: "msg2" } or { errors: [...] }
  if (typeof data === "object" && !Array.isArray(data)) {
    const entries = Object.entries(data).filter(
      ([k, v]) => typeof v === "string" && k !== "status" && k !== "error" && k !== "path" && k !== "timestamp"
    );
    if (entries.length > 0) return entries.map(([k, v]) => `${k}: ${v}`).join(" | ");
  }
  if (typeof data === "string" && data) return data;
  return fallback;
}

function asArray(v) {
  if (Array.isArray(v)) return v;
  if (v && typeof v === "object" && Array.isArray(v.content)) return v.content;
  return [];
}

function fmtDate(d) {
  if (!d) return "-";
  return String(d);
}

function sessionLabel(sessionType) {
  const v = String(sessionType || "").toUpperCase();
  if (v === "RATTRAPAGE") return { label: "Rattrapage", cls: "badge badge-warn" };
  if (v === "NORMAL") return { label: "Session normale", cls: "badge badge-neutral" };
  return { label: v || "-", cls: "badge" };
}

function statusLabel(ev) {
  const etat = String(ev?.etat || "").toUpperCase();
  if (etat === "TERMINEE") return { label: "Publiée", cls: "badge badge-ok" };

  const total = Number(ev?.participantsTotal ?? 0);
  const done = Number(ev?.notesSaisies ?? 0);
  const missing = Number(ev?.notesManquantes ?? Math.max(0, total - done));

  if (!total) return { label: "Planifiée", cls: "badge badge-neutral" };
  if (done <= 0) return { label: "Planifiée", cls: "badge badge-neutral" };
  if (missing > 0) return { label: "Saisie en cours", cls: "badge badge-warn" };
  return { label: "Prête à publier", cls: "badge badge-ok" };
}

function buildRows(participants, results) {
  const byStagiaireId = new Map();
  asArray(results).forEach((r) => {
    const sid = r?.stagiaire?.id;
    if (sid != null) byStagiaireId.set(sid, r);
  });

  return asArray(participants).map((p) => {
    const r = byStagiaireId.get(p.id);
    return {
      stagiaireId: p.id,
      nom: `${p?.prenom || ""} ${p?.nom || ""}`.trim() || p?.email || `#${p.id}`,
      email: p?.email || "",
      absent: Boolean(r?.absent),
      note: r?.absent ? "" : r?.note ?? "",
      commentaire: r?.commentaire || "",
    };
  });
}

function computeCompletion(participants, results) {
  const total = asArray(participants).length;
  const byId = new Map();
  asArray(results).forEach((r) => {
    const sid = r?.stagiaire?.id;
    if (sid != null) byId.set(sid, r);
  });

  let done = 0;
  asArray(participants).forEach((p) => {
    const r = byId.get(p.id);
    const absent = Boolean(r?.absent);
    const hasNote = r?.note !== null && r?.note !== undefined;
    const ok = (absent && !hasNote) || (!absent && hasNote);
    if (ok) done += 1;
  });
  return { total, done, missing: Math.max(0, total - done) };
}

function computeFailedCount(participants, results, seuil) {
  const byId = new Map();
  asArray(results).forEach((r) => {
    const sid = r?.stagiaire?.id;
    if (sid != null) byId.set(sid, r);
  });

  let failed = 0;
  asArray(participants).forEach((p) => {
    const r = byId.get(p.id);
    if (!r) return;
    if (Boolean(r?.absent)) {
      failed += 1;
      return;
    }
    const note = r?.note;
    if (note === null || note === undefined) return;
    if (Number(note) < Number(seuil)) failed += 1;
  });
  return failed;
}

// ---------- component ----------
export default function Evaluations() {
  const user = getCurrentUser();
  const role = String(user?.role || "").toUpperCase();
  const isAdmin = role === "ADMIN";
  const isFormateur = role === "FORMATEUR";
  const isStagiaire = role === "STAGIAIRE";
  const canManage = isAdmin || isFormateur;

  const [evaluations, setEvaluations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [flash, setFlash] = useState(null); // {type, text}

  // Create
  const [showCreate, setShowCreate] = useState(false);
  const [formations, setFormations] = useState([]);
  const [creating, setCreating] = useState(false);
  const [createFlash, setCreateFlash] = useState(null);
  const [createForm, setCreateForm] = useState({
    titre: "",
    description: "",
    seuilReussite: 10,
    dateEvaluation: "",
    formationId: "",
  });

  // Details
  const [showDetails, setShowDetails] = useState(false);
  const [selectedEval, setSelectedEval] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [results, setResults] = useState([]);
  const [completion, setCompletion] = useState({ total: 0, done: 0, missing: 0 });
  const [failedCount, setFailedCount] = useState(0);
  const [detailsBusy, setDetailsBusy] = useState(false);

  // Notes
  const [showNotes, setShowNotes] = useState(false);
  const [noteRows, setNoteRows] = useState([]);
  const [notesQ, setNotesQ] = useState("");
  const [savingNotes, setSavingNotes] = useState(false);
  const [notesFlash, setNotesFlash] = useState(null);

  // Publish
  const [showPublish, setShowPublish] = useState(false);
  const [publishDate, setPublishDate] = useState("");
  const [publishing, setPublishing] = useState(false);
  const [reopening, setReopening] = useState(false);

  const existingNormalByFormation = useMemo(() => {
    const set = new Set();
    asArray(evaluations).forEach((ev) => {
      const st = String(ev?.sessionType || "").toUpperCase();
      if (st !== "NORMAL") return;
      if (ev?.parentEvaluationId != null) return;
      const fid = ev?.formation?.id;
      if (fid != null) set.add(Number(fid));
    });
    return set;
  }, [evaluations]);

  async function refresh() {
    setLoading(true);
    setFlash(null);
    try {
      const res = await api.get("/api/evaluations");
      setEvaluations(res.data || []);
    } catch (e) {
      setFlash({ type: "error", text: getApiErrorMessage(e, "Impossible de charger les évaluations.") });
    } finally {
      setLoading(false);
    }
  }

  async function loadFormationsForCreate() {
    try {
      let res;
      if (isFormateur && user?.id) {
        res = await api.get(`/api/formateur/${user.id}/formations-dto`);
      } else {
        res = await api.get("/api/formations");
      }
      const all = asArray(res.data);
      // Création possible sur une formation en cours (ACTIVE) ou terminée.
      const eligible = all.filter((f) => {
        const s = String(f?.statut || "").toUpperCase();
        return s === "ACTIVE" || s === "TERMINEE";
      });
      setFormations(eligible);
    } catch {
      setFormations([]);
    }
  }

  useEffect(() => {
    refresh();
    if (canManage) loadFormationsForCreate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const displayedEvaluations = useMemo(() => {
    // Pas de filtre en haut: la page doit rester simple.
    const rows = asArray(evaluations).slice();
    rows.sort((a, b) => (String(b?.dateLimite || "")).localeCompare(String(a?.dateLimite || "")));
    return rows;
  }, [evaluations]);

  const createFormationsOptions = useMemo(() => {
    // on masque les formations qui ont déjà une évaluation principale
    return asArray(formations).filter((f) => !existingNormalByFormation.has(Number(f?.id)));
  }, [formations, existingNormalByFormation]);

  function resetCreate() {
    setCreateForm({ titre: "", description: "", seuilReussite: 10, dateEvaluation: "", formationId: "" });
  }

  async function openCreate() {
    setFlash(null);
    setCreateFlash(null);
    resetCreate();
    if (canManage) await loadFormationsForCreate();
    setShowCreate(true);
  }

  async function createEvaluation() {
    try {
      setCreating(true);
      setCreateFlash(null);

      if (!createForm.formationId) throw new Error("Choisis une formation.");
      if (!createForm.dateEvaluation) throw new Error("Choisis une date.");
      const titre = String(createForm.titre || "").trim();
      if (!titre) throw new Error("Titre requis.");
      const seuil = Number(createForm.seuilReussite);
      if (Number.isNaN(seuil) || seuil < 0 || seuil > 20) throw new Error("Seuil invalide (0..20).");

      const payload = {
        titre,
        description: String(createForm.description || "").trim(),
        seuilReussite: seuil,
        dateLimite: createForm.dateEvaluation,
        formationId: Number(createForm.formationId),
        sessionType: "NORMAL",
      };

      await api.post("/api/evaluations", payload);
      setShowCreate(false);
      setCreateFlash(null);
      setFlash({ type: "success", text: "Évaluation créée." });
      await refresh();
    } catch (e) {
      setCreateFlash({ type: "error", text: getApiErrorMessage(e, "Création impossible.") });
    } finally {
      setCreating(false);
    }
  }

  async function openDetails(ev) {
    if (!ev?.id) return;
    setFlash(null);
    setDetailsBusy(true);
    setShowDetails(true);
    setSelectedEval(ev);
    setParticipants([]);
    setResults([]);
    setCompletion({ total: 0, done: 0, missing: 0 });
    setFailedCount(0);

    try {
      // DTO à jour (stats, rattrapageId...)
      const evRes = await api.get(`/api/evaluations/${ev.id}`);
      const evFresh = evRes.data || ev;
      setSelectedEval(evFresh);

      const resResults = await api.get(`/api/evaluations/${ev.id}/resultats`);
      const resArr = asArray(resResults.data);
      setResults(resArr);

      if (canManage) {
        const resParticipants = await api.get(`/api/evaluations/${ev.id}/participants`);
        const partArr = asArray(resParticipants.data);
        setParticipants(partArr);

        const comp = computeCompletion(partArr, resArr);
        setCompletion(comp);
        setFailedCount(computeFailedCount(partArr, resArr, evFresh?.seuilReussite));
      }
    } catch (e) {
      setFlash({ type: "error", text: getApiErrorMessage(e, "Impossible de charger les détails.") });
    } finally {
      setDetailsBusy(false);
    }
  }

  function closeDetails() {
    setShowDetails(false);
    setSelectedEval(null);
    setParticipants([]);
    setResults([]);
    setCompletion({ total: 0, done: 0, missing: 0 });
    setFailedCount(0);
  }

  const isDateEvalFuture = useMemo(() => {
    const d = selectedEval?.dateLimite;
    if (!d) return false;
    const dateStr = String(d).slice(0, 10);
    const today = new Date().toISOString().slice(0, 10);
    return dateStr > today;
  }, [selectedEval?.dateLimite]);

  const isEvalTerminee = String(selectedEval?.etat || "").toUpperCase() === "TERMINEE";
  const isNormal = String(selectedEval?.sessionType || "").toUpperCase() === "NORMAL";
  const rattrapageChild = isNormal && selectedEval?.rattrapageId
    ? asArray(evaluations).find((e) => Number(e?.id) === Number(selectedEval.rattrapageId))
    : null;
  const todayStr = new Date().toISOString().slice(0, 10);
  const hasRattrapagePast = rattrapageChild && (String(rattrapageChild?.dateLimite || "").slice(0, 10) < todayStr);
  const canSaisirNotes = canManage && !isDateEvalFuture && !isEvalTerminee;
  const canPublier = canManage && !isDateEvalFuture && !isEvalTerminee;
  const canReopen = isAdmin && isEvalTerminee && !hasRattrapagePast;

  function openNotes() {
    if (!selectedEval) return;
    if (isDateEvalFuture) {
      setNotesFlash({ type: "error", text: "Disponible à partir du " + String(selectedEval?.dateLimite || "").slice(0, 10) + "." });
      return;
    }
    if (isEvalTerminee) {
      setNotesFlash({ type: "error", text: "Évaluation publiée. Réouvrez-la pour modifier les notes." });
      return;
    }
    setNotesFlash(null);
    setNotesQ("");
    setNoteRows(buildRows(participants, results));
    setShowNotes(true);
  }

  function closeNotes() {
    setShowNotes(false);
    setNoteRows([]);
    setNotesQ("");
    setNotesFlash(null);
  }

  const filteredNoteRows = useMemo(() => {
    const q = notesQ.trim().toLowerCase();
    if (!q) return noteRows;
    return asArray(noteRows).filter((r) =>
      String(r?.nom || "").toLowerCase().includes(q) || String(r?.email || "").toLowerCase().includes(q)
    );
  }, [noteRows, notesQ]);

  function updateRow(stagiaireId, patch) {
    setNoteRows((prev) =>
      asArray(prev).map((r) => (Number(r.stagiaireId) === Number(stagiaireId) ? { ...r, ...patch } : r))
    );
  }

  async function saveNotes() {
    if (!selectedEval?.id) return;
    try {
      setSavingNotes(true);
      setNotesFlash(null);

      // validation locale: chaque ligne doit être NOTE ou ABSENT
      const payload = asArray(noteRows).map((r) => {
        const absent = Boolean(r.absent);
        let note = r.note;
        if (absent) note = null;
        else {
          const n = Number(note);
          if (Number.isNaN(n)) throw new Error(`Note invalide pour ${r.nom}`);
          if (n < 0 || n > 20) throw new Error(`Note hors limite (0..20) pour ${r.nom}`);
          note = n;
        }
        return {
          evaluationId: Number(selectedEval.id),
          stagiaireId: Number(r.stagiaireId),
          note,
          absent,
          commentaire: String(r.commentaire || "").trim(),
        };
      });

      await api.post(`/api/evaluations/${selectedEval.id}/resultats/bulk`, payload);

      // reload results + stats
      const resResults = await api.get(`/api/evaluations/${selectedEval.id}/resultats`);
      const resArr = asArray(resResults.data);
      setResults(resArr);
      const comp = computeCompletion(participants, resArr);
      setCompletion(comp);
      setFailedCount(computeFailedCount(participants, resArr, selectedEval?.seuilReussite));

      // refresh evaluation DTO (notesSaisies/participantsTotal)
      try {
        const evRes = await api.get(`/api/evaluations/${selectedEval.id}`);
        setSelectedEval(evRes.data || selectedEval);
      } catch {
        // ignore
      }

      setNotesFlash({ type: "success", text: "Notes enregistrées." });
      await refresh();
    } catch (e) {
      setNotesFlash({ type: "error", text: getApiErrorMessage(e, "Enregistrement impossible.") });
    } finally {
      setSavingNotes(false);
    }
  }

  function openPublish() {
    if (!selectedEval?.id) return;
    if (isDateEvalFuture) {
      setFlash({ type: "error", text: "Disponible à partir du " + String(selectedEval?.dateLimite || "").slice(0, 10) + "." });
      return;
    }
    if (isEvalTerminee) {
      setFlash({ type: "info", text: "Évaluation publiée. Réouvrez-la pour modifier les notes." });
      return;
    }

    // notes complètes ?
    if (completion.missing > 0) {
      setFlash({ type: "error", text: "Complète toutes les notes avant de publier." });
      return;
    }

    // si session normale et échecs => date rattrapage obligatoire
    const st = String(selectedEval?.sessionType || "").toUpperCase();
    if (st === "NORMAL" && failedCount > 0) {
      setPublishDate("");
      setShowPublish(true);
      return;
    }

    // sinon publier direct
    doPublish(null);
  }

  async function doPublish(date) {
    if (!selectedEval?.id) return;
    try {
      setPublishing(true);
      setFlash(null);
      const body = date ? { dateRattrapage: date } : {};
      const res = await api.post(`/api/evaluations/${selectedEval.id}/publish`, body);
      setShowPublish(false);
      setPublishDate("");

      const r = res.data || {};
      const st = String(selectedEval?.sessionType || "").toUpperCase();
      if (st === "NORMAL") {
        if (Number(r?.failedCount || 0) > 0) {
          setFlash({
            type: "success",
            text: "Notes publiées. Un rattrapage a été ajouté pour les stagiaires concernés.",
          });
        } else {
          setFlash({ type: "success", text: "Notes publiées." });
        }
      } else {
        setFlash({ type: "success", text: "Résultats publiés." });
      }

      await refresh();

      // reload details
      try {
        const evRes = await api.get(`/api/evaluations/${selectedEval.id}`);
        const evFresh = evRes.data || selectedEval;
        setSelectedEval(evFresh);
        const resResults = await api.get(`/api/evaluations/${selectedEval.id}/resultats`);
        const resArr = asArray(resResults.data);
        setResults(resArr);
        const comp = computeCompletion(participants, resArr);
        setCompletion(comp);
        setFailedCount(computeFailedCount(participants, resArr, evFresh?.seuilReussite));
      } catch {
        // ignore
      }
    } catch (e) {
      setFlash({ type: "error", text: getApiErrorMessage(e, "Publication impossible.") });
    } finally {
      setPublishing(false);
    }
  }

  async function doReopen() {
    if (!selectedEval?.id) return;
    try {
      setReopening(true);
      setFlash(null);
      const res = await api.post(`/api/evaluations/${selectedEval.id}/reopen`);
      const evFresh = res.data || selectedEval;
      setSelectedEval(evFresh);
      setFlash({ type: "success", text: "Évaluation réouverte. Vous pouvez modifier les notes et republier." });
      await refresh();

      // reload participants/results
      try {
        const resResults = await api.get(`/api/evaluations/${selectedEval.id}/resultats`);
        const resArr = asArray(resResults.data);
        setResults(resArr);
        if (canManage) {
          const resParticipants = await api.get(`/api/evaluations/${selectedEval.id}/participants`);
          const partArr = asArray(resParticipants.data);
          setParticipants(partArr);
          const comp = computeCompletion(partArr, resArr);
          setCompletion(comp);
          setFailedCount(computeFailedCount(partArr, resArr, evFresh?.seuilReussite));
        }
      } catch {
        // ignore
      }
    } catch (e) {
      setFlash({ type: "error", text: getApiErrorMessage(e, "Réouverture impossible.") });
    } finally {
      setReopening(false);
    }
  }

  // ---------- UI blocks ----------
  const flashClass = (t) =>
    t === "success" ? "alert-success" : t === "error" ? "alert-error" : "alert-info";

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="title">Évaluations</div>
          <div className="muted mt-1">Planifier une évaluation, saisir les notes, puis publier les résultats.</div>
        </div>

        {canManage && (
          <button className="btn btn-primary" onClick={openCreate}>
            + Nouvelle évaluation
          </button>
        )}
      </div>

      {flash?.text && <div className={flashClass(flash.type)}>{flash.text}</div>}

      <div className="card p-0 overflow-hidden">
        <div className="p-4 border-b border-white/10 flex items-center justify-between">
          <div className="text-sm text-slate-200">Liste des évaluations</div>
          {loading && <div className="muted text-sm">Chargement…</div>}
        </div>

        <div className="overflow-auto">
          <table className="table">
            <thead>
              <tr>
                <th>Titre</th>
                <th>Formation</th>
                <th>Session</th>
                <th>Date</th>
                <th>Statut</th>
                {canManage && <th>Notes</th>}
                <th></th>
              </tr>
            </thead>
            <tbody>
              {displayedEvaluations.length === 0 && (
                <tr>
                  <td colSpan={canManage ? 7 : 6} className="py-6 px-4 text-slate-300">
                    Aucune évaluation pour le moment.
                  </td>
                </tr>
              )}

              {displayedEvaluations.map((ev) => {
                const sess = sessionLabel(ev?.sessionType);
                const st = statusLabel(ev);
                const notesTxt = canManage
                  ? `${Number(ev?.notesSaisies ?? 0)}/${Number(ev?.participantsTotal ?? 0) || "-"}`
                  : null;

                return (
                  <tr key={ev.id}>
                    <td className="font-medium text-slate-100">{ev?.titre || "-"}</td>
                    <td className="text-slate-200">{ev?.formation?.titre || "-"}</td>
                    <td><span className={sess.cls}>{sess.label}</span></td>
                    <td>{fmtDate(ev?.dateLimite)}</td>
                    <td><span className={st.cls}>{st.label}</span></td>
                    {canManage && <td className="text-slate-200">{notesTxt}</td>}
                    <td className="text-right">
                      <button className="btn" onClick={() => openDetails(ev)}>
                        Détails
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ---------- Create modal ---------- */}
      {showCreate && (
        <Modal title="Nouvelle évaluation" onClose={() => { setShowCreate(false); setCreateFlash(null); }} size="lg">
          <div className="space-y-3">
            {createFlash?.text && <div className={flashClass(createFlash.type)}>{createFlash.text}</div>}

            <div className="alert-info">
              Choisis une formation en cours ou terminée. Une seule évaluation principale est autorisée par formation.
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <div className="text-xs text-slate-300 mb-1">Formation</div>
                <select
                  className="fc-field"
                  value={createForm.formationId}
                  onChange={(e) => {
                    const fid = e.target.value;
                    const f = createFormationsOptions.find((x) => Number(x?.id) === Number(fid));
                    setCreateForm((p) => ({
                      ...p,
                      formationId: fid,
                      titre: p.titre ? p.titre : (f?.titre ? `Évaluation - ${f.titre}` : p.titre),
                    }));
                  }}
                >
                  <option value="">— Sélectionner —</option>
                  {createFormationsOptions.map((f) => (
                    <option key={f.id} value={f.id}>{f?.titre || f?.nom || `#${f.id}`}</option>
                  ))}
                </select>
                {createFormationsOptions.length === 0 && (
                  <div className="muted text-xs mt-1">Aucune formation disponible (toutes ont déjà une évaluation).</div>
                )}
              </div>

              <div>
                <div className="text-xs text-slate-300 mb-1">Date de l’évaluation</div>
                <input
                  type="date"
                  className="fc-field"
                  value={createForm.dateEvaluation}
                  onChange={(e) => setCreateForm((p) => ({ ...p, dateEvaluation: e.target.value }))}
                />
              </div>

              <div>
                <div className="text-xs text-slate-300 mb-1">Titre</div>
                <input
                  className="fc-field"
                  value={createForm.titre}
                  onChange={(e) => setCreateForm((p) => ({ ...p, titre: e.target.value }))}
                  placeholder="Ex: Évaluation de fin de formation"
                />
              </div>

              <div>
                <div className="text-xs text-slate-300 mb-1">Seuil de réussite (/20)</div>
                <input
                  type="number"
                  className="fc-field"
                  min={0}
                  max={20}
                  value={createForm.seuilReussite}
                  onChange={(e) => setCreateForm((p) => ({ ...p, seuilReussite: e.target.value }))}
                />
              </div>
            </div>

            <div>
              <div className="text-xs text-slate-300 mb-1">Description (optionnel)</div>
              <textarea
                className="fc-field"
                rows={3}
                value={createForm.description}
                onChange={(e) => setCreateForm((p) => ({ ...p, description: e.target.value }))}
                placeholder="Informations utiles pour les participants"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button className="btn" onClick={() => setShowCreate(false)} disabled={creating}>Annuler</button>
              <button className="btn btn-primary" onClick={createEvaluation} disabled={creating || createFormationsOptions.length === 0}>
                {creating ? "Création…" : "Créer"}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* ---------- Details modal ---------- */}
      {showDetails && (
        <Modal title="Détails de l’évaluation" onClose={closeDetails} size="xl">
          <div className="space-y-4">
            {detailsBusy && <div className="alert-info">Chargement…</div>}

            {!!selectedEval && (
              <>
                <div className="card p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="text-lg font-semibold text-slate-100">{selectedEval.titre || "Évaluation"}</div>
                      <div className="muted mt-1">
                        {selectedEval?.formation?.titre ? `Formation : ${selectedEval.formation.titre}` : ""}
                        {selectedEval?.dateLimite ? ` • Date : ${fmtDate(selectedEval.dateLimite)}` : ""}
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-2">
                      <div className="flex gap-2">
                        <span className={sessionLabel(selectedEval?.sessionType).cls}>{sessionLabel(selectedEval?.sessionType).label}</span>
                        <span className={statusLabel(selectedEval).cls}>{statusLabel(selectedEval).label}</span>
                      </div>

                      {canManage && (
                        <div className="text-xs text-slate-300">
                          Notes : {completion.done}/{completion.total} {completion.total ? "" : ""}
                        </div>
                      )}
                    </div>
                  </div>

                  {selectedEval?.description && (
                    <div className="muted mt-3">{selectedEval.description}</div>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mt-4">
                    <div className="card p-3">
                      <div className="text-xs text-slate-300">Seuil</div>
                      <div className="text-lg font-semibold">{selectedEval?.seuilReussite ?? "-"}/20</div>
                    </div>
                    {canManage && (
                      <>
                        <div className="card p-3">
                          <div className="text-xs text-slate-300">Participants</div>
                          <div className="text-lg font-semibold">{completion.total}</div>
                        </div>
                        <div className="card p-3">
                          <div className="text-xs text-slate-300">Échecs / Absents</div>
                          <div className="text-lg font-semibold">{failedCount}</div>
                        </div>
                        <div className="card p-3">
                          <div className="text-xs text-slate-300">Statut</div>
                          <div className="text-lg font-semibold">{statusLabel(selectedEval).label}</div>
                        </div>
                      </>
                    )}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-wrap gap-2 items-center">
                  {isDateEvalFuture && canManage && (
                    <div className="alert-info text-sm">
                      Disponible à partir du {String(selectedEval?.dateLimite || "").slice(0, 10)}.
                    </div>
                  )}
                  {canManage && (
                    <>
                      {canSaisirNotes && (
                        <button className="btn" onClick={openNotes}>
                          Saisir les notes
                        </button>
                      )}
                      {canPublier && (
                        <button className="btn btn-primary" onClick={openPublish} disabled={publishing}>
                          {publishing ? "Publication…" : "Publier"}
                        </button>
                      )}
                      {canReopen && (
                        <button className="btn btn-primary" onClick={doReopen} disabled={reopening}>
                          {reopening ? "Réouverture…" : "Réouvrir"}
                        </button>
                      )}
                      {isEvalTerminee && isAdmin && hasRattrapagePast && (
                        <div className="alert-info text-sm" title="Un rattrapage déjà passé existe pour cette évaluation normale.">
                          Impossible de réouvrir l&apos;évaluation normale : un rattrapage déjà passé existe. Réouvrez le rattrapage.
                        </div>
                      )}
                      {isEvalTerminee && isFormateur && (
                        <div className="alert-info text-sm">
                          Évaluation publiée.
                        </div>
                      )}
                      {String(selectedEval?.sessionType || "").toUpperCase() === "NORMAL" && selectedEval?.rattrapageId && (
                        <button
                          className="btn"
                          onClick={() => {
                            const target = asArray(evaluations).find((x) => Number(x?.id) === Number(selectedEval.rattrapageId));
                            if (target) openDetails(target);
                            else openDetails({ id: selectedEval.rattrapageId });
                          }}
                        >
                          Voir le rattrapage
                        </button>
                      )}
                    </>
                  )}

                  {isStagiaire && (
                    <div className="alert-info">
                      Ta note apparaît ici une fois les résultats publiés.
                    </div>
                  )}
                </div>

                {/* Résultats */}
                <div className="card p-0 overflow-hidden">
                  <div className="p-4 border-b border-white/10 flex items-center justify-between">
                    <div className="text-sm text-slate-200">Résultats</div>
                    {canManage && (
                      <div className="muted text-sm">
                        {completion.total ? `Saisie : ${completion.done}/${completion.total}` : ""}
                      </div>
                    )}
                  </div>

                  <div className="overflow-auto">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Stagiaire</th>
                          <th>Note</th>
                          <th>Statut</th>
                          <th>Commentaire</th>
                        </tr>
                      </thead>
                      <tbody>
                        {asArray(results).length === 0 && (
                          <tr>
                            <td colSpan={4} className="py-6 px-4 text-slate-300">
                              Aucun résultat pour le moment.
                            </td>
                          </tr>
                        )}

                        {asArray(results).map((r) => {
                          const absent = Boolean(r?.absent);
                          const note = absent ? "Absent" : (r?.note ?? "-");
                          const ok = Boolean(r?.reussi) && !absent;
                          const status = absent ? { label: "Absent", cls: "badge badge-warn" } : ok ? { label: "Validé", cls: "badge badge-ok" } : { label: "Non validé", cls: "badge badge-bad" };
                          const nom = `${r?.stagiaire?.prenom || ""} ${r?.stagiaire?.nom || ""}`.trim() || r?.stagiaire?.email || "-";
                          return (
                            <tr key={r?.id || `${r?.stagiaire?.id}-${note}`}
                              className=""
                            >
                              <td className="text-slate-100 font-medium">{nom}</td>
                              <td>{absent ? "—" : `${note}/20`}</td>
                              <td><span className={status.cls}>{status.label}</span></td>
                              <td className="text-slate-200">{r?.commentaire || ""}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              </>
            )}
          </div>
        </Modal>
      )}

      {/* ---------- Notes modal (sibling, no nesting) ---------- */}
      {showNotes && (
        <Modal title="Saisie des notes" onClose={closeNotes} size="xl">
          <div className="space-y-3">
            {notesFlash?.text && <div className={flashClass(notesFlash.type)}>{notesFlash.text}</div>}

            <div className="flex items-center justify-between gap-3">
              <div className="muted">
                Saisis une note (/20) ou coche <b>Absent</b>. Ensuite, enregistre.
              </div>
              <input
                className="fc-field-sm w-64"
                placeholder="Rechercher…"
                value={notesQ}
                onChange={(e) => setNotesQ(e.target.value)}
              />
            </div>

            <div className="card p-0 overflow-hidden">
              <div className="overflow-auto">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Stagiaire</th>
                      <th className="w-[140px]">Note</th>
                      <th className="w-[120px]">Absent</th>
                      <th>Commentaire</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredNoteRows.length === 0 && (
                      <tr>
                        <td colSpan={4} className="py-6 px-4 text-slate-300">
                          Aucun stagiaire.
                        </td>
                      </tr>
                    )}

                    {filteredNoteRows.map((r) => (
                      <tr key={r.stagiaireId}>
                        <td className="font-medium text-slate-100">
                          {r.nom}
                          <div className="muted text-xs">{r.email}</div>
                        </td>
                        <td>
                          <input
                            type="number"
                            min={0}
                            max={20}
                            step="0.5"
                            className="fc-field-sm w-[120px]"
                            value={r.absent ? "" : r.note}
                            disabled={r.absent}
                            onChange={(e) => updateRow(r.stagiaireId, { note: e.target.value })}
                          />
                        </td>
                        <td>
                          <label className="inline-flex items-center gap-2">
                            <input
                              type="checkbox"
                              checked={Boolean(r.absent)}
                              onChange={(e) => updateRow(r.stagiaireId, { absent: e.target.checked, note: e.target.checked ? "" : r.note })}
                            />
                            <span className="text-sm text-slate-200">Absent</span>
                          </label>
                        </td>
                        <td>
                          <input
                            className="fc-field-sm w-full"
                            value={r.commentaire}
                            onChange={(e) => updateRow(r.stagiaireId, { commentaire: e.target.value })}
                            placeholder="Optionnel"
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button className="btn" onClick={closeNotes} disabled={savingNotes}>Fermer</button>
              <button className="btn btn-primary" onClick={saveNotes} disabled={savingNotes || noteRows.length === 0}>
                {savingNotes ? "Enregistrement…" : "Enregistrer"}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* ---------- Publish modal ---------- */}
      {showPublish && (
        <Modal title="Date de rattrapage" onClose={() => setShowPublish(false)} size="md">
          <div className="space-y-3">
            <div className="alert-info">
              Certains stagiaires n'ont pas validé. Choisis une date pour la session de rattrapage.
            </div>
            <div>
              <div className="text-xs text-slate-300 mb-1">Date</div>
              <input type="date" className="fc-field" value={publishDate} onChange={(e) => setPublishDate(e.target.value)} />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button className="btn" onClick={() => setShowPublish(false)} disabled={publishing}>Annuler</button>
              <button className="btn btn-primary" disabled={publishing} onClick={() => {
                if (!publishDate) {
                  setFlash({ type: "error", text: "Date obligatoire." });
                  return;
                }
                doPublish(publishDate);
              }}>
                {publishing ? "Publication…" : "Publier"}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
