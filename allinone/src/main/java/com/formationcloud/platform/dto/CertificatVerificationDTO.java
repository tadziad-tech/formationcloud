package com.formationcloud.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CertificatVerificationDTO {
    private String numeroCertificat;
    private String statut;
    private LocalDate dateObtention;
    private BigDecimal noteObtenue;
    private String nomComplet;
    private String formation;
    private String formateur;
}
