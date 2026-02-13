package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.TpCorrectionRequest;
import com.formationcloud.platform.dto.TpSoumissionDTO;
import com.formationcloud.platform.dto.TpSoumissionRequest;
import com.formationcloud.platform.dto.UtilisateurSummaryDTO;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.FormationRepository;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.repository.TpRessourceRepository;
import com.formationcloud.platform.repository.TpSoumissionRepository;
import com.formationcloud.platform.repository.UtilisateurRepository;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.security.UserPrincipal;
import com.formationcloud.platform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TpSoumissionService {

	private final TpSoumissionRepository tpSoumissionRepository;
	private final TpRessourceRepository tpRessourceRepository;
	private final FormationRepository formationRepository;
	private final InscriptionRepository inscriptionRepository;
	private final UtilisateurRepository utilisateurRepository;
	private final NotificationService notificationService;
	private final StorageService storageService;

	public TpSoumissionDTO submit(Long tpId, TpSoumissionRequest req) {
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));

		if (tp.getDateLimite() != null && LocalDateTime.now().isAfter(tp.getDateLimite())) {
			throw new BadRequestException("Date limite dépassée. Soumission impossible.");
		}

		UserPrincipal currentUser = SecurityUtils.currentUser();
		if (currentUser == null) {
			throw new AccessDeniedException("Non authentifié");
		}

		// Vérifier que l'utilisateur est un stagiaire inscrit à la formation
		Formation formation = tp.getFormation();
		boolean isInscrit = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
				currentUser.getId(),
				formation.getId(),
				List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
		);
		if (!isInscrit) {
			throw new AccessDeniedException("Vous devez être inscrit à cette formation pour soumettre un TP");
		}

		// Vérifier si une soumission existe déjà
		TpSoumission soumission = tpSoumissionRepository.findByTp_IdAndStagiaire_Id(tpId, currentUser.getId())
				.orElse(null);

		// Récupérer le vrai utilisateur depuis la base
		Utilisateur stagiaire = utilisateurRepository.findById(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

		if (soumission == null) {
			soumission = new TpSoumission();
			soumission.setTp(tp);
			soumission.setStagiaire(stagiaire);
		} else {
			// Mettre à jour le stagiaire si nécessaire
			soumission.setStagiaire(stagiaire);
		}

		soumission.setStatut(StatutTpSoumission.SOUMIS);
		soumission.setFichierSoumisUrl(req.getFichierSoumisUrl().trim());
		if (req.getCommentaire() != null && !req.getCommentaire().isBlank()) {
			soumission.setCommentaire(req.getCommentaire().trim());
		}

		TpSoumission saved = tpSoumissionRepository.save(soumission);
		
		// Notifier le formateur de la nouvelle soumission
		Utilisateur formateur = tp.getFormation().getFormateur();
		if (formateur != null) {
			notificationService.envoyerSiAbsent(formateur, TypeNotification.NOUVELLE_SOUMISSION_TP,
					"Nouvelle soumission TP: " + tp.getTitre() + " par " + stagiaire.getPrenom() + " " + stagiaire.getNom(),
					"/formations?tpId=" + tp.getId());
		}
		
		return toDTO(saved);
	}

	public TpSoumissionDTO submitWithFile(Long tpId, MultipartFile file, String commentaire) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Fichier vide ou absent");
		}
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));

		if (tp.getDateLimite() != null && LocalDateTime.now().isAfter(tp.getDateLimite())) {
			throw new BadRequestException("Date limite dépassée. Soumission impossible.");
		}

		UserPrincipal currentUser = SecurityUtils.currentUser();
		if (currentUser == null) {
			throw new AccessDeniedException("Non authentifié");
		}

		Formation formation = tp.getFormation();
		boolean isInscrit = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
				currentUser.getId(),
				formation.getId(),
				List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
		);
		if (!isInscrit) {
			throw new AccessDeniedException("Vous devez être inscrit à cette formation pour soumettre un TP");
		}

		String path = storageService.store("soumissions", file);

		TpSoumission soumission = tpSoumissionRepository.findByTp_IdAndStagiaire_Id(tpId, currentUser.getId())
				.orElse(null);

		Utilisateur stagiaire = utilisateurRepository.findById(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

		if (soumission == null) {
			soumission = new TpSoumission();
			soumission.setTp(tp);
			soumission.setStagiaire(stagiaire);
		} else {
			soumission.setStagiaire(stagiaire);
			String oldPath = soumission.getFichierSoumisUrl();
			if (oldPath != null && !oldPath.isBlank()) {
				try {
					storageService.delete(oldPath);
				} catch (Exception ignored) {
				}
			}
		}

		soumission.setStatut(StatutTpSoumission.SOUMIS);
		soumission.setFichierSoumisUrl(path);
		if (commentaire != null && !commentaire.isBlank()) {
			soumission.setCommentaire(commentaire.trim());
		}

		TpSoumission saved = tpSoumissionRepository.save(soumission);
		
		// Notifier le formateur de la nouvelle soumission
		Utilisateur formateur = tp.getFormation().getFormateur();
		if (formateur != null) {
			notificationService.envoyerSiAbsent(formateur, TypeNotification.NOUVELLE_SOUMISSION_TP,
					"Nouvelle soumission TP: " + tp.getTitre() + " par " + stagiaire.getPrenom() + " " + stagiaire.getNom(),
					"/formations?formationId=" + tp.getFormation().getId() + "&tab=ressources&tpId=" + tp.getId());
		}
		
		return toDTO(saved);
	}

	public ResponseEntity<Resource> downloadSoumissionFile(Long soumissionId) {
		TpSoumission soumission = tpSoumissionRepository.findById(soumissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Soumission introuvable"));

		UserPrincipal currentUser = SecurityUtils.currentUser();
		if (currentUser == null) {
			throw new AccessDeniedException("Non authentifié");
		}

		Formation formation = soumission.getTp().getFormation();
		String role = String.valueOf(currentUser.getRole()).toUpperCase();

		boolean allowed = false;
		if ("ADMIN".equals(role)) {
			allowed = true;
		} else if ("FORMATEUR".equals(role)) {
			allowed = formation.getFormateur() != null && formation.getFormateur().getId().equals(currentUser.getId());
		} else {
			allowed = soumission.getStagiaire() != null && soumission.getStagiaire().getId().equals(currentUser.getId());
		}
		if (!allowed) {
			throw new AccessDeniedException("Accès interdit");
		}

		String path = soumission.getFichierSoumisUrl();
		if (path == null || path.isBlank()) {
			throw new ResourceNotFoundException("Aucun fichier associé à cette soumission");
		}
		Resource resource = storageService.loadAsResource(path);
		String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
		String contentType = contentTypeFromFileName(fileName);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
				.body(resource);
	}

	private static String contentTypeFromFileName(String fileName) {
		if (fileName == null) return "application/octet-stream";
		String lower = fileName.toLowerCase();
		if (lower.endsWith(".pdf")) return "application/pdf";
		if (lower.endsWith(".doc")) return "application/msword";
		if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		if (lower.endsWith(".zip")) return "application/zip";
		return "application/octet-stream";
	}

	public TpSoumissionDTO correct(Long soumissionId, TpCorrectionRequest req) {
		TpSoumission soumission = tpSoumissionRepository.findById(soumissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Soumission introuvable"));

		Formation formation = soumission.getTp().getFormation();
		assertCanManageFormation(formation);

		soumission.setStatut(req.getStatut());
		if (req.getNote() != null) {
			soumission.setNote(req.getNote());
		}
		if (req.getCommentaire() != null && !req.getCommentaire().isBlank()) {
			soumission.setFeedback(req.getCommentaire().trim());
		}

		TpSoumission saved = tpSoumissionRepository.save(soumission);

		// Notifier le stagiaire
		if (soumission.getStagiaire() != null) {
			notificationService.envoyerNotificationTpCorrige(saved);
		}

		return toDTO(saved);
	}

	public List<TpSoumissionDTO> listByTp(Long tpId) {
		UserPrincipal currentUser = SecurityUtils.currentUser();
		if (currentUser != null && "STAGIAIRE".equalsIgnoreCase(String.valueOf(currentUser.getRole()))) {
			throw new AccessDeniedException("Accès interdit");
		}

		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));

		Formation formation = tp.getFormation();
		assertCanManageFormation(formation);

		return tpSoumissionRepository.findByTp_IdOrderByDateSoumissionDesc(tpId)
				.stream()
				.map(this::toDTO)
				.toList();
	}

	public List<TpSoumissionDTO> listByStagiaire(Long stagiaireId) {
		UserPrincipal currentUser = SecurityUtils.currentUser();
		if (currentUser == null) {
			throw new AccessDeniedException("Non authentifié");
		}

		// Un stagiaire ne peut voir que ses propres soumissions
		if (!currentUser.getId().equals(stagiaireId) && !"ADMIN".equalsIgnoreCase(String.valueOf(currentUser.getRole()))) {
			throw new AccessDeniedException("Accès interdit");
		}

		return tpSoumissionRepository.findByStagiaire_IdOrderByDateSoumissionDesc(stagiaireId)
				.stream()
				.map(this::toDTO)
				.toList();
	}

	public TpSoumissionDTO get(Long soumissionId) {
		TpSoumission soumission = tpSoumissionRepository.findById(soumissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Soumission introuvable"));

		UserPrincipal currentUser = SecurityUtils.currentUser();
		if (currentUser == null) {
			throw new AccessDeniedException("Non authentifié");
		}

		// Vérifier les droits d'accès
		Formation formation = soumission.getTp().getFormation();
		String role = String.valueOf(currentUser.getRole()).toUpperCase();

		if ("ADMIN".equals(role) || "FORMATEUR".equals(role)) {
			// Admin ou formateur de la formation peut voir
			if ("FORMATEUR".equals(role)) {
				if (formation.getFormateur() == null || !formation.getFormateur().getId().equals(currentUser.getId())) {
					throw new AccessDeniedException("Accès interdit");
				}
			}
		} else {
			// Stagiaire ne peut voir que ses propres soumissions
			if (soumission.getStagiaire() == null || !soumission.getStagiaire().getId().equals(currentUser.getId())) {
				throw new AccessDeniedException("Accès interdit");
			}
		}

		return toDTO(soumission);
	}

	private void assertCanManageFormation(Formation formation) {
		UserPrincipal u = SecurityUtils.currentUser();
		if (u == null) throw new AccessDeniedException("Non authentifié");
		String role = String.valueOf(u.getRole()).toUpperCase();

		if ("ADMIN".equals(role)) return;

		if ("FORMATEUR".equals(role)) {
			if (formation.getFormateur() != null && formation.getFormateur().getId() != null
					&& formation.getFormateur().getId().equals(u.getId())) return;
		}
		throw new AccessDeniedException("Accès interdit");
	}

	private TpSoumissionDTO toDTO(TpSoumission soumission) {
		TpSoumissionDTO dto = new TpSoumissionDTO();
		dto.setId(soumission.getId());
		dto.setTpId(soumission.getTp() != null ? soumission.getTp().getId() : null);
		dto.setStagiaireId(soumission.getStagiaire() != null ? soumission.getStagiaire().getId() : null);
		dto.setStatut(soumission.getStatut());
		dto.setFichierSoumisUrl(soumission.getFichierSoumisUrl());
		dto.setCommentaire(soumission.getCommentaire());
		dto.setFeedback(soumission.getFeedback());
		dto.setNote(soumission.getNote());
		dto.setDateSoumission(soumission.getDateSoumission());
		dto.setDateModification(soumission.getDateModification());

		if (soumission.getStagiaire() != null) {
			UtilisateurSummaryDTO stagiaireDTO = new UtilisateurSummaryDTO();
			stagiaireDTO.setId(soumission.getStagiaire().getId());
			stagiaireDTO.setNom(soumission.getStagiaire().getNom());
			stagiaireDTO.setPrenom(soumission.getStagiaire().getPrenom());
			stagiaireDTO.setEmail(soumission.getStagiaire().getEmail());
			stagiaireDTO.setRole(soumission.getStagiaire().getRole());
			stagiaireDTO.setPhotoProfil(soumission.getStagiaire().getPhotoProfil());
			dto.setStagiaire(stagiaireDTO);
		}

		return dto;
	}
}
