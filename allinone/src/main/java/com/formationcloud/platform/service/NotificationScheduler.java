package com.formationcloud.platform.service;

import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduler pour les notifications "site vivant".
 * Exécute des jobs périodiques pour envoyer des notifications utiles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationScheduler {

	private final SeanceRepository seanceRepository;
	private final PresenceRepository presenceRepository;
	private final InscriptionRepository inscriptionRepository;
	private final TpRessourceRepository tpRessourceRepository;
	private final TpSoumissionRepository tpSoumissionRepository;
	private final NotificationService notificationService;

	/**
	 * Job principal qui vérifie et envoie toutes les notifications programmées.
	 * Rate configurable via app.notifications.scheduler-rate-ms (défaut: 3600000 = 1h)
	 */
	@Scheduled(fixedDelayString = "${app.notifications.scheduler-rate-ms:3600000}")
	public void processNotifications() {
		log.info("[SCHEDULER] Démarrage du traitement des notifications...");
		
		try {
			processSeancesDans24h();
			processPresencesACompleter();
			processTpDeadlineProche();
			processTpEnRetard();
			
			log.info("[SCHEDULER] Traitement terminé.");
		} catch (Exception e) {
			log.error("[SCHEDULER] Erreur lors du traitement des notifications", e);
		}
	}

	/**
	 * a) Séances dans 24h : notifier participants confirmés + formateur
	 */
	private void processSeancesDans24h() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime in24h = now.plusHours(24);
		
		List<Seance> seances = seanceRepository.findAll().stream()
				.filter(s -> s.getDateDebut() != null 
						&& s.getDateDebut().isAfter(now) 
						&& s.getDateDebut().isBefore(in24h))
				.collect(Collectors.toList());
		
		for (Seance seance : seances) {
			// Participants confirmés
			List<Inscription> inscriptions = inscriptionRepository.findByFormation_IdAndStatutIn(
					seance.getFormation().getId(),
					List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
			);
			
			for (Inscription inscription : inscriptions) {
				Utilisateur stagiaire = inscription.getStagiaire();
				if (stagiaire != null) {
					notificationService.envoyerSiAbsent(
							stagiaire,
							TypeNotification.SEANCE_RAPPEL,
							"Rappel: Séance '" + seance.getTitre() + "' dans moins de 24h (Formation: " + seance.getFormation().getNom() + ")",
							"/formations?formationId=" + seance.getFormation().getId() + "&tab=seances&seanceId=" + seance.getId()
					);
				}
			}
			
			// Formateur
			Utilisateur formateur = seance.getFormation().getFormateur();
			if (formateur != null) {
				notificationService.envoyerSiAbsent(
						formateur,
						TypeNotification.SEANCE_RAPPEL,
						"Rappel: Séance '" + seance.getTitre() + "' dans moins de 24h (Formation: " + seance.getFormation().getNom() + ")",
						"/formations?formationId=" + seance.getFormation().getId() + "&tab=seances&seanceId=" + seance.getId()
				);
			}
		}
		
		if (!seances.isEmpty()) {
			log.info("[SCHEDULER] {} séance(s) dans 24h traitées", seances.size());
		}
	}

	/**
	 * b) Présences à compléter : séances finies avec au moins une présence NON_MARQUE
	 */
	private void processPresencesACompleter() {
		LocalDateTime now = LocalDateTime.now();
		
		List<Seance> seancesFinies = seanceRepository.findAll().stream()
				.filter(s -> s.getDateFin() != null && s.getDateFin().isBefore(now))
				.collect(Collectors.toList());
		
		for (Seance seance : seancesFinies) {
			List<Presence> presences = presenceRepository.findBySeance_IdOrderByStagiaire_PrenomAscStagiaire_NomAsc(seance.getId());
			
			boolean hasNonMarque = presences.stream()
					.anyMatch(p -> p.getStatut() == StatutPresence.NON_MARQUE);
			
			if (hasNonMarque) {
				// Notifier le formateur
				Utilisateur formateur = seance.getFormation().getFormateur();
				if (formateur != null) {
					notificationService.envoyerSiAbsent(
							formateur,
							TypeNotification.PRESENCE_A_COMPLETER,
							"Présence à compléter pour la séance '" + seance.getTitre() + "' (Formation: " + seance.getFormation().getNom() + ")",
							"/formations?formationId=" + seance.getFormation().getId() + "&tab=seances&seanceId=" + seance.getId()
					);
				}
			}
		}
		
		if (!seancesFinies.isEmpty()) {
			log.info("[SCHEDULER] {} séance(s) finie(s) vérifiées pour présences", seancesFinies.size());
		}
	}

	/**
	 * c) TP deadline < 72h : notifier stagiaires inscrits actifs sans soumission
	 */
	private void processTpDeadlineProche() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime in72h = now.plusHours(72);
		
		List<TpRessource> tps = tpRessourceRepository.findAll().stream()
				.filter(tp -> tp.getType() == TypeTpRessource.TP
						&& tp.getDateLimite() != null
						&& tp.getDateLimite().isAfter(now)
						&& tp.getDateLimite().isBefore(in72h))
				.collect(Collectors.toList());
		
		for (TpRessource tp : tps) {
			// Stagiaires inscrits actifs de la formation
			List<Inscription> inscriptions = inscriptionRepository.findByFormation_IdAndStatutIn(
					tp.getFormation().getId(),
					List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
			);
			
			for (Inscription inscription : inscriptions) {
				Utilisateur stagiaire = inscription.getStagiaire();
				if (stagiaire == null) continue;
				
				// Vérifier si pas de soumission
				if (tpSoumissionRepository.findByTp_IdAndStagiaire_Id(tp.getId(), stagiaire.getId()).isEmpty()) {
					notificationService.envoyerSiAbsent(
							stagiaire,
							TypeNotification.TP_DEADLINE_PROCHE,
							"TP '" + tp.getTitre() + "' : deadline dans moins de 72h (Formation: " + tp.getFormation().getNom() + ")",
							"/formations?formationId=" + tp.getFormation().getId() + "&tab=ressources&tpId=" + tp.getId()
					);
				}
			}
		}
		
		if (!tps.isEmpty()) {
			log.info("[SCHEDULER] {} TP(s) avec deadline proche traités", tps.size());
		}
	}

	/**
	 * d) TP en retard : deadline dépassée sans soumission
	 */
	private void processTpEnRetard() {
		LocalDateTime now = LocalDateTime.now();
		
		List<TpRessource> tpsEnRetard = tpRessourceRepository.findAll().stream()
				.filter(tp -> tp.getType() == TypeTpRessource.TP
						&& tp.getDateLimite() != null
						&& tp.getDateLimite().isBefore(now))
				.collect(Collectors.toList());
		
		for (TpRessource tp : tpsEnRetard) {
			// Stagiaires inscrits actifs de la formation
			List<Inscription> inscriptions = inscriptionRepository.findByFormation_IdAndStatutIn(
					tp.getFormation().getId(),
					List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
			);
			
			for (Inscription inscription : inscriptions) {
				Utilisateur stagiaire = inscription.getStagiaire();
				if (stagiaire == null) continue;
				
				// Vérifier si pas de soumission
				if (tpSoumissionRepository.findByTp_IdAndStagiaire_Id(tp.getId(), stagiaire.getId()).isEmpty()) {
					notificationService.envoyerSiAbsent(
							stagiaire,
							TypeNotification.TP_EN_RETARD,
							"TP '" + tp.getTitre() + "' : deadline dépassée (Formation: " + tp.getFormation().getNom() + ")",
							"/formations?formationId=" + tp.getFormation().getId() + "&tab=ressources&tpId=" + tp.getId()
					);
				}
			}
		}
		
		if (!tpsEnRetard.isEmpty()) {
			log.info("[SCHEDULER] {} TP(s) en retard traités", tpsEnRetard.size());
		}
	}
}
