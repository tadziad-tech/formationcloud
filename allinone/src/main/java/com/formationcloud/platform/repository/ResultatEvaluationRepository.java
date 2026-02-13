package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Evaluation;
import com.formationcloud.platform.model.ResultatEvaluation;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultatEvaluationRepository extends JpaRepository<ResultatEvaluation, Long> {

	List<ResultatEvaluation> findByEvaluation(Evaluation evaluation);

	List<ResultatEvaluation> findByStagiaire(Utilisateur stagiaire);

	Optional<ResultatEvaluation> findByEvaluationAndStagiaire(Evaluation evaluation, Utilisateur stagiaire);

	boolean existsByEvaluationAndStagiaire(Evaluation evaluation, Utilisateur stagiaire);

	// Progression
	List<ResultatEvaluation> findByEvaluation_Formation_Id(Long formationId);


	@Query("SELECT r FROM ResultatEvaluation r WHERE r.stagiaire.id = :stagiaireId AND r.reussi = true")
	List<ResultatEvaluation> findResultatsReussisByStagiaire(Long stagiaireId);

	@Query("SELECT AVG(r.note) FROM ResultatEvaluation r WHERE r.evaluation.formation.id = :formationId")
	Double findMoyenneNotesByFormation(Long formationId);

	// Dashboard
	long countByEvaluation_Id(Long evaluationId);

	@Query("SELECT COUNT(r) FROM ResultatEvaluation r WHERE r.evaluation.id = :evaluationId AND r.reussi = true")
	long countReussisByEvaluationId(Long evaluationId);
}
