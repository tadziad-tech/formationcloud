package com.formationcloud.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EvaluationDTO {
    private Long id;
    private String titre;
    private String description;
    private BigDecimal seuilReussite;
    private LocalDate dateLimite;
    private String sessionType;
    private String etat;
    private Long parentEvaluationId;
    private java.time.LocalDateTime datePublicationNotes;
    private FormationSummaryDTO formation;

    // ---- UI Pro (stats) ----
    // Permet d'afficher l'avancement de saisie et des compteurs (sans requêtes supplémentaires côté front).
    private Integer participantsTotal;
    private Integer notesSaisies;
    private Integer notesManquantes;
    private Integer validesCount;
    private Integer echecsCount;
    private Integer absentsCount;

    // Si session normale : id du rattrapage créé (si existant)
    private Long rattrapageId;
}
