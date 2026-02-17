package com.formationcloud.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Le nom est obligatoire")
	@Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères")
	@Column(nullable = false, length = 50)
	private String nom;

	@NotBlank(message = "Le prénom est obligatoire")
	@Size(max = 50, message = "Le prénom ne doit pas dépasser 50 caractères")
	@Column(nullable = false, length = 50)
	private String prenom;

	@NotBlank(message = "L'email est obligatoire")
	@Email(message = "L'email doit être valide")
	@Column(unique = true, nullable = false)
	private String email;

	@NotBlank(message = "Le mot de passe est obligatoire")
	@Column(nullable = false)
	@JsonIgnore
	private String motDePasse;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "type_formateur")
	private TypeFormateur typeFormateur;

	@Column(name = "statut_validation")
	private Boolean statutValidation = false;

	@Column(name = "telephone", length = 20)
	private String telephone;

	@Column(name = "adresse")
	private String adresse;

	@Column(name = "photo_profil")
	private String photoProfil;

	@Column(name = "actif")
	private Boolean actif = true;

	@CreationTimestamp
	@Column(name = "date_creation", updatable = false)
	private LocalDateTime dateCreation;

	@UpdateTimestamp
	@Column(name = "date_modification")
	private LocalDateTime dateModification;

	// Relations
	@OneToMany(mappedBy = "formateur", cascade = CascadeType.ALL)
		@JsonIgnore
	private List<Formation> formationsAssignees = new ArrayList<>();

	@OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL)
		@JsonIgnore
	private List<Inscription> inscriptions = new ArrayList<>();

	@OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL)
		@JsonIgnore
	private List<Certificat> certificats = new ArrayList<>();

	@OneToMany(mappedBy = "destinataire", cascade = CascadeType.ALL)
		@JsonIgnore
	private List<Notification> notifications = new ArrayList<>();

	@OneToMany(mappedBy = "stagiaire", cascade = CascadeType.ALL)
		@JsonIgnore
	private List<TpSoumission> tpSoumissions = new ArrayList<>();

	// Méthodes utilitaires
	public String getNomComplet() {
		return prenom + " " + nom;
	}

	public boolean isFormateur() {
		return role == Role.FORMATEUR;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	public boolean isStagiaire() {
		return role == Role.STAGIAIRE;
	}
}