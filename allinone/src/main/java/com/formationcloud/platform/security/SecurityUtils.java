package com.formationcloud.platform.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        if (auth.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) auth.getPrincipal();
        }
        return null;
    }

    public static boolean isAdmin() {
        UserPrincipal u = currentUser();
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    public static boolean isFormateur() {
        UserPrincipal u = currentUser();
        return u != null && "FORMATEUR".equalsIgnoreCase(u.getRole());
    }

    public static boolean isStagiaire() {
        UserPrincipal u = currentUser();
        return u != null && "STAGIAIRE".equalsIgnoreCase(u.getRole());
    }

    public static void assertAdminOrSelf(Long userId) {
        UserPrincipal u = currentUser();
        if (u == null) {
            throw new AccessDeniedException("Non authentifié");
        }
        if (isAdmin()) return;
        if (userId == null || !userId.equals(u.getId())) {
            throw new AccessDeniedException("Accès interdit");
        }
    }

    public static void assertAdminOrSelfByOwnerId(Long ownerId) {
        assertAdminOrSelf(ownerId);
    }
}
