package com.formationcloud.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TpCorrectionRequest {
	@NotNull(message = "Le statut est obligatoire")
	private com.formationcloud.platform.model.StatutTpSoumission statut;

	@Min(value = 0, message = "La note doit être au moins 0")
	@Max(value = 20, message = "La note doit être au maximum 20")
	private BigDecimal note;

	private String commentaire;
}
