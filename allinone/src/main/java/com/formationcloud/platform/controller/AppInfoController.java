package com.formationcloud.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Fournit un identifiant d'instance qui change à chaque redémarrage.
 * Le front l'utilise pour forcer une déconnexion quand le backend est relancé.
 */
@RestController
@RequestMapping("/api/app")
public class AppInfoController {

    private final String instanceId = UUID.randomUUID().toString();

    @GetMapping("/instance")
    public Map<String, String> instance() {
        return Map.of("instanceId", instanceId);
    }
}
