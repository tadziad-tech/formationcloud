package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.AdminDashboardOverviewDTO;
import com.formationcloud.platform.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Un seul endpoint pour alimenter le dashboard admin.
     * @param days période (7..60) pour la courbe des inscriptions.
     *             Valeur 0 (ou négative) => mode "Tout" (pas de filtre de période).
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardOverviewDTO> overview(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(adminDashboardService.getOverview(days));
    }
}
