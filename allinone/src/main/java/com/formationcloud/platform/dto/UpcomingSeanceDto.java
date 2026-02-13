package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingSeanceDto {
    private Long seanceId;
    private Long formationId;
    private String formationTitre;
    private String titre;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String mode;
    private String lieu;
    private String lienZoom;
}
