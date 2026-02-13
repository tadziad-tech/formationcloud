package com.formationcloud.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                // Public: auth + React SPA
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/app/**").permitAll()
                // Seuls les avatars / profils sont publics
                .requestMatchers("/uploads/profile/**").permitAll()
                .requestMatchers("/", "/app", "/app/", "/app/**", "/favicon.ico").permitAll()

                // Public lecture formations (page d'accueil)
                .requestMatchers(HttpMethod.GET, "/api/formations/**").permitAll()

                // Public: vérification certificat via QR
                .requestMatchers(HttpMethod.GET, "/api/certificats/verify/**").permitAll()

                // Tout le reste de l'API nécessite un token
                .requestMatchers("/api/**").authenticated()

                // Non-API (assets) => public
                .anyRequest().permitAll()
        );

        http.exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
