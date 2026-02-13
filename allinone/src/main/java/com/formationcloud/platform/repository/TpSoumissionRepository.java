package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.StatutTpSoumission;
import com.formationcloud.platform.model.TpSoumission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TpSoumissionRepository extends JpaRepository<TpSoumission, Long> {
	List<TpSoumission> findByTp_IdOrderByDateSoumissionDesc(Long tpId);

	List<TpSoumission> findByStagiaire_IdOrderByDateSoumissionDesc(Long stagiaireId);

	Optional<TpSoumission> findByTp_IdAndStagiaire_Id(Long tpId, Long stagiaireId);

	List<TpSoumission> findByTp_IdAndStatutOrderByDateSoumissionDesc(Long tpId, StatutTpSoumission statut);

	boolean existsByTp_IdAndStagiaire_Id(Long tpId, Long stagiaireId);

	@Query("SELECT s FROM TpSoumission s WHERE s.statut = :statut AND s.tp.formation.formateur.id = :formateurId ORDER BY s.dateSoumission DESC")
	List<TpSoumission> findSoumisByFormateur(@Param("formateurId") Long formateurId, @Param("statut") StatutTpSoumission statut, org.springframework.data.domain.Pageable pageable);
}
