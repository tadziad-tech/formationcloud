package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.PresenceDTO;
import com.formationcloud.platform.dto.PresenceUpdateRequest;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.PresenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;
    private final FormationAccessService formationAccessService;

    @GetMapping("/seances/{seanceId}/presences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PresenceDTO>> list(@PathVariable Long seanceId) {
        formationAccessService.assertCanAccessFormationDataBySeanceId(seanceId);
        return ResponseEntity.ok(presenceService.listForSeance(seanceId));
    }

    @PutMapping("/seances/{seanceId}/presences/bulk")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PresenceDTO>> bulk(@PathVariable Long seanceId, @Valid @RequestBody List<PresenceUpdateRequest> reqs) {
        formationAccessService.assertCanAccessFormationDataBySeanceId(seanceId);
        return ResponseEntity.ok(presenceService.bulkUpdate(seanceId, reqs));
    }

    @GetMapping("/seances/{seanceId}/presences/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresenceDTO> me(@PathVariable Long seanceId) {
        formationAccessService.assertCanAccessFormationDataBySeanceId(seanceId);
        return ResponseEntity.ok(presenceService.myPresence(seanceId));
    }
}
