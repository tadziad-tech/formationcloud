package com.formationcloud.platform.dto;

import com.formationcloud.platform.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

	private String token;
	private Long id;
	private String email;
	private String nom;
	private String prenom;
	private Role role;
	private Boolean statutValidation;
}
