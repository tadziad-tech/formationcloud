package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.TypeNotification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private TypeNotification type;
    private String message;
    private Boolean lue;
    private LocalDateTime dateCreation;

    private String titre;
    private String lien;
    private Long utilisateurId;
    private String utilisateurNom;
    private String utilisateurPrenom;
}

