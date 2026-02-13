package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.LoginRequest;
import com.formationcloud.platform.dto.LoginResponse;
import com.formationcloud.platform.dto.RegisterRequest;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.repository.UtilisateurRepository;
import com.formationcloud.platform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

	private final UtilisateurRepository utilisateurRepository;
	private final PasswordEncoder passwordEncoder;
	private final UtilisateurService utilisateurService;
	private final JwtUtil jwtUtil;

	public LoginResponse login(LoginRequest request) {

		Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

		if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasse())) {
			throw new BadCredentialsException("Email ou mot de passe incorrect");
		}

		if (Boolean.FALSE.equals(utilisateur.getActif())) {
			throw new BadCredentialsException("Votre compte est désactivé");
		}


		// Flux "Demande + validation": certains comptes (FORMATEUR/ADMIN) sont en attente
		if (Boolean.FALSE.equals(utilisateur.getStatutValidation())) {
			throw new BadCredentialsException("Votre compte est en attente de validation par un administrateur");
		}

		String token = jwtUtil.generateToken(utilisateur.getEmail(), utilisateur.getRole().name());

		log.info("Connexion réussie pour: {} ({})", utilisateur.getEmail(), utilisateur.getRole());

		return new LoginResponse(token, utilisateur.getId(), utilisateur.getEmail(), utilisateur.getNom(),
				utilisateur.getPrenom(), utilisateur.getRole(), utilisateur.getStatutValidation());
	}

	public Utilisateur register(RegisterRequest request) {
		// Inscription publique (portail):
		// - STAGIAIRE : accès direct
		// - FORMATEUR / ADMIN : demande d'accès (validation par ADMIN)
		return utilisateurService.createDemandeAcces(request);
	}
}
