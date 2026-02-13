package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutPresence;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PresenceUpdateRequest {
    @NotNull
    private Long stagiaireId;

    @NotNull
    private StatutPresence statut;

    private String remarque;
}
