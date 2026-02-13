package com.formationcloud.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

	private final SecretKey key;
	private final long expirationMs;

	public JwtUtil(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
		// IMPORTANT: secret doit faire au moins 32 caractères pour HS256
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(String email, String role) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationMs);

		return Jwts.builder().subject(email).claim("role", role).issuedAt(now).expiration(exp)
				.signWith(key, Jwts.SIG.HS256).compact();
	}

	public String extractEmail(String token) {
		return parse(token).getPayload().getSubject();
	}

	public String extractRole(String token) {
		Object r = parse(token).getPayload().get("role");
		return r == null ? null : r.toString();
	}

	public boolean isTokenValid(String token, String email) {
		try {
			String subject = extractEmail(token);
			return subject != null && subject.equals(email) && !isExpired(token);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isExpired(String token) {
		Date exp = parse(token).getPayload().getExpiration();
		return exp == null || exp.before(new Date());
	}

	private Jws<Claims> parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
	}
}
