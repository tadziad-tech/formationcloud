package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.TpRessourceDTO;
import com.formationcloud.platform.dto.TpRessourceRequest;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.FormationRepository;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.repository.TpRessourceRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TpRessourceService {

	private final TpRessourceRepository tpRessourceRepository;
	private final FormationRepository formationRepository;
	private final InscriptionRepository inscriptionRepository;
	private final NotificationService notificationService;
	private final StorageService storageService;

	public List<TpRessourceDTO> listByFormation(Long formationId) {
		Formation formation = formationRepository.findById(formationId)
				.orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

		assertCanViewFormation(formation);

		return tpRessourceRepository.findByFormation_IdOrderByDateCreationDesc(formationId)
				.stream()
				.map(this::toDTO)
				.toList();
	}

	public List<TpRessourceDTO> listByFormationAndType(Long formationId, TypeTpRessource type) {
		Formation formation = formationRepository.findById(formationId)
				.orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

		assertCanViewFormation(formation);

		return tpRessourceRepository.findByFormation_IdAndTypeOrderByDateCreationDesc(formationId, type)
				.stream()
				.map(this::toDTO)
				.toList();
	}

	public TpRessourceDTO create(Long formationId, TpRessourceRequest req) {
		Formation formation = formationRepository.findById(formationId)
				.orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

		assertCanManageFormation(formation);

		TpRessource tp = new TpRessource();
		tp.setFormation(formation);
		applyRequest(tp, req);

		TpRessource saved = tpRessourceRepository.save(tp);

		// Notifier les stagiaires inscrits
		List<Utilisateur> destinataires = inscriptionRepository
				.findByFormation_IdAndStatutIn(formationId, List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE))
				.stream()
				.map(Inscription::getStagiaire)
				.toList();
		if (!destinataires.isEmpty()) {
			notificationService.envoyerNotificationTpPublie(saved, destinataires);
		}

		return toDTO(saved);
	}

	public TpRessourceDTO update(Long tpId, TpRessourceRequest req) {
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));

		Formation formation = tp.getFormation();
		assertCanManageFormation(formation);

		applyRequest(tp, req);
		TpRessource saved = tpRessourceRepository.save(tp);

		return toDTO(saved);
	}

	public void delete(Long tpId) {
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));

		assertCanManageFormation(tp.getFormation());
		tpRessourceRepository.delete(tp);
	}

	public TpRessourceDTO get(Long tpId) {
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));

		assertCanViewFormation(tp.getFormation());
		return toDTO(tp);
	}

	public TpRessourceDTO uploadFichier(Long tpId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Fichier vide ou absent");
		}
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));
		assertCanManageFormation(tp.getFormation());

		String path = storageService.store("tp", file);
		String oldPath = tp.getFichierUrl();
		if (oldPath != null && !oldPath.isBlank()) {
			try {
				storageService.delete(oldPath);
			} catch (Exception ignored) {
				// best effort
			}
		}
		tp.setFichierUrl(path);
		TpRessource saved = tpRessourceRepository.save(tp);
		return toDTO(saved);
	}

	public ResponseEntity<Resource> downloadFichier(Long tpId) {
		TpRessource tp = tpRessourceRepository.findById(tpId)
				.orElseThrow(() -> new ResourceNotFoundException("TP/Ressource introuvable"));
		assertCanViewFormation(tp.getFormation());

		String path = tp.getFichierUrl();
		if (path == null || path.isBlank()) {
			throw new ResourceNotFoundException("Aucun fichier associé à cette ressource");
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

	private void assertCanViewFormation(Formation formation) {
		UserPrincipal u = SecurityUtils.currentUser();
		if (u == null) throw new AccessDeniedException("Non authentifié");
		String role = String.valueOf(u.getRole()).toUpperCase();

		if ("ADMIN".equals(role)) return;

		if ("FORMATEUR".equals(role)) {
			if (formation.getFormateur() != null && formation.getFormateur().getId() != null
					&& formation.getFormateur().getId().equals(u.getId())) return;
			throw new AccessDeniedException("Accès interdit");
		}

		// STAGIAIRE
		boolean ok = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
				u.getId(),
				formation.getId(),
				List.of(StatutInscription.CONFIRMEE, StatutInscription.EN_COURS, StatutInscription.TERMINEE)
		);
		if (!ok) throw new AccessDeniedException("Accès interdit");
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

	private void applyRequest(TpRessource tp, TpRessourceRequest req) {
		tp.setTitre(req.getTitre().trim());
		if (req.getDescription() != null) {
			tp.setDescription(req.getDescription().trim());
		}
		tp.setType(req.getType());
		tp.setFichierUrl(req.getFichierUrl());
		tp.setDateLimite(req.getDateLimite());
	}

	private TpRessourceDTO toDTO(TpRessource tp) {
		TpRessourceDTO dto = new TpRessourceDTO();
		dto.setId(tp.getId());
		dto.setFormationId(tp.getFormation() != null ? tp.getFormation().getId() : null);
		dto.setTitre(tp.getTitre());
		dto.setDescription(tp.getDescription());
		dto.setType(tp.getType());
		dto.setFichierUrl(tp.getFichierUrl());
		dto.setDateLimite(tp.getDateLimite());
		dto.setDateCreation(tp.getDateCreation());
		dto.setDateModification(tp.getDateModification());
		return dto;
	}
}
