package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.EvaluationRepository;
import com.formationcloud.platform.repository.ResultatEvaluationRepository;
import com.formationcloud.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ResultatEvaluationRepository resultatRepository;
    private final FormationService formationService;
    private final UtilisateurService utilisateurService;
    private final CertificatService certificatService;
    private final NotificationService notificationService;
    private final InscriptionService inscriptionService;

    // Petit DTO interne pour exposer des stats "prêtes UI".
    @lombok.Data
    public static class EvaluationStats {
        private int participantsTotal;
        private int notesSaisies;
        private int notesManquantes;
        private int validesCount;
        private int echecsCount;
        private int absentsCount;
        private Long rattrapageId;
    }

    // =====================
    // Lecture (avec sécurité)
    // =====================

    public List<Evaluation> findAllForCurrentUser() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (SecurityUtils.isAdmin()) {
            return evaluationRepository.findAll();
        }

        String role = (principal.getRole() == null ? "" : principal.getRole().toUpperCase());

        if ("FORMATEUR".equals(role)) {
            return evaluationRepository.findByFormationFormateurId(principal.getId());
        }

        if ("STAGIAIRE".equals(role)) {
            List<Long> formationIds = inscriptionService.findActiveFormationIdsByStagiaire(principal.getId());
            if (formationIds == null || formationIds.isEmpty()) return List.of();
            return evaluationRepository.findByFormationIdIn(formationIds);
        }

        return List.of();
    }

    /**
     * Stats d'avancement de saisie des notes + compteurs.
     * Sécurité : ce calcul doit être appelé uniquement sur des évaluations déjà filtrées/autorisé.
     */
    @Transactional(readOnly = true)
    public EvaluationStats computeStats(Evaluation evaluation) {
        if (evaluation == null) return null;

        EvaluationStats s = new EvaluationStats();

        // Participants attendus selon le type de session.
        List<Utilisateur> expected = expectedParticipants(evaluation);
        s.setParticipantsTotal(expected.size());

        // Résultats actuels.
        List<ResultatEvaluation> results = resultatRepository.findByEvaluation(evaluation);
        Map<Long, ResultatEvaluation> byId = results.stream()
                .filter(r -> r != null && r.getStagiaire() != null && r.getStagiaire().getId() != null)
                .collect(Collectors.toMap(r -> r.getStagiaire().getId(), r -> r, (a, b) -> a));

        int done = 0;
        int abs = 0;
        int ok = 0;
        int ko = 0;

        for (Utilisateur u : expected) {
            if (u == null || u.getId() == null) continue;
            ResultatEvaluation r = byId.get(u.getId());
            if (r == null) continue;

            boolean absent = Boolean.TRUE.equals(r.getAbsent());
            boolean hasNote = r.getNote() != null;
            boolean completed = (absent && !hasNote) || (!absent && hasNote);
            if (completed) done++;

            if (absent) {
                abs++;
            } else if (hasNote) {
                if (r.isReussi()) ok++; else ko++;
            }
        }

        s.setNotesSaisies(done);
        s.setNotesManquantes(Math.max(0, expected.size() - done));
        s.setAbsentsCount(abs);
        s.setValidesCount(ok);
        s.setEchecsCount(ko);

        // Rattrapage lié (si session normale)
        if (evaluation.getSessionType() == SessionEvaluationType.NORMAL) {
            List<Evaluation> kids = evaluationRepository.findByParentEvaluation_Id(evaluation.getId());
            Long rid = kids.stream()
                    .filter(e -> e != null && e.getSessionType() == SessionEvaluationType.RATTRAPAGE)
                    .map(Evaluation::getId)
                    .findFirst()
                    .orElse(null);
            s.setRattrapageId(rid);
        }

        return s;
    }

    public Evaluation findById(Long id) {
        Evaluation e = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", "id", id));
        assertCanAccessEvaluation(e);
        return e;
    }

    public List<Evaluation> findByFormation(Long formationId) {
        Formation f = formationService.findById(formationId);

        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (!SecurityUtils.isAdmin()) {
            String role = (principal.getRole() == null ? "" : principal.getRole().toUpperCase());
            if ("FORMATEUR".equals(role)) {
                Long ownerId = (f.getFormateur() != null ? f.getFormateur().getId() : null);
                if (ownerId == null || !ownerId.equals(principal.getId())) {
                    throw new AccessDeniedException("Accès interdit");
                }
            } else if ("STAGIAIRE".equals(role)) {
                if (!inscriptionService.isStagiaireActifDansFormation(principal.getId(), formationId)) {
                    throw new AccessDeniedException("Accès interdit");
                }
            } else {
                throw new AccessDeniedException("Accès interdit");
            }
        }

        return evaluationRepository.findByFormationId(formationId);
    }

    public List<Evaluation> findByFormateur(Long formateurId) {
        // ADMIN ou soi-même (formateur)
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (!SecurityUtils.isAdmin() && !Objects.equals(principal.getId(), formateurId)) {
            throw new AccessDeniedException("Accès interdit");
        }

        return evaluationRepository.findByFormationFormateurId(formateurId);
    }

    // =====================
    // Création / modification
    // =====================

    public Evaluation createEvaluation(Evaluation evaluation, Long formationId) {
        Formation formation = formationService.findById(formationId);

// Pro : on peut planifier l'évaluation sur une formation ACTIVE ou TERMINEE.
// (Dans ce projet, "ACTIVE" correspond à "en cours").
if (formation.getStatut() != null
        && formation.getStatut() != StatutFormation.ACTIVE
        && formation.getStatut() != StatutFormation.TERMINEE) {
    throw new BadRequestException("La formation doit être en cours (ACTIVE) ou terminée pour créer une évaluation");
}


        // Sécurité: ADMIN => ok, FORMATEUR => uniquement ses formations
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (!SecurityUtils.isAdmin()) {
            if (!"FORMATEUR".equalsIgnoreCase(principal.getRole())) {
                throw new AccessDeniedException("Accès interdit");
            }
            Long ownerId = (formation.getFormateur() != null ? formation.getFormateur().getId() : null);
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("Accès interdit");
            }
        }

        evaluation.setFormation(formation);

        // valeurs par défaut
        if (evaluation.getSessionType() == null) evaluation.setSessionType(SessionEvaluationType.NORMAL);
        if (evaluation.getEtat() == null) evaluation.setEtat(EtatEvaluation.EN_COURS);

        // Le rattrapage est AUTO (créé à la publication des notes). On ne le crée pas manuellement.
        if (evaluation.getSessionType() == SessionEvaluationType.RATTRAPAGE) {
            throw new BadRequestException("Le rattrapage est créé automatiquement lors de la publication");
        }

        // Une seule évaluation NORMAL par formation (évite la confusion côté jury)
        List<Evaluation> existing = evaluationRepository.findByFormationId(formationId);
        boolean alreadyHasNormal = existing.stream()
                .anyMatch(e -> e.getSessionType() == SessionEvaluationType.NORMAL && e.getParentEvaluation() == null);
        if (alreadyHasNormal) {
            throw new BadRequestException("Une évaluation (session normale) existe déjà pour cette formation");
        }

        // Validation du seuil
        if (evaluation.getSeuilReussite() == null
                || evaluation.getSeuilReussite().compareTo(BigDecimal.ZERO) < 0
                || evaluation.getSeuilReussite().compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new BadRequestException("Le seuil de réussite doit être entre 0 et 20");
        }

        Evaluation saved = evaluationRepository.save(evaluation);

        // Notifier uniquement pour une session normale (ou une évaluation manuelle)
        List<Inscription> inscriptions = inscriptionService.findByFormation(formationId);
        List<Utilisateur> stagiaires = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.CONFIRMEE || i.getStatut() == StatutInscription.EN_COURS)
                .map(Inscription::getStagiaire)
                .toList();

        if (!stagiaires.isEmpty()) {
            notificationService.envoyerNotificationEvaluationDisponible(saved, stagiaires);
        }

        log.info("Évaluation créée : {} ({}) pour la formation {}",
                saved.getTitre(),
                saved.getSessionType(),
                formation.getNom());
        return saved;
    }

    public Evaluation updateEvaluation(Long id, Evaluation evaluationDetails) {
        Evaluation evaluation = findById(id);
        assertCanManageEvaluation(evaluation);

        evaluation.setTitre(evaluationDetails.getTitre());
        evaluation.setDescription(evaluationDetails.getDescription());
        evaluation.setSeuilReussite(evaluationDetails.getSeuilReussite());
        evaluation.setDateEvaluation(evaluationDetails.getDateEvaluation());
        evaluation.setDureeMinutes(evaluationDetails.getDureeMinutes());
        if (evaluationDetails.getSessionType() != null) {
            // même règle: pas de rattrapage manuel
            if (evaluationDetails.getSessionType() == SessionEvaluationType.RATTRAPAGE) {
                throw new BadRequestException("Le rattrapage est géré automatiquement");
            }
            evaluation.setSessionType(evaluationDetails.getSessionType());
        }

        return evaluationRepository.save(evaluation);
    }

    public void deleteEvaluation(Long id) {
        Evaluation evaluation = findById(id);
        assertCanManageEvaluation(evaluation);

        if (evaluation.getResultats() != null && !evaluation.getResultats().isEmpty()) {
            throw new BadRequestException("Impossible de supprimer une évaluation avec des résultats");
        }

        evaluationRepository.delete(evaluation);
        log.info("Évaluation supprimée : {}", evaluation.getTitre());
    }

    // =====================
    // Participants (UI)
    // =====================

    public List<UtilisateurSummaryDTO> getParticipantsForEvaluation(Long evaluationId) {
        Evaluation evaluation = findById(evaluationId);
        assertCanManageEvaluation(evaluation);

        List<Utilisateur> participants;

        if (evaluation.getSessionType() == SessionEvaluationType.RATTRAPAGE) {
            Evaluation parent = evaluation.getParentEvaluation();
            if (parent == null) {
                participants = List.of();
            } else {
                participants = getFailedParticipants(parent);
            }
        } else {
            // NORMAL => tous les participants confirmés/en cours
            participants = inscriptionService.findByFormation(evaluation.getFormation().getId()).stream()
                    .filter(i -> i.getStatut() == StatutInscription.CONFIRMEE || i.getStatut() == StatutInscription.EN_COURS || i.getStatut() == StatutInscription.TERMINEE)
                    .map(Inscription::getStagiaire)
                    .distinct()
                    .toList();
        }

        return participants.stream().map(this::toSummary).toList();
    }

    private UtilisateurSummaryDTO toSummary(Utilisateur u) {
        UtilisateurSummaryDTO dto = new UtilisateurSummaryDTO();
        dto.setId(u.getId());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setPhotoProfil(u.getPhotoProfil());
        return dto;
    }

    // =====================
    // Saisie des notes
    // =====================

    public ResultatEvaluation enregistrerResultat(Long evaluationId, Long stagiaireId, BigDecimal note, Boolean absent, String commentaire) {
        Evaluation evaluation = findById(evaluationId);
        assertCanManageEvaluation(evaluation);
        assertDateEvaluationAtteinte(evaluation);

        if (evaluation.getEtat() == EtatEvaluation.TERMINEE) {
            throw new BadRequestException("Évaluation déjà publiée. Réouvrez-la pour modifier les notes.");
        }

        Utilisateur stagiaire = utilisateurService.findById(stagiaireId);

        // Normal: stagiaire doit être participant; Rattrapage: il doit être dans la liste des échoués
        if (evaluation.getSessionType() == SessionEvaluationType.RATTRAPAGE) {
            Evaluation parent = evaluation.getParentEvaluation();
            if (parent == null) throw new BadRequestException("Rattrapage invalide (évaluation parent manquante)");
            List<Long> failedIds = getFailedParticipants(parent).stream().map(Utilisateur::getId).toList();
            if (!failedIds.contains(stagiaireId)) {
                throw new BadRequestException("Ce stagiaire n'est pas concerné par le rattrapage");
            }
        } else {
            if (evaluation.getFormation() != null
                    && !inscriptionService.isStagiaireActifDansFormation(stagiaireId, evaluation.getFormation().getId())) {
                throw new BadRequestException("Ce stagiaire n'est pas inscrit dans cette formation");
            }
        }

        boolean isAbsent = Boolean.TRUE.equals(absent);
        if (!isAbsent) {
            if (note == null) {
                throw new BadRequestException("La note est obligatoire (ou cochez ABSENT)");
            }
            if (note.compareTo(BigDecimal.ZERO) < 0 || note.compareTo(BigDecimal.valueOf(20)) > 0) {
                throw new BadRequestException("La note doit être entre 0 et 20");
            }
        } else {
            if (note != null) {
                throw new BadRequestException("Si ABSENT = true, la note doit être vide");
            }
        }

        // Upsert
        ResultatEvaluation resultat = resultatRepository.findByEvaluationAndStagiaire(evaluation, stagiaire)
                .orElseGet(ResultatEvaluation::new);
        resultat.setEvaluation(evaluation);
        resultat.setStagiaire(stagiaire);
        resultat.setAbsent(isAbsent);
        resultat.setNote(note);
        resultat.setCommentaire(commentaire);
        if (isAbsent) {
            resultat.setReussi(false);
        } else {
            resultat.setReussi(note.compareTo(evaluation.getSeuilReussite()) >= 0);
        }

        ResultatEvaluation saved = resultatRepository.save(resultat);

        // IMPORTANT: certificat généré à la PUBLICATION (pas à la saisie)

        log.info("Résultat enregistré : {} ({}) pour {} - Note : {}/20",
                evaluation.getTitre(),
                evaluation.getSessionType(),
                stagiaire.getNomComplet(),
                isAbsent ? "ABSENT" : String.valueOf(note));

        return saved;
    }

    /**
     * Saisie en masse (UI pro): toutes les notes d'une évaluation en une seule opération.
     * Règles: chaque ligne doit être soit NOTE (0..20) soit ABSENT (note null).
     */
    public List<ResultatEvaluation> enregistrerResultatsBulk(Long evaluationId, List<com.formationcloud.platform.dto.EvaluationResultRequest> lignes) {
        if (lignes == null) return List.of();

        Evaluation evaluation = findById(evaluationId);
        assertCanManageEvaluation(evaluation);
        assertDateEvaluationAtteinte(evaluation);

        if (evaluation.getEtat() == EtatEvaluation.TERMINEE) {
            throw new BadRequestException("Évaluation déjà publiée. Réouvrez-la pour modifier les notes.");
        }

        List<ResultatEvaluation> saved = new ArrayList<>();
        for (var r : lignes) {
            if (r == null) continue;
            Long evId = (r.getEvaluationId() != null ? r.getEvaluationId() : evaluationId);
            if (!Objects.equals(evId, evaluationId)) {
                throw new BadRequestException("EvaluationId incohérent dans la saisie en masse");
            }
            // utilise la logique d'upsert/validation existante
            saved.add(enregistrerResultat(evaluationId, r.getStagiaireId(), r.getNote(), r.getAbsent(), r.getCommentaire()));
        }
        return saved;
    }

    public List<ResultatEvaluation> findResultatsByEvaluation(Long evaluationId) {
        Evaluation evaluation = findById(evaluationId);
        // Pour stagiaire, on ne renvoie que son résultat (privacy)
        var principal = SecurityUtils.currentUser();
        if (principal != null && "STAGIAIRE".equalsIgnoreCase(principal.getRole())) {
            return resultatRepository.findByEvaluation(evaluation).stream()
                    .filter(r -> r.getStagiaire() != null && Objects.equals(r.getStagiaire().getId(), principal.getId()))
                    .toList();
        }
        return resultatRepository.findByEvaluation(evaluation);
    }

    public List<ResultatEvaluation> findResultatsByStagiaire(Long stagiaireId) {
        Utilisateur stagiaire = utilisateurService.findById(stagiaireId);

        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (!SecurityUtils.isAdmin() && !Objects.equals(principal.getId(), stagiaireId)) {
            throw new AccessDeniedException("Accès interdit");
        }

        return resultatRepository.findByStagiaire(stagiaire);
    }

    // =====================
    // Publication des notes (terminer)
    // =====================

    public Map<String, Object> publierEvaluation(Long evaluationId, LocalDate dateRattrapage) {
        Evaluation evaluation = findById(evaluationId);
        assertCanManageEvaluation(evaluation);

        assertDateEvaluationAtteinte(evaluation);

        if (evaluation.getEtat() == EtatEvaluation.TERMINEE) {
            if (SecurityUtils.isAdmin()) {
                throw new BadRequestException("Évaluation déjà publiée. Réouvrez-la pour modifier les notes.");
            }
            throw new BadRequestException("Évaluation déjà publiée.");
        }

        assertNormalReopenAllowed(evaluation);

        // Vérifier que toutes les notes sont saisies (note OU absent)
        List<Utilisateur> expected = expectedParticipants(evaluation);
        List<ResultatEvaluation> results = resultatRepository.findByEvaluation(evaluation);

        Map<Long, ResultatEvaluation> byStagiaireId = results.stream()
                .filter(r -> r.getStagiaire() != null && r.getStagiaire().getId() != null)
                .collect(Collectors.toMap(
                        r -> r.getStagiaire().getId(),
                        r -> r,
                        (a, b) -> a
                ));

        List<Utilisateur> missing = expected.stream().filter(u -> {
            ResultatEvaluation r = byStagiaireId.get(u.getId());
            if (r == null) return true;
            boolean absent = Boolean.TRUE.equals(r.getAbsent());
            boolean hasNote = r.getNote() != null;
            // il faut exactement: absent==true OU note!=null
            return !((absent && !hasNote) || (!absent && hasNote));
        }).toList();

        if (!missing.isEmpty()) {
            String noms = missing.stream().map(Utilisateur::getNomComplet).limit(8).collect(Collectors.joining(", "));
            throw new BadRequestException("Notes manquantes pour: " + noms + (missing.size() > 8 ? "..." : ""));
        }

        Long rattrapageId = null;
        int failedCount = 0;
        int finalFailCount = 0;
        int certificatsGeneres = 0;

        // Pré-check: si NORMAL et il y a des échoués/absents => date rattrapage obligatoire
        if (evaluation.getSessionType() == SessionEvaluationType.NORMAL) {
            List<Utilisateur> failedPre = getFailedParticipants(evaluation);
            if (!failedPre.isEmpty() && dateRattrapage == null) {
                throw new BadRequestException("Date de rattrapage obligatoire");
            }
        }

        // On marque l'évaluation terminée seulement après les validations.
        evaluation.setEtat(EtatEvaluation.TERMINEE);
        evaluation.setDatePublicationNotes(LocalDateTime.now());
        evaluationRepository.save(evaluation);

        if (evaluation.getSessionType() == SessionEvaluationType.NORMAL) {
            // 1) Générer certificats + marquer réussite
            for (ResultatEvaluation r : results) {
                if (r == null || r.getStagiaire() == null) continue;
                if (Boolean.TRUE.equals(r.getAbsent())) {
                    certificatService.revoquerSiExiste(r.getStagiaire(), evaluation.getFormation());
                    continue;
                }
                if (r.isReussi()) {
                    certificatService.genererCertificatAutomatique(r);
                    certificatsGeneres++;
                    try {
                        inscriptionService.marquerReussiteFinale(evaluation.getFormation().getId(), r.getStagiaire().getId());
                    } catch (Exception ex) {
                        log.warn("Impossible de marquer TERMINEE pour stagiaire {}: {}", r.getStagiaire().getId(), ex.getMessage());
                    }
                } else {
                    certificatService.revoquerSiExiste(r.getStagiaire(), evaluation.getFormation());
                }
            }

            // 2) Rattrapage pour échoués/absents
            List<Utilisateur> failed = getFailedParticipants(evaluation);
            failedCount = failed.size();

            if (!failed.isEmpty()) {
                Evaluation rattrapage = getOrCreateRattrapage(evaluation, failed, dateRattrapage);
                rattrapageId = rattrapage.getId();
            }
        } else if (evaluation.getSessionType() == SessionEvaluationType.RATTRAPAGE) {
            // ceux qui échouent au rattrapage => doit repasser formation
            List<ResultatEvaluation> res = resultatRepository.findByEvaluation(evaluation);
            for (ResultatEvaluation r : res) {
                if (r == null || r.getStagiaire() == null) continue;
                if (Boolean.TRUE.equals(r.getAbsent())) {
                    certificatService.revoquerSiExiste(r.getStagiaire(), evaluation.getFormation());
                    continue;
                }
                if (r.isReussi()) {
                    certificatService.genererCertificatAutomatique(r);
                    certificatsGeneres++;
                    try {
                        inscriptionService.marquerReussiteFinale(evaluation.getFormation().getId(), r.getStagiaire().getId());
                    } catch (Exception ex) {
                        log.warn("Impossible de marquer TERMINEE pour stagiaire {}: {}", r.getStagiaire().getId(), ex.getMessage());
                    }
                } else {
                    certificatService.revoquerSiExiste(r.getStagiaire(), evaluation.getFormation());
                }
            }

            List<ResultatEvaluation> fails = res.stream().filter(r -> r != null && (Boolean.TRUE.equals(r.getAbsent()) || !Boolean.TRUE.equals(r.getReussi()))).toList();
            finalFailCount = fails.size();

            for (ResultatEvaluation r : fails) {
                if (r.getStagiaire() == null) continue;
                try {
                    inscriptionService.marquerEchecFinal(
                            evaluation.getFormation().getId(),
                            r.getStagiaire().getId(),
                            "Échec rattrapage"
                    );
                } catch (Exception ex) {
                    log.warn("Impossible de marquer l'échec final pour stagiaire {} : {}", r.getStagiaire().getId(), ex.getMessage());
                }
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("evaluationId", evaluation.getId());
        resp.put("etat", evaluation.getEtat().name());
        resp.put("sessionType", evaluation.getSessionType().name());
        resp.put("rattrapageId", rattrapageId);
        resp.put("failedCount", failedCount);
        resp.put("finalFailCount", finalFailCount);
        resp.put("certificatsGeneres", certificatsGeneres);
        return resp;
    }

    private List<Utilisateur> expectedParticipants(Evaluation evaluation) {
        if (evaluation.getSessionType() == SessionEvaluationType.RATTRAPAGE) {
            Evaluation parent = evaluation.getParentEvaluation();
            if (parent == null) return List.of();
            return getFailedParticipants(parent);
        }
        return inscriptionService.findByFormation(evaluation.getFormation().getId()).stream()
                .filter(i -> i.getStatut() == StatutInscription.CONFIRMEE || i.getStatut() == StatutInscription.EN_COURS || i.getStatut() == StatutInscription.TERMINEE)
                .map(Inscription::getStagiaire)
                .distinct()
                .toList();
    }

    private List<Utilisateur> getFailedParticipants(Evaluation evaluationNormal) {
        List<ResultatEvaluation> results = resultatRepository.findByEvaluation(evaluationNormal);
        return results.stream()
                .filter(r -> r.getStagiaire() != null)
                .filter(r -> !Boolean.TRUE.equals(r.isReussi()))
                .map(ResultatEvaluation::getStagiaire)
                .distinct()
                .toList();
    }

    private Evaluation getOrCreateRattrapage(Evaluation normal, List<Utilisateur> failed, LocalDate dateRattrapage) {
        // déjà existant ?
        List<Evaluation> existing = evaluationRepository.findByParentEvaluation_Id(normal.getId());
        Optional<Evaluation> rOpt = existing.stream()
                .filter(e -> e.getSessionType() == SessionEvaluationType.RATTRAPAGE)
                .findFirst();

        if (rOpt.isPresent()) {
            Evaluation existingR = rOpt.get();
            if (existingR.getEtat() != EtatEvaluation.TERMINEE) {
                // on met à jour la date choisie par l'admin/formateur
                if (dateRattrapage != null) {
                    existingR.setDateEvaluation(dateRattrapage);
                    evaluationRepository.save(existingR);
                }
            }
            return existingR;
        }

        Evaluation r = new Evaluation();
        r.setFormation(normal.getFormation());
        r.setParentEvaluation(normal);
        r.setSessionType(SessionEvaluationType.RATTRAPAGE);
        r.setEtat(EtatEvaluation.EN_COURS);
        r.setTitre(normal.getTitre() + " - Rattrapage");
        r.setDescription("Session de rattrapage suite à l'évaluation normale.");
        r.setSeuilReussite(normal.getSeuilReussite());

        // Date de rattrapage choisie au moment de la publication.
        // Fallback: +7 jours si non fourni.
        LocalDate chosen = dateRattrapage;
        if (chosen == null) {
            LocalDate baseDate = (normal.getDatePublicationNotes() != null)
                    ? normal.getDatePublicationNotes().toLocalDate()
                    : LocalDate.now();
            chosen = baseDate.plusDays(7);
        }
        r.setDateEvaluation(chosen);

        Evaluation saved = evaluationRepository.save(r);

        // Notif seulement les concernés
        notificationService.envoyerNotificationEvaluationDisponible(saved, failed);

        log.info("Rattrapage auto créé pour {} stagiaires, evalNormal={}, evalRattrapage={}",
                failed.size(), normal.getId(), saved.getId());

        return saved;
    }

    // =====================
    // Helpers sécurité
    // =====================

    private void assertDateEvaluationAtteinte(Evaluation evaluation) {
        if (evaluation.getDateEvaluation() != null) {
            LocalDate today = LocalDate.now();
            if (today.isBefore(evaluation.getDateEvaluation())) {
                throw new BadRequestException("Impossible avant la date de l'évaluation (" + evaluation.getDateEvaluation() + ")");
            }
        }
    }

    /**
     * Retourne l'évaluation enfant de type RATTRAPAGE pour une évaluation NORMAL, si elle existe.
     */
    private Optional<Evaluation> findRattrapageChild(Evaluation normalEval) {
        if (normalEval == null || normalEval.getId() == null) return Optional.empty();
        if (normalEval.getSessionType() != SessionEvaluationType.NORMAL) return Optional.empty();
        List<Evaluation> children = evaluationRepository.findByParentEvaluation_Id(normalEval.getId());
        return children.stream()
                .filter(e -> e != null && e.getSessionType() == SessionEvaluationType.RATTRAPAGE)
                .findFirst();
    }

    /**
     * Bloque la réouverture/republikation d'une évaluation NORMAL si un rattrapage lié existe déjà et sa date est passée.
     */
    private void assertNormalReopenAllowed(Evaluation evaluation) {
        if (evaluation == null || evaluation.getSessionType() != SessionEvaluationType.NORMAL) return;
        Optional<Evaluation> rattrapageOpt = findRattrapageChild(evaluation);
        if (rattrapageOpt.isEmpty()) return;
        Evaluation rattrapage = rattrapageOpt.get();
        if (rattrapage.getDateEvaluation() != null && rattrapage.getDateEvaluation().isBefore(LocalDate.now())) {
            throw new BadRequestException("Impossible de republier l'évaluation normale : un rattrapage déjà passé existe. Republiez le rattrapage.");
        }
    }

    private void assertCanAccessEvaluation(Evaluation evaluation) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (SecurityUtils.isAdmin()) return;

        String role = (principal.getRole() == null ? "" : principal.getRole().toUpperCase());

        if ("FORMATEUR".equals(role)) {
            Long ownerId = (evaluation.getFormation() != null && evaluation.getFormation().getFormateur() != null)
                    ? evaluation.getFormation().getFormateur().getId()
                    : null;
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("Accès interdit");
            }
            return;
        }

        if ("STAGIAIRE".equals(role)) {
            Long formationId = (evaluation.getFormation() != null ? evaluation.getFormation().getId() : null);
            if (formationId == null) throw new AccessDeniedException("Accès interdit");
            if (!inscriptionService.isStagiaireActifDansFormation(principal.getId(), formationId)) {
                throw new AccessDeniedException("Accès interdit");
            }
            return;
        }

        throw new AccessDeniedException("Accès interdit");
    }

    private void assertCanManageEvaluation(Evaluation evaluation) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (SecurityUtils.isAdmin()) return;

        if (!"FORMATEUR".equalsIgnoreCase(principal.getRole())) {
            throw new AccessDeniedException("Accès interdit");
        }

        Long ownerId = (evaluation.getFormation() != null && evaluation.getFormation().getFormateur() != null)
                ? evaluation.getFormation().getFormateur().getId()
                : null;

        if (ownerId == null || !ownerId.equals(principal.getId())) {
            throw new AccessDeniedException("Accès interdit");
        }
    }

    // =====================
    // Réouverture (admin uniquement)
    // =====================

    /**
     * Réouvre une évaluation TERMINEE pour permettre la modification des notes.
     * Seul un ADMIN peut réouvrir. Les certificats VALIDE de la formation sont révoqués.
     */
    public Evaluation reouvrirEvaluation(Long evaluationId) {
        Evaluation evaluation = findById(evaluationId);

        var principal = SecurityUtils.currentUser();
        if (principal == null || !SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Seul un administrateur peut réouvrir une évaluation.");
        }

        if (evaluation.getEtat() != EtatEvaluation.TERMINEE) {
            return evaluation; // déjà ouverte, rien à faire
        }

        assertNormalReopenAllowed(evaluation);

        evaluation.setEtat(EtatEvaluation.EN_COURS);
        evaluation.setDatePublicationNotes(null);
        evaluationRepository.save(evaluation);

        // Révoquer tous les certificats VALIDE liés à cette formation
        Long formationId = (evaluation.getFormation() != null ? evaluation.getFormation().getId() : null);
        if (formationId != null) {
            int revoked = certificatService.revoquerTousLesCertificatsValidesDeFormation(formationId);
            log.info("Réouverture évaluation {} : {} certificat(s) révoqué(s)", evaluationId, revoked);
        }

        log.info("Évaluation {} réouverte par admin {}", evaluationId, principal.getId());
        return evaluation;
    }

    // =====================
    // Dashboard helper
    // =====================

    public Double getMoyenneNotesByFormation(Long formationId) {
        return resultatRepository.findMoyenneNotesByFormation(formationId);
    }
}
