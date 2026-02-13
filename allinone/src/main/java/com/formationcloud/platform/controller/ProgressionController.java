package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.ParticipantProgressDTO;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.ProgressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProgressionController {

    private final ProgressionService progressionService;
    private final FormationAccessService formationAccessService;

    @GetMapping("/formations/{formationId}/progression/participants")
    @PreAuthorize("hasAnyRole('ADMIN','FORMATEUR')")
    public ResponseEntity<List<ParticipantProgressDTO>> participants(@PathVariable Long formationId) {
        formationAccessService.assertAdminOrAssignedFormateur(formationId);
        return ResponseEntity.ok(progressionService.progressionParticipants(formationId));
    }

    @GetMapping("/formations/{formationId}/progression/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParticipantProgressDTO> me(@PathVariable Long formationId) {
        formationAccessService.assertCanAccessFormationData(formationId);
        return ResponseEntity.ok(progressionService.maProgression(formationId));
    }
}
