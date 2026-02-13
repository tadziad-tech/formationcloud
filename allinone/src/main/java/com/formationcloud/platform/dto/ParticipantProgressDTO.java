package com.formationcloud.platform.dto;

import lombok.Data;

@Data
public class ParticipantProgressDTO {
    private UtilisateurSummaryDTO utilisateur;
    private Integer progression;      // 0..100
    private Double tauxPresence;      // 0..1 (nullable)
    private Integer seancesTotal;     // séances prises en compte
    private Integer absences;
    private Integer nonMarque;
    private Double meilleureNote;     // /20 (nullable)
}
