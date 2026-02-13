package com.formationcloud.platform.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PublishEvaluationRequest {
    /**
     * Date de la session de rattrapage (obligatoire à la publication d'une session NORMAL
     * lorsqu'il y a au moins un stagiaire en échec/absent).
     */
    private LocalDate dateRattrapage;
}
