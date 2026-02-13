package com.formationcloud.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EvaluationRequest {

    @NotNull
    private Long formationId;

    @NotBlank
    private String titre;

    private String description;

    @NotNull
    private BigDecimal seuilReussite;

    // Front envoie "dateLimite"
    @NotNull
    private LocalDate dateLimite;

    // NORMAL / RATTRAPAGE (optionnel)
    private String sessionType;
}
