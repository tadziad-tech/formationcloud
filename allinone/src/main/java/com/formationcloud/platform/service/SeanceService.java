package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.SeanceDTO;
import com.formationcloud.platform.dto.SeanceRequest;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.FormationRepository;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.repository.SeanceRepository;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SeanceService {

    private final SeanceRepository seanceRepository;
    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NotificationService notificationService;

    public List<SeanceDTO> listByFormation(Long formationId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

        assertCanViewFormation(formation);

        return seanceRepository.findByFormation_IdOrderByDateDebutAsc(formationId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public SeanceDTO create(Long formationId, SeanceRequest req) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

        assertCanManageFormation(formation);

        validateSeanceRequest(req, formation);

        Seance s = new Seance();
        s.setFormation(formation);
        applyReq(s, req);

        Seance saved = seanceRepository.save(s);

        // Notify participants (optional mais utile)
        List<Utilisateur> destinataires = inscriptionRepository
                .findByFormation_IdAndStatutIn(formationId, List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE))
                .stream()
                .map(Inscription::getStagiaire)
                .toList();
        if (!destinataires.isEmpty()) {
            notificationService.envoyerNotificationSeancePlanifiee(saved, destinataires);
        }

        return toDTO(saved);
    }

    public SeanceDTO update(Long seanceId, SeanceRequest req) {
        Seance s = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        Formation formation = s.getFormation();
        assertCanManageFormation(formation);

        validateSeanceRequest(req, formation);
        applyReq(s, req);

        Seance saved = seanceRepository.save(s);

        List<Utilisateur> destinataires = inscriptionRepository
                .findByFormation_IdAndStatutIn(formation.getId(), List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE))
                .stream()
                .map(Inscription::getStagiaire)
                .toList();
        if (!destinataires.isEmpty()) {
            notificationService.envoyerNotificationSeanceModifiee(saved, destinataires);
        }

        return toDTO(saved);
    }

    public void delete(Long seanceId) {
        Seance s = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));
        assertCanManageFormation(s.getFormation());
        seanceRepository.delete(s);
    }

    public SeanceDTO get(Long seanceId) {
        Seance s = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        assertCanViewFormation(s.getFormation());
        return toDTO(s);
    }

    private void assertCanViewFormation(Formation formation) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) throw new AccessDeniedException("Non authentifié");
        String role = String.valueOf(u.getRole()).toUpperCase();

        if ("ADMIN".equals(role)) return;

        if ("FORMATEUR".equals(role)) {
            if (formation.getFormateur() != null && formation.getFormateur().getId() != null
                    && formation.getFormateur().getId().equals(u.getId())) return;
            throw new AccessDeniedException("Accès interdit");
        }

        // STAGIAIRE
        boolean ok = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
                u.getId(),
                formation.getId(),
                List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
        );
        if (!ok) throw new AccessDeniedException("Accès interdit");
    }

    private void assertCanManageFormation(Formation formation) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) throw new AccessDeniedException("Non authentifié");
        String role = String.valueOf(u.getRole()).toUpperCase();

        if ("ADMIN".equals(role)) return;

        if ("FORMATEUR".equals(role)) {
            if (formation.getFormateur() != null && formation.getFormateur().getId() != null
                    && formation.getFormateur().getId().equals(u.getId())) return;
        }
        throw new AccessDeniedException("Accès interdit");
    }

    private void validateSeanceRequest(SeanceRequest req, Formation formation) {
        if (req.getDateDebut() == null || req.getDateFin() == null) {
            throw new BadRequestException("Dates de séance invalides");
        }
        if (!req.getDateFin().isAfter(req.getDateDebut())) {
            throw new BadRequestException("La date de fin doit être après la date de début");
        }

        // Optional: check inside formation date range (only if formation dates set)
        if (formation.getDateDebut() != null && req.getDateDebut().toLocalDate().isBefore(formation.getDateDebut())) {
            throw new BadRequestException("La séance ne peut pas commencer avant la date de début de la formation");
        }
        if (formation.getDateFin() != null && req.getDateFin().toLocalDate().isAfter(formation.getDateFin())) {
            throw new BadRequestException("La séance ne peut pas finir après la date de fin de la formation");
        }

        ModeSeance mode = req.getMode();
        if (mode == null) throw new BadRequestException("Mode de séance invalide");

        if (mode == ModeSeance.DISTANCIEL) {
            String link = (req.getZoomLink() == null) ? "" : req.getZoomLink().trim();
            if (link.isEmpty()) throw new BadRequestException("Le lien Zoom est obligatoire pour une séance à distance");
        } else {
            String lieu = (req.getLieu() == null) ? "" : req.getLieu().trim();
            if (lieu.isEmpty()) throw new BadRequestException("Le lieu est obligatoire pour une séance présentielle");
        }
    }

    private void applyReq(Seance s, SeanceRequest req) {
        s.setTitre(req.getTitre().trim());
        s.setDescription(req.getDescription());
        s.setDateDebut(req.getDateDebut());
        s.setDateFin(req.getDateFin());
        s.setMode(req.getMode());
        s.setZoomLink(req.getZoomLink());
        s.setLieu(req.getLieu());
        if (req.getStatut() != null) s.setStatut(req.getStatut());
    }

    private SeanceDTO toDTO(Seance s) {
        SeanceDTO dto = new SeanceDTO();
        dto.setId(s.getId());
        dto.setFormationId(s.getFormation() != null ? s.getFormation().getId() : null);
        dto.setTitre(s.getTitre());
        dto.setDescription(s.getDescription());
        dto.setDateDebut(s.getDateDebut());
        dto.setDateFin(s.getDateFin());
        dto.setMode(s.getMode());
        dto.setZoomLink(s.getZoomLink());
        dto.setLieu(s.getLieu());
        dto.setStatut(s.getStatut());
        return dto;
    }
}
