package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.FormateurDashboardOverviewDTO;
import com.formationcloud.platform.dto.StagiaireDashboardOverviewDTO;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleDashboardService {

    private final UtilisateurRepository utilisateurRepository;
    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EvaluationRepository evaluationRepository;
    private final ResultatEvaluationRepository resultatEvaluationRepository;
    private final CertificatRepository certificatRepository;
    private final TacheRepository tacheRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public FormateurDashboardOverviewDTO getFormateurOverview(Long formateurId, int days) {
        // days = 0 (ou négatif) => mode "Tout" (pas de filtre de période)
        final boolean allMode = days <= 0;
        // Série temporelle lisible même en mode "Tout"
        final int chartDays = allMode ? 30 : Math.max(7, Math.min(days, 60));

        final LocalDate today = LocalDate.now();
        final LocalDateTime from = LocalDateTime.now().minusDays(chartDays - 1L)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        FormateurDashboardOverviewDTO dto = new FormateurDashboardOverviewDTO();

        // Formations
        List<Formation> formations = formationRepository.findByFormateur_Id(formateurId);
        long active = formations.stream().filter(f -> f.getStatut() == StatutFormation.ACTIVE).count();

        // Inscriptions
        long totalInscriptions = inscriptionRepository.countByFormateurId(formateurId);
        List<Inscription> pending = inscriptionRepository.findInscriptionsEnAttenteByFormateur(formateurId);

        // Evaluations
        List<Evaluation> evals = evaluationRepository.findByFormationFormateurId(formateurId);
        long evalTotal = evals.size();
        List<Evaluation> upcoming = evals.stream()
                .filter(e -> e.getDateEvaluation() != null && !e.getDateEvaluation().isBefore(today))
                .sorted(Comparator.comparing(Evaluation::getDateEvaluation))
                .limit(8)
                .toList();

        // Certificats (délivrés sur les formations du formateur)
        long certDelivres = certificatRepository.countByFormateurId(formateurId);

        // Totals
        FormateurDashboardOverviewDTO.Totals totals = new FormateurDashboardOverviewDTO.Totals();
        totals.setTotalFormations(formations.size());
        totals.setFormationsActives(active);
        totals.setTotalInscriptions(totalInscriptions);
        totals.setInscriptionsEnAttente(pending.size());
        totals.setTotalEvaluations(evalTotal);
        totals.setEvaluationsAVenir(upcoming.size());
        totals.setCertificatsDelivres(certDelivres);
        dto.setTotals(totals);

        // Inscriptions par jour (période)
        Map<LocalDate, Long> perDay = new LinkedHashMap<>();
        for (int i = chartDays - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0L);
        }
        for (Object[] row : inscriptionRepository.countByDaySinceForFormateur(formateurId, from)) {
            if (row == null || row.length < 2) continue;
            LocalDate d;
            try {
                Object v = row[0];
                if (v instanceof java.sql.Date sqlDate) d = sqlDate.toLocalDate();
                else d = LocalDate.parse(String.valueOf(v));
            } catch (Exception ex) {
                continue;
            }
            long c = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            if (perDay.containsKey(d)) perDay.put(d, c);
        }
        dto.setInscriptionsParJour(perDay.entrySet().stream()
                .map(e -> new FormateurDashboardOverviewDTO.TimePoint(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList()));

        // Pending inscriptions (max 10)
        dto.setInscriptionsEnAttente(pending.stream()
                .sorted(Comparator.comparing(Inscription::getDateInscription, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::toPendingInscription)
                .toList());

        // Upcoming evaluations
        dto.setEvaluationsAVenir(upcoming.stream().map(this::toSimpleEvaluation).toList());

        // Formations list (max 8)
        dto.setFormations(formations.stream()
                .sorted(Comparator.comparing(Formation::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(this::toSimpleFormation)
                .toList());

        return dto;
    }

    @Transactional(readOnly = true)
    public StagiaireDashboardOverviewDTO getStagiaireOverview(Long stagiaireId) {
        return getStagiaireOverview(stagiaireId, 14);
    }

    /**
     * Overview stagiaire.
     * Le paramètre days est utilisé côté front pour homogénéiser les dashboards,
     * même si la vue stagiaire ne nécessite pas forcément une série temporelle.
     */
    @Transactional(readOnly = true)
    public StagiaireDashboardOverviewDTO getStagiaireOverview(Long stagiaireId, int days) {
        Utilisateur u = utilisateurRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire introuvable"));

        StagiaireDashboardOverviewDTO dto = new StagiaireDashboardOverviewDTO();

        List<Inscription> allInsc = inscriptionRepository.findByStagiaire(u);
        List<Inscription> activeInsc = inscriptionRepository.findInscriptionsActivesByStagiaire(stagiaireId);
        long pending = allInsc.stream().filter(i -> i.getStatut() == StatutInscription.EN_ATTENTE).count();

        // Certificats
        long certCount = certificatRepository.countByStagiaireId(stagiaireId);

        // Tâches
        List<Tache> tasks = tacheRepository.findByStagiaire(u);
        long totalTasks = tasks.size();
        long doneTasks = tasks.stream().filter(t -> t.getStatut() == StatutTache.TERMINEE).count();
        List<Tache> lateTasks = tacheRepository.findTachesEnRetardByStagiaire(stagiaireId, LocalDate.now());

        // Notifications
        long unreadNotifs = notificationRepository.countNotificationsNonLuesByDestinataire(stagiaireId);
        List<Notification> notifs = notificationRepository.findNotificationsNonLuesByDestinataire(stagiaireId)
                .stream().limit(6).toList();

        // Evaluations à venir pour ses formations actives
        LocalDate today = LocalDate.now();
        Set<Long> formationIds = activeInsc.stream()
                .map(i -> i.getFormation() != null ? i.getFormation().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Evaluation> upcoming = new ArrayList<>();
        for (Long fid : formationIds) {
            List<Evaluation> ev = evaluationRepository.findByFormationId(fid);
            for (Evaluation e : ev) {
                if (e.getDateEvaluation() == null) continue;
                if (e.getDateEvaluation().isBefore(today)) continue;
                // ne pas proposer une évaluation déjà passée par le stagiaire
                if (resultatEvaluationRepository.existsByEvaluationAndStagiaire(e, u)) continue;
                upcoming.add(e);
            }
        }
        upcoming.sort(Comparator.comparing(Evaluation::getDateEvaluation));
        List<Evaluation> upcomingTop = upcoming.stream().limit(8).toList();

        // Totals
        StagiaireDashboardOverviewDTO.Totals totals = new StagiaireDashboardOverviewDTO.Totals();
        totals.setTotalInscriptions(allInsc.size());
        totals.setInscriptionsActives(activeInsc.size());
        totals.setInscriptionsEnAttente(pending);
        totals.setCertificatsObtenus(certCount);
        totals.setTachesTotal(totalTasks);
        totals.setTachesTerminees(doneTasks);
        totals.setTachesEnRetard(lateTasks.size());
        totals.setNotificationsNonLues(unreadNotifs);
        totals.setEvaluationsACompleter(upcoming.size());
        totals.setProgressionTaches(totalTasks == 0 ? 0 : (int) Math.round((doneTasks * 100.0) / totalTasks));
        dto.setTotals(totals);

        // lists
        dto.setInscriptions(allInsc.stream()
                .sorted(Comparator.comparing(Inscription::getDateInscription, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(this::toSimpleInscription)
                .toList());

        dto.setTachesPrioritaires(tasks.stream()
                .filter(t -> t.getStatut() != StatutTache.TERMINEE)
                .sorted(Comparator.comparing(Tache::getDateFin, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(6)
                .map(this::toSimpleTache)
                .toList());

        dto.setEvaluations(upcomingTop.stream().map(this::toSimpleEvaluationStagiaire).toList());

        dto.setCertificats(certificatRepository.findByStagiaire(u).stream()
                .sorted(Comparator.comparing(Certificat::getDateObtention, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(this::toSimpleCertificat)
                .toList());

        dto.setNotifications(notifs.stream().map(this::toSimpleNotification).toList());

        return dto;
    }

    private FormateurDashboardOverviewDTO.PendingInscription toPendingInscription(Inscription i) {
        Utilisateur s = i.getStagiaire();
        Formation f = i.getFormation();
        return new FormateurDashboardOverviewDTO.PendingInscription(
                i.getId(),
                i.getDateInscription() != null ? i.getDateInscription().toString() : null,
                i.getStatut() != null ? i.getStatut().name() : null,
                s != null ? new FormateurDashboardOverviewDTO.SimpleUser(s.getId(), s.getNom(), s.getPrenom(), s.getEmail()) : null,
                f != null ? toSimpleFormation(f) : null
        );
    }

    private FormateurDashboardOverviewDTO.SimpleFormation toSimpleFormation(Formation f) {
        long inscrits = 0;
        try {
            inscrits = inscriptionRepository.countParticipantsForFormation(f.getId());
        } catch (Exception ignored) {}
        return new FormateurDashboardOverviewDTO.SimpleFormation(
                f.getId(),
                f.getNom(),
                f.getStatut() != null ? f.getStatut().name() : null,
                f.getCategorie() != null ? f.getCategorie().getNom() : null,
                f.getCapaciteMax(),
                inscrits
        );
    }

    private FormateurDashboardOverviewDTO.SimpleEvaluation toSimpleEvaluation(Evaluation e) {
        Formation f = e.getFormation();
        Double seuil = e.getSeuilReussite() != null ? e.getSeuilReussite().doubleValue() : null;
        return new FormateurDashboardOverviewDTO.SimpleEvaluation(
                e.getId(),
                e.getTitre(),
                e.getDateEvaluation() != null ? e.getDateEvaluation().toString() : null,
                e.getDureeMinutes(),
                seuil,
                f != null ? toSimpleFormation(f) : null
        );
    }

    private StagiaireDashboardOverviewDTO.SimpleInscription toSimpleInscription(Inscription i) {
        Formation f = i.getFormation();
        return new StagiaireDashboardOverviewDTO.SimpleInscription(
                i.getId(),
                i.getDateInscription() != null ? i.getDateInscription().toString() : null,
                i.getStatut() != null ? i.getStatut().name() : null,
                f != null ? f.getId() : null,
                f != null ? f.getNom() : null
        );
    }

    private StagiaireDashboardOverviewDTO.SimpleTache toSimpleTache(Tache t) {
        return new StagiaireDashboardOverviewDTO.SimpleTache(
                t.getId(),
                t.getTitre(),
                t.getDateFin() != null ? t.getDateFin().toString() : null,
                t.getStatut() != null ? t.getStatut().name() : null,
                t.getPourcentageAccomplissement()
        );
    }

    private StagiaireDashboardOverviewDTO.SimpleEvaluation toSimpleEvaluationStagiaire(Evaluation e) {
        Formation f = e.getFormation();
        Double seuil = e.getSeuilReussite() != null ? e.getSeuilReussite().doubleValue() : null;
        return new StagiaireDashboardOverviewDTO.SimpleEvaluation(
                e.getId(),
                e.getTitre(),
                e.getDateEvaluation() != null ? e.getDateEvaluation().toString() : null,
                e.getDureeMinutes(),
                seuil,
                f != null ? f.getId() : null,
                f != null ? f.getNom() : null
        );
    }

    private StagiaireDashboardOverviewDTO.SimpleCertificat toSimpleCertificat(Certificat c) {
        Formation f = c.getFormation();
        Double note = c.getNoteFinale() != null ? c.getNoteFinale().doubleValue() : null;
        return new StagiaireDashboardOverviewDTO.SimpleCertificat(
                c.getId(),
                c.getNumeroUnique(),
                c.getDateObtention() != null ? c.getDateObtention().toString() : null,
                note,
                f != null ? f.getId() : null,
                f != null ? f.getNom() : null
        );
    }

    private StagiaireDashboardOverviewDTO.SimpleNotification toSimpleNotification(Notification n) {
        String titre;
        try {
            // L'entité Notification ne contient pas un champ "titre" dans ce projet.
            // On génère donc un titre lisible à partir du type.
            titre = (n.getType() != null) ? formatNotificationTitle(n.getType().name()) : "Notification";
        } catch (Exception ex) {
            titre = "Notification";
        }
        return new StagiaireDashboardOverviewDTO.SimpleNotification(
                n.getId(),
                titre,
                n.getMessage(),
                n.getDateCreation() != null ? n.getDateCreation().toString() : null
        );
    }

    private String formatNotificationTitle(String typeName) {
        // Ex: INSCRIPTION -> "Inscription"
        if (typeName == null || typeName.isBlank()) return "Notification";
        String s = typeName.toLowerCase().replace('_', ' ');
        // capitalize first letter
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
