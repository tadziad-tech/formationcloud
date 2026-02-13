package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Inscription;
import com.formationcloud.platform.model.StatutInscription;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

	List<Inscription> findByStagiaire(Utilisateur stagiaire);

	List<Inscription> findByFormation(Formation formation);

	List<Inscription> findByFormationAndStatut(Formation formation, StatutInscription statut);

	Optional<Inscription> findByStagiaireAndFormation(Utilisateur stagiaire, Formation formation);

	Optional<Inscription> findByFormation_IdAndStagiaire_Id(Long formationId, Long stagiaireId);

	boolean existsByStagiaireAndFormation(Utilisateur stagiaire, Formation formation);

	@Query("SELECT i FROM Inscription i WHERE i.formation.formateur.id = :formateurId AND i.statut = 'EN_ATTENTE'")
	List<Inscription> findInscriptionsEnAttenteByFormateur(Long formateurId);

	@Query("SELECT i FROM Inscription i WHERE i.stagiaire.id = :stagiaireId AND i.statut IN ('CONFIRMEE', 'EN_COURS')")
	List<Inscription> findInscriptionsActivesByStagiaire(Long stagiaireId);

	@Query("SELECT COUNT(i) FROM Inscription i WHERE i.statut = :statut")
	long countByStatut(StatutInscription statut);

	// Dashboard
	List<Inscription> findTop8ByOrderByDateInscriptionDesc();

	List<Inscription> findTop10ByStatutOrderByDateInscriptionDesc(StatutInscription statut);

	long countByDateInscriptionGreaterThanEqual(LocalDateTime from);

	@Query("SELECT i.statut, COUNT(i) FROM Inscription i GROUP BY i.statut")
	List<Object[]> countAllByStatut();

	@Query("SELECT FUNCTION('DATE', i.dateInscription), COUNT(i) FROM Inscription i WHERE i.dateInscription >= :from GROUP BY FUNCTION('DATE', i.dateInscription) ORDER BY FUNCTION('DATE', i.dateInscription)")
	List<Object[]> countByDaySince(@Param("from") LocalDateTime from);


	@Query("SELECT DISTINCT i.formation.id FROM Inscription i WHERE i.stagiaire.id = :stagiaireId AND i.statut IN ('CONFIRMEE','EN_COURS','TERMINEE')")
	List<Long> findActiveFormationIdsByStagiaire(@Param("stagiaireId") Long stagiaireId);

	@Query("SELECT COUNT(i) FROM Inscription i WHERE i.formation.id = :formationId AND i.statut IN ('CONFIRMEE','EN_COURS','TERMINEE')")
	long countParticipantsForFormation(Long formationId);

	@Query("SELECT COUNT(i) FROM Inscription i WHERE i.formation.formateur.id = :formateurId")
	long countByFormateurId(@Param("formateurId") Long formateurId);

	@Query("SELECT FUNCTION('DATE', i.dateInscription), COUNT(i) FROM Inscription i WHERE i.formation.formateur.id = :formateurId AND i.dateInscription >= :from GROUP BY FUNCTION('DATE', i.dateInscription) ORDER BY FUNCTION('DATE', i.dateInscription)")
	List<Object[]> countByDaySinceForFormateur(@Param("formateurId") Long formateurId, @Param("from") LocalDateTime from);

	long countByFormation_Id(Long formationId);

    // Séances / Présences
    boolean existsByStagiaire_IdAndFormation_IdAndStatutIn(Long stagiaireId, Long formationId, java.util.List<StatutInscription> statuts);

    java.util.List<Inscription> findByFormation_IdAndStatutIn(Long formationId, java.util.List<StatutInscription> statuts);

    List<Inscription> findByStatutInOrderByDateInscriptionDesc(List<StatutInscription> statuts, Pageable pageable);

}
