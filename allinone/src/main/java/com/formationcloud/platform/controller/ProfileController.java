package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.*;
import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.Certificat;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Inscription;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.service.CertificatService;
import com.formationcloud.platform.service.FormationService;
import com.formationcloud.platform.service.InscriptionService;
import com.formationcloud.platform.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProfileController {

    private final UtilisateurService utilisateurService;
    private final CertificatService certificatService;
    private final InscriptionService inscriptionService;
    private final FormationService formationService;

    private static final long MAX_IMAGE_BYTES = 3L * 1024L * 1024L; // 3MB
    private static final int PROFILE_SIZE = 320;
    private static final float JPEG_QUALITY = 0.85f;

    /**
     * Profil de l'utilisateur connecté (tous les rôles).
     */
    @GetMapping("/profile/me")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<ProfilDTO> me() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Utilisateur u = utilisateurService.findById(principal.getId());

        ProfilDTO dto = new ProfilDTO();
        dto.setId(u.getId());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setTypeFormateur(u.getTypeFormateur());
        dto.setStatutValidation(u.getStatutValidation());
        dto.setTelephone(u.getTelephone());
        dto.setAdresse(u.getAdresse());
        dto.setPhotoProfil(u.getPhotoProfil());
        dto.setDateCreation(u.getDateCreation());

        // Certificats: utile surtout pour le stagiaire (si vide => OK)
        List<Certificat> certs = certificatService.findByStagiaire(u.getId());
        dto.setCertificats(certs.stream().map(this::toCertificatDTO).toList());

        // STAGIAIRE => inscriptions + formations
        if (u.isStagiaire()) {
            List<Inscription> inscriptions = inscriptionService.findByStagiaire(u.getId());
            dto.setInscriptions(inscriptions.stream().map(this::toInscriptionDTO).toList());
        }

        // FORMATEUR => formations encadrées
        if (u.isFormateur()) {
            List<Formation> formations = formationService.findByFormateur(u.getId());
            dto.setFormationsFormateur(formations.stream().map(this::toFormationSummaryDTO).toList());
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * Profil d'un utilisateur par id (lecture uniquement).
     * Utilisé dans la page Utilisateurs: clic => détails.
     */
    @GetMapping("/profile/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<ProfilDTO> byId(@PathVariable Long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        utilisateurService.assertCanViewUser(principal.getId(), principal.getRole() != null ? Role.valueOf(principal.getRole()) : null, id);

        Utilisateur u = utilisateurService.findById(id);

        ProfilDTO dto = new ProfilDTO();
        dto.setId(u.getId());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setTypeFormateur(u.getTypeFormateur());
        dto.setStatutValidation(u.getStatutValidation());
        dto.setTelephone(u.getTelephone());
        dto.setAdresse(u.getAdresse());
        dto.setPhotoProfil(u.getPhotoProfil());
        dto.setDateCreation(u.getDateCreation());

        // Certificats: uniquement pertinent pour stagiaire
        if (u.isStagiaire()) {
            List<Certificat> certs = certificatService.findByStagiaire(u.getId());
            dto.setCertificats(certs.stream().map(this::toCertificatDTO).toList());

            List<Inscription> inscriptions = inscriptionService.findByStagiaire(u.getId());
            dto.setInscriptions(inscriptions.stream().map(this::toInscriptionDTO).toList());
        }

        // Formateur => formations encadrées
        if (u.isFormateur()) {
            List<Formation> formations = formationService.findByFormateur(u.getId());
            dto.setFormationsFormateur(formations.stream().map(this::toFormationSummaryDTO).toList());
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * Mise à jour du profil (nom/prénom/téléphone/adresse + optionnel : nouveau mot de passe)
     */
    @PutMapping("/profile/me")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<ProfilDTO> updateMe(@RequestBody ProfileUpdateRequest req) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        utilisateurService.updateSelf(principal.getId(), req);
        return me();
    }

    /**
     * Upload de photo de profil (fichier) depuis le Profil.
     * Stockage local dans ./uploads/ puis accès via /uploads/**
     */
    @PostMapping(value = "/profile/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<ProfilDTO> uploadPhoto(@RequestPart("file") MultipartFile file) throws IOException {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            return ResponseEntity.status(413).build();
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().build();
        }

        Path uploadDir = Paths.get("uploads").resolve("profile").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        // On stocke toujours en JPEG optimisé (taille fixe) pour performance.
        String filename = "user_" + principal.getId() + ".jpg";
        Path target = uploadDir.resolve(filename);

        // Recadrage + resize + compression côté serveur (même si le front recadre déjà)
        boolean ok = processAndSaveAsJpegSquare(file, target, PROFILE_SIZE, JPEG_QUALITY);
        if (!ok) {
            return ResponseEntity.badRequest().build();
        }

        String publicPath = "/uploads/profile/" + filename;
        utilisateurService.updatePhotoProfil(principal.getId(), publicPath);

        return me();
    }

    @DeleteMapping("/profile/me/photo")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<ProfilDTO> removePhoto() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        utilisateurService.updatePhotoProfil(principal.getId(), null);
        return me();
    }

    // =====================
    // Mapping helpers
    // =====================

    private CertificatDTO toCertificatDTO(Certificat c) {
        CertificatDTO dto = new CertificatDTO();
        dto.setId(c.getId());
        dto.setNumeroCertificat(c.getNumeroUnique());
        dto.setDateObtention(c.getDateObtention());
        dto.setNoteObtenue(c.getNoteFinale());

        if (c.getFormation() != null) {
            dto.setFormation(toFormationSummaryDTO(c.getFormation()));
        }

        if (c.getStagiaire() != null) {
            UtilisateurSummaryDTO u = new UtilisateurSummaryDTO();
            u.setId(c.getStagiaire().getId());
            u.setNom(c.getStagiaire().getNom());
            u.setPrenom(c.getStagiaire().getPrenom());
            u.setEmail(c.getStagiaire().getEmail());
            u.setRole(c.getStagiaire().getRole());
            dto.setStagiaire(u);
        }

        return dto;
    }

    private InscriptionDTO toInscriptionDTO(Inscription i) {
        InscriptionDTO dto = new InscriptionDTO();
        dto.setId(i.getId());
        dto.setDateInscription(i.getDateInscription());
        dto.setStatut(i.getStatut());

        if (i.getStagiaire() != null) {
            UtilisateurSummaryDTO u = new UtilisateurSummaryDTO();
            u.setId(i.getStagiaire().getId());
            u.setNom(i.getStagiaire().getNom());
            u.setPrenom(i.getStagiaire().getPrenom());
            u.setEmail(i.getStagiaire().getEmail());
            u.setRole(i.getStagiaire().getRole());
            dto.setUtilisateur(u);
        }

        if (i.getFormation() != null) {
            dto.setFormation(toFormationSummaryDTO(i.getFormation()));
        }

        return dto;
    }

    private FormationSummaryDTO toFormationSummaryDTO(Formation f) {
        FormationSummaryDTO s = new FormationSummaryDTO();
        s.setId(f.getId());
        s.setTitre(f.getNom());
        s.setType(f.getType());
        s.setDateDebut(f.getDateDebut());
        s.setDateFin(f.getDateFin());
        s.setStatut(f.getStatut());
        return s;
    }

    /**
     * Convertit l'image uploadée en JPEG carré optimisé.
     * - crop centre vers un carré
     * - resize vers size x size
     * - compression JPEG qualité (0..1)
     */
    private boolean processAndSaveAsJpegSquare(MultipartFile file, Path target, int size, float quality) {
        try {
            BufferedImage src = ImageIO.read(file.getInputStream());
            if (src == null) {
                return false;
            }

            int w = src.getWidth();
            int h = src.getHeight();
            int square = Math.min(w, h);
            int x = (w - square) / 2;
            int y = (h - square) / 2;
            BufferedImage cropped = src.getSubimage(x, y, square, square);

            // Resize en RGB (fond blanc si PNG alpha)
            BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            g.drawImage(cropped, 0, 0, size, size, null);
            g.dispose();

            // Write JPEG with compression quality
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                return false;
            }

            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality)));
            }

            try (OutputStream os = Files.newOutputStream(target);
                 ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(resized, null, null), param);
            } finally {
                writer.dispose();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
