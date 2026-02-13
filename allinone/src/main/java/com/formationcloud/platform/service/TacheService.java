package com.formationcloud.platform.service;

import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.StatutTache;
import com.formationcloud.platform.model.Tache;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TacheService {

	private final TacheRepository tacheRepository;
	private final UtilisateurService utilisateurService;
	private final NotificationService notificationService;

	public List<Tache> findAll() {
		return tacheRepository.findAll();
	}

	public Tache findById(Long id) {
		return tacheRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tache", "id", id));
	}

	public List<Tache> findByStagiaire(Long stagiaireId) {
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
		return tacheRepository.findByStagiaire(stagiaire);
	}

	public List<Tache> findTachesEnRetard(Long stagiaireId) {
		return tacheRepository.findTachesEnRetardByStagiaire(stagiaireId, LocalDate.now());
	}

	public Tache createTache(Tache tache, Long stagiaireId) {
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);

		if (!stagiaire.isStagiaire()) {
			throw new RuntimeException("Les tâches ne peuvent être assignées qu'aux stagiaires");
		}

		tache.setStagiaire(stagiaire);
		tache.setStatut(StatutTache.ASSIGNEE);
		tache.setPourcentageAccomplissement(0);

		Tache saved = tacheRepository.save(tache);

		// Notifier le stagiaire
		notificationService.envoyerNotificationTacheAssignee(saved);

		log.info("Tâche créée: {} pour {}", saved.getTitre(), stagiaire.getNomComplet());
		return saved;
	}

	public Tache updateTache(Long id, Tache tacheDetails) {
		Tache tache = findById(id);

		tache.setTitre(tacheDetails.getTitre());
		tache.setDescription(tacheDetails.getDescription());
		tache.setDateDebut(tacheDetails.getDateDebut());
		tache.setDateFin(tacheDetails.getDateFin());

		return tacheRepository.save(tache);
	}

	public Tache updatePourcentage(Long id, Integer pourcentage) {
		Tache tache = findById(id);

		if (pourcentage < 0 || pourcentage > 100) {
			throw new RuntimeException("Le pourcentage doit être entre 0 et 100");
		}

		tache.setPourcentageAccomplissement(pourcentage);

		// Mettre à jour le statut automatiquement
		if (pourcentage == 0) {
			tache.setStatut(StatutTache.ASSIGNEE);
		} else if (pourcentage < 100) {
			tache.setStatut(StatutTache.EN_COURS);
		} else {
			tache.setStatut(StatutTache.TERMINEE);
		}

		// Vérifier si en retard
		if (tache.isEnRetard() && tache.getStatut() != StatutTache.TERMINEE) {
			tache.setStatut(StatutTache.EN_RETARD);
		}

		return tacheRepository.save(tache);
	}

	public void marquerCommeTerminee(Long id) {
		Tache tache = findById(id);
		tache.setStatut(StatutTache.TERMINEE);
		tache.setPourcentageAccomplissement(100);
		tacheRepository.save(tache);
		log.info("Tâche terminée: {}", tache.getTitre());
	}

	public void deleteTache(Long id) {
		Tache tache = findById(id);
		tacheRepository.delete(tache);
		log.info("Tâche supprimée: {}", tache.getTitre());
	}

	public long countByStatut(StatutTache statut) {
		return tacheRepository.countByStatut(statut);
	}

	public long countTachesTermineesByStagiaire(Long stagiaireId) {
		return tacheRepository.countTachesTermineesByStagiaire(stagiaireId);
	}
}
