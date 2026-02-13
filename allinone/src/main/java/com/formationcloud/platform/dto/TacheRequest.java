package com.formationcloud.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TacheRequest {

    @NotBlank
    private String titre;

    private String description;

    @NotNull
    private Long utilisateurId;

    @NotNull
    private LocalDate dateDebut;

    @NotNull
    private LocalDate dateFin;
}
