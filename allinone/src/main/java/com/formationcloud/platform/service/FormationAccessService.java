package com.formationcloud.platform.service;

import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Seance;
import com.formationcloud.platform.model.StatutInscription;
import com.formationcloud.platform.model.TpRessource;
import com.formationcloud.platform.repository.FormationRepository;
import com.formationcloud.platform.repository.InscriptionRepository;
import com.formationcloud.platform.repository.SeanceRepository;
import com.formationcloud.platform.repository.TpRessourceRepository;
import com.formationcloud.platform.security.SecurityUtils;
import com.formationcloud.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Formation-level authorization: who can access sensitive formation data.
 * - ADMIN: full access.
 * - FORMATEUR: only if formation.formateur.id == currentUser.id.
 * - STAGIAIRE: only if enrolled with statut CONFIRMEE/EN_COURS/TERMINEE.
 */
@Service
@RequiredArgsConstructor
public class FormationAccessService {

    private static final List<StatutInscription> CONFIRMED_STATUTS = List.of(
            StatutInscription.CONFIRMEE,
            StatutInscription.EN_COURS,
            StatutInscription.TERMINEE
    );

    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final SeanceRepository seanceRepository;
    private final TpRessourceRepository tpRessourceRepository;

    /**
     * ADMIN: OK. FORMATEUR: OK only if assigned to this formation. Otherwise 403.
     */
    public void assertAdminOrAssignedFormateur(Long formationId) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) {
            throw new AccessDeniedException("Non authentifié");
        }
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", "id", formationId));
        if (SecurityUtils.isFormateur()
                && formation.getFormateur() != null
                && formation.getFormateur().getId() != null
                && formation.getFormateur().getId().equals(u.getId())) {
            return;
        }
        throw new AccessDeniedException("Accès interdit");
    }

    /**
     * ADMIN: OK. STAGIAIRE: OK only if inscription exists with CONFIRMEE/EN_COURS/TERMINEE. Otherwise 403.
     */
    public void assertAdminOrEnrolledStagiaire(Long formationId) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) {
            throw new AccessDeniedException("Non authentifié");
        }
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isFormateur()) {
            Formation formation = formationRepository.findById(formationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Formation", "id", formationId));
            if (formation.getFormateur() != null && formation.getFormateur().getId() != null
                    && formation.getFormateur().getId().equals(u.getId())) {
                return;
            }
            throw new AccessDeniedException("Accès interdit");
        }
        // STAGIAIRE
        boolean enrolled = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
                u.getId(), formationId, CONFIRMED_STATUTS);
        if (!enrolled) {
            throw new AccessDeniedException("Accès interdit");
        }
    }

    /**
     * ADMIN: OK. FORMATEUR: OK if assigned. STAGIAIRE: OK if enrolled (CONFIRMEE/EN_COURS/TERMINEE). Otherwise 403.
     */
    public void assertCanAccessFormationData(Long formationId) {
        UserPrincipal u = SecurityUtils.currentUser();
        if (u == null) {
            throw new AccessDeniedException("Non authentifié");
        }
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", "id", formationId));
        if (SecurityUtils.isFormateur()
                && formation.getFormateur() != null
                && formation.getFormateur().getId() != null
                && formation.getFormateur().getId().equals(u.getId())) {
            return;
        }
        if (SecurityUtils.isStagiaire()) {
            boolean enrolled = inscriptionRepository.existsByStagiaire_IdAndFormation_IdAndStatutIn(
                    u.getId(), formationId, CONFIRMED_STATUTS);
            if (enrolled) {
                return;
            }
        }
        throw new AccessDeniedException("Accès interdit");
    }

    /**
     * Resolves formation from seance, then asserts canAccessFormationData.
     */
    public void assertCanAccessFormationDataBySeanceId(Long seanceId) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", seanceId));
        Formation formation = seance.getFormation();
        if (formation == null || formation.getId() == null) {
            throw new AccessDeniedException("Accès interdit");
        }
        assertCanAccessFormationData(formation.getId());
    }

    /**
     * Resolves formation from seance, then asserts admin or assigned formateur.
     */
    public void assertAdminOrAssignedFormateurBySeanceId(Long seanceId) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", seanceId));
        Formation formation = seance.getFormation();
        if (formation == null || formation.getId() == null) {
            throw new AccessDeniedException("Accès interdit");
        }
        assertAdminOrAssignedFormateur(formation.getId());
    }

    /**
     * Resolves formation from TP, then asserts canAccessFormationData.
     */
    public void assertCanAccessFormationDataByTpId(Long tpId) {
        TpRessource tp = tpRessourceRepository.findById(tpId)
                .orElseThrow(() -> new ResourceNotFoundException("TP/Ressource", "id", tpId));
        Formation formation = tp.getFormation();
        if (formation == null || formation.getId() == null) {
            throw new AccessDeniedException("Accès interdit");
        }
        assertCanAccessFormationData(formation.getId());
    }

    /**
     * Resolves formation from TP, then asserts admin or assigned formateur.
     */
    public void assertAdminOrAssignedFormateurByTpId(Long tpId) {
        TpRessource tp = tpRessourceRepository.findById(tpId)
                .orElseThrow(() -> new ResourceNotFoundException("TP/Ressource", "id", tpId));
        Formation formation = tp.getFormation();
        if (formation == null || formation.getId() == null) {
            throw new AccessDeniedException("Accès interdit");
        }
        assertAdminOrAssignedFormateur(formation.getId());
    }

    /**
     * Resolves formation from TP, then asserts admin or enrolled stagiaire (CONFIRMEE/EN_COURS/TERMINEE).
     */
    public void assertAdminOrEnrolledStagiaireByTpId(Long tpId) {
        TpRessource tp = tpRessourceRepository.findById(tpId)
                .orElseThrow(() -> new ResourceNotFoundException("TP/Ressource", "id", tpId));
        Formation formation = tp.getFormation();
        if (formation == null || formation.getId() == null) {
            throw new AccessDeniedException("Accès interdit");
        }
        assertAdminOrEnrolledStagiaire(formation.getId());
    }
}
