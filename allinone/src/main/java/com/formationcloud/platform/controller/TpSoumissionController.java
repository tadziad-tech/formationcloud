package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.TpCorrectionRequest;
import com.formationcloud.platform.dto.TpSoumissionDTO;
import com.formationcloud.platform.dto.TpSoumissionRequest;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.TpSoumissionService;
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
public class TpSoumissionController {

	private final TpSoumissionService tpSoumissionService;
	private final FormationAccessService formationAccessService;

	@PostMapping("/tp-ressources/{tpId}/soumissions")
	@PreAuthorize("hasRole('STAGIAIRE')")
	public ResponseEntity<TpSoumissionDTO> submit(
			@PathVariable Long tpId,
			@Valid @RequestBody TpSoumissionRequest request) {
		formationAccessService.assertAdminOrEnrolledStagiaireByTpId(tpId);
		return ResponseEntity.ok(tpSoumissionService.submit(tpId, request));
	}

	@PutMapping("/tp-soumissions/{soumissionId}/corriger")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<TpSoumissionDTO> correct(
			@PathVariable Long soumissionId,
			@Valid @RequestBody TpCorrectionRequest request) {
		return ResponseEntity.ok(tpSoumissionService.correct(soumissionId, request));
	}

	@GetMapping("/tp-ressources/{tpId}/soumissions")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<List<TpSoumissionDTO>> listByTp(@PathVariable Long tpId) {
		formationAccessService.assertAdminOrAssignedFormateurByTpId(tpId);
		return ResponseEntity.ok(tpSoumissionService.listByTp(tpId));
	}

	@GetMapping("/stagiaires/{stagiaireId}/tp-soumissions")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAGIAIRE')")
	public ResponseEntity<List<TpSoumissionDTO>> listByStagiaire(@PathVariable Long stagiaireId) {
		return ResponseEntity.ok(tpSoumissionService.listByStagiaire(stagiaireId));
	}

	@GetMapping("/tp-soumissions/{soumissionId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<TpSoumissionDTO> get(@PathVariable Long soumissionId) {
		return ResponseEntity.ok(tpSoumissionService.get(soumissionId));
	}

	@PostMapping(value = "/tp-ressources/{tpId}/soumissions/upload", consumes = "multipart/form-data")
	@PreAuthorize("hasRole('STAGIAIRE')")
	public ResponseEntity<TpSoumissionDTO> submitWithFile(
			@PathVariable Long tpId,
			@RequestPart("file") MultipartFile file,
			@RequestPart(value = "commentaire", required = false) String commentaire) {
		formationAccessService.assertAdminOrEnrolledStagiaireByTpId(tpId);
		return ResponseEntity.ok(tpSoumissionService.submitWithFile(tpId, file, commentaire));
	}

	@GetMapping("/tp-soumissions/{soumissionId}/fichier")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> downloadSoumissionFile(@PathVariable Long soumissionId) {
		return tpSoumissionService.downloadSoumissionFile(soumissionId);
	}
}
