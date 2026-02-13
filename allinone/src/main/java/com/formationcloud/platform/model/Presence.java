package com.formationcloud.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "presence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_presence_seance_stagiaire", columnNames = {"seance_id", "stagiaire_id"})
        },
        indexes = {
                @Index(name = "idx_presence_seance", columnList = "seance_id"),
                @Index(name = "idx_presence_stagiaire", columnList = "stagiaire_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seance_id", nullable = false)
    @JsonBackReference
    private Seance seance;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Utilisateur stagiaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPresence statut = StatutPresence.NON_MARQUE;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
