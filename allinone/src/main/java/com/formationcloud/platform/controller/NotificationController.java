package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.NotificationDTO;
import com.formationcloud.platform.model.Notification;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.service.NotificationService;
import com.formationcloud.platform.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getAll() {
        return ResponseEntity.ok(notificationService.findAll().stream().map(this::toDTO).toList());
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<List<NotificationDTO>> getByUtilisateur(@PathVariable Long utilisateurId) {
        SecurityUtils.assertAdminOrSelf(utilisateurId);
        Utilisateur u = utilisateurService.findById(utilisateurId);
        return ResponseEntity.ok(notificationService.findByDestinataire(u).stream().map(this::toDTO).toList());
    }

    @GetMapping("/utilisateur/{utilisateurId}/non-lues")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<List<NotificationDTO>> getNonLues(@PathVariable Long utilisateurId) {
        SecurityUtils.assertAdminOrSelf(utilisateurId);
        return ResponseEntity.ok(notificationService.findNotificationsNonLues(utilisateurId).stream().map(this::toDTO).toList());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(@RequestParam(required = false) Boolean unreadOnly) {
        Long currentUserId = SecurityUtils.currentUser().getId();
        List<Notification> notifications;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationService.findNotificationsNonLues(currentUserId);
        } else {
            Utilisateur u = utilisateurService.findById(currentUserId);
            notifications = notificationService.findByDestinataire(u);
        }
        return ResponseEntity.ok(notifications.stream().map(this::toDTO).toList());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<Long> getUnreadCount() {
        Long currentUserId = SecurityUtils.currentUser().getId();
        return ResponseEntity.ok(notificationService.countNotificationsNonLues(currentUserId));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Notification n = notificationService.findById(id);
        SecurityUtils.assertAdminOrSelf(n.getDestinataire().getId());
        notificationService.marquerCommeLu(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<Void> markAllAsRead() {
        Long currentUserId = SecurityUtils.currentUser().getId();
        notificationService.marquerToutesCommeLues(currentUserId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/lire")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<Void> markAsReadLegacy(@PathVariable Long id) {
        Notification n = notificationService.findById(id);
        SecurityUtils.assertAdminOrSelf(n.getDestinataire().getId());
        notificationService.marquerCommeLu(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/utilisateur/{utilisateurId}/lire-tout")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<Void> markAllAsReadLegacy(@PathVariable Long utilisateurId) {
        SecurityUtils.assertAdminOrSelf(utilisateurId);
        notificationService.marquerToutesCommeLues(utilisateurId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR','STAGIAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Notification n = notificationService.findById(id);
        SecurityUtils.assertAdminOrSelf(n.getDestinataire().getId());
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }

    private String deriveTitre(Notification n) {
        if (n == null || n.getType() == null) return "Notification";
        return switch (n.getType()) {
            case NOUVELLE_FORMATION -> "Nouvelle formation";
            case EVALUATION_DISPONIBLE -> "Évaluation disponible";
            case TACHE_ASSIGNEE -> "Nouvelle tâche";
            case RAPPEL_DEADLINE -> "Rappel deadline";
            case INSCRIPTION_VALIDEE -> "Inscription validée";
            case INSCRIPTION_REFUSEE -> "Inscription refusée";
            case CERTIFICAT_OBTENU -> "Certificat obtenu";
            case NOUVELLE_INSCRIPTION -> "Nouvelle inscription";
            case SEANCE_PLANIFIEE -> "Séance planifiée";
            case SEANCE_MODIFIEE -> "Séance modifiée";
            case TP_PUBLIE -> "TP publié";
            case TP_CORRIGE -> "TP corrigé";
            case NOUVELLE_SOUMISSION_TP -> "Nouvelle soumission TP";
            case PRESENCE_A_COMPLETER -> "Présence à compléter";
            case SEANCE_RAPPEL -> "Rappel séance";
            case TP_DEADLINE_PROCHE -> "TP deadline proche";
            case TP_EN_RETARD -> "TP en retard";
            case INSCRIPTION_EN_ATTENTE_ADMIN -> "Inscription en attente";
            case FORMATION_ASSIGNEE -> "Formation assignée";
        };
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setMessage(n.getMessage());
        dto.setDateCreation(n.getDateCreation());
        dto.setLue(n.getLu());
        dto.setLien(n.getLien());
        dto.setTitre(deriveTitre(n));
        if (n.getDestinataire() != null) {
            dto.setUtilisateurId(n.getDestinataire().getId());
            dto.setUtilisateurNom(n.getDestinataire().getNom());
            dto.setUtilisateurPrenom(n.getDestinataire().getPrenom());
        }

        return dto;
    }
}
