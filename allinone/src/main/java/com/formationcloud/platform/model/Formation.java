package com.formationcloud.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
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
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "formation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la formation est obligatoire")
    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeFormation type;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    @JsonBackReference
    private Categorie categorie;

    @Min(value = 1, message = "La capacité doit être au moins 1")
    @Column(name = "capacite_max", nullable = false)
    private Integer capaciteMax = 30;

    @ManyToOne
    @JoinColumn(name = "formateur_id")
    private Utilisateur formateur;

    @ManyToOne
    @JoinColumn(name = "prerequis_id")
    private Formation prerequis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutFormation statut = StatutFormation.ACTIVE;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "duree_heures")
    private Integer dureeHeures;

    @Column(name = "prix", precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Relations
    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL)
    private List<Inscription> inscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL)
    private List<Evaluation> evaluations = new ArrayList<>();

    @OneToMany(mappedBy = "prerequis", cascade = CascadeType.ALL)
    private List<Formation> formationsSupérieures = new ArrayList<>();

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL)
    private List<TpRessource> tpRessources = new ArrayList<>();

    // Méthodes utilitaires
    public int getNombreInscrits() {
        return (int) inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.CONFIRMEE
                        || i.getStatut() == StatutInscription.EN_COURS)
                .count();
    }

    public int getPlacesDisponibles() {
        return capaciteMax - getNombreInscrits();
    }

    public boolean isPleine() {
        return getNombreInscrits() >= capaciteMax;
    }

    public boolean isActive() {
        return statut == StatutFormation.ACTIVE;
    }
}
