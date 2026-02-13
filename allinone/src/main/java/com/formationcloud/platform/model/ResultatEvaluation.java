package com.formationcloud.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resultat_evaluation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultatEvaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "evaluation_id", nullable = false)
	@JsonIgnore
	private Evaluation evaluation;

	@ManyToOne
	@JoinColumn(name = "stagiaire_id", nullable = false)
	private Utilisateur stagiaire;

	@Min(value = 0, message = "La note doit être entre 0 et 20")
	@Max(value = 20, message = "La note doit être entre 0 et 20")
	@Column(name = "note", nullable = true, precision = 10, scale = 2)
	private BigDecimal note;

	/**
	 * Si ABSENT = true, alors la note peut être null.
	 * On considère que l'étudiant n'a pas réussi (reussi=false).
	 */
	@Column(name = "absent", nullable = false)
	private Boolean absent = Boolean.FALSE;

	@CreationTimestamp
	@Column(name = "date_passage", updatable = false)
	private LocalDateTime datePassage;

	@Column(columnDefinition = "TEXT")
	private String commentaire;

	@Column(name = "reussi")
	private Boolean reussi;

	public boolean isReussi() {
		if (Boolean.TRUE.equals(absent)) return false;
		return reussi != null && reussi;
	}
}
