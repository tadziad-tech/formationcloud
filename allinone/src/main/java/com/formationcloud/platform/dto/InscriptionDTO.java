package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.StatutInscription;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InscriptionDTO {
    private Long id;
    private LocalDateTime dateInscription;
    private StatutInscription statut;

    private UtilisateurSummaryDTO utilisateur;
    private FormationSummaryDTO formation;
}
