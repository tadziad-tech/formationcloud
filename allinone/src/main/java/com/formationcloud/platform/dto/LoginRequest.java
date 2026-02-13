package com.formationcloud.platform.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class LoginRequest {

	@JsonAlias({ "email", "username" })
	private String email;

	@JsonAlias({ "motDePasse", "password" })
	private String motDePasse;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMotDePasse() {
		return motDePasse;
	}

	public void setMotDePasse(String motDePasse) {
		this.motDePasse = motDePasse;
	}
}
