package com.formationcloud.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "destinataire_id", nullable = false)
	private Utilisateur destinataire;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TypeNotification type;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(nullable = false)
	private Boolean lu = false;

	@CreationTimestamp
	@Column(name = "date_creation", updatable = false)
	private LocalDateTime dateCreation;

	@Column(name = "date_lecture")
	private LocalDateTime dateLecture;

	@Column(name = "lien")
	private String lien;

	public void marquerCommeLu() {
		this.lu = true;
		this.dateLecture = LocalDateTime.now();
	}
}
