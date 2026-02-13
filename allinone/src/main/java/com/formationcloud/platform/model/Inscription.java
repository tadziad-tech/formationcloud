package com.formationcloud.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "stagiaire_id", nullable = false)
	private Utilisateur stagiaire;

	@ManyToOne
	@JoinColumn(name = "formation_id", nullable = false)
	private Formation formation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatutInscription statut = StatutInscription.EN_ATTENTE;

	@CreationTimestamp
	@Column(name = "date_inscription", updatable = false)
	private LocalDateTime dateInscription;

	@Column(name = "date_validation")
	private LocalDateTime dateValidation;

	@Column(name = "motif_refus", columnDefinition = "TEXT")
	private String motifRefus;

	@Column(name = "commentaire", columnDefinition = "TEXT")
	private String commentaire;

	@Column(name = "position_liste_attente")
	private Integer positionListeAttente;

	public boolean isValidee() {
		return statut == StatutInscription.CONFIRMEE;
	}

	public boolean isEnAttente() {
		return statut == StatutInscription.EN_ATTENTE;
	}

	public boolean isRefusee() {
		return statut == StatutInscription.REFUSEE;
	}
}
