package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.TypeTpRessource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TpRessourceRequest {
	@NotBlank(message = "Le titre est obligatoire")
	private String titre;

	private String description;

	@NotNull(message = "Le type est obligatoire")
	private TypeTpRessource type;

	private String fichierUrl;

	private LocalDateTime dateLimite;
}
