package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard Formateur (1 seul appel API) - données 100% BD.
 * Objectif: pilotage opérationnel (validation inscriptions) + aperçu d'activité.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormateurDashboardOverviewDTO {

    private Totals totals = new Totals();

    /** Inscriptions en attente de validation sur les formations du formateur */
    private List<PendingInscription> inscriptionsEnAttente = new ArrayList<>();

    /** Inscriptions par jour (sur une période) */
    private List<TimePoint> inscriptionsParJour = new ArrayList<>();

    /** Prochaines évaluations */
    private List<SimpleEvaluation> evaluationsAVenir = new ArrayList<>();

    /** Formations récentes / actives */
    private List<SimpleFormation> formations = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private long totalFormations;
        private long formationsActives;

        private long totalInscriptions;
        private long inscriptionsEnAttente;

        private long totalEvaluations;
        private long evaluationsAVenir;

        private long certificatsDelivres;
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
    public static class SimpleUser {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleFormation {
        private Long id;
        private String titre;
        private String statut;
        private String categorie;
        private Integer capaciteMax;
        private Long inscrits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingInscription {
        private Long id;
        private String dateInscription;
        private String statut;
        private SimpleUser stagiaire;
        private SimpleFormation formation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleEvaluation {
        private Long id;
        private String titre;
        private String dateEvaluation;
        private Integer dureeMinutes;
        private Double seuilReussite;
        private SimpleFormation formation;
    }
}
