package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.TypeFormateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requête de gestion utilisateur depuis l'interface "Utilisateurs".
 * - ADMIN : peut tout modifier + changer les rôles
 * - FORMATEUR : peut modifier/supprimer uniquement un STAGIAIRE ou lui-même
 */
@Data
public class UtilisateurManageRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 50)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 50)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email
    private String email;

    // ADMIN uniquement (ignoré pour FORMATEUR)
    private Role role;

    // ADMIN uniquement et seulement si role=FORMATEUR (ignoré sinon)
    private TypeFormateur typeFormateur;

    // Optionnels (profil)
    private String telephone;
    private String adresse;

    // Optionnel
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;
}
