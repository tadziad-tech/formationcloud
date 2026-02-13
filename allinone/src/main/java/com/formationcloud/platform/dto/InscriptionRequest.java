package com.formationcloud.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InscriptionRequest {
    @NotNull
    private Long utilisateurId;

    @NotNull
    private Long formationId;
}
