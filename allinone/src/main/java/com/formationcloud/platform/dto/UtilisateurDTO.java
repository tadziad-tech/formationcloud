package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.TypeFormateur;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UtilisateurDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private TypeFormateur typeFormateur;
    private String photoProfil;
    private Boolean valide;
    private LocalDateTime dateCreation;
}
