package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.ParticipantProgressDTO;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.*;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressionService {

    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final SeanceRepository seanceRepository;
    private final PresenceRepository presenceRepository;
    private final ResultatEvaluationRepository resultatEvaluationRepository;

    public List<ParticipantProgressDTO> progressionParticipants(Long formationId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));
        assertCanManageFormation(formation);

        // Participants confirmés/en cours/terminée
        List<Inscription> inscriptions = inscriptionRepository.findByFormation_IdAndStatutIn(
                formationId,
                List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
        );
        List<Utilisateur> participants = inscriptions.stream().map(Inscription::getStagiaire).toList();

        // Séances prises en compte (réalisées ou déjà terminées, non annulées)
        LocalDateTime now = LocalDateTime.now();
        List<Seance> seances = seanceRepository.findByFormation_IdOrderByDateDebutAsc(formationId).stream()
                .filter(s -> s.getStatut() != StatutSeance.ANNULEE)
                .filter(s -> s.getStatut() == StatutSeance.REALISEE || (s.getDateFin() != null && !s.getDateFin().isAfter(now)))
                .toList();
        Set<Long> seanceIds = seances.stream().map(Seance::getId).collect(Collectors.toSet());
        int totalSeances = seanceIds.size();

        // Présences existantes
        Map<String, StatutPresence> presenceMap = new HashMap<>();
        if (!seanceIds.isEmpty()) {
            for (Presence p : presenceRepository.findBySeance_Formation_Id(formationId)) {
                if (p.getSeance() == null || p.getSeance().getId() == null) continue;
                Long sid = p.getSeance().getId();
                if (!seanceIds.contains(sid)) continue;
                if (p.getStagiaire() == null || p.getStagiaire().getId() == null) continue;
                presenceMap.put(key(sid, p.getStagiaire().getId()), p.getStatut() == null ? StatutPresence.NON_MARQUE : p.getStatut());
            }
        }

        // Meilleure note (/20) par stagiaire (toutes sessions d'évaluation confondues)
        Map<Long, Double> bestNote = new HashMap<>();
        for (ResultatEvaluation r : resultatEvaluationRepository.findByEvaluation_Formation_Id(formationId)) {
            if (r.getStagiaire() == null || r.getStagiaire().getId() == null) continue;
            if (Boolean.TRUE.equals(r.getAbsent())) continue;
            if (r.getNote() == null) continue;
            bestNote.merge(r.getStagiaire().getId(), r.getNote().doubleValue(), Math::max);
        }

        List<ParticipantProgressDTO> out = new ArrayList<>();
        for (Utilisateur stag : participants) {
            ParticipantProgressDTO dto = new ParticipantProgressDTO();
            dto.setUtilisateur(toUserSummary(stag));

            int abs = 0, non = 0;
            double points = 0.0;

            if (totalSeances > 0) {
                for (Seance s : seances) {
                    StatutPresence st = presenceMap.getOrDefault(key(s.getId(), stag.getId()), StatutPresence.NON_MARQUE);
                    if (st == StatutPresence.ABSENT) abs++;
                    if (st == StatutPresence.NON_MARQUE) non++;

                    points += presencePoints(st);
                }
                dto.setSeancesTotal(totalSeances);
                dto.setAbsences(abs);
                dto.setNonMarque(non);

                double taux = points / totalSeances;
                dto.setTauxPresence(taux);
            } else {
                dto.setSeancesTotal(0);
                dto.setAbsences(0);
                dto.setNonMarque(0);
                dto.setTauxPresence(null);
            }

            Double bn = bestNote.get(stag.getId());
            dto.setMeilleureNote(bn);

            double evalRate = (bn != null) ? clamp01(bn / 20.0) : 0.0;
            double attendRate = (dto.getTauxPresence() != null) ? clamp01(dto.getTauxPresence()) : 0.0;

            double progress = (totalSeances > 0) ? (0.70 * attendRate + 0.30 * evalRate) : evalRate;
            int pct = (int) Math.round(clamp01(progress) * 100.0);
            dto.setProgression(pct);

            out.add(dto);
        }

        // Tri: plus avancé d'abord
        out.sort(Comparator.comparing(ParticipantProgressDTO::getProgression, Comparator.nullsLast(Integer::compareTo)).reversed());
        return out;
    }

    public ParticipantProgressDTO maProgression(Long formationId) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) throw new AccessDeniedException("Non authentifié");

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

        // doit être inscrit (confirmé/en cours/terminée)
        boolean ok = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
                u.getId(), formationId,
                List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
        );
        if (!ok) throw new AccessDeniedException("Accès interdit");

        List<ParticipantProgressDTO> list = progressionParticipantsForIds(formationId, List.of(u.getId()));
        return list.isEmpty() ? new ParticipantProgressDTO() : list.get(0);
    }

    private List<ParticipantProgressDTO> progressionParticipantsForIds(Long formationId, List<Long> userIds) {
        // Same as progressionParticipants but filtered for specific users (used for /me)
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

        LocalDateTime now = LocalDateTime.now();
        List<Seance> seances = seanceRepository.findByFormation_IdOrderByDateDebutAsc(formationId).stream()
                .filter(s -> s.getStatut() != StatutSeance.ANNULEE)
                .filter(s -> s.getStatut() == StatutSeance.REALISEE || (s.getDateFin() != null && !s.getDateFin().isAfter(now)))
                .toList();
        Set<Long> seanceIds = seances.stream().map(Seance::getId).collect(Collectors.toSet());
        int totalSeances = seanceIds.size();

        Map<String, StatutPresence> presenceMap = new HashMap<>();
        if (!seanceIds.isEmpty()) {
            for (Presence p : presenceRepository.findBySeance_Formation_Id(formationId)) {
                if (p.getSeance() == null || p.getSeance().getId() == null) continue;
                Long sid = p.getSeance().getId();
                if (!seanceIds.contains(sid)) continue;
                if (p.getStagiaire() == null || p.getStagiaire().getId() == null) continue;
                if (!userIds.contains(p.getStagiaire().getId())) continue;
                presenceMap.put(key(sid, p.getStagiaire().getId()), p.getStatut() == null ? StatutPresence.NON_MARQUE : p.getStatut());
            }
        }

        Map<Long, Double> bestNote = new HashMap<>();
        for (ResultatEvaluation r : resultatEvaluationRepository.findByEvaluation_Formation_Id(formationId)) {
            if (r.getStagiaire() == null || r.getStagiaire().getId() == null) continue;
            if (!userIds.contains(r.getStagiaire().getId())) continue;
            if (Boolean.TRUE.equals(r.getAbsent())) continue;
            if (r.getNote() == null) continue;
            bestNote.merge(r.getStagiaire().getId(), r.getNote().doubleValue(), Math::max);
        }

        List<ParticipantProgressDTO> out = new ArrayList<>();
        for (Long uid : userIds) {
            UtilisateurSummaryDTO us = new UtilisateurSummaryDTO();
            us.setId(uid);

            ParticipantProgressDTO dto = new ParticipantProgressDTO();
            dto.setUtilisateur(us);

            int abs = 0, non = 0;
            double points = 0.0;
            if (totalSeances > 0) {
                for (Seance s : seances) {
                    StatutPresence st = presenceMap.getOrDefault(key(s.getId(), uid), StatutPresence.NON_MARQUE);
                    if (st == StatutPresence.ABSENT) abs++;
                    if (st == StatutPresence.NON_MARQUE) non++;
                    points += presencePoints(st);
                }
                dto.setSeancesTotal(totalSeances);
                dto.setAbsences(abs);
                dto.setNonMarque(non);
                dto.setTauxPresence(points / totalSeances);
            } else {
                dto.setSeancesTotal(0);
                dto.setAbsences(0);
                dto.setNonMarque(0);
                dto.setTauxPresence(null);
            }

            Double bn = bestNote.get(uid);
            dto.setMeilleureNote(bn);

            double evalRate = (bn != null) ? clamp01(bn / 20.0) : 0.0;
            double attendRate = (dto.getTauxPresence() != null) ? clamp01(dto.getTauxPresence()) : 0.0;

            double progress = (totalSeances > 0) ? (0.70 * attendRate + 0.30 * evalRate) : evalRate;
            dto.setProgression((int) Math.round(clamp01(progress) * 100.0));
            out.add(dto);
        }
        return out;
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

    private UtilisateurSummaryDTO toUserSummary(Utilisateur u) {
        UtilisateurSummaryDTO dto = new UtilisateurSummaryDTO();
        dto.setId(u.getId());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setPhotoProfil(u.getPhotoProfil());
        return dto;
    }

    private static String key(Long seanceId, Long userId) {
        return seanceId + ":" + userId;
    }

    private static double presencePoints(StatutPresence st) {
        if (st == null) return 0.0;
        return switch (st) {
            case PRESENT -> 1.0;
            case RETARD -> 0.75;
            case NON_MARQUE -> 0.0;
            case ABSENT -> 0.0;
        };
    }

    private static double clamp01(double v) {
        if (v < 0) return 0.0;
        if (v > 1) return 1.0;
        return v;
    }
}
