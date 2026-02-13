package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.FormationAccessDTO;
import com.formationcloud.platform.dto.FormationMeAccessDTO;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.FormationRepository;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.security.SecurityUtils;
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
public class FormationService {

	private static final List<StatutInscription> CONFIRMED_STATUTS = List.of(
			StatutInscription.CONFIRMEE,
			StatutInscription.EN_COURS,
			StatutInscription.TERMINEE
	);

	private final FormationRepository formationRepository;
	private final InscriptionRepository inscriptionRepository;
	private final UtilisateurService utilisateurService;
	private final NotificationService notificationService;

	public List<Formation> findAll() {
		return formationRepository.findAll();
	}

	public Formation findById(Long id) {
		return formationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Formation", "id", id));
	}

	public List<Formation> findByStatut(StatutFormation statut) {
		return formationRepository.findByStatut(statut);
	}

	public List<Formation> findByFormateur(Long formateurId) {
		Utilisateur formateur = utilisateurService.findById(formateurId);
		return formationRepository.findByFormateur(formateur);
	}

	public List<Formation> findFormationsActives() {
		return formationRepository.findByStatut(StatutFormation.ACTIVE);
	}

	public List<Formation> findFormationsAVenir() {
		return formationRepository.findFormationsAVenir(LocalDate.now());
	}

	public List<Formation> findFormationsEnCours() {
		return formationRepository.findFormationsEnCours(LocalDate.now());
	}

	public Formation createFormation(Formation formation, Long formateurId) {
		// Valider le formateur
		Utilisateur formateur = utilisateurService.findById(formateurId);
		if (!formateur.isFormateur()) {
			throw new BadRequestException("L'utilisateur n'est pas un formateur");
		}
		if (!formateur.getStatutValidation()) {
			throw new BadRequestException("Le formateur n'est pas validé");
		}

		// Valider les dates
		if (formation.getDateFin().isBefore(formation.getDateDebut())) {
			throw new BadRequestException("La date de fin doit être après la date de début");
		}

		formation.setFormateur(formateur);
		formation.setStatut(StatutFormation.ACTIVE);

		Formation saved = formationRepository.save(formation);
		log.info("Formation créée: {} par {}", saved.getNom(), formateur.getNomComplet());

		// Notifier le formateur assigné
		notificationService.notifierFormateurAssigne(saved);

		// Notifier les stagiaires actifs
		List<Utilisateur> stagiaires = utilisateurService.findStagiairesActifs();
		notificationService.envoyerNotificationNouvelleFormation(saved, stagiaires);

		return saved;
	}

	public Formation updateFormation(Long id, Formation formationDetails) {
		Formation formation = findById(id);

		Long oldFormateurId = formation.getFormateur() != null ? formation.getFormateur().getId() : null;

		formation.setNom(formationDetails.getNom());
		formation.setDescription(formationDetails.getDescription());
		formation.setType(formationDetails.getType());
		formation.setCategorie(formationDetails.getCategorie());
		formation.setCapaciteMax(formationDetails.getCapaciteMax());
		formation.setDateDebut(formationDetails.getDateDebut());
		formation.setDateFin(formationDetails.getDateFin());
		formation.setLieu(formationDetails.getLieu());
		formation.setDureeHeures(formationDetails.getDureeHeures());
		formation.setPrix(formationDetails.getPrix());
		if (formationDetails.getFormateur() != null) {
			formation.setFormateur(formationDetails.getFormateur());
		}

		Formation saved = formationRepository.save(formation);

		// Notifier le nouveau formateur si le formateur a changé
		if (saved.getFormateur() != null) {
			Long newFormateurId = saved.getFormateur().getId();
			if (!newFormateurId.equals(oldFormateurId)) {
				notificationService.notifierFormateurAssigne(saved);
			}
		}

		return saved;
	}

	public void changerStatut(Long id, StatutFormation nouveauStatut) {
		Formation formation = findById(id);
		formation.setStatut(nouveauStatut);
		formationRepository.save(formation);
		log.info("Statut de la formation {} changé à {}", formation.getNom(), nouveauStatut);
	}

