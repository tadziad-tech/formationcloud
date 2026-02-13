package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.LoginRequest;
import com.formationcloud.platform.dto.LoginResponse;
import com.formationcloud.platform.dto.RegisterRequest;
import com.formationcloud.platform.dto.UtilisateurDTO;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		log.info("Tentative de connexion pour: {}", request.getEmail());
		LoginResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/register")
	public ResponseEntity<UtilisateurDTO> register(@Valid @RequestBody RegisterRequest request) {
		log.info("Tentative d'inscription pour: {}", request.getEmail());
		Utilisateur utilisateur = authService.register(request);
		return ResponseEntity.ok(toDTO(utilisateur));
	}

	private UtilisateurDTO toDTO(Utilisateur u) {
		UtilisateurDTO dto = new UtilisateurDTO();
		dto.setId(u.getId());
		dto.setNom(u.getNom());
		dto.setPrenom(u.getPrenom());
		dto.setEmail(u.getEmail());
		dto.setRole(u.getRole());
		dto.setTypeFormateur(u.getTypeFormateur());
		dto.setValide(u.getStatutValidation());
		dto.setDateCreation(u.getDateCreation());
		return dto;
	}

}