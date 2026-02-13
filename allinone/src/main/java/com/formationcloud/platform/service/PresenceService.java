package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.PresenceDTO;
import com.formationcloud.platform.dto.PresenceUpdateRequest;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.repository.PresenceRepository;
import com.formationcloud.platform.repository.SeanceRepository;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final SeanceRepository seanceRepository;
    private final InscriptionRepository inscriptionRepository;

    public List<PresenceDTO> listForSeance(Long seanceId) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        assertCanManage(seance.getFormation());

        // Ensure rows exist for all participants
        List<Inscription> participants = inscriptionRepository.findByFormation_IdAndStatutIn(
                seance.getFormation().getId(),
                List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
        );

        for (Inscription i : participants) {
            Long stagiaireId = i.getStagiaire().getId();
            presenceRepository.findBySeance_IdAndStagiaire_Id(seanceId, stagiaireId)
                    .orElseGet(() -> {
                        Presence p = new Presence();
                        p.setSeance(seance);
                        p.setStagiaire(i.getStagiaire());
                        p.setStatut(StatutPresence.NON_MARQUE);
                        return presenceRepository.save(p);
                    });
        }

        return presenceRepository.findBySeance_IdOrderByStagiaire_PrenomAscStagiaire_NomAsc(seanceId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Utilisé quand une inscription passe à CONFIRMEE : si des séances existent déjà, on crée les lignes de présence
     * pour ce stagiaire, afin que le front (stagiaire + formateur) ait des données cohérentes.
     */
    public void ensurePresencesForFormationStagiaire(Long formationId, Long stagiaireId) {
        if (formationId == null || stagiaireId == null) return;

        // Récupérer l'entité stagiaire via l'inscription (évite d'ajouter une dépendance à UtilisateurService)
        Utilisateur stagiaire = inscriptionRepository.findByFormation_IdAndStatutIn(
                        formationId,
                        List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
                ).stream()
                .filter(i -> i.getStagiaire() != null && stagiaireId.equals(i.getStagiaire().getId()))
                .map(Inscription::getStagiaire)
                .findFirst()
                .orElse(null);

        if (stagiaire == null) return;

        List<Seance> seances = seanceRepository.findByFormation_IdOrderByDateDebutAsc(formationId);
        for (Seance seance : seances) {
            presenceRepository.findBySeance_IdAndStagiaire_Id(seance.getId(), stagiaireId)
                    .orElseGet(() -> {
                        Presence p = new Presence();
                        p.setSeance(seance);
                        p.setStagiaire(stagiaire);
                        p.setStatut(StatutPresence.NON_MARQUE);
                        return presenceRepository.save(p);
                    });
        }
    }



public List<PresenceDTO> bulkUpdate(Long seanceId, List<PresenceUpdateRequest> reqs) {
        if (reqs == null) throw new BadRequestException("Liste vide");
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        assertCanManage(seance.getFormation());

        if (seance.getDateFin() != null && seance.getDateFin().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Impossible de marquer la présence avant la fin de la séance");
        }

        Set<Long> allowed = inscriptionRepository.findByFormation_IdAndStatutIn(
                        seance.getFormation().getId(),
                        List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
                )
                .stream()
                .map(i -> i.getStagiaire().getId())
                .collect(Collectors.toSet());

        for (PresenceUpdateRequest r : reqs) {
            if (r.getStagiaireId() == null || r.getStatut() == null) continue;
            if (!allowed.contains(r.getStagiaireId())) {
                throw new BadRequestException("Stagiaire non inscrit à la formation");
            }
            Presence p = presenceRepository.findBySeance_IdAndStagiaire_Id(seanceId, r.getStagiaireId())
                    .orElseGet(() -> {
                        Presence np = new Presence();
                        np.setSeance(seance);
                        // stagiaire entity: safe to reference via formation inscriptions
                        Utilisateur stag = inscriptionRepository.findByFormation_IdAndStatutIn(
                                        seance.getFormation().getId(),
                                        List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
                                ).stream()
                                .map(Inscription::getStagiaire)
                                .filter(u -> u.getId().equals(r.getStagiaireId()))
                                .findFirst()
                                .orElseThrow(() -> new BadRequestException("Stagiaire introuvable"));
                        np.setStagiaire(stag);
                        np.setStatut(StatutPresence.NON_MARQUE);
                        return np;
                    });

            p.setStatut(r.getStatut());
            p.setRemarque(r.getRemarque());
            presenceRepository.save(p);
        }

        return presenceRepository.findBySeance_IdOrderByStagiaire_PrenomAscStagiaire_NomAsc(seanceId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public PresenceDTO myPresence(Long seanceId) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) throw new AccessDeniedException("Non authentifié");

        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        // Must be enrolled
        boolean ok = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
                u.getId(),
                seance.getFormation().getId(),
                List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
        );
        if (!ok) throw new AccessDeniedException("Accès interdit");

        Presence p = presenceRepository.findBySeance_IdAndStagiaire_Id(seanceId, u.getId())
                .orElseGet(() -> {
                    // create default row
                    Presence np = new Presence();
                    np.setSeance(seance);
                    Utilisateur stag = seance.getFormation().getInscriptions().stream()
                            .map(Inscription::getStagiaire)
                            .filter(s -> s.getId().equals(u.getId()))
                            .findFirst()
                            .orElse(null);
                    if (stag == null) {
                        // Fallback: do not create if can't map entity cleanly
                        throw new BadRequestException("Stagiaire introuvable");
                    }
                    np.setStagiaire(stag);
                    np.setStatut(StatutPresence.NON_MARQUE);
                    return presenceRepository.save(np);
                });

        return toDTO(p);
    }

    private void assertCanManage(Formation formation) {
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

    private PresenceDTO toDTO(Presence p) {
        PresenceDTO dto = new PresenceDTO();
        dto.setId(p.getId());
        dto.setSeanceId(p.getSeance() != null ? p.getSeance().getId() : null);
        dto.setStatut(p.getStatut());
        dto.setRemarque(p.getRemarque());
        dto.setDateModification(p.getDateModification());

        Utilisateur u = p.getStagiaire();
        if (u != null) {
            UtilisateurSummaryDTO su = new UtilisateurSummaryDTO();
            su.setId(u.getId());
            su.setNom(u.getNom());
            su.setPrenom(u.getPrenom());
            su.setEmail(u.getEmail());
            su.setRole(u.getRole());
            su.setPhotoProfil(u.getPhotoProfil());
            dto.setStagiaire(su);
        }
        return dto;
    }
}