	public void assignerFormateur(Long formationId, Long formateurId) {
		Formation formation = findById(formationId);
		Utilisateur formateur = utilisateurService.findById(formateurId);

		if (!formateur.isFormateur()) {
			throw new BadRequestException("L'utilisateur n'est pas un formateur");
		}

		formation.setFormateur(formateur);
		Formation saved = formationRepository.save(formation);
		log.info("Formateur {} assigné à la formation {}", formateur.getNomComplet(), formation.getNom());
		notificationService.notifierFormateurAssigne(saved);
	}

	/**
	 * Suppression "safe" : on interdit le hard-delete si des inscriptions existent,
	 * sauf si l'admin force explicitement (ex: pour nettoyage / démo).
	 */
	public void deleteFormation(Long id) {
		deleteFormation(id, false);
	}

	public void deleteFormation(Long id, boolean force) {
		Formation formation = findById(id);

		// Suppression directe autorisée si la formation est TERMINEE.
		if (!force && formation.getStatut() != StatutFormation.TERMINEE && !formation.getInscriptions().isEmpty()) {
			throw new BadRequestException("Impossible de supprimer une formation avec des inscriptions");
		}

		formationRepository.delete(formation);
		log.info("Formation supprimée{}: {}", force ? " (FORCE)" : "", formation.getNom());
	}

	public boolean verifierPrerequisRempli(Long formationId, Long stagiaireId) {
		Formation formation = findById(formationId);

		if (formation.getPrerequis() == null) {
			return true; // Pas de prérequis
		}

		// Vérifier si le stagiaire a un certificat pour la formation prérequise
		// Cette logique sera implémentée dans CertificatService
		return true;
	}

	public long countByStatut(StatutFormation statut) {
		return formationRepository.countByStatut(statut);
	}

	/**
	 * Indique si l'utilisateur courant a accès aux données sensibles de la formation
	 * (formateur assigné ou stagiaire inscrit avec statut confirmé).
	 * N'exige pas d'autorisation formation-level : tout utilisateur authentifié peut appeler.
	 */
	public FormationMeAccessDTO getMeAccess(Long formationId) {
		var u = SecurityUtils.currentUser();
		boolean isAssignedFormateur = false;
		boolean isEnrolledStagiaire = false;
		if (u == null) {
			return new FormationMeAccessDTO(false, false);
		}
		Formation formation = formationRepository.findById(formationId)
				.orElseThrow(() -> new ResourceNotFoundException("Formation", "id", formationId));
		if (formation.getFormateur() != null && formation.getFormateur().getId() != null
				&& formation.getFormateur().getId().equals(u.getId())) {
			isAssignedFormateur = true;
		}
		if ("STAGIAIRE".equalsIgnoreCase(u.getRole())) {
			isEnrolledStagiaire = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
					u.getId(), formationId, CONFIRMED_STATUTS);
		}
		return new FormationMeAccessDTO(isAssignedFormateur, isEnrolledStagiaire);
	}

	/**
	 * Détails d'accès de l'utilisateur courant à une formation (rôle, formateur assigné, statut inscription).
	 * Tout utilisateur authentifié peut appeler.
	 */
	public FormationAccessDTO getFormationAccess(Long formationId) {
		var u = SecurityUtils.currentUser();
		String roleStr = u != null ? String.valueOf(u.getRole()).toUpperCase() : "STAGIAIRE";
		boolean isAdmin = "ADMIN".equalsIgnoreCase(roleStr);
		boolean isAssignedFormateur = false;
		String inscriptionStatus = null;
		boolean isEnrolledConfirmed = false;

		if (u == null) {
			return new FormationAccessDTO(roleStr, isAdmin, false, null, false);
		}

		Formation formation = formationRepository.findById(formationId)
				.orElseThrow(() -> new ResourceNotFoundException("Formation", "id", formationId));

		if (formation.getFormateur() != null && formation.getFormateur().getId() != null
				&& formation.getFormateur().getId().equals(u.getId())) {
			isAssignedFormateur = true;
		}

		if ("STAGIAIRE".equalsIgnoreCase(roleStr)) {
			var optInsc = inscriptionRepository.findByFormation_IdAndStagiaire_Id(formationId, u.getId());
			if (optInsc.isPresent()) {
				StatutInscription st = optInsc.get().getStatut();
				inscriptionStatus = st != null ? st.name() : null;
				isEnrolledConfirmed = CONFIRMED_STATUTS.contains(st);
			}
		}

		return new FormationAccessDTO(roleStr, isAdmin, isAssignedFormateur, inscriptionStatus, isEnrolledConfirmed);
	}
}
