package com.formationcloud.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tp_soumission", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"tp_id", "stagiaire_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TpSoumission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "tp_id", nullable = false)
	@NotNull
	private TpRessource tp;

	@ManyToOne
	@JoinColumn(name = "stagiaire_id", nullable = false)
	@NotNull
	private Utilisateur stagiaire;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@NotNull
	private StatutTpSoumission statut = StatutTpSoumission.SOUMIS;

	@Column(name = "fichier_soumis_url")
	private String fichierSoumisUrl;

	@Column(name = "commentaire", columnDefinition = "TEXT")
	private String commentaire;

	@Column(name = "feedback", columnDefinition = "TEXT")
	private String feedback;

	@Column(name = "note", precision = 10, scale = 2)
	private BigDecimal note;

	@CreationTimestamp
	@Column(name = "date_soumission", updatable = false)
	private LocalDateTime dateSoumission;

	@UpdateTimestamp
	@Column(name = "date_modification")
	private LocalDateTime dateModification;
}
