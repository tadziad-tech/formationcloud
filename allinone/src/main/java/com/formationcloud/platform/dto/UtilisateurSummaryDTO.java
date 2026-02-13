package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import lombok.Data;

@Data
public class UtilisateurSummaryDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private String photoProfil;
}
