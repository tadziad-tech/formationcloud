package com.formationcloud.platform.security;

import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur u = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));

        String role = u.getRole() != null ? u.getRole().name() : "STAGIAIRE";
        boolean enabled = Boolean.TRUE.equals(u.getActif());

        return new UserPrincipal(u.getId(), u.getEmail(), u.getMotDePasse(), role, enabled);
    }
}
