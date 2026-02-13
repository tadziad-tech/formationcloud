package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard Stagiaire (1 seul appel API) - données 100% BD.
 * Objectif: vue perso + à faire + progression.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StagiaireDashboardOverviewDTO {

    private Totals totals = new Totals();

    /** Dernières inscriptions de l'utilisateur */
    private List<SimpleInscription> inscriptions = new ArrayList<>();

    /** Tâches prioritaires (retard / à venir) */
    private List<SimpleTache> tachesPrioritaires = new ArrayList<>();

    /** Évaluations à venir / à compléter */
    private List<SimpleEvaluation> evaluations = new ArrayList<>();

    /** Certificats récents */
    private List<SimpleCertificat> certificats = new ArrayList<>();

    /** Notifications non lues */
    private List<SimpleNotification> notifications = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private long totalInscriptions;
        private long inscriptionsActives;
        private long inscriptionsEnAttente;

        private long certificatsObtenus;

        private long tachesTotal;
        private long tachesTerminees;
        private long tachesEnRetard;
        private long notificationsNonLues;

        private long evaluationsACompleter;

        /** Progression globale des tâches (0-100) */
        private int progressionTaches;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleInscription {
        private Long id;
        private String dateInscription;
        private String statut;
        private Long formationId;
        private String formationTitre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleTache {
        private Long id;
        private String titre;
        private String dateFin;
        private String statut;
        private Integer progression;
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
        private Long formationId;
        private String formationTitre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleCertificat {
        private Long id;
        private String numeroUnique;
        private String dateObtention;
        private Double noteFinale;
        private Long formationId;
        private String formationTitre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleNotification {
        private Long id;
        private String titre;
        private String message;
        private String dateCreation;
    }
}
