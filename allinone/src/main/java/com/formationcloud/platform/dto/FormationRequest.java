package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutFormation;
import com.formationcloud.platform.model.TypeFormation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FormationRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

	/**
	 * Catégorie optionnelle (l'application peut fonctionner sans catégories).
	 */
	private Long categorieId;

    @NotNull(message = "Le type est obligatoire")
    private TypeFormation type;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @NotNull
    @Min(1)
    private Integer capaciteMax;

    // 0 = gratuit
    private BigDecimal prix;

    // Le service peut écraser, mais on accepte le champ pour compat front.
    private StatutFormation statut;

    // Optionnels
    private Long formateurId;
    private Long prerequisId;
}
