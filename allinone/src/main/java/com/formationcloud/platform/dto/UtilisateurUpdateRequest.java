package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.TypeFormateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UtilisateurUpdateRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 50)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 50)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email
    private String email;

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    private TypeFormateur typeFormateur;

    // Optionnel en update
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;
}
