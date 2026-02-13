package com.formationcloud.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String numeroUnique;

	@ManyToOne(optional = false)
	@JoinColumn(name = "stagiaire_id")
	private Utilisateur stagiaire;

	@ManyToOne(optional = false)
	@JoinColumn(name = "formation_id")
	private Formation formation;

	private LocalDate dateObtention;

	private java.math.BigDecimal noteFinale; //

	@Column(name = "url_pdf")
	private String urlPdf;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private CertificatStatut statut = CertificatStatut.VALIDE;

	@Column(name = "date_revocation")
	private LocalDate dateRevocation;

	@PrePersist
	public void prePersist() {
		if (numeroUnique == null || numeroUnique.isEmpty()) {
			// fallback (service will set final number later)
			numeroUnique = "TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		}
		if (statut == null) {
			statut = CertificatStatut.VALIDE;
		}
	}
}