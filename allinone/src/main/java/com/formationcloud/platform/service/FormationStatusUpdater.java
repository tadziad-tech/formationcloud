package com.formationcloud.platform.service;

import com.formationcloud.platform.model.StatutFormation;
import com.formationcloud.platform.repository.FormationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Met à jour automatiquement le statut des formations en fonction de la date.
 *
 * Règle: une formation ACTIVE devient TERMINEE quand sa date_fin est strictement
 * passée (date_fin < aujourd'hui).
 *
 * Note: on n'écrase jamais ANNULEE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormationStatusUpdater {

    private final FormationRepository formationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public int updateFinishedFormations() {
        try {
            int updated = formationRepository.markFinished(StatutFormation.ACTIVE, StatutFormation.TERMINEE, LocalDate.now());
            if (updated > 0) {
                log.info("{} formation(s) passées automatiquement à TERMINEE", updated);
            }
            return updated;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des statuts formations (markFinished): {}", e.getMessage());
            return 0;
        }
    }

	/**
	 * Mise à jour automatique quotidienne (sécurité) : même si personne ne charge
	 * les pages, les formations expirées passent à TERMINEE.
	 */
	@Scheduled(cron = "0 5 0 * * *")
	public void scheduledDailyUpdate() {
		updateFinishedFormations();
	}

	/**
	 * Exécution unique au démarrage de l'application pour synchroniser les statuts
	 * sans attendre le prochain cron quotidien.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		try {
			log.info("Démarrage: synchronisation des statuts formations…");
			updateFinishedFormations();
		} catch (Exception e) {
			log.error("Démarrage: échec synchronisation statuts formations (l'app continue): {}", e.getMessage());
		}
	}
}
