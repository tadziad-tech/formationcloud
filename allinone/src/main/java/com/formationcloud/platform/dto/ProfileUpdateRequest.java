package com.formationcloud.platform.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Mise à jour du profil par l'utilisateur connecté.
 * On évite de permettre la modification du rôle ou de l'email ici.
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 50)
    private String nom;

    @Size(max = 50)
    private String prenom;

    @Size(max = 30)
    private String telephone;

    @Size(max = 255)
    private String adresse;

    /**
     * Optionnel : changer le mot de passe depuis le profil.
     * Si vide => pas de changement.
     */
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String nouveauMotDePasse;
}
