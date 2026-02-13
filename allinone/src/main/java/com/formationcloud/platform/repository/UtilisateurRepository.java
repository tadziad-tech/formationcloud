package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

	Optional<Utilisateur> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	List<Utilisateur> findByRole(Role role);

	List<Utilisateur> findByRoleOrId(Role role, Long id);

	List<Utilisateur> findByRoleAndStatutValidation(Role role, Boolean statutValidation);

	@Query("SELECT u FROM Utilisateur u WHERE u.role = 'FORMATEUR' AND u.statutValidation = true")
	List<Utilisateur> findFormateursValides();

	@Query("SELECT u FROM Utilisateur u WHERE u.role = 'STAGIAIRE' AND u.actif = true")
	List<Utilisateur> findStagiairesActifs();

	@Query("SELECT COUNT(u) FROM Utilisateur u WHERE u.role = :role")
	long countByRole(Role role);
}
