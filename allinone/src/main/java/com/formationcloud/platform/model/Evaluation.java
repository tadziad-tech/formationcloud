package com.formationcloud.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "formation_id", nullable = false)
	private Formation formation;

	@NotBlank(message = "Le titre est obligatoire")
	@Column(nullable = false)
	private String titre;

	@Column(columnDefinition = "TEXT")
	private String description;

	@NotNull(message = "Le seuil de réussite est obligatoire")
	@Min(value = 0, message = "Le seuil doit être entre 0 et 20")
	@Max(value = 20, message = "Le seuil doit être entre 0 et 20")
	@Column(name = "seuil_reussite", nullable = false)
	private java.math.BigDecimal seuilReussite;

	@NotNull(message = "La date d'évaluation est obligatoire")
	@Column(name = "date_evaluation", nullable = false)
	private LocalDate dateEvaluation;

	@Column(name = "duree_minutes")
	private Integer dureeMinutes;

	@CreationTimestamp
	@Column(name = "date_creation", updatable = false)
	private LocalDateTime dateCreation;


	@Enumerated(EnumType.STRING)
	@Column(name = "session_type", nullable = false)
	private SessionEvaluationType sessionType = SessionEvaluationType.NORMAL;

	@Enumerated(EnumType.STRING)
	@Column(name = "etat", nullable = false)
	private EtatEvaluation etat = EtatEvaluation.EN_COURS;

	@ManyToOne
	@JoinColumn(name = "parent_evaluation_id")
	@lombok.ToString.Exclude
	@lombok.EqualsAndHashCode.Exclude
	private Evaluation parentEvaluation;

	@Column(name = "date_publication_notes")
	private LocalDateTime datePublicationNotes;


	@OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL)
		@JsonIgnore
	private List<ResultatEvaluation> resultats = new ArrayList<>();
}