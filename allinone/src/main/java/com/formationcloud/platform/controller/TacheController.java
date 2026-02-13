package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.ProgressionRequest;
import com.formationcloud.platform.dto.TacheDTO;
import com.formationcloud.platform.dto.TacheRequest;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.model.Tache;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.service.TacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.formationcloud.platform.security.SecurityUtils;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TacheController {

    private final TacheService tacheService;

    // ======================
    // Compat Front: Tâches
    // ======================

    @GetMapping("/taches")
    public ResponseEntity<List<TacheDTO>> getAllTaches() {
        return ResponseEntity.ok(tacheService.findAll().stream().map(this::toDTO).toList());
    }

    @GetMapping("/taches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<TacheDTO> getTacheById(@PathVariable Long id) {
        Tache t = tacheService.findById(id);

        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        // Un stagiaire ne peut lire que SES tâches
        if ("STAGIAIRE".equalsIgnoreCase(principal.getRole())) {
            Long ownerId = (t.getStagiaire() != null ? t.getStagiaire().getId() : null);
            SecurityUtils.assertAdminOrSelf(ownerId);
        }

        return ResponseEntity.ok(toDTO(t));
    }

    @GetMapping("/taches/utilisateur/{userId}")
    public ResponseEntity<List<TacheDTO>> getTachesByUser(@PathVariable Long userId) {
        SecurityUtils.assertAdminOrSelf(userId);
        return ResponseEntity.ok(tacheService.findByStagiaire(userId).stream().map(this::toDTO).toList());
    }

    @PostMapping("/taches")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<TacheDTO> createTache(@Valid @RequestBody TacheRequest request) {
        Tache t = new Tache();
        t.setTitre(request.getTitre());
        t.setDescription(request.getDescription());
        t.setDateDebut(request.getDateDebut());
        t.setDateFin(request.getDateFin());

        Tache created = tacheService.createTache(t, request.getUtilisateurId());
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/taches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<TacheDTO> updateTache(@PathVariable Long id, @Valid @RequestBody TacheRequest request) {
        Tache t = new Tache();
        t.setTitre(request.getTitre());
        t.setDescription(request.getDescription());
        t.setDateDebut(request.getDateDebut());
        t.setDateFin(request.getDateFin());

        Tache updated = tacheService.updateTache(id, t);
        return ResponseEntity.ok(toDTO(updated));
    }

    @PutMapping("/taches/{id}/progression")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAGIAIRE')")
    public ResponseEntity<TacheDTO> updateProgress(@PathVariable Long id, @Valid @RequestBody ProgressionRequest request) {
        Long ownerId = tacheService.findById(id).getStagiaire().getId();
        SecurityUtils.assertAdminOrSelf(ownerId);
        Tache updated = tacheService.updatePourcentage(id, request.getProgression());
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/taches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Void> deleteTache(@PathVariable Long id) {
        tacheService.deleteTache(id);
        return ResponseEntity.ok().build();
    }

    // ======================
    // Legacy endpoints kept
    // ======================

    // ======================

private TacheDTO toDTO(Tache t) {
        TacheDTO dto = new TacheDTO();
        dto.setId(t.getId());
        dto.setTitre(t.getTitre());
        dto.setDescription(t.getDescription());
        dto.setDateDebut(t.getDateDebut());
        dto.setDateFin(t.getDateFin());
        dto.setStatut(t.getStatut());
        dto.setProgression(t.getPourcentageAccomplissement());

        Utilisateur stagiaire = t.getStagiaire();
        if (stagiaire != null) {
            UtilisateurSummaryDTO u = new UtilisateurSummaryDTO();
            u.setId(stagiaire.getId());
            u.setNom(stagiaire.getNom());
            u.setPrenom(stagiaire.getPrenom());
            u.setEmail(stagiaire.getEmail());
            u.setRole(stagiaire.getRole());
            dto.setUtilisateur(u);
        }

        return dto;
    }
}