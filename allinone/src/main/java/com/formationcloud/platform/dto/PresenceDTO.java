package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutPresence;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PresenceDTO {
    private Long id;
    private Long seanceId;

    private UtilisateurSummaryDTO stagiaire;

    private StatutPresence statut;
    private String remarque;

    private LocalDateTime dateModification;
}
