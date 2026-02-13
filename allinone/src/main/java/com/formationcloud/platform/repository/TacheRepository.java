package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.StatutTache;
import com.formationcloud.platform.model.Tache;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {

	List<Tache> findByStagiaire(Utilisateur stagiaire);

	List<Tache> findByStagiaireAndStatut(Utilisateur stagiaire, StatutTache statut);

	List<Tache> findByStatut(StatutTache statut);

	@Query("SELECT t FROM Tache t WHERE t.stagiaire.id = :stagiaireId AND t.dateFin < :date AND t.statut != 'TERMINEE'")
	List<Tache> findTachesEnRetardByStagiaire(Long stagiaireId, LocalDate date);

	@Query("SELECT COUNT(t) FROM Tache t WHERE t.stagiaire.id = :stagiaireId AND t.statut = 'TERMINEE'")
	long countTachesTermineesByStagiaire(Long stagiaireId);

	@Query("SELECT COUNT(t) FROM Tache t WHERE t.statut = :statut")
	long countByStatut(StatutTache statut);
}
