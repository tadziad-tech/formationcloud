package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.CertificatDTO;
import com.formationcloud.platform.dto.CertificatVerificationDTO;
import com.formationcloud.platform.dto.FormationSummaryDTO;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.Certificat;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.security.UserPrincipal;
import com.formationcloud.platform.service.CertificatService;
import com.formationcloud.platform.service.FormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CertificatController {

    private final CertificatService certificatService;
    private final FormationService formationService;

    @GetMapping("/certificats/verify/{code}")
    public ResponseEntity<CertificatVerificationDTO> verify(@PathVariable String code) {
        Certificat c = certificatService.findByNumeroUnique(code);
        CertificatVerificationDTO dto = new CertificatVerificationDTO();
        dto.setNumeroCertificat(c.getNumeroUnique());
        dto.setStatut(c.getStatut() == null ? null : c.getStatut().name());
        dto.setDateObtention(c.getDateObtention());
        dto.setNoteObtenue(c.getNoteFinale());
        dto.setNomComplet(c.getStagiaire() != null ? c.getStagiaire().getNomComplet() : "-");
        dto.setFormation(c.getFormation() != null ? c.getFormation().getNom() : "-");
        dto.setFormateur(
                (c.getFormation() != null && c.getFormation().getFormateur() != null)
                        ? c.getFormation().getFormateur().getNomComplet()
                        : "-"
        );
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/certificats")
    public ResponseEntity<List<CertificatDTO>> getAllCertificats() {
        return ResponseEntity.ok(certificatService.findAll().stream().map(this::toDTO).toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    @GetMapping("/certificats/{id}")
    public ResponseEntity<CertificatDTO> getCertificatById(@PathVariable Long id) {
        Certificat c = certificatService.findById(id);
        // On laisse l'admin tout voir; pour stagiaire/formateur la sécurité réelle est sur /pdf
        return ResponseEntity.ok(toDTO(c));
    }

    @GetMapping("/certificats/utilisateur/{userId}")
    public ResponseEntity<List<CertificatDTO>> getCertificatsByUser(@PathVariable Long userId) {
        SecurityUtils.assertAdminOrSelf(userId);
        return ResponseEntity.ok(certificatService.findByStagiaire(userId).stream().map(this::toDTO).toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    @GetMapping("/certificats/formation/{formationId}")
    public ResponseEntity<List<CertificatDTO>> getCertificatsByFormation(@PathVariable Long formationId) {
        // Sécurité: un formateur ne doit pas pouvoir lire les certificats d'une formation qu'il ne possède pas.
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");
        if (!SecurityUtils.isAdmin() && "FORMATEUR".equalsIgnoreCase(principal.getRole())) {
            Formation f = formationService.findById(formationId);
            Long ownerId = (f.getFormateur() != null ? f.getFormateur().getId() : null);
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("Accès interdit");
            }
        }
        return ResponseEntity.ok(certificatService.findByFormation(formationId).stream().map(this::toDTO).toList());
    }

    /**
     * FORMATEUR: voir les certificats attribués dans ses formations.
     * ADMIN: peut aussi appeler ce endpoint, et verra tous.
     */
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    @GetMapping("/certificats/formateur/me")
    public ResponseEntity<List<CertificatDTO>> getCertificatsForCurrentFormateur() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");

        if (SecurityUtils.isAdmin()) {
            return ResponseEntity.ok(certificatService.findAll().stream().map(this::toDTO).toList());
        }

        // Formateur: filtré côté repo
        return ResponseEntity.ok(certificatService.findByFormateur(principal.getId()).stream().map(this::toDTO).toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    @GetMapping(value = "/certificats/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) throws IOException {
        Certificat c = certificatService.findById(id);
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) throw new AccessDeniedException("Non authentifié");

        if (!SecurityUtils.isAdmin()) {
            String role = (u.getRole() == null ? "" : u.getRole().toUpperCase());
            if (role.equals("STAGIAIRE")) {
                Long ownerId = (c.getStagiaire() != null ? c.getStagiaire().getId() : null);
                if (ownerId == null || !ownerId.equals(u.getId())) {
                    throw new AccessDeniedException("Accès interdit");
                }
            } else if (role.equals("FORMATEUR")) {
                Long formateurId = (c.getFormation() != null && c.getFormation().getFormateur() != null)
                        ? c.getFormation().getFormateur().getId()
                        : null;
                if (formateurId == null || !formateurId.equals(u.getId())) {
                    throw new AccessDeniedException("Accès interdit");
                }
            } else {
                throw new AccessDeniedException("Accès interdit");
            }
        }

        // Assure que le PDF existe
        c = certificatService.finaliserNumeroEtPdf(c);

        Path pdfPath = Paths.get("uploads").resolve("certificats")
                .resolve(c.getNumeroUnique() + ".pdf")
                .toAbsolutePath().normalize();

        if (!Files.exists(pdfPath)) {
            throw new ResourceNotFoundException("PDF certificat", "path", pdfPath.toString());
        }

        Resource resource = new FileSystemResource(pdfPath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + c.getNumeroUnique() + ".pdf\"")
                .body(resource);
    }

    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    @PostMapping("/certificats/{id}/revoke")
    public ResponseEntity<CertificatDTO> revoke(@PathVariable Long id) {
        // ADMIN: tout / FORMATEUR: uniquement ses formations
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");
        if (!SecurityUtils.isAdmin() && "FORMATEUR".equalsIgnoreCase(principal.getRole())) {
            Certificat c = certificatService.findById(id);
            Long ownerId = (c.getFormation() != null && c.getFormation().getFormateur() != null)
                    ? c.getFormation().getFormateur().getId() : null;
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("Accès interdit");
            }
        }
        return ResponseEntity.ok(toDTO(certificatService.revoquerCertificat(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/certificats/{id}/restore")
    public ResponseEntity<CertificatDTO> restore(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(certificatService.restaurerCertificat(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    @DeleteMapping("/certificats/{id}")
    public ResponseEntity<Void> deleteCertificat(@PathVariable Long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) throw new AccessDeniedException("Non authentifié");
        if (!SecurityUtils.isAdmin() && "FORMATEUR".equalsIgnoreCase(principal.getRole())) {
            Certificat c = certificatService.findById(id);
            Long ownerId = (c.getFormation() != null && c.getFormation().getFormateur() != null)
                    ? c.getFormation().getFormateur().getId() : null;
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("Accès interdit");
            }
        }
        certificatService.deleteCertificat(id);
        return ResponseEntity.ok().build();
    }

    private CertificatDTO toDTO(Certificat c) {
        CertificatDTO dto = new CertificatDTO();
        dto.setId(c.getId());
        dto.setNumeroCertificat(c.getNumeroUnique());
        dto.setDateObtention(c.getDateObtention());
        dto.setDateRevocation(c.getDateRevocation());
        dto.setNoteObtenue(c.getNoteFinale());
        dto.setStatut(c.getStatut() == null ? null : c.getStatut().name());
        dto.setUrlPdf(c.getUrlPdf());

        Utilisateur stagiaire = c.getStagiaire();
        if (stagiaire != null) {
            UtilisateurSummaryDTO u = new UtilisateurSummaryDTO();
            u.setId(stagiaire.getId());
            u.setNom(stagiaire.getNom());
            u.setPrenom(stagiaire.getPrenom());
            u.setEmail(stagiaire.getEmail());
            u.setRole(stagiaire.getRole());
            u.setPhotoProfil(stagiaire.getPhotoProfil());
            dto.setStagiaire(u);
        }

        Formation f = c.getFormation();
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
