package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO unique pour alimenter un dashboard admin "vrai" (données 100% BD).
 * L'idée: 1 seul appel API = UI rapide et fiable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardOverviewDTO {

    private Totals totals = new Totals();

    private List<LabelCount> utilisateursParRole = new ArrayList<>();
    private List<LabelCount> formationsActivesParType = new ArrayList<>();
    private List<LabelCount> formationsActivesParCategorie = new ArrayList<>();

    private List<LabelCount> inscriptionsParStatut = new ArrayList<>();
    private List<TimePoint> inscriptionsParJour = new ArrayList<>();
    private List<RecentInscription> dernieresInscriptions = new ArrayList<>();

    private List<RecentCertificat> derniersCertificats = new ArrayList<>();

    private List<RecentEvaluation> dernieresEvaluations = new ArrayList<>();
    private EvaluationSummary evaluationSummary = new EvaluationSummary();

    /** Dernières formations (pour l'affichage sur le dashboard sans appeler d'autres endpoints) */
    private List<SimpleFormation> dernieresFormations = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private long totalUtilisateurs;
        private long totalAdmins;
        private long totalFormateurs;
        private long totalStagiaires;

        private long formationsActives;
        private long totalFormations;

        private long totalInscriptions;
        /** Inscriptions sur la période (jours demandés) */
        private long inscriptionsSurPeriode;
        private long totalCertificats;
        /** Certificats délivrés sur la période (jours demandés) */
        private long certificatsSurPeriode;
        private long totalEvaluations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelCount {
        private String label;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimePoint {
        /** ISO date yyyy-MM-dd */
        private String date;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentInscription {
        private Long id;
        private String dateInscription;
        private String statut;
        private SimpleUser stagiaire;
        private SimpleFormation formation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentCertificat {
        private Long id;
        private String numeroUnique;
        private String dateObtention;
        private Double noteFinale;
        private SimpleUser stagiaire;
        private SimpleFormation formation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentEvaluation {
        private Long id;
        private String titre;
        private String dateEvaluation;
        private Integer dureeMinutes;
        private Double seuilReussite;
        private SimpleFormation formation;
        // Agrégats "dashboard"
        private long participantsCibles;
        private long participationsRecues;
        private long reussites;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationSummary {
        /** Evaluations passées (dateEvaluation < today) */
        private long evaluationsPassees;
        /** Evaluations à venir ou du jour (dateEvaluation >= today) */
        private long evaluationsAVenir;

        /** Total participants attendus (basé sur inscriptions confirmées/en cours/terminées) */
        private long participantsAttendus;
        /** Total résultats reçus */
        private long participantsAyantPasse;
        /** Total réussites (reussi=true) */
        private long participantsReussis;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleUser {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleFormation {
        private Long id;
        private String titre;
        private String type;
        private String statut;
        private String categorie;
    }
}
