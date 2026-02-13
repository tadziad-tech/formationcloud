package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Evaluation;
import com.formationcloud.platform.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

	List<Evaluation> findByFormation(Formation formation);

	List<Evaluation> findByFormationId(Long formationId);

	List<Evaluation> findByDateEvaluationBetween(LocalDate debut, LocalDate fin);

	List<Evaluation> findByFormationFormateurId(Long formateurId);

	List<Evaluation> findByFormationIdIn(List<Long> formationIds);

	List<Evaluation> findByParentEvaluation_Id(Long parentId);

	// Dashboard
	List<Evaluation> findTop8ByOrderByDateCreationDesc();

	long countByDateEvaluationBefore(LocalDate date);
	long countByDateEvaluationGreaterThanEqual(LocalDate date);
}
