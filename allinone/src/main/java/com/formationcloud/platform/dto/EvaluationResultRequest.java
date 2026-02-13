package com.formationcloud.platform.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EvaluationResultRequest {
    @NotNull
    private Long evaluationId;

    @NotNull
    @JsonAlias({"utilisateurId"})
    private Long stagiaireId;

    /**
     * Note sur 20. Peut être null si absent = true.
     */
    private BigDecimal note;

    /**
     * true => stagiaire absent (note doit être null).
     */
    private Boolean absent;

    private String commentaire;
}
