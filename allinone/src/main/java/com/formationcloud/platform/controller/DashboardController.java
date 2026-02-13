package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.DashboardStatsDTO;
import com.formationcloud.platform.dto.FormateurDashboardOverviewDTO;
import com.formationcloud.platform.dto.StagiaireDashboardOverviewDTO;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.service.DashboardService;
import com.formationcloud.platform.service.RoleDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;
	private final RoleDashboardService roleDashboardService;

	@GetMapping("/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<DashboardStatsDTO> getGlobalStats() {
		return ResponseEntity.ok(dashboardService.getGlobalStats());
	}
	@GetMapping("/admin/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<DashboardStatsDTO> getAdminStatsCompat() {
		return ResponseEntity.ok(dashboardService.getGlobalStats());
	}


	@GetMapping("/formateur/{formateurId}/stats")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<DashboardStatsDTO> getFormateurStats(@PathVariable Long formateurId) {
		SecurityUtils.assertAdminOrSelf(formateurId);
		return ResponseEntity.ok(dashboardService.getFormateurStats(formateurId));
	}

	@GetMapping("/formateur/{formateurId}/overview")
	@PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
	public ResponseEntity<FormateurDashboardOverviewDTO> getFormateurOverview(
			@PathVariable Long formateurId,
			@RequestParam(defaultValue = "14") int days) {
		SecurityUtils.assertAdminOrSelf(formateurId);
		return ResponseEntity.ok(roleDashboardService.getFormateurOverview(formateurId, days));
	}

	@GetMapping("/stagiaire/{stagiaireId}/stats")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAGIAIRE')")
	public ResponseEntity<DashboardStatsDTO> getStagiaireStats(@PathVariable Long stagiaireId) {
		SecurityUtils.assertAdminOrSelf(stagiaireId);
		return ResponseEntity.ok(dashboardService.getStagiaireStats(stagiaireId));
	}

	@GetMapping("/stagiaire/{stagiaireId}/overview")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAGIAIRE')")
	public ResponseEntity<StagiaireDashboardOverviewDTO> getStagiaireOverview(
			@PathVariable Long stagiaireId,
			@RequestParam(defaultValue = "14") int days) {
		SecurityUtils.assertAdminOrSelf(stagiaireId);
		return ResponseEntity.ok(roleDashboardService.getStagiaireOverview(stagiaireId, days));
	}
}
