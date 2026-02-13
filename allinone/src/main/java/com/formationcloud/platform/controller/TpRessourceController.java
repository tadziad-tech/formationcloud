package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.TpRessourceDTO;
import com.formationcloud.platform.dto.TpRessourceRequest;
import com.formationcloud.platform.model.TypeTpRessource;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.TpRessourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TpRessourceController {

	private final TpRessourceService tpRessourceService;
	private final FormationAccessService formationAccessService;

	@GetMapping("/formations/{formationId}/tp-ressources")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<TpRessourceDTO>> listByFormation(@PathVariable Long formationId) {
		formationAccessService.assertCanAccessFormationData(formationId);
		return ResponseEntity.ok(tpRessourceService.listByFormation(formationId));
	}

	@GetMapping("/formations/{formationId}/tp-ressources/type/{type}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<TpRessourceDTO>> listByFormationAndType(
			@PathVariable Long formationId,
			@PathVariable TypeTpRessource type) {
		formationAccessService.assertCanAccessFormationData(formationId);
		return ResponseEntity.ok(tpRessourceService.listByFormationAndType(formationId, type));
	}

	@PostMapping("/formations/{formationId}/tp-ressources")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<TpRessourceDTO> create(
			@PathVariable Long formationId,
			@Valid @RequestBody TpRessourceRequest request) {
		formationAccessService.assertAdminOrAssignedFormateur(formationId);
		return ResponseEntity.ok(tpRessourceService.create(formationId, request));
	}

	@GetMapping("/tp-ressources/{tpId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<TpRessourceDTO> get(@PathVariable Long tpId) {
		formationAccessService.assertCanAccessFormationDataByTpId(tpId);
		return ResponseEntity.ok(tpRessourceService.get(tpId));
	}

	@PutMapping("/tp-ressources/{tpId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<TpRessourceDTO> update(
			@PathVariable Long tpId,
			@Valid @RequestBody TpRessourceRequest request) {
		formationAccessService.assertAdminOrAssignedFormateurByTpId(tpId);
		return ResponseEntity.ok(tpRessourceService.update(tpId, request));
	}

	@DeleteMapping("/tp-ressources/{tpId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<Void> delete(@PathVariable Long tpId) {
		formationAccessService.assertAdminOrAssignedFormateurByTpId(tpId);
		tpRessourceService.delete(tpId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(value = "/tp-ressources/{tpId}/fichier", consumes = "multipart/form-data")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<TpRessourceDTO> uploadFichier(
			@PathVariable Long tpId,
			@RequestPart("file") MultipartFile file) {
		formationAccessService.assertAdminOrAssignedFormateurByTpId(tpId);
		return ResponseEntity.ok(tpRessourceService.uploadFichier(tpId, file));
	}

	@GetMapping("/tp-ressources/{tpId}/fichier")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> downloadFichier(@PathVariable Long tpId) {
		formationAccessService.assertCanAccessFormationDataByTpId(tpId);
		return tpRessourceService.downloadFichier(tpId);
	}
}
