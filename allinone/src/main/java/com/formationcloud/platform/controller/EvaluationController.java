package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.EvaluationDTO;
import com.formationcloud.platform.dto.EvaluationRequest;
import com.formationcloud.platform.dto.EvaluationResultRequest;
import com.formationcloud.platform.dto.PublishEvaluationRequest;
import com.formationcloud.platform.dto.FormationSummaryDTO;
import com.formationcloud.platform.model.Evaluation;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.ResultatEvaluation;
import com.formationcloud.platform.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import com.formationcloud.platform.security.SecurityUtils;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // ======================
    // Compat Front: Evaluations
    // ======================

    @GetMapping("/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<List<EvaluationDTO>> getAllEvaluations() {
        return ResponseEntity.ok(evaluationService.findAllForCurrentUser().stream().map(this::toDTO).toList());
    }

    @PostMapping("/evaluations/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<?> publishEvaluation(@PathVariable Long id,
                                               @RequestBody(required = false) PublishEvaluationRequest request) {
        return ResponseEntity.ok(evaluationService.publierEvaluation(
                id,
                request != null ? request.getDateRattrapage() : null
        ));
    }

    // Alias FR (si le front appelle /publier)
    @PostMapping("/evaluations/{id}/publier")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<?> publierEvaluation(@PathVariable Long id,
                                               @RequestBody(required = false) PublishEvaluationRequest request) {
        return publishEvaluation(id, request);
    }

    @PostMapping("/evaluations/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EvaluationDTO> reopenEvaluation(@PathVariable Long id) {
        Evaluation reopened = evaluationService.reouvrirEvaluation(id);
        return ResponseEntity.ok(toDTO(reopened));
    }

    @GetMapping("/evaluations/{id}/participants")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<?> getParticipantsForEvaluation(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getParticipantsForEvaluation(id));
    }

    @GetMapping("/evaluations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<EvaluationDTO> getEvaluationById(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(evaluationService.findById(id)));
    }

    @GetMapping("/evaluations/formation/{formationId}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<List<EvaluationDTO>> getEvaluationsByFormation(@PathVariable Long formationId) {
        return ResponseEntity.ok(evaluationService.findByFormation(formationId).stream().map(this::toDTO).toList());
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<EvaluationDTO> createEvaluation(@Valid @RequestBody EvaluationRequest request) {
        Evaluation evaluation = new Evaluation();
        evaluation.setTitre(request.getTitre());
        evaluation.setDescription(request.getDescription());
        evaluation.setSeuilReussite(request.getSeuilReussite());
        evaluation.setDateEvaluation(request.getDateLimite());
        if (request.getSessionType() != null && !request.getSessionType().isBlank()) {
            evaluation.setSessionType(com.formationcloud.platform.model.SessionEvaluationType.valueOf(request.getSessionType().trim().toUpperCase()));
        }

        Evaluation saved = evaluationService.createEvaluation(evaluation, request.getFormationId());
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/evaluations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<EvaluationDTO> updateEvaluation(@PathVariable Long id, @Valid @RequestBody EvaluationRequest request) {
        Evaluation evaluation = new Evaluation();
        evaluation.setTitre(request.getTitre());
        evaluation.setDescription(request.getDescription());
        evaluation.setSeuilReussite(request.getSeuilReussite());
        evaluation.setDateEvaluation(request.getDateLimite());
        if (request.getSessionType() != null && !request.getSessionType().isBlank()) {
            evaluation.setSessionType(com.formationcloud.platform.model.SessionEvaluationType.valueOf(request.getSessionType().trim().toUpperCase()));
        }

        Evaluation updated = evaluationService.updateEvaluation(id, evaluation);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/evaluations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Void> deleteEvaluation(@PathVariable Long id) {
        evaluationService.deleteEvaluation(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/evaluations/resultats")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<ResultatEvaluation> submitResult(@Valid @RequestBody EvaluationResultRequest request) {
        return ResponseEntity.ok(evaluationService.enregistrerResultat(
                request.getEvaluationId(),
                request.getStagiaireId(),
                request.getNote(),
                request.getAbsent(),
                request.getCommentaire()
        ));
    }

    /**
     * Saisie en masse des résultats (UI pro: tableau complet).
     * Body: liste de {evaluationId, stagiaireId, note|null, absent, commentaire}
     */
    @PostMapping("/evaluations/{evaluationId}/resultats/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<ResultatEvaluation>> submitResultsBulk(@PathVariable Long evaluationId,
                                                                      @Valid @RequestBody List<@Valid EvaluationResultRequest> requests) {
        // Sécurise: si un client oublie evaluationId dans certains items, on le force.
        List<EvaluationResultRequest> normalized = new ArrayList<>();
        for (EvaluationResultRequest r : (requests == null ? List.<EvaluationResultRequest>of() : requests)) {
            if (r == null) continue;
            if (r.getEvaluationId() == null) r.setEvaluationId(evaluationId);
            normalized.add(r);
        }
        return ResponseEntity.ok(evaluationService.enregistrerResultatsBulk(evaluationId, normalized));
    }

    @GetMapping("/evaluations/{evaluationId}/resultats")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<List<ResultatEvaluation>> getResultatsByEvaluation(@PathVariable Long evaluationId) {
        return ResponseEntity.ok(evaluationService.findResultatsByEvaluation(evaluationId));
    }

        @PreAuthorize("hasAnyRole('ADMIN','STAGIAIRE')")
@GetMapping("/evaluations/resultats/utilisateur/{userId}")
    public ResponseEntity<List<ResultatEvaluation>> getResultatsByUser(@PathVariable Long userId) {
        SecurityUtils.assertAdminOrSelf(userId);
        return ResponseEntity.ok(evaluationService.findResultatsByStagiaire(userId));
    }

    // ======================
    // Legacy endpoints kept
    // ======================

    @GetMapping("/formation/{formationId}/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'STAGIAIRE')")
    public ResponseEntity<List<Evaluation>> getEvaluationsByFormationLegacy(@PathVariable Long formationId) {
        return ResponseEntity.ok(evaluationService.findByFormation(formationId));
    }

    @GetMapping("/formateur/{formateurId}/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Evaluation>> getEvaluationsByFormateurLegacy(@PathVariable Long formateurId) {
        return ResponseEntity.ok(evaluationService.findByFormateur(formateurId));
    }

    @PostMapping("/formateur/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Evaluation> createEvaluationLegacy(@Valid @RequestBody Evaluation evaluation, @RequestParam Long formationId) {
        return ResponseEntity.ok(evaluationService.createEvaluation(evaluation, formationId));
    }

    @PutMapping("/formateur/evaluations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Evaluation> updateEvaluationLegacy(@PathVariable Long id, @Valid @RequestBody Evaluation evaluation) {
        return ResponseEntity.ok(evaluationService.updateEvaluation(id, evaluation));
    }

    @PostMapping("/formateur/evaluations/{evaluationId}/resultats")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<ResultatEvaluation> enregistrerResultatLegacy(@PathVariable Long evaluationId,
                                                                       @RequestParam Long stagiaireId,
                                                                       @RequestParam(required = false) Double note,
                                                                       @RequestParam(required = false, defaultValue = "false") Boolean absent,
                                                                       @RequestParam(required = false) String commentaire) {
        return ResponseEntity.ok(evaluationService.enregistrerResultat(evaluationId, stagiaireId, note != null ? BigDecimal.valueOf(note) : null, absent, commentaire));
    }

    @GetMapping("/stagiaire/{stagiaireId}/resultats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAGIAIRE')")
    public ResponseEntity<List<ResultatEvaluation>> getResultatsByStagiaireLegacy(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(evaluationService.findResultatsByStagiaire(stagiaireId));
    }

    @DeleteMapping("/formateur/evaluations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Void> deleteEvaluationLegacy(@PathVariable Long id) {
        evaluationService.deleteEvaluation(id);
        return ResponseEntity.ok().build();
    }

    private EvaluationDTO toDTO(Evaluation e) {
        EvaluationDTO dto = new EvaluationDTO();
        dto.setId(e.getId());
        dto.setTitre(e.getTitre());
        dto.setDescription(e.getDescription());
        dto.setSeuilReussite(e.getSeuilReussite());
        dto.setDateLimite(e.getDateEvaluation());
        dto.setSessionType(e.getSessionType() == null ? null : e.getSessionType().name());
        dto.setEtat(e.getEtat() == null ? null : e.getEtat().name());
        dto.setParentEvaluationId(e.getParentEvaluation() != null ? e.getParentEvaluation().getId() : null);
        dto.setDatePublicationNotes(e.getDatePublicationNotes());

        // Stats "UI pro" : utiles surtout pour ADMIN/FORMATEUR.
        // On évite de surcharger la vue STAGIAIRE.
        var principal = SecurityUtils.currentUser();
        boolean isStagiaire = principal != null && "STAGIAIRE".equalsIgnoreCase(principal.getRole());
        if (!isStagiaire) {
            var s = evaluationService.computeStats(e);
            if (s != null) {
                dto.setParticipantsTotal(s.getParticipantsTotal());
                dto.setNotesSaisies(s.getNotesSaisies());
                dto.setNotesManquantes(s.getNotesManquantes());
                dto.setValidesCount(s.getValidesCount());
                dto.setEchecsCount(s.getEchecsCount());
                dto.setAbsentsCount(s.getAbsentsCount());
                dto.setRattrapageId(s.getRattrapageId());
            }
        }

        Formation f = e.getFormation();
        if (f != null) {
            FormationSummaryDTO s = new FormationSummaryDTO();
            s.setId(f.getId());
            s.setTitre(f.getNom());
            s.setType(f.getType());
            s.setDateDebut(f.getDateDebut());
            s.setDateFin(f.getDateFin());
            s.setStatut(f.getStatut());
            dto.setFormation(s);
        }
        return dto;
    }
}