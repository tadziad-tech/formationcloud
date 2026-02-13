package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.AdminDashboardOverviewDTO;
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
public class AdminDashboardService {

    private final UtilisateurRepository utilisateurRepository;
    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final CertificatRepository certificatRepository;
    private final EvaluationRepository evaluationRepository;
    private final ResultatEvaluationRepository resultatEvaluationRepository;

    @Transactional(readOnly = true)
    public AdminDashboardOverviewDTO getOverview(int days) {
        // days = 0 (ou négatif) => mode "Tout" (pas de filtre de période)
        final boolean allMode = days <= 0;
        // La série temporelle reste limitée (lisible) même en mode "Tout"
        final int chartDays = allMode ? 30 : Math.max(7, Math.min(days, 60));

        final LocalDate today = LocalDate.now();
        final LocalDateTime from = LocalDateTime.now().minusDays(chartDays - 1L)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        AdminDashboardOverviewDTO dto = new AdminDashboardOverviewDTO();

        // ===== Totals =====
        AdminDashboardOverviewDTO.Totals totals = new AdminDashboardOverviewDTO.Totals();
        totals.setTotalUtilisateurs(utilisateurRepository.count());
        totals.setTotalAdmins(utilisateurRepository.countByRole(Role.ADMIN));
        totals.setTotalFormateurs(utilisateurRepository.countByRole(Role.FORMATEUR));
        totals.setTotalStagiaires(utilisateurRepository.countByRole(Role.STAGIAIRE));

        totals.setTotalFormations(formationRepository.count());
        totals.setFormationsActives(formationRepository.countByStatut(StatutFormation.ACTIVE));

        long totalInsc = inscriptionRepository.count();
        totals.setTotalInscriptions(totalInsc);
        totals.setInscriptionsSurPeriode(allMode ? totalInsc : inscriptionRepository.countByDateInscriptionGreaterThanEqual(from));

        long totalCerts = certificatRepository.count();
        totals.setTotalCertificats(totalCerts);
        totals.setCertificatsSurPeriode(allMode ? totalCerts : certificatRepository.countByDateObtentionGreaterThanEqual(today.minusDays(chartDays - 1L)));
        totals.setTotalEvaluations(evaluationRepository.count());
        dto.setTotals(totals);

        // ===== Users by role (percent côté front) =====
        dto.setUtilisateursParRole(Arrays.stream(Role.values())
                .map(r -> new AdminDashboardOverviewDTO.LabelCount(r.name(), utilisateurRepository.countByRole(r)))
                .collect(Collectors.toList()));

        // ===== Formations (ACTIVE) breakdown =====
        List<AdminDashboardOverviewDTO.LabelCount> byType = new ArrayList<>();
        for (Object[] row : formationRepository.countActiveByType()) {
            String label = String.valueOf(row[0]);
            long count = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            byType.add(new AdminDashboardOverviewDTO.LabelCount(label, count));
        }
        dto.setFormationsActivesParType(byType);

        List<AdminDashboardOverviewDTO.LabelCount> byCat = new ArrayList<>();
        for (Object[] row : formationRepository.countActiveByCategorieName()) {
            String label = String.valueOf(row[0]);
            long count = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            byCat.add(new AdminDashboardOverviewDTO.LabelCount(label, count));
        }
        long noCat = formationRepository.countActiveWithoutCategorie();
        if (noCat > 0) byCat.add(new AdminDashboardOverviewDTO.LabelCount("Sans catégorie", noCat));
        dto.setFormationsActivesParCategorie(byCat);

        // ===== Inscriptions =====
        List<AdminDashboardOverviewDTO.LabelCount> byStatut = new ArrayList<>();
        for (Object[] row : inscriptionRepository.countAllByStatut()) {
            String label = String.valueOf(row[0]);
            long count = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            byStatut.add(new AdminDashboardOverviewDTO.LabelCount(label, count));
        }
        dto.setInscriptionsParStatut(byStatut);

        // time series (last N days) - group in Java for portability
        Map<LocalDate, Long> perDay = new LinkedHashMap<>();
        for (int i = chartDays - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0L);
        }
        // Series: DB aggregation (plus léger que findAll)
        for (Object[] row : inscriptionRepository.countByDaySince(from)) {
            if (row == null || row.length < 2) continue;
            LocalDate d;
            try {
                // MySQL/H2 can return java.sql.Date or String
                Object v = row[0];
                if (v instanceof java.sql.Date sqlDate) d = sqlDate.toLocalDate();
                else d = LocalDate.parse(String.valueOf(v));
            } catch (Exception ex) {
                continue;
            }
            long c = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            if (perDay.containsKey(d)) perDay.put(d, c);
        }
        List<AdminDashboardOverviewDTO.TimePoint> points = perDay.entrySet().stream()
                .map(e -> new AdminDashboardOverviewDTO.TimePoint(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList());
        dto.setInscriptionsParJour(points);

        // latest inscriptions table
        List<Inscription> latestInsc = inscriptionRepository.findTop8ByOrderByDateInscriptionDesc();
        dto.setDernieresInscriptions(latestInsc.stream().map(this::toRecentInscription).collect(Collectors.toList()));

        // ===== Certificats =====
        List<Certificat> latestCerts = certificatRepository.findTop8ByOrderByDateObtentionDesc();
        dto.setDerniersCertificats(latestCerts.stream().map(this::toRecentCertificat).collect(Collectors.toList()));

        // ===== Evaluations =====
        AdminDashboardOverviewDTO.EvaluationSummary evalSum = new AdminDashboardOverviewDTO.EvaluationSummary();
        evalSum.setEvaluationsPassees(evaluationRepository.countByDateEvaluationBefore(today));
        evalSum.setEvaluationsAVenir(evaluationRepository.countByDateEvaluationGreaterThanEqual(today));

        List<Evaluation> latestEvals = evaluationRepository.findTop8ByOrderByDateCreationDesc();
        List<AdminDashboardOverviewDTO.RecentEvaluation> evalDtos = new ArrayList<>();

        long expected = 0;
        long done = 0;
        long success = 0;

        for (Evaluation e : latestEvals) {
            long participants = 0;
            if (e.getFormation() != null && e.getFormation().getId() != null) {
                participants = inscriptionRepository.countParticipantsForFormation(e.getFormation().getId());
            }
            long received = resultatEvaluationRepository.countByEvaluation_Id(e.getId());
            long ok = resultatEvaluationRepository.countReussisByEvaluationId(e.getId());
            expected += participants;
            done += received;
            success += ok;
            evalDtos.add(toRecentEvaluation(e, participants, received, ok));
        }

        evalSum.setParticipantsAttendus(expected);
        evalSum.setParticipantsAyantPasse(done);
        evalSum.setParticipantsReussis(success);
        dto.setEvaluationSummary(evalSum);
        dto.setDernieresEvaluations(evalDtos);

        // ===== Dernières formations =====
        dto.setDernieresFormations(formationRepository.findTop5ByOrderByDateCreationDesc().stream()
                .map(f -> new AdminDashboardOverviewDTO.SimpleFormation(
                        f.getId(),
                        safeTitre(f),
                        f.getType() != null ? f.getType().name() : null,
                        f.getStatut() != null ? f.getStatut().name() : null,
                        (f.getCategorie() != null ? f.getCategorie().getNom() : null)
                ))
                .collect(Collectors.toList()));

        return dto;
    }

    private AdminDashboardOverviewDTO.RecentInscription toRecentInscription(Inscription i) {
        Utilisateur u = i.getStagiaire();
        Formation f = i.getFormation();
        return new AdminDashboardOverviewDTO.RecentInscription(
                i.getId(),
                i.getDateInscription() != null ? i.getDateInscription().toString() : null,
                i.getStatut() != null ? i.getStatut().name() : null,
                u != null ? new AdminDashboardOverviewDTO.SimpleUser(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole() != null ? u.getRole().name() : null) : null,
                f != null ? new AdminDashboardOverviewDTO.SimpleFormation(f.getId(), safeTitre(f), f.getType() != null ? f.getType().name() : null, f.getStatut() != null ? f.getStatut().name() : null, (f.getCategorie() != null ? f.getCategorie().getNom() : null)) : null
        );
    }

    private AdminDashboardOverviewDTO.RecentCertificat toRecentCertificat(Certificat c) {
        Utilisateur u = c.getStagiaire();
        Formation f = c.getFormation();
        Double note = c.getNoteFinale() != null ? c.getNoteFinale().doubleValue() : null;
        return new AdminDashboardOverviewDTO.RecentCertificat(
                c.getId(),
                c.getNumeroUnique(),
                c.getDateObtention() != null ? c.getDateObtention().toString() : null,
                note,
                u != null ? new AdminDashboardOverviewDTO.SimpleUser(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole() != null ? u.getRole().name() : null) : null,
                f != null ? new AdminDashboardOverviewDTO.SimpleFormation(f.getId(), safeTitre(f), f.getType() != null ? f.getType().name() : null, f.getStatut() != null ? f.getStatut().name() : null, (f.getCategorie() != null ? f.getCategorie().getNom() : null)) : null
        );
    }

    private AdminDashboardOverviewDTO.RecentEvaluation toRecentEvaluation(Evaluation e, long participants, long received, long ok) {
        Formation f = e.getFormation();
        Double seuil = e.getSeuilReussite() != null ? e.getSeuilReussite().doubleValue() : null;
        return new AdminDashboardOverviewDTO.RecentEvaluation(
                e.getId(),
                e.getTitre(),
                e.getDateEvaluation() != null ? e.getDateEvaluation().toString() : null,
                e.getDureeMinutes(),
                seuil,
                f != null ? new AdminDashboardOverviewDTO.SimpleFormation(f.getId(), safeTitre(f), f.getType() != null ? f.getType().name() : null, f.getStatut() != null ? f.getStatut().name() : null, (f.getCategorie() != null ? f.getCategorie().getNom() : null)) : null,
                participants,
                received,
                ok
        );
    }

    private String safeTitre(Formation f) {
        // compat: certains endroits parlent de "titre", d'autres "nom"
        if (f.getNom() != null && !f.getNom().isBlank()) return f.getNom();
        try {
            // si DTO précédent a ajouté un getter titre quelque part
            return f.getNom();
        } catch (Exception ex) {
            return "Formation";
        }
    }
}
