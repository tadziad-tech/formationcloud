package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormationMeAccessDTO {
    private boolean isAssignedFormateur;
    private boolean isEnrolledStagiaire;
}
