package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.CategorieDTO;
import com.formationcloud.platform.dto.FormationDTO;
import com.formationcloud.platform.dto.FormationAccessDTO;
import com.formationcloud.platform.dto.FormationMeAccessDTO;
import com.formationcloud.platform.dto.FormationRequest;
import com.formationcloud.platform.dto.FormationStatusRequest;
import com.formationcloud.platform.dto.AddParticipantRequest;
import com.formationcloud.platform.dto.FormationSummaryDTO;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.Categorie;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.repository.CategorieRepository;
import com.formationcloud.platform.repository.FormationRepository;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.FormationService;
import com.formationcloud.platform.service.InscriptionService;
import com.formationcloud.platform.security.SecurityUtils;

import java.math.BigDecimal;
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
public class FormationController {

    private final FormationService formationService;
    private final FormationAccessService formationAccessService;
    private final CategorieRepository categorieRepository;
    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final InscriptionService inscriptionService;

    // ======================
    // Compat Front: Formations
    // ======================

    @GetMapping("/formations")
    public ResponseEntity<List<FormationDTO>> getAllFormations() {
        return ResponseEntity.ok(formationService.findAll().stream().map(this::toDTO).toList());
    }

    @GetMapping("/formations/{id}")
    public ResponseEntity<FormationDTO> getFormationById(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(formationService.findById(id)));
    }

    @GetMapping("/formations/{id}/access")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FormationAccessDTO> getFormationAccess(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getFormationAccess(id));
    }

    @GetMapping("/formations/{id}/me-access")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FormationMeAccessDTO> getFormationMeAccess(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getMeAccess(id));
    }

    @GetMapping("/formations/actives")
    public ResponseEntity<List<FormationDTO>> getFormationsActives() {
        return ResponseEntity.ok(formationService.findFormationsActives().stream().map(this::toDTO).toList());
    }

    @GetMapping("/formations/a-venir")
    public ResponseEntity<List<FormationDTO>> getFormationsAVenir() {
        return ResponseEntity.ok(formationService.findFormationsAVenir().stream().map(this::toDTO).toList());
    }

    @GetMapping("/formations/en-cours")
    public ResponseEntity<List<FormationDTO>> getFormationsEnCours() {
        return ResponseEntity.ok(formationService.findFormationsEnCours().stream().map(this::toDTO).toList());
    }

    @GetMapping("/formations/categorie/{categorieId}")
    public ResponseEntity<List<FormationDTO>> getFormationsByCategorie(@PathVariable Long categorieId) {
		return ResponseEntity.ok(formationRepository.findByCategorieIdAndActive(categorieId).stream().map(this::toDTO).toList());
    }

    @GetMapping("/formations/categories")
    public ResponseEntity<List<CategorieDTO>> getCategories() {
        return ResponseEntity.ok(categorieRepository.findAll().stream().map(this::toCategorieDTO).toList());
    }

    @PostMapping("/formations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormationDTO> createFormation(@Valid @RequestBody FormationRequest request) {
        if (request.getFormateurId() == null) {
            throw new BadRequestException("Veuillez sélectionner un formateur");
        }

        Formation formation = fromRequest(request);
        Formation saved = formationService.createFormation(formation, request.getFormateurId());
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/formations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormationDTO> updateFormation(@PathVariable Long id, @Valid @RequestBody FormationRequest request) {
        Formation details = fromRequest(request);

        // update core fields
        Formation updated = formationService.updateFormation(id, details);

        // optional: change statut if provided
        if (request.getStatut() != null) {
            formationService.changerStatut(id, request.getStatut());
            updated = formationService.findById(id);
        }

        // optional: assign trainer
        if (request.getFormateurId() != null) {
            formationService.assignerFormateur(id, request.getFormateurId());
            updated = formationService.findById(id);
        }

        return ResponseEntity.ok(toDTO(updated));
    }

	@PutMapping("/formations/{id}/statut")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<FormationDTO> updateFormationStatus(@PathVariable Long id, @RequestBody FormationStatusRequest request) {
		if (request == null || request.getStatut() == null) {
			throw new BadRequestException("Le statut est obligatoire");
		}
		formationService.changerStatut(id, request.getStatut());
		return ResponseEntity.ok(toDTO(formationService.findById(id)));
	}

    @DeleteMapping("/formations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFormation(@PathVariable Long id,
                                                @RequestParam(required = false, defaultValue = "false") boolean force) {
        formationService.deleteFormation(id, force);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/formations/{formationId}/formateur/{formateurId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignTrainer(@PathVariable Long formationId, @PathVariable Long formateurId) {
        formationService.assignerFormateur(formationId, formateurId);
        return ResponseEntity.ok().build();
    }


    // ======================
    // Participants (Inscription validée)
    // ======================

    @PostMapping("/formations/{formationId}/participants")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<Void> ajouterParticipant(@PathVariable Long formationId,
                                                   @Valid @RequestBody AddParticipantRequest request) {
        formationAccessService.assertAdminOrAssignedFormateur(formationId);
        inscriptionService.ajouterParticipant(formationId, request.getStagiaireId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/formations/{formationId}/participants/{stagiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<Void> retirerParticipant(@PathVariable Long formationId,
                                                   @PathVariable Long stagiaireId) {
        formationAccessService.assertAdminOrAssignedFormateur(formationId);
        inscriptionService.retirerParticipant(formationId, stagiaireId);
        return ResponseEntity.ok().build();
    }

    // ======================
    // Legacy endpoints kept
    // ======================

    @GetMapping("/formateur/{formateurId}/formations")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<FormationDTO>> getFormationsByFormateur(@PathVariable Long formateurId) {
        // évite la sérialisation infinie Entity -> Relations -> Entity (page blanche côté React)
        SecurityUtils.assertAdminOrSelf(formateurId);
        return ResponseEntity.ok(formationService.findByFormateur(formateurId).stream().map(this::toDTO).toList());
    }

    /**
     * Endpoint DTO pour le front React (évite les écarts de champs entre Entity et DTO).
     */
    @GetMapping("/formateur/{formateurId}/formations-dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<FormationDTO>> getFormationsByFormateurDto(@PathVariable Long formateurId) {
        // un formateur ne doit pas pouvoir lire les formations d'un autre compte
        SecurityUtils.assertAdminOrSelf(formateurId);
        return ResponseEntity.ok(formationService.findByFormateur(formateurId).stream().map(this::toDTO).toList());
    }

    @PostMapping("/admin/formations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormationDTO> createFormationLegacy(@Valid @RequestBody Formation formation,
                                                          @RequestParam Long formateurId) {
        Formation created = formationService.createFormation(formation, formateurId);
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/admin/formations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormationDTO> updateFormationLegacy(@PathVariable Long id, @Valid @RequestBody Formation formation) {
        Formation updated = formationService.updateFormation(id, formation);
        return ResponseEntity.ok(toDTO(updated));
    }

    @PutMapping("/admin/formations/{formationId}/formateur/{formateurId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignerFormateurLegacy(@PathVariable Long formationId, @PathVariable Long formateurId) {
        formationService.assignerFormateur(formationId, formateurId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/formations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFormationLegacy(@PathVariable Long id,
                                                      @RequestParam(required = false, defaultValue = "false") boolean force) {
        formationService.deleteFormation(id, force);
        return ResponseEntity.ok().build();
    }

    // ======================
    // Mapping helpers
    // ======================

    private Formation fromRequest(FormationRequest request) {
        Formation f = new Formation();
        f.setNom(request.getTitre());
        f.setDescription(request.getDescription());
        f.setType(request.getType());
        f.setDateDebut(request.getDateDebut());
        f.setDateFin(request.getDateFin());
        f.setCapaciteMax(request.getCapaciteMax());
        // prix: null => 0 (gratuit)
        f.setPrix(request.getPrix() != null ? request.getPrix() : BigDecimal.ZERO);

        if (request.getStatut() != null) {
            f.setStatut(request.getStatut());
        }

        // Catégorie optionnelle
        if (request.getCategorieId() != null) {
            Categorie categorie = categorieRepository.findById(request.getCategorieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categorie", "id", request.getCategorieId()));
            f.setCategorie(categorie);
        } else {
            f.setCategorie(null);
        }

        if (request.getPrerequisId() != null) {
            Formation prerequis = formationService.findById(request.getPrerequisId());
            f.setPrerequis(prerequis);
        }

        return f;
    }

    private FormationDTO toDTO(Formation formation) {
        FormationDTO dto = new FormationDTO();
        dto.setId(formation.getId());
        dto.setTitre(formation.getNom());
        dto.setDescription(formation.getDescription());
        dto.setType(formation.getType());
        dto.setDateDebut(formation.getDateDebut());
        dto.setDateFin(formation.getDateFin());
        dto.setCapaciteMax(formation.getCapaciteMax());
        dto.setDureeHeures(formation.getDureeHeures());
        dto.setPrix(formation.getPrix());
        dto.setStatut(formation.getStatut());

        if (formation.getCategorie() != null) {
            dto.setCategorie(toCategorieDTO(formation.getCategorie()));
        }

        if (formation.getFormateur() != null) {
            UtilisateurSummaryDTO u = new UtilisateurSummaryDTO();
            u.setId(formation.getFormateur().getId());
            u.setNom(formation.getFormateur().getNom());
            u.setPrenom(formation.getFormateur().getPrenom());
            u.setRole(formation.getFormateur().getRole());
            dto.setFormateur(u);
        }

        if (formation.getPrerequis() != null) {
            dto.setPrerequis(toFormationSummaryDTO(formation.getPrerequis()));
        }

        try {
            dto.setNombreInscrits(inscriptionRepository.countByFormation_Id(formation.getId()));
        } catch (Exception e) {
            dto.setNombreInscrits(0L);
        }

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

    private CategorieDTO toCategorieDTO(Categorie categorie) {
        CategorieDTO c = new CategorieDTO();
        c.setId(categorie.getId());
        c.setNom(categorie.getNom());
        c.setDescription(categorie.getDescription());
        c.setIcone(categorie.getIcone());
        c.setCouleur(categorie.getCouleur());
        return c;
    }
}
