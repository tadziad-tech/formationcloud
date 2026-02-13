package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutTpSoumission;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TpSoumissionDTO {
	private Long id;
	private Long tpId;
	private Long stagiaireId;
	private UtilisateurSummaryDTO stagiaire;
	private StatutTpSoumission statut;
	private String fichierSoumisUrl;
	private String commentaire;
	private String feedback;
	private BigDecimal note;
	private LocalDateTime dateSoumission;
	private LocalDateTime dateModification;
}
