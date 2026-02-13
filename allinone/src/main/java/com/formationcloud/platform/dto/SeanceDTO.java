package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.ModeSeance;
import com.formationcloud.platform.model.StatutSeance;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeanceDTO {
    private Long id;
    private Long formationId;

    private String titre;
    private String description;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private ModeSeance mode;
    private String zoomLink;
    private String lieu;

    private StatutSeance statut;
}
