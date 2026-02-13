package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.RegisterRequest;
import com.formationcloud.platform.dto.UtilisateurDTO;
import com.formationcloud.platform.dto.UtilisateurManageRequest;
import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    // ======================
    // Compat Front: Utilisateurs
    // ======================

    @GetMapping("/utilisateurs")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'STAGIAIRE')")
    public ResponseEntity<List<UtilisateurDTO>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.findAll().stream().map(this::toDTO).toList());
    }

    @GetMapping("/utilisateurs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'STAGIAIRE')")
    public ResponseEntity<UtilisateurDTO> getUtilisateurById(@PathVariable Long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        utilisateurService.assertCanViewUser(principal.getId(), Role.valueOf(principal.getRole()), id);
        return ResponseEntity.ok(toDTO(utilisateurService.findById(id)));
    }

    @PostMapping("/utilisateurs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurDTO> createUtilisateur(@Valid @RequestBody RegisterRequest request) {
        Utilisateur created = utilisateurService.createUtilisateur(request);
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/utilisateurs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<UtilisateurDTO> updateUtilisateur(@PathVariable Long id, @Valid @RequestBody UtilisateurManageRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Utilisateur updated = utilisateurService.updateUtilisateurManaged(principal.getId(), Role.valueOf(principal.getRole()), id, request);
        return ResponseEntity.ok(toDTO(updated));
    }

    @PutMapping("/utilisateurs/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> validerUtilisateurCompat(@PathVariable Long id) {
        utilisateurService.validerUtilisateur(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/utilisateurs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<Void> deleteUtilisateurCompat(@PathVariable Long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        utilisateurService.deleteUtilisateurManaged(principal.getId(), Role.valueOf(principal.getRole()), id);
        return ResponseEntity.ok().build();
    }

    // ======================
    // Legacy endpoints kept
    // ======================
    @GetMapping("/utilisateurs/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Utilisateur>> getUtilisateursByRole(@PathVariable Role role) {
        return ResponseEntity.ok(utilisateurService.findByRole(role));
    }

    @GetMapping("/formateurs/valides")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UtilisateurDTO>> getFormateursValides() {
        // Retour DTO pour éviter la sérialisation infinie (relations JPA)
        return ResponseEntity.ok(utilisateurService.findFormateursValides().stream().map(this::toDTO).toList());
    }

    @GetMapping("/stagiaires/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<UtilisateurDTO>> getStagiairesActifs() {
        // Retour DTO pour éviter la sérialisation infinie (relations JPA)
        return ResponseEntity.ok(utilisateurService.findStagiairesActifs().stream().map(this::toDTO).toList());
    }

    @PutMapping("/admin/utilisateurs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Utilisateur> updateUtilisateurLegacy(@PathVariable Long id, @RequestBody Utilisateur utilisateur) {
        return ResponseEntity.ok(utilisateurService.updateUtilisateur(id, utilisateur));
    }

    @PostMapping("/admin/utilisateurs/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> validerUtilisateurLegacy(@PathVariable Long id) {
        utilisateurService.validerUtilisateur(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/utilisateurs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUtilisateurLegacy(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.ok().build();
    }

    private UtilisateurDTO toDTO(Utilisateur u) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(u.getId());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setTypeFormateur(u.getTypeFormateur());
        dto.setPhotoProfil(u.getPhotoProfil());
        dto.setValide(u.getStatutValidation());
        dto.setDateCreation(u.getDateCreation());
        return dto;
    }
}
