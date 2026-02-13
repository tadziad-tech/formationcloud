package com.formationcloud.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CertificatDTO {
    private Long id;
    private String numeroCertificat;
    private LocalDate dateObtention;
    private LocalDate dateRevocation;
    private BigDecimal noteObtenue;
    private String statut;
    private String urlPdf;
    private FormationSummaryDTO formation;
    /**
     * Pour l'admin/formateur : afficher "délivré à".
     * Pour un stagiaire, ce champ peut rester null.
     */
    private UtilisateurSummaryDTO stagiaire;
}
