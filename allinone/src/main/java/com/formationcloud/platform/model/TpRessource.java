package com.formationcloud.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tp_ressource")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TpRessource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "formation_id", nullable = false)
	@JsonBackReference
	@NotNull
	private Formation formation;

	@NotBlank(message = "Le titre est obligatoire")
	@Column(nullable = false)
	private String titre;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@NotNull
	private TypeTpRessource type;

	@Column(name = "fichier_url")
	private String fichierUrl;

	@Column(name = "date_limite")
	private LocalDateTime dateLimite;

	@CreationTimestamp
	@Column(name = "date_creation", updatable = false)
	private LocalDateTime dateCreation;

	@UpdateTimestamp
	@Column(name = "date_modification")
	private LocalDateTime dateModification;
}
