package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.*;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.*;
import com.formationcloud.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoService {

    private static final int MAX_UPCOMING = 5;
    private static final int MAX_TODO = 10;
    private static final int MAX_OVERDUE = 10;

    private final UtilisateurService utilisateurService;
    private final InscriptionRepository inscriptionRepository;
    private final SeanceRepository seanceRepository;
    private final TpRessourceRepository tpRessourceRepository;
    private final TpSoumissionRepository tpSoumissionRepository;
    private final PresenceRepository presenceRepository;

    public TodoResponseDto getForCurrentUser() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            return emptyResponse();
        }
        Utilisateur me = utilisateurService.findById(principal.getId());
        if (me == null) {
            return emptyResponse();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upcomingEnd = now.plusDays(7);
        LocalDateTime soonEnd = now.plusHours(72);
        LocalDateTime lateCutoff = now.minusHours(24);

        if (SecurityUtils.isAdmin()) {
            return buildAdmin(now, soonEnd, lateCutoff);
        }
        if (SecurityUtils.isFormateur()) {
            return buildFormateur(me.getId(), now, soonEnd, lateCutoff);
        }
        if (SecurityUtils.isStagiaire()) {
            return buildStagiaire(me.getId(), now, soonEnd, lateCutoff);
        }

        return emptyResponse();
    }

    private TodoResponseDto emptyResponse() {
        return TodoResponseDto.builder()
                .upcomingSeances(List.of())
                .todo(List.of())
                .overdue(List.of())
                .build();
    }

    private TodoResponseDto buildAdmin(LocalDateTime now, LocalDateTime soonEnd, LocalDateTime lateCutoff) {
        List<Inscription> enAttente = inscriptionRepository.findByStatutInOrderByDateInscriptionDesc(
                List.of(StatutInscription.EN_ATTENTE),
                PageRequest.of(0, MAX_TODO));
        List<TodoItemDto> todo = enAttente.stream()
                .map(i -> TodoItemDto.builder()
                        .type("INSCRIPTION")
                        .title("Inscription en attente")
                        .message("Inscription de " + i.getStagiaire().getNomComplet() + " pour « " + i.getFormation().getNom() + " ».")
                        .link("/formations?formationId=" + i.getFormation().getId() + "&focus=pending")
                        .severity("WARN")
                        .build())
                .limit(MAX_TODO)
                .collect(Collectors.toList());

        return TodoResponseDto.builder()
                .upcomingSeances(List.of())
                .todo(todo)
                .overdue(List.of())
                .build();
    }

    private TodoResponseDto buildFormateur(Long formateurId, LocalDateTime now, LocalDateTime soonEnd, LocalDateTime lateCutoff) {
        LocalDateTime upcomingEnd = now.plusDays(7);
        // Upcoming séances (formateur): dateDebut between now and now+7d
        List<Seance> upcomingSeances = seanceRepository.findByFormation_Formateur_IdAndDateDebutBetweenOrderByDateDebutAsc(formateurId, now, upcomingEnd);
        List<UpcomingSeanceDto> upcomingDtos = upcomingSeances.stream()
                .limit(MAX_UPCOMING)
                .map(this::toUpcomingSeanceDto)
                .collect(Collectors.toList());

        List<TodoItemDto> todo = new ArrayList<>();
        // a) Présence à compléter: dateFin < now AND dateFin > now-7d, au moins une présence NON_MARQUE
        LocalDateTime presenceFrom = now.minusDays(7);
        List<Long> seanceIdsPresence = presenceRepository.findSeanceIdsWithStatutByFormateurAndDateFinBetween(
                StatutPresence.NON_MARQUE, presenceFrom, now, formateurId);
        for (Long seanceId : seanceIdsPresence) {
            if (todo.size() >= MAX_TODO) break;
            seanceRepository.findById(seanceId).ifPresent(s -> {
                todo.add(TodoItemDto.builder()
                        .type("PRESENCE")
                        .title("Présence à compléter")
                        .message("Séance « " + s.getTitre() + " » – " + s.getFormation().getNom())
                        .link("/formations?formationId=" + s.getFormation().getId() + "&tab=programme&seanceId=" + s.getId())
                        .severity("WARN")
                        .build());
            });
        }
        // b) TP à corriger: soumissions SOUMIS sur TP des formations du formateur
        List<TpSoumission> soumissions = tpSoumissionRepository.findSoumisByFormateur(
                formateurId, StatutTpSoumission.SOUMIS, PageRequest.of(0, MAX_TODO - todo.size()));
        for (TpSoumission sub : soumissions) {
            if (todo.size() >= MAX_TODO) break;
            TpRessource tp = sub.getTp();
            todo.add(TodoItemDto.builder()
                    .type("TP_CORRECTION")
                    .title("TP à corriger")
                    .message("Soumission de " + sub.getStagiaire().getNomComplet() + " – « " + tp.getTitre() + " »")
                    .link("/formations?formationId=" + tp.getFormation().getId() + "&tab=ressources&tpId=" + tp.getId())
                    .severity("INFO")
                    .build());
        }

        List<TodoItemDto> overdue = new ArrayList<>();
        List<Long> seanceIdsLate = presenceRepository.findSeanceIdsWithStatutByFormateurAndDateFinBefore(
                StatutPresence.NON_MARQUE, lateCutoff, formateurId);
        for (Long seanceId : seanceIdsLate) {
            if (overdue.size() >= MAX_OVERDUE) break;
            seanceRepository.findById(seanceId).ifPresent(s -> {
                overdue.add(TodoItemDto.builder()
                        .type("PRESENCE")
                        .title("Présence en retard")
                        .message("Séance « " + s.getTitre() + " » – " + s.getFormation().getNom())
                        .link("/formations?formationId=" + s.getFormation().getId() + "&tab=programme&seanceId=" + s.getId())
                        .severity("URGENT")
                        .build());
            });
        }

        return TodoResponseDto.builder()
                .upcomingSeances(upcomingDtos)
                .todo(todo)
                .overdue(overdue)
                .build();
    }

    private TodoResponseDto buildStagiaire(Long stagiaireId, LocalDateTime now, LocalDateTime soonEnd, LocalDateTime lateCutoff) {
        List<Long> formationIds = inscriptionRepository.findActiveFormationIdsByStagiaire(stagiaireId);
        if (formationIds.isEmpty()) {
            return TodoResponseDto.builder()
                    .upcomingSeances(List.of())
                    .todo(List.of())
                    .overdue(List.of())
                    .build();
        }

        LocalDateTime upcomingEnd = now.plusDays(7);
        List<Seance> upcomingSeances = seanceRepository.findByFormation_IdInAndDateDebutBetweenOrderByDateDebutAsc(formationIds, now, upcomingEnd);
        List<UpcomingSeanceDto> upcomingDtos = upcomingSeances.stream()
                .limit(MAX_UPCOMING)
                .map(this::toUpcomingSeanceDto)
                .collect(Collectors.toList());

        List<TodoItemDto> todo = new ArrayList<>();
        List<TpRessource> tpsSoon = tpRessourceRepository.findByFormation_IdInAndTypeAndDateLimiteBetweenOrderByDateLimiteAsc(
                formationIds, TypeTpRessource.TP, now, soonEnd);
        for (TpRessource tp : tpsSoon) {
            if (todo.size() >= MAX_TODO) break;
            if (tpSoumissionRepository.existsByTp_IdAndStagiaire_Id(tp.getId(), stagiaireId)) continue;
            todo.add(TodoItemDto.builder()
                    .type("TP")
                    .title("TP à rendre")
                    .message("« " + tp.getTitre() + " » – " + tp.getFormation().getNom() + " (limite " + formatDate(tp.getDateLimite()) + ")")
                    .link("/formations?formationId=" + tp.getFormation().getId() + "&tab=ressources&tpId=" + tp.getId())
                    .severity("WARN")
                    .build());
        }

        List<TodoItemDto> overdue = new ArrayList<>();
        List<TpRessource> tpsOverdue = tpRessourceRepository.findByFormation_IdInAndTypeAndDateLimiteBeforeOrderByDateLimiteDesc(
                formationIds, TypeTpRessource.TP, now);
        for (TpRessource tp : tpsOverdue) {
            if (overdue.size() >= MAX_OVERDUE) break;
            if (tpSoumissionRepository.existsByTp_IdAndStagiaire_Id(tp.getId(), stagiaireId)) continue;
            overdue.add(TodoItemDto.builder()
                    .type("TP")
                    .title("TP en retard")
                    .message("« " + tp.getTitre() + " » – " + tp.getFormation().getNom())
                    .link("/formations?formationId=" + tp.getFormation().getId() + "&tab=ressources&tpId=" + tp.getId())
                    .severity("URGENT")
                    .build());
        }

        return TodoResponseDto.builder()
                .upcomingSeances(upcomingDtos)
                .todo(todo)
                .overdue(overdue)
                .build();
    }

    private UpcomingSeanceDto toUpcomingSeanceDto(Seance s) {
        return UpcomingSeanceDto.builder()
                .seanceId(s.getId())
                .formationId(s.getFormation().getId())
                .formationTitre(s.getFormation().getNom())
                .titre(s.getTitre())
                .dateDebut(s.getDateDebut())
                .dateFin(s.getDateFin())
                .mode(s.getMode() != null ? s.getMode().name() : null)
                .lieu(s.getLieu())
                .lienZoom(s.getZoomLink())
                .build();
    }

    private static String formatDate(LocalDateTime d) {
        return d == null ? "" : d.toLocalDate().toString();
    }
}
