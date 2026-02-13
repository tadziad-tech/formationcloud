package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.FormationSummaryDTO;
import com.formationcloud.platform.dto.InscriptionDTO;
import com.formationcloud.platform.dto.InscriptionRequest;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Inscription;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.InscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.formationcloud.platform.security.SecurityUtils;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final FormationAccessService formationAccessService;

    // ======================
    // Compat Front: Inscriptions
    // ======================

        @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
@GetMapping("/inscriptions")
    public ResponseEntity<List<InscriptionDTO>> getAllInscriptions() {
        return ResponseEntity.ok(inscriptionService.findAll().stream().map(this::toDTO).toList());
    }

        @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
@GetMapping("/inscriptions/{id}")
    public ResponseEntity<InscriptionDTO> getInscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(inscriptionService.findById(id)));
    }

    @GetMapping("/inscriptions/utilisateur/{userId}")
    public ResponseEntity<List<InscriptionDTO>> getInscriptionsByUser(@PathVariable Long userId) {
        SecurityUtils.assertAdminOrSelf(userId);
        return ResponseEntity.ok(inscriptionService.findByStagiaire(userId).stream().map(this::toDTO).toList());
    }

    /**
     * Statut d'inscription de l'utilisateur courant pour une formation.
     * Retourne 200 + DTO si une inscription existe, 404 sinon.
     */
    @GetMapping("/inscriptions/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InscriptionDTO> getMyInscriptionForFormation(@RequestParam Long formationId) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return inscriptionService.findByFormationAndStagiaire(formationId, principal.getId())
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    @GetMapping("/inscriptions/formation/{formationId}")
    public ResponseEntity<List<InscriptionDTO>> getInscriptionsByFormation(@PathVariable Long formationId) {
        formationAccessService.assertAdminOrAssignedFormateur(formationId);
        List<Inscription> list = inscriptionService.findByFormation(formationId);
        return ResponseEntity.ok(list.stream().map(this::toDTO).toList());
    }

    @PostMapping("/inscriptions")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<InscriptionDTO> createInscription(@Valid @RequestBody InscriptionRequest request) {
        // IMPORTANT SÉCURITÉ:
        // Le stagiaire ne doit PAS pouvoir inscrire un autre utilisateur en changeant utilisateurId côté front.
        // On ignore utilisateurId et on utilise l'utilisateur authentifié.
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Inscription created = inscriptionService.inscrireStagiaire(principal.getId(), request.getFormationId());
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/inscriptions/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<InscriptionDTO> validerInscription(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(inscriptionService.validerInscription(id)));
    }

    @PutMapping("/inscriptions/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<InscriptionDTO> refuserInscription(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(inscriptionService.refuserInscription(id, "Refusée")));
    }

    @DeleteMapping("/inscriptions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAGIAIRE')")
    public ResponseEntity<Void> deleteInscription(@PathVariable Long id) {
        inscriptionService.annulerInscription(id);
        return ResponseEntity.ok().build();
    }

    // ======================

    private InscriptionDTO toDTO(Inscription inscription) {
        InscriptionDTO dto = new InscriptionDTO();
        dto.setId(inscription.getId());
        dto.setDateInscription(inscription.getDateInscription());
        dto.setStatut(inscription.getStatut());

        Utilisateur stagiaire = inscription.getStagiaire();
        if (stagiaire != null) {
            dto.setUtilisateur(toUtilisateurSummaryDTO(stagiaire));
        }

        Formation formation = inscription.getFormation();
        if (formation != null) {
            dto.setFormation(toFormationSummaryDTO(formation));
        }

        return dto;
    }

    private UtilisateurSummaryDTO toUtilisateurSummaryDTO(Utilisateur u) {
        UtilisateurSummaryDTO dto = new UtilisateurSummaryDTO();
        dto.setId(u.getId());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setPhotoProfil(u.getPhotoProfil());
        return dto;
    }

    private FormationSummaryDTO toFormationSummaryDTO(Formation formation) {
        FormationSummaryDTO s = new FormationSummaryDTO();
        s.setId(formation.getId());
        s.setTitre(formation.getNom());
        s.setType(formation.getType());
        s.setDateDebut(formation.getDateDebut());
        s.setDateFin(formation.getDateFin());
        s.setStatut(formation.getStatut());
        return s;
    }
}
