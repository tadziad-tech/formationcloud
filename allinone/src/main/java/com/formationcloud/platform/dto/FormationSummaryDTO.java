package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutFormation;
import com.formationcloud.platform.model.TypeFormation;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FormationSummaryDTO {
    private Long id;
    private String titre;
    private TypeFormation type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutFormation statut;
}
