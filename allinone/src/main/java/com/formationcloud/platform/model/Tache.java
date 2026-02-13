package com.formationcloud.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tache")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tache {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Le titre est obligatoire")
	@Column(nullable = false)
	private String titre;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne
	@JoinColumn(name = "stagiaire_id", nullable = false)
	private Utilisateur stagiaire;

	@ManyToOne
	@JoinColumn(name = "formation_id")
	private Formation formation;

	@Min(value = 0, message = "Le pourcentage doit être entre 0 et 100")
	@Max(value = 100, message = "Le pourcentage doit être entre 0 et 100")
	@Column(name = "pourcentage_accomplissement")
	private Integer pourcentageAccomplissement = 0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatutTache statut = StatutTache.ASSIGNEE;

	@NotNull(message = "La date de début est obligatoire")
	@Column(name = "date_debut", nullable = false)
	private LocalDate dateDebut;

	@NotNull(message = "La date de fin est obligatoire")
	@Column(name = "date_fin", nullable = false)
	private LocalDate dateFin;

	@CreationTimestamp
	@Column(name = "date_creation", updatable = false)
	private LocalDateTime dateCreation;

	@UpdateTimestamp
	@Column(name = "date_modification")
	private LocalDateTime dateModification;

	public boolean isEnRetard() {
		return LocalDate.now().isAfter(dateFin) && statut != StatutTache.TERMINEE;
	}

	public boolean isTerminee() {
		return statut == StatutTache.TERMINEE || pourcentageAccomplissement >= 100;
	}
}
