package com.formationcloud.platform.service;

import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.NotificationRepository;
import com.formationcloud.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

	private final NotificationRepository notificationRepository;

		public Notification findById(Long id) {
		return notificationRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Notification non trouvée"));
	}

public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

	public void deleteNotification(Long id) {
		notificationRepository.deleteById(id);
	}

	public List<Notification> findByDestinataire(Utilisateur destinataire) {
		if (destinataire == null) {
			return List.of();
		}
		return notificationRepository.findByDestinataireOrderByDateCreationDesc(destinataire);
	}

	public List<Notification> findNotificationsNonLues(Long destinataireId) {
		return notificationRepository.findNotificationsNonLuesByDestinataire(destinataireId);
	}

	public long countNotificationsNonLues(Long destinataireId) {
		return notificationRepository.countNotificationsNonLuesByDestinataire(destinataireId);
	}

	public void marquerCommeLu(Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification non trouvée"));
		notification.marquerCommeLu();
		notificationRepository.save(notification);
	}

	public void marquerToutesCommeLues(Long destinataireId) {
		List<Notification> notifications = notificationRepository
				.findNotificationsNonLuesByDestinataire(destinataireId);
		notifications.forEach(Notification::marquerCommeLu);
		notificationRepository.saveAll(notifications);
	}

	// Méthodes pour créer des notifications spécifiques

	public void envoyerNotificationNouvelleFormation(Formation formation, List<Utilisateur> destinataires) {
		for (Utilisateur destinataire : destinataires) {
			Notification notification = new Notification();
			notification.setDestinataire(destinataire);
			notification.setType(TypeNotification.NOUVELLE_FORMATION);
			notification.setMessage("Nouvelle formation disponible: " + formation.getNom());
			notification.setLien("/formations?formationId=" + formation.getId());
			notificationRepository.save(notification);
		}
		log.info("Notifications envoyées pour nouvelle formation: {}", formation.getNom());
	}

	public void envoyerNotificationInscriptionValidee(Inscription inscription) {
		Notification notification = new Notification();
		notification.setDestinataire(inscription.getStagiaire());
		notification.setType(TypeNotification.INSCRIPTION_VALIDEE);
		notification.setMessage(
				"Votre inscription à la formation '" + inscription.getFormation().getNom() + "' a été validée");
		notification.setLien("/formations?formationId=" + inscription.getFormation().getId());
		notificationRepository.save(notification);
		log.info("Notification d'inscription validée envoyée à {}", inscription.getStagiaire().getEmail());
	}

	public void envoyerNotificationInscriptionRefusee(Inscription inscription) {
		Notification notification = new Notification();
		notification.setDestinataire(inscription.getStagiaire());
		notification.setType(TypeNotification.INSCRIPTION_REFUSEE);
		notification.setMessage(
				"Votre inscription à la formation '" + inscription.getFormation().getNom() + "' a été refusée");
		notification.setLien("/formations?formationId=" + inscription.getFormation().getId());
		notificationRepository.save(notification);
	}

	public void envoyerNotificationNouvelleInscription(Inscription inscription) {
		Notification notification = new Notification();
		notification.setDestinataire(inscription.getFormation().getFormateur());
		notification.setType(TypeNotification.NOUVELLE_INSCRIPTION);
		notification.setMessage(inscription.getStagiaire().getNomComplet() + " s'est inscrit à votre formation '"
				+ inscription.getFormation().getNom() + "'");
		notification.setLien("/formations?formationId=" + inscription.getFormation().getId());
		notificationRepository.save(notification);
	}

	public void envoyerNotificationTacheAssignee(Tache tache) {
		Notification notification = new Notification();
		notification.setDestinataire(tache.getStagiaire());
		notification.setType(TypeNotification.TACHE_ASSIGNEE);
		notification.setMessage("Nouvelle tâche assignée: " + tache.getTitre());
		notification.setLien("/taches/" + tache.getId());
		notificationRepository.save(notification);
	}

	public void envoyerNotificationCertificatObtenu(Certificat certificat) {
		Notification notification = new Notification();
		notification.setDestinataire(certificat.getStagiaire());
		notification.setType(TypeNotification.CERTIFICAT_OBTENU);
		notification.setMessage("Félicitations ! Vous avez obtenu le certificat pour la formation '"
				+ certificat.getFormation().getNom() + "'");
		notification.setLien("/formations?formationId=" + certificat.getFormation().getId());
		notificationRepository.save(notification);
	}

	public void envoyerNotificationEvaluationDisponible(Evaluation evaluation, List<Utilisateur> stagiaires) {
		for (Utilisateur stagiaire : stagiaires) {
			Notification notification = new Notification();
			notification.setDestinataire(stagiaire);
			notification.setType(TypeNotification.EVALUATION_DISPONIBLE);
			notification.setMessage("Nouvelle évaluation disponible: " + evaluation.getTitre());
			notification.setLien("/evaluations/" + evaluation.getId());
			notificationRepository.save(notification);
		}
	}

	public void envoyerNotificationRappelDeadline(Tache tache) {
		Notification notification = new Notification();
		notification.setDestinataire(tache.getStagiaire());
		notification.setType(TypeNotification.RAPPEL_DEADLINE);
		notification
				.setMessage("Rappel: La tâche '" + tache.getTitre() + "' arrive à échéance le " + tache.getDateFin());
		notification.setLien("/taches/" + tache.getId());
		notificationRepository.save(notification);
	}

	public void envoyerNotificationValidationCompte(Utilisateur utilisateur) {
		Notification notification = new Notification();
		notification.setDestinataire(utilisateur);
		notification.setType(TypeNotification.INSCRIPTION_VALIDEE);
		notification.setMessage("Votre compte a été validé. Vous pouvez maintenant accéder à la plateforme.");
		notification.setLien("/dashboard");
		notificationRepository.save(notification);
	}


    public void envoyerNotificationSeancePlanifiee(Seance seance, List<Utilisateur> destinataires) {
        for (Utilisateur destinataire : destinataires) {
            Notification notification = new Notification();
            notification.setDestinataire(destinataire);
            notification.setType(TypeNotification.SEANCE_PLANIFIEE);
            notification.setMessage("Nouvelle séance planifiée : " + seance.getTitre() + " (Formation : " + seance.getFormation().getNom() + ")");
            notification.setLien("/formations?formationId=" + seance.getFormation().getId() + "&tab=seances&seanceId=" + seance.getId());
            notificationRepository.save(notification);
        }
    }

    public void envoyerNotificationSeanceModifiee(Seance seance, List<Utilisateur> destinataires) {
        for (Utilisateur destinataire : destinataires) {
            Notification notification = new Notification();
            notification.setDestinataire(destinataire);
            notification.setType(TypeNotification.SEANCE_MODIFIEE);
            notification.setMessage("Séance mise à jour : " + seance.getTitre() + " (Formation : " + seance.getFormation().getNom() + ")");
            notification.setLien("/formations?formationId=" + seance.getFormation().getId() + "&tab=seances&seanceId=" + seance.getId());
            notificationRepository.save(notification);
        }
    }

    public void envoyerNotificationTpPublie(TpRessource tpRessource, List<Utilisateur> destinataires) {
        for (Utilisateur destinataire : destinataires) {
            Notification notification = new Notification();
            notification.setDestinataire(destinataire);
            notification.setType(TypeNotification.TP_PUBLIE);
            notification.setMessage("Nouveau " + (tpRessource.getType() == TypeTpRessource.TP ? "TP" : "cours") + " publié : " + tpRessource.getTitre() + " (Formation : " + tpRessource.getFormation().getNom() + ")");
            notification.setLien("/formations?formationId=" + tpRessource.getFormation().getId() + "&tab=ressources&tpId=" + tpRessource.getId());
            notificationRepository.save(notification);
        }
        log.info("Notifications TP publié envoyées pour : {}", tpRessource.getTitre());
    }

    public void envoyerNotificationTpCorrige(TpSoumission soumission) {
        if (soumission.getStagiaire() == null) {
            log.warn("Impossible d'envoyer notification TP corrigé : stagiaire manquant");
            return;
        }

        Notification notification = new Notification();
        notification.setDestinataire(soumission.getStagiaire());
        notification.setType(TypeNotification.TP_CORRIGE);
        
        String message = "Votre TP '" + soumission.getTp().getTitre() + "' a été corrigé";
        if (soumission.getNote() != null) {
            message += " (Note : " + soumission.getNote() + "/20)";
        }
        notification.setMessage(message);
        Long formationId = soumission.getTp() != null && soumission.getTp().getFormation() != null 
                ? soumission.getTp().getFormation().getId() 
                : null;
        Long tpId = soumission.getTp() != null ? soumission.getTp().getId() : null;
        if (formationId != null && tpId != null) {
            notification.setLien("/formations?formationId=" + formationId + "&tab=ressources&tpId=" + tpId);
        } else if (formationId != null) {
            notification.setLien("/formations?formationId=" + formationId + "&tab=ressources");
        } else {
            notification.setLien("/formations");
        }
        notificationRepository.save(notification);
        log.info("Notification TP corrigé envoyée à {}", soumission.getStagiaire().getEmail());
    }

	/**
	 * Notifie le formateur qu'il a été assigné à une formation (création ou changement).
	 * Anti-doublon du jour via envoyerSiAbsent.
	 */
	public void notifierFormateurAssigne(Formation formation) {
		if (formation == null || formation.getFormateur() == null) return;
		Utilisateur f = formation.getFormateur();
		String msg = "Vous avez été désigné formateur pour la formation '" + formation.getNom() + "'.";
		String lien = "/formations?formationId=" + formation.getId();
		envoyerSiAbsent(f, TypeNotification.FORMATION_ASSIGNEE, msg, lien);
	}

	/**
	 * Envoie une notification uniquement si elle n'existe pas déjà aujourd'hui (anti-doublons).
	 * Utilisé par le scheduler pour éviter le spam.
	 */
	public void envoyerSiAbsent(Utilisateur dest, TypeNotification type, String message, String lien) {
		if (dest == null || dest.getId() == null) {
			log.warn("Impossible d'envoyer notification : destinataire manquant");
			return;
		}
		
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		if (notificationRepository.existsByDestinataire_IdAndTypeAndLienAndDateCreationAfter(
				dest.getId(), type, lien, startOfDay)) {
			log.debug("Notification déjà envoyée aujourd'hui pour {} (type={}, lien={})", dest.getEmail(), type, lien);
			return;
		}
		
		Notification notification = new Notification();
		notification.setDestinataire(dest);
		notification.setType(type);
		notification.setMessage(message);
		notification.setLien(lien);
		notification.setLu(false);
		notificationRepository.save(notification);
		log.info("Notification envoyée à {} (type={})", dest.getEmail(), type);
	}

}
