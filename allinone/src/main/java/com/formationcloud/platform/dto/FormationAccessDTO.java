package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormationAccessDTO {
    /** ADMIN, FORMATEUR or STAGIAIRE */
    private String role;
    private boolean isAdmin;
    private boolean isAssignedFormateur;
    /** e.g. EN_ATTENTE, CONFIRMEE, EN_COURS, TERMINEE, REFUSEE, ABANDONNEE; null if no inscription */
    private String inscriptionStatus;
    /** true if inscriptionStatus is CONFIRMEE, EN_COURS or TERMINEE */
    private boolean isEnrolledConfirmed;
}
