package com.formationcloud.platform.service;

import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InscriptionService {

	private final InscriptionRepository inscriptionRepository;
	private final FormationService formationService;
	private final UtilisateurService utilisateurService;
	private final CertificatService certificatService;
	private final NotificationService notificationService;
	private final PresenceService presenceService;


	private void assertCanManageFormation(Formation formation) {
		var principal = SecurityUtils.currentUser();
		if (principal == null) {
			throw new AccessDeniedException("Non authentifié");
		}
		if (SecurityUtils.isAdmin()) return;
		if ("FORMATEUR".equalsIgnoreCase(principal.getRole())) {
			Long ownerId = formation != null && formation.getFormateur() != null ? formation.getFormateur().getId() : null;
			if (ownerId == null || !ownerId.equals(principal.getId())) {
				throw new AccessDeniedException("Accès interdit: vous n'êtes pas le formateur de cette formation");
			}
			return;
		}
		throw new AccessDeniedException("Accès interdit");
	}


	public List<Inscription> findAll() {
		return inscriptionRepository.findAll();
	}

	public Inscription findById(Long id) {
		return inscriptionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Inscription", "id", id));
	}

	public List<Inscription> findByStagiaire(Long stagiaireId) {
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
		return inscriptionRepository.findByStagiaire(stagiaire);
	}

	public List<Long> findActiveFormationIdsByStagiaire(Long stagiaireId) {
		return inscriptionRepository.findActiveFormationIdsByStagiaire(stagiaireId);
	}


	public List<Inscription> findByFormation(Long formationId) {
		Formation formation = formationService.findById(formationId);
		return inscriptionRepository.findByFormation(formation);
	}

	/**
	 * Inscription de un stagiaire pour une formation, si elle existe.
	 */
	public Optional<Inscription> findByFormationAndStagiaire(Long formationId, Long stagiaireId) {
		return inscriptionRepository.findByFormation_IdAndStagiaire_Id(formationId, stagiaireId);
	}

	public List<Inscription> findInscriptionsEnAttenteByFormateur(Long formateurId) {
		return inscriptionRepository.findInscriptionsEnAttenteByFormateur(formateurId);
	}

    /**
     * Vrai si le stagiaire est réellement participant de la formation (inscription validée).
     * Sert par exemple à empêcher de donner un certificat à un utilisateur non inscrit.
     */
    public boolean isStagiaireActifDansFormation(Long stagiaireId, Long formationId) {
        Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
        Formation formation = formationService.findById(formationId);

        return inscriptionRepository.findByStagiaireAndFormation(stagiaire, formation)
                .map(i -> {
                    var st = i.getStatut();
                    return st == StatutInscription.CONFIRMEE
                            || st == StatutInscription.EN_COURS
                            || st == StatutInscription.TERMINEE;
                })
                .orElse(false);
    }

	public Inscription inscrireStagiaire(Long stagiaireId, Long formationId) {
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
		Formation formation = formationService.findById(formationId);

		// Vérifier si le stagiaire est bien un stagiaire
		if (!stagiaire.isStagiaire()) {
			throw new BadRequestException("Seuls les stagiaires peuvent s'inscrire aux formations");
		}

		// Vérifier si la formation est active
		if (!formation.isActive()) {
			throw new BadRequestException("Cette formation n'est pas disponible pour inscription");
		}

		// Vérifier les prérequis
		if (formation.getPrerequis() != null) {
			boolean prerequisRempli = certificatService.verifierCertificatExiste(stagiaireId,
					formation.getPrerequis().getId());
			if (!prerequisRempli) {
				throw new BadRequestException(
						"Vous devez d'abord compléter la formation prérequise: " + formation.getPrerequis().getNom());
			}
		}

		// Vérifier si déjà inscrit
		// On autorise une nouvelle demande si l'ancienne était REFUSEE ou ABANDONNEE.
		var existingOpt = inscriptionRepository.findByStagiaireAndFormation(stagiaire, formation);
		if (existingOpt.isPresent()) {
			Inscription existing = existingOpt.get();
			StatutInscription st = existing.getStatut();
			if (st == StatutInscription.REFUSEE || st == StatutInscription.ABANDONNEE) {
				// Ré-ouvrir une demande
				existing.setStatut(StatutInscription.EN_ATTENTE);
				existing.setMotifRefus(null);
				existing.setPositionListeAttente(null);
				existing.setDateValidation(null);

				// Capacité: si pleine => liste d'attente
				if (formation.isPleine()) {
					existing.setPositionListeAttente(getProchainPositionListeAttente(formation));
				}

				Inscription saved = inscriptionRepository.save(existing);
				notificationService.envoyerNotificationNouvelleInscription(saved);
				// Notifier aussi les admins
				List<Utilisateur> admins = utilisateurService.findByRole(Role.ADMIN);
				for (Utilisateur admin : admins) {
					notificationService.envoyerSiAbsent(admin, TypeNotification.INSCRIPTION_EN_ATTENTE_ADMIN,
							"Nouvelle demande d'inscription: " + stagiaire.getNomComplet() + " pour la formation '" + formation.getNom() + "'",
							"/formations?formationId=" + formation.getId());
				}
				log.info("Nouvelle demande créée (réactivation) pour {} sur {}", stagiaire.getNomComplet(), formation.getNom());
				return saved;
			}
			throw new BadRequestException("Vous êtes déjà inscrit à cette formation");
		}

		Inscription inscription = new Inscription();
		inscription.setStagiaire(stagiaire);
		inscription.setFormation(formation);

		// Vérifier la capacité
		if (formation.isPleine()) {
			inscription.setStatut(StatutInscription.EN_ATTENTE);
			inscription.setPositionListeAttente(getProchainPositionListeAttente(formation));
			log.info("Formation pleine. Inscription mise en liste d'attente pour {}", stagiaire.getNomComplet());
		} else {
			inscription.setStatut(StatutInscription.EN_ATTENTE);
		}

		Inscription saved = inscriptionRepository.save(inscription);

		// Présence: si des séances existent déjà, créer les lignes de présence pour ce stagiaire
		presenceService.ensurePresencesForFormationStagiaire(formation.getId(), inscription.getStagiaire().getId());

		// Notifier le formateur
		notificationService.envoyerNotificationNouvelleInscription(saved);
		
		// Notifier aussi les admins
		List<Utilisateur> admins = utilisateurService.findByRole(Role.ADMIN);
		for (Utilisateur admin : admins) {
			notificationService.envoyerSiAbsent(admin, TypeNotification.INSCRIPTION_EN_ATTENTE_ADMIN,
					"Nouvelle demande d'inscription: " + stagiaire.getNomComplet() + " pour la formation '" + formation.getNom() + "'",
					"/formations?formationId=" + formation.getId());
		}

		log.info("Inscription créée: {} pour la formation {}", stagiaire.getNomComplet(), formation.getNom());
		return saved;
	}

	public Inscription validerInscription(Long inscriptionId) {
		Inscription inscription = findById(inscriptionId);

		if (inscription.getStatut() != StatutInscription.EN_ATTENTE) {
			throw new BadRequestException("Cette inscription n'est pas en attente de validation");
		}

		Formation formation = inscription.getFormation();

		assertCanManageFormation(formation);

		// Vérifier la capacité
		if (formation.isPleine()) {
			throw new BadRequestException("La formation a atteint sa capacité maximale");
		}

		inscription.setStatut(StatutInscription.CONFIRMEE);
		inscription.setDateValidation(LocalDateTime.now());

		Inscription saved = inscriptionRepository.save(inscription);

		presenceService.ensurePresencesForFormationStagiaire(formation.getId(), inscription.getStagiaire().getId());

		// Notifier le stagiaire
		notificationService.envoyerNotificationInscriptionValidee(saved);

		log.info("Inscription validée pour {}", inscription.getStagiaire().getNomComplet());
		return saved;
	}

	public Inscription refuserInscription(Long inscriptionId, String motif) {
		Inscription inscription = findById(inscriptionId);

		if (inscription.getStatut() != StatutInscription.EN_ATTENTE) {
			throw new BadRequestException("Cette inscription n'est pas en attente de validation");
		}

		assertCanManageFormation(inscription.getFormation());

		inscription.setStatut(StatutInscription.REFUSEE);
		inscription.setMotifRefus(motif);
		inscription.setDateValidation(LocalDateTime.now());

		Inscription saved = inscriptionRepository.save(inscription);

		// Notifier le stagiaire
		notificationService.envoyerNotificationInscriptionRefusee(saved);

		log.info("Inscription refusée pour {}", inscription.getStagiaire().getNomComplet());
		return saved;
	}

	public void annulerInscription(Long inscriptionId) {
		Inscription inscription = findById(inscriptionId);

		// SÉCURITÉ / LOGIQUE MÉTIER:
		// - ADMIN peut annuler n'importe quelle inscription
		// - STAGIAIRE ne peut annuler que sa propre inscription (quitter / annuler demande)
		var principal = SecurityUtils.currentUser();
		if (principal == null) {
			throw new AccessDeniedException("Non authentifié");
		}
		if (!SecurityUtils.isAdmin()) {
			if (!"STAGIAIRE".equalsIgnoreCase(principal.getRole())) {
				throw new AccessDeniedException("Accès interdit");
			}
			Long ownerId = inscription.getStagiaire() != null ? inscription.getStagiaire().getId() : null;
			if (ownerId == null || !ownerId.equals(principal.getId())) {
				throw new AccessDeniedException("Accès interdit: inscription d'un autre utilisateur");
			}
		}

		if (inscription.getStatut() == StatutInscription.TERMINEE) {
			throw new BadRequestException("Impossible d'annuler une inscription terminée");
		}

		inscription.setStatut(StatutInscription.ABANDONNEE);
		inscriptionRepository.save(inscription);

		log.info("Inscription annulée pour {}", inscription.getStagiaire().getNomComplet());
	}

	public void marquerCommeEnCours(Long inscriptionId) {
		Inscription inscription = findById(inscriptionId);
		inscription.setStatut(StatutInscription.EN_COURS);
		inscriptionRepository.save(inscription);
	}

	public void marquerCommeTerminee(Long inscriptionId) {
		Inscription inscription = findById(inscriptionId);
		inscription.setStatut(StatutInscription.TERMINEE);
		inscriptionRepository.save(inscription);
	}

	private int getProchainPositionListeAttente(Formation formation) {
		List<Inscription> listeAttente = inscriptionRepository.findByFormationAndStatut(formation,
				StatutInscription.EN_ATTENTE);
		return listeAttente.size() + 1;
	}


	public Inscription ajouterParticipant(Long formationId, Long stagiaireId) {
		Formation formation = formationService.findById(formationId);
		assertCanManageFormation(formation);
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);

		if (!stagiaire.isStagiaire()) {
			throw new BadRequestException("Le participant doit être un stagiaire");
		}

		// Vérifier capacité
		if (formation.isPleine()) {
			throw new BadRequestException("La formation a atteint sa capacité maximale");
		}

		var existingOpt = inscriptionRepository.findByStagiaireAndFormation(stagiaire, formation);
		if (existingOpt.isPresent()) {
			Inscription existing = existingOpt.get();
			if (existing.getStatut() == StatutInscription.CONFIRMEE || existing.getStatut() == StatutInscription.EN_COURS) {
				throw new BadRequestException("Ce stagiaire est déjà inscrit à cette formation");
			}
			// Réactiver
			existing.setStatut(StatutInscription.CONFIRMEE);
			existing.setDateValidation(LocalDateTime.now());
			existing.setMotifRefus(null);
			existing.setPositionListeAttente(null);
			Inscription saved = inscriptionRepository.save(existing);
			presenceService.ensurePresencesForFormationStagiaire(formation.getId(), existing.getStagiaire().getId());
			notificationService.envoyerNotificationInscriptionValidee(saved);
			return saved;
		}

		Inscription inscription = new Inscription();
		inscription.setStagiaire(stagiaire);
		inscription.setFormation(formation);
		inscription.setStatut(StatutInscription.CONFIRMEE);
		inscription.setDateValidation(LocalDateTime.now());

		Inscription saved = inscriptionRepository.save(inscription);
		notificationService.envoyerNotificationInscriptionValidee(saved);
		return saved;
	}

	public void retirerParticipant(Long formationId, Long stagiaireId) {
		Formation formation = formationService.findById(formationId);
		assertCanManageFormation(formation);
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
		Inscription inscription = inscriptionRepository.findByStagiaireAndFormation(stagiaire, formation)
				.orElseThrow(() -> new ResourceNotFoundException("Inscription", "stagiaire/formation", stagiaireId + "/" + formationId));

		// On ne supprime pas physiquement, on le retire de la liste
		inscription.setStatut(StatutInscription.ABANDONNEE);
		inscriptionRepository.save(inscription);
	}

	public void marquerEchecFinal(Long formationId, Long stagiaireId, String commentaire) {
		Formation formation = formationService.findById(formationId);
		assertCanManageFormation(formation);
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
		Inscription inscription = inscriptionRepository.findByStagiaireAndFormation(stagiaire, formation)
				.orElseThrow(() -> new ResourceNotFoundException("Inscription", "stagiaire/formation", stagiaireId + "/" + formationId));

		inscription.setStatut(StatutInscription.ABANDONNEE);
		if (commentaire != null && !commentaire.isBlank()) {
			inscription.setMotifRefus(commentaire);
		}
		inscriptionRepository.save(inscription);
	}

	/**
	 * Marque une inscription TERMINEE (réussite) pour un stagiaire dans une formation.
	 * Utilisé après publication des notes (normal ou rattrapage).
	 */
	public void marquerReussiteFinale(Long formationId, Long stagiaireId) {
		Formation formation = formationService.findById(formationId);
		assertCanManageFormation(formation);
		Utilisateur stagiaire = utilisateurService.findById(stagiaireId);
		Inscription inscription = inscriptionRepository.findByStagiaireAndFormation(stagiaire, formation)
				.orElseThrow(() -> new ResourceNotFoundException("Inscription", "stagiaire/formation", stagiaireId + "/" + formationId));

		inscription.setStatut(StatutInscription.TERMINEE);
		inscriptionRepository.save(inscription);
	}


	public long countByStatut(StatutInscription statut) {
		return inscriptionRepository.countByStatut(statut);
	}
}
