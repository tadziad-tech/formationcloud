package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Presence;
import com.formationcloud.platform.model.StatutPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<Presence, Long> {
    List<Presence> findBySeance_IdOrderByStagiaire_PrenomAscStagiaire_NomAsc(Long seanceId);
    Optional<Presence> findBySeance_IdAndStagiaire_Id(Long seanceId, Long stagiaireId);

    List<Presence> findBySeance_Formation_Id(Long formationId);

    @Query("SELECT DISTINCT p.seance.id FROM Presence p WHERE p.statut = :statut AND p.seance.dateFin BETWEEN :from AND :to AND p.seance.formation.formateur.id = :formateurId")
    List<Long> findSeanceIdsWithStatutByFormateurAndDateFinBetween(@Param("statut") StatutPresence statut, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("formateurId") Long formateurId);

    @Query("SELECT DISTINCT p.seance.id FROM Presence p WHERE p.statut = :statut AND p.seance.dateFin < :before AND p.seance.formation.formateur.id = :formateurId")
    List<Long> findSeanceIdsWithStatutByFormateurAndDateFinBefore(@Param("statut") StatutPresence statut, @Param("before") LocalDateTime before, @Param("formateurId") Long formateurId);
}
