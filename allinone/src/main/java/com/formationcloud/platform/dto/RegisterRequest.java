package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.TypeFormateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

	@NotBlank(message = "Le nom est obligatoire")
	@Size(max = 50)
	private String nom;

	@NotBlank(message = "Le prénom est obligatoire")
	@Size(max = 50)
	private String prenom;

	@NotBlank(message = "L'email est obligatoire")
	@Email(message = "L'email doit être valide")
	private String email;

	@NotBlank(message = "Le mot de passe est obligatoire")
	@Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
	private String motDePasse;

	@NotNull(message = "Le rôle est obligatoire")
	private Role role;

	private TypeFormateur typeFormateur;

	private String telephone;

	private String adresse;

	// Photo de profil : gérée depuis la page Profil via upload (pas à l'inscription)
}
