package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.DashboardStatsDTO;
import com.formationcloud.platform.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private final UtilisateurService utilisateurService;
	private final FormationService formationService;
	private final InscriptionService inscriptionService;
	private final CertificatService certificatService;
	private final NotificationService notificationService;

	public DashboardStatsDTO getGlobalStats() {
		DashboardStatsDTO stats = new DashboardStatsDTO();

		// Statistiques utilisateurs
		stats.setTotalUtilisateurs(utilisateurService.findAll().size());
		stats.setTotalAdmins(utilisateurService.countByRole(Role.ADMIN));
		stats.setTotalFormateurs(utilisateurService.countByRole(Role.FORMATEUR));
		stats.setTotalStagiaires(utilisateurService.countByRole(Role.STAGIAIRE));

		// Statistiques formations
		stats.setTotalFormations(formationService.findAll().size());
		stats.setFormationsActives(formationService.countByStatut(StatutFormation.ACTIVE));

		// Statistiques inscriptions
		stats.setTotalInscriptions(inscriptionService.findAll().size());
		stats.setInscriptionsEnAttente(inscriptionService.countByStatut(StatutInscription.EN_ATTENTE));

		// Statistiques certificats
		stats.setTotalCertificats(certificatService.findAll().size());

		return stats;
	}

	public DashboardStatsDTO getFormateurStats(Long formateurId) {
		DashboardStatsDTO stats = new DashboardStatsDTO();

		// Formations du formateur
		stats.setTotalFormations(formationService.findByFormateur(formateurId).size());
		stats.setFormationsActives(formationService.findByFormateur(formateurId).stream()
				.filter(f -> f.getStatut() == StatutFormation.ACTIVE).count());

		// Inscriptions en attente pour ses formations
		stats.setInscriptionsEnAttente(inscriptionService.findInscriptionsEnAttenteByFormateur(formateurId).size());

		return stats;
	}

	public DashboardStatsDTO getStagiaireStats(Long stagiaireId) {
		DashboardStatsDTO stats = new DashboardStatsDTO();

		// Inscriptions du stagiaire
		stats.setTotalInscriptions(inscriptionService.findByStagiaire(stagiaireId).size());

		// Certificats obtenus
		stats.setTotalCertificats(certificatService.findByStagiaire(stagiaireId).size());

		// Notifications non lues
		stats.setNotificationsNonLues(notificationService.countNotificationsNonLues(stagiaireId));

		return stats;
	}
}
