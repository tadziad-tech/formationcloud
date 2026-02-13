package com.formationcloud.platform.service;

import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.Certificat;
import com.formationcloud.platform.model.CertificatStatut;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.ResultatEvaluation;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.repository.CertificatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CertificatService {

    private final CertificatRepository certificatRepository;
    private final UtilisateurService utilisateurService;
    private final FormationService formationService;
    private final NotificationService notificationService;
    private final CertificatPdfService certificatPdfService;

    public List<Certificat> findAll() {
        return certificatRepository.findAll();
    }

    public Certificat findById(Long id) {
        return certificatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificat", "id", id));
    }

    public List<Certificat> findByFormation(Long formationId) {
        Formation formation = formationService.findById(formationId);
        return certificatRepository.findByFormation(formation);
    }

    public List<Certificat> findByFormateur(Long formateurId) {
        return certificatRepository.findByFormateurId(formateurId);
    }

    public List<Certificat> findByStagiaire(Long stagiaireId) {
        Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
        return certificatRepository.findByStagiaire(stagiaire);
    }

    public Certificat findByNumeroUnique(String numeroUnique) {
        return certificatRepository.findByNumeroUnique(numeroUnique)
                .orElseThrow(() -> new ResourceNotFoundException("Certificat", "numéro", numeroUnique));
    }

    public boolean verifierCertificatExiste(Long stagiaireId, Long formationId) {
        return certificatRepository.existsByStagiaireIdAndFormationId(stagiaireId, formationId);
    }

    /**
     * Génère automatiquement un certificat si le stagiaire a réussi l'évaluation.
     * Idempotent: si un certificat existe déjà (stagiaire_id, formation_id), on met à jour au lieu d'insérer.
     */
    public Certificat genererCertificatAutomatique(ResultatEvaluation resultat) {
        if (resultat == null) return null;
        if (!resultat.isReussi()) {
            log.info("Certificat non généré: résultat non réussi pour {}", resultat.getStagiaire().getNomComplet());
            return null;
        }

        Formation formation = resultat.getEvaluation().getFormation();
        Utilisateur stagiaire = resultat.getStagiaire();
        Long stagiaireId = stagiaire.getId();
        Long formationId = formation.getId();
        if (stagiaireId == null || formationId == null) return null;

        Optional<Certificat> existingOpt = certificatRepository.findByStagiaireIdAndFormationId(stagiaireId, formationId);
        if (existingOpt.isPresent()) {
            Certificat ex = existingOpt.get();
            if (ex.getStatut() == CertificatStatut.REVOQUE) {
                ex.setStatut(CertificatStatut.VALIDE);
                ex.setDateRevocation(null);
            }
            ex.setNoteFinale(resultat.getNote());
            ex.setDateObtention(ex.getDateObtention() == null ? LocalDate.now() : ex.getDateObtention());
            Certificat saved = certificatRepository.save(ex);
            return finaliserNumeroEtPdf(saved);
        }

        Certificat certificat = new Certificat();
        certificat.setStagiaire(stagiaire);
        certificat.setFormation(formation);
        certificat.setDateObtention(LocalDate.now());
        certificat.setNoteFinale(resultat.getNote());
        certificat.setStatut(CertificatStatut.VALIDE);

        try {
            Certificat saved = certificatRepository.save(certificat);
            saved = finaliserNumeroEtPdf(saved);
            notificationService.envoyerNotificationCertificatObtenu(saved);
            log.info("Certificat généré automatiquement: {} ({})", saved.getNumeroUnique(), stagiaire.getNomComplet());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate certificat (concurrence) pour stagiaire {} / formation {}: refetch et update", stagiaireId, formationId);
            Optional<Certificat> refetched = certificatRepository.findByStagiaireIdAndFormationId(stagiaireId, formationId);
            if (refetched.isPresent()) {
                Certificat ex = refetched.get();
                if (ex.getStatut() == CertificatStatut.REVOQUE) {
                    ex.setStatut(CertificatStatut.VALIDE);
                    ex.setDateRevocation(null);
                }
                ex.setNoteFinale(resultat.getNote());
                ex.setDateObtention(ex.getDateObtention() == null ? LocalDate.now() : ex.getDateObtention());
                Certificat saved = certificatRepository.save(ex);
                return finaliserNumeroEtPdf(saved);
            }
            throw e;
        }
    }

    /**
     * Révoque le certificat d'un stagiaire pour une formation (si existe et encore VALIDE).
     * Utile lors d'une correction de note / annulation.
     */
    public void revoquerSiExiste(Long stagiaireId, Long formationId) {
        try {
            if (stagiaireId == null || formationId == null) return;
            certificatRepository.findByStagiaireIdAndFormationId(stagiaireId, formationId).ifPresent(c -> {
                if (c.getStatut() != CertificatStatut.REVOQUE) {
                    revoquerCertificat(c.getId());
                }
            });
        } catch (Exception ignored) {
            // On ne bloque jamais la saisie de note à cause d'un souci de certificat.
        }
    }

    // NOTE:
    // La génération de certificats est volontairement AUTOMATIQUE (après réussite d'une évaluation).
    // Pas de création manuelle exposée côté API, pour éviter les "certificats fake".

    public Certificat revoquerCertificat(Long id) {
        Certificat c = findById(id);
        if (c.getStatut() == CertificatStatut.REVOQUE) return c;
        c.setStatut(CertificatStatut.REVOQUE);
        c.setDateRevocation(LocalDate.now());
        return certificatRepository.save(c);
    }

    /**
     * Révoque le certificat d'un stagiaire pour une formation si il existe.
     * Utile si une note est corrigée en dessous du seuil.
     */
    public void revoquerSiExiste(Utilisateur stagiaire, Formation formation) {
        if (stagiaire == null || formation == null) return;
        Long sid = stagiaire.getId();
        Long fid = formation.getId();
        if (sid == null || fid == null) return;
        certificatRepository.findByStagiaireIdAndFormationId(sid, fid)
                .ifPresent(c -> {
                    if (c.getStatut() != CertificatStatut.REVOQUE) {
                        c.setStatut(CertificatStatut.REVOQUE);
                        c.setDateRevocation(LocalDate.now());
                        certificatRepository.save(c);
                    }
                });
    }

    public Certificat restaurerCertificat(Long id) {
        Certificat c = findById(id);
        if (c.getStatut() == CertificatStatut.VALIDE) return c;
        c.setStatut(CertificatStatut.VALIDE);
        c.setDateRevocation(null);
        if (c.getDateObtention() == null) c.setDateObtention(LocalDate.now());
        return certificatRepository.save(c);
    }

    public void deleteCertificat(Long id) {
        Certificat certificat = findById(id);
        certificatRepository.delete(certificat);
        log.info("Certificat supprimé: {}", certificat.getNumeroUnique());
    }

    public long countByStagiaire(Long stagiaireId) {
        return certificatRepository.countByStagiaireId(stagiaireId);
    }

    /**
     * Révoque en masse tous les certificats VALIDE d'une formation.
     * Utilisé lors de la réouverture d'une évaluation par un admin.
     */
    public int revoquerTousLesCertificatsValidesDeFormation(Long formationId) {
        if (formationId == null) return 0;
        return certificatRepository.revoquerTousPourFormation(formationId);
    }

    /**
     * Numéro humain: FC-YYYY-000001 (basé sur l'ID en base)
     */
    private String buildHumanCode(Certificat c) {
        int year = (c.getDateObtention() != null ? c.getDateObtention().getYear() : LocalDate.now().getYear());
        String serial = (c.getId() == null) ? "000000" : String.format("%06d", c.getId());
        return "FC-" + year + "-" + serial;
    }

    /**
     * Finalise le certificat:
     *  - force un code lisible
     *  - génère le PDF (Modèle 1) dans uploads/certificats/
     */
    public Certificat finaliserNumeroEtPdf(Certificat c) {
        if (c == null) return null;

        // 1) Numéro final
        String code = buildHumanCode(c);
        if (c.getNumeroUnique() == null
                || c.getNumeroUnique().isBlank()
                || c.getNumeroUnique().startsWith("TMP-")
                || c.getNumeroUnique().startsWith("CERT-")) {
            c.setNumeroUnique(code);
            c = certificatRepository.save(c);
        }

        // 2) PDF
        try {
            Path pdfPath = certificatPdfService.defaultPdfPath(c.getNumeroUnique());
            boolean exists = Files.exists(pdfPath) && Files.size(pdfPath) > 500;
            if (!exists) {
                byte[] pdf = certificatPdfService.renderCertificatPdfModeleA(c);
                Files.write(pdfPath, pdf);
            }

            String publicUrl = "/uploads/certificats/" + c.getNumeroUnique() + ".pdf";
            if (c.getUrlPdf() == null || c.getUrlPdf().isBlank()) {
                c.setUrlPdf(publicUrl);
                c = certificatRepository.save(c);
            }
        } catch (Exception e) {
            log.warn("PDF non généré pour {} (continue)", c.getNumeroUnique(), e);
        }

        return c;
    }
}
