package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutTache;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TacheDTO {
    private Long id;
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutTache statut;

    // compat front
    private Integer progression;

    private UtilisateurSummaryDTO utilisateur;
}
