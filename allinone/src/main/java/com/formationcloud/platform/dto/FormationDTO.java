package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutFormation;
import com.formationcloud.platform.model.TypeFormation;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FormationDTO {
    private Long id;
    private String titre;
    private String description;
    private TypeFormation type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer capaciteMax;
    private Integer dureeHeures;
    private BigDecimal prix;
    private StatutFormation statut;

    private CategorieDTO categorie;
    private UtilisateurSummaryDTO formateur;
    private FormationSummaryDTO prerequis;

    private Long nombreInscrits;
}
