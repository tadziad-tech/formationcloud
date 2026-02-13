package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Certificat;
import com.formationcloud.platform.model.Formation;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificatRepository extends JpaRepository<Certificat, Long> {

	List<Certificat> findByStagiaire(Utilisateur stagiaire);

	List<Certificat> findByFormation(Formation formation);

	Optional<Certificat> findByStagiaireAndFormation(Utilisateur stagiaire, Formation formation);

	boolean existsByStagiaireAndFormation(Utilisateur stagiaire, Formation formation);

	Optional<Certificat> findByStagiaireIdAndFormationId(Long stagiaireId, Long formationId);

	boolean existsByStagiaireIdAndFormationId(Long stagiaireId, Long formationId);

	Optional<Certificat> findByNumeroUnique(String numeroUnique);

	@Query("SELECT COUNT(c) FROM Certificat c WHERE c.stagiaire.id = :stagiaireId")
	long countByStagiaireId(Long stagiaireId);

	@Query("SELECT COUNT(c) FROM Certificat c WHERE c.formation.formateur.id = :formateurId")
	long countByFormateurId(Long formateurId);

	@Query("SELECT c FROM Certificat c WHERE c.formation.formateur.id = :formateurId")
	List<Certificat> findByFormateurId(Long formateurId);

	long countByDateObtentionGreaterThanEqual(LocalDate from);

	// Dashboard
	List<Certificat> findTop8ByOrderByDateObtentionDesc();

	@Modifying
	@Query("UPDATE Certificat c SET c.statut = com.formationcloud.platform.model.CertificatStatut.REVOQUE, " +
			"c.dateRevocation = CURRENT_DATE " +
			"WHERE c.formation.id = :formationId AND c.statut <> com.formationcloud.platform.model.CertificatStatut.REVOQUE")
	int revoquerTousPourFormation(@Param("formationId") Long formationId);
}
