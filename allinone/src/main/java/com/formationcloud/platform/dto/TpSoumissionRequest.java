package com.formationcloud.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TpSoumissionRequest {
	@NotBlank(message = "L'URL du fichier soumis est obligatoire")
	private String fichierSoumisUrl;

	/** Commentaire optionnel du stagiaire (conservé, non écrasé par la correction). */
	private String commentaire;
}
