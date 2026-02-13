package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.TpRessource;
import com.formationcloud.platform.model.TypeTpRessource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TpRessourceRepository extends JpaRepository<TpRessource, Long> {
	List<TpRessource> findByFormation_IdOrderByDateCreationDesc(Long formationId);

	List<TpRessource> findByFormation_IdAndTypeOrderByDateCreationDesc(Long formationId, TypeTpRessource type);

	List<TpRessource> findByFormation_IdInAndTypeAndDateLimiteBetweenOrderByDateLimiteAsc(List<Long> formationIds, TypeTpRessource type, LocalDateTime from, LocalDateTime to);

	List<TpRessource> findByFormation_IdInAndTypeAndDateLimiteBeforeOrderByDateLimiteDesc(List<Long> formationIds, TypeTpRessource type, LocalDateTime before);
}
