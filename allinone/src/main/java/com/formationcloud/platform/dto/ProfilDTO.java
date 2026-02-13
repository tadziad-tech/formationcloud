package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.TypeFormateur;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProfilDTO {

    // Infos utilisateur
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private TypeFormateur typeFormateur;
    private Boolean statutValidation;
    private String telephone;
    private String adresse;
    private String photoProfil;
    private LocalDateTime dateCreation;

    // Contenu profil
    private List<CertificatDTO> certificats = new ArrayList<>();

    /**
     * Pour un STAGIAIRE: toutes ses inscriptions (avec statut + formation).
     */
    private List<InscriptionDTO> inscriptions = new ArrayList<>();

    /**
     * Pour un FORMATEUR: formations qu'il encadre (en cours / terminées).
     */
    private List<FormationSummaryDTO> formationsFormateur = new ArrayList<>();
}
