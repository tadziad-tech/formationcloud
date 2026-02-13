package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.TypeTpRessource;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TpRessourceDTO {
	private Long id;
	private Long formationId;
	private String titre;
	private String description;
	private TypeTpRessource type;
	private String fichierUrl;
	private LocalDateTime dateLimite;
	private LocalDateTime dateCreation;
	private LocalDateTime dateModification;
}
