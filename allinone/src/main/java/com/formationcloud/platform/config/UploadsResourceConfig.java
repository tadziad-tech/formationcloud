package com.formationcloud.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expose uniquement le sous-dossier profile pour les photos de profil.
 * Les autres contenus (ex: fichiers TP) ne sont pas exposés ici — téléchargement via /api uniquement.
 */
@Configuration
public class UploadsResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        // Uniquement /uploads/profile/** (avatars). Fichiers TP servis via endpoints /api.
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(location + "profile/")
                .setCachePeriod(0);
    }
}
