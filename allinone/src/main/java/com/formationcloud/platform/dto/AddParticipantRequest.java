package com.formationcloud.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddParticipantRequest {
    @NotNull
    private Long stagiaireId;
}
