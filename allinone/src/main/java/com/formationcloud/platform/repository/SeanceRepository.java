package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeanceRepository extends JpaRepository<Seance, Long> {
    List<Seance> findByFormation_IdOrderByDateDebutAsc(Long formationId);

    List<Seance> findByFormation_Formateur_IdAndDateDebutBetweenOrderByDateDebutAsc(Long formateurId, LocalDateTime from, LocalDateTime to);

    List<Seance> findByFormation_IdInAndDateDebutBetweenOrderByDateDebutAsc(List<Long> formationIds, LocalDateTime from, LocalDateTime to);
}
