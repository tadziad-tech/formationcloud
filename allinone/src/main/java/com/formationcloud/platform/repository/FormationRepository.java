package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.StatutFormation;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {

	List<Formation> findByStatut(StatutFormation statut);

	List<Formation> findByFormateur(Utilisateur formateur);

	// Dashboard / utilitaire
	List<Formation> findByFormateur_Id(Long formateurId);

	List<Formation> findByFormateurAndStatut(Utilisateur formateur, StatutFormation statut);

	@Query("SELECT f FROM Formation f WHERE f.statut = 'ACTIVE' AND f.dateDebut > :date")
	List<Formation> findFormationsAVenir(LocalDate date);

	@Query("SELECT f FROM Formation f WHERE f.statut = 'ACTIVE' AND f.dateDebut <= :date AND f.dateFin >= :date")
	List<Formation> findFormationsEnCours(LocalDate date);

	@Query("SELECT f FROM Formation f WHERE f.categorie.id = :categorieId AND f.statut = 'ACTIVE'")
	List<Formation> findByCategorieIdAndActive(Long categorieId);

	@Query("SELECT f FROM Formation f WHERE f.prerequis.id = :prerequisId")
	List<Formation> findFormationsNecessitantPrerequis(Long prerequisId);

	@Query("SELECT COUNT(f) FROM Formation f WHERE f.statut = :statut")
	long countByStatut(StatutFormation statut);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Formation f SET f.statut = :newStatut WHERE f.statut = :oldStatut AND f.dateFin < :today")
	int markFinished(@Param("oldStatut") StatutFormation oldStatut, @Param("newStatut") StatutFormation newStatut, @Param("today") LocalDate today);

	// Dashboard breakdowns
	@Query("SELECT f.type, COUNT(f) FROM Formation f WHERE f.statut = 'ACTIVE' GROUP BY f.type")
	List<Object[]> countActiveByType();

	@Query("SELECT c.nom, COUNT(f) FROM Formation f JOIN f.categorie c WHERE f.statut = 'ACTIVE' GROUP BY c.nom ORDER BY COUNT(f) DESC")
	List<Object[]> countActiveByCategorieName();

	@Query("SELECT COUNT(f) FROM Formation f WHERE f.statut = 'ACTIVE' AND f.categorie IS NULL")
	long countActiveWithoutCategorie();

	// Dashboard
	List<Formation> findTop5ByOrderByDateCreationDesc();
}
