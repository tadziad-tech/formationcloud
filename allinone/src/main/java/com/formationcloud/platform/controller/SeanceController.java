package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.SeanceDTO;
import com.formationcloud.platform.dto.SeanceRequest;
import com.formationcloud.platform.service.FormationAccessService;
import com.formationcloud.platform.service.SeanceService;
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
public class SeanceController {

    private final SeanceService seanceService;
    private final FormationAccessService formationAccessService;

    @GetMapping("/formations/{formationId}/seances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeanceDTO>> list(@PathVariable Long formationId) {
        formationAccessService.assertCanAccessFormationData(formationId);
        return ResponseEntity.ok(seanceService.listByFormation(formationId));
    }

    @PostMapping("/formations/{formationId}/seances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeanceDTO> create(@PathVariable Long formationId, @Valid @RequestBody SeanceRequest req) {
        formationAccessService.assertAdminOrAssignedFormateur(formationId);
        return ResponseEntity.ok(seanceService.create(formationId, req));
    }

    @GetMapping("/seances/{seanceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeanceDTO> get(@PathVariable Long seanceId) {
        formationAccessService.assertCanAccessFormationDataBySeanceId(seanceId);
        return ResponseEntity.ok(seanceService.get(seanceId));
    }

    @PutMapping("/seances/{seanceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeanceDTO> update(@PathVariable Long seanceId, @Valid @RequestBody SeanceRequest req) {
        formationAccessService.assertAdminOrAssignedFormateurBySeanceId(seanceId);
        return ResponseEntity.ok(seanceService.update(seanceId, req));
    }

    @DeleteMapping("/seances/{seanceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long seanceId) {
        formationAccessService.assertAdminOrAssignedFormateurBySeanceId(seanceId);
        seanceService.delete(seanceId);
        return ResponseEntity.noContent().build();
    }
}
