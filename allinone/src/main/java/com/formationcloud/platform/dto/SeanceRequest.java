package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.ModeSeance;
import com.formationcloud.platform.model.StatutSeance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeanceRequest {
    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime dateFin;

    @NotNull(message = "Le mode est obligatoire")
    private ModeSeance mode;

    private String zoomLink;
    private String lieu;

    private StatutSeance statut;
}
