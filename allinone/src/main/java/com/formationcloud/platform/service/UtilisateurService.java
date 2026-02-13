package com.formationcloud.platform.service;

import com.formationcloud.platform.dto.RegisterRequest;
import com.formationcloud.platform.dto.ProfileUpdateRequest;
import com.formationcloud.platform.dto.UtilisateurManageRequest;
import com.formationcloud.platform.exception.BadRequestException;
import com.formationcloud.platform.exception.ResourceNotFoundException;
import com.formationcloud.platform.model.Role;
import com.formationcloud.platform.model.Utilisateur;
import com.formationcloud.platform.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UtilisateurService {

	private final UtilisateurRepository utilisateurRepository;
	private final PasswordEncoder passwordEncoder;
	private final NotificationService notificationService;

	public List<Utilisateur> findAll() {
		return utilisateurRepository.findAll();
	}

	public List<Utilisateur> findVisibleForActor(Long actorId, Role actorRole) {
		if (actorRole == null || actorId == null) {
			throw new AccessDeniedException("Accès interdit");
		}
		if (actorRole == Role.ADMIN) {
			return utilisateurRepository.findAll();
		}
		if (actorRole == Role.FORMATEUR) {
			// Formateur: lui-même + tous les stagiaires
			return utilisateurRepository.findByRoleOrId(Role.STAGIAIRE, actorId);
		}
		if (actorRole == Role.STAGIAIRE) {
			return List.of(findById(actorId));
		}
		throw new AccessDeniedException("Accès interdit");
	}

	public void assertCanViewUser(Long actorId, Role actorRole, Long targetId) {
		if (actorRole == null || actorId == null || targetId == null) {
			throw new AccessDeniedException("Accès interdit");
		}
		if (actorRole == Role.ADMIN) return;
		if (actorRole == Role.FORMATEUR) {
			Utilisateur target = findById(targetId);
			if (actorId.equals(targetId) || target.getRole() == Role.STAGIAIRE) return;
			throw new AccessDeniedException("Accès interdit");
		}
		if (actorRole == Role.STAGIAIRE) {
			if (actorId.equals(targetId)) return;
			throw new AccessDeniedException("Accès interdit");
		}
		throw new AccessDeniedException("Accès interdit");
	}

	public Utilisateur findById(Long id) {
		return utilisateurRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
	}

	public Utilisateur findByEmail(String email) {
		return utilisateurRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
	}

	public List<Utilisateur> findByRole(Role role) {
		return utilisateurRepository.findByRole(role);
	}

	public List<Utilisateur> findFormateursValides() {
		return utilisateurRepository.findFormateursValides();
	}

	public List<Utilisateur> findStagiairesActifs() {
		return utilisateurRepository.findStagiairesActifs();
	}

	public Utilisateur createUtilisateur(RegisterRequest request) {
		if (utilisateurRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Un utilisateur avec cet email existe déjà");
		}

		Utilisateur utilisateur = new Utilisateur();
		utilisateur.setNom(request.getNom());
		utilisateur.setPrenom(request.getPrenom());
		utilisateur.setEmail(request.getEmail());
		utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
		utilisateur.setRole(request.getRole());
		utilisateur.setTypeFormateur(request.getTypeFormateur());
		utilisateur.setTelephone(request.getTelephone());
		utilisateur.setAdresse(request.getAdresse());
		// Photo de profil ajoutée/modifiée depuis la page Profil via upload
		utilisateur.setPhotoProfil(null);

		// Création par un ADMIN (via /api/utilisateurs) => compte directement utilisable.
		// Si l'admin veut une validation manuelle, il peut désactiver/retirer ensuite.
		utilisateur.setStatutValidation(true);

		Utilisateur saved = utilisateurRepository.save(utilisateur);
		log.info("Nouvel utilisateur créé: {} {} ({})", saved.getPrenom(), saved.getNom(), saved.getRole());

		return saved;
	}

	/**
	 * Inscription publique (portail):
	 * - STAGIAIRE : compte activé immédiatement (validation automatique)
	 * - FORMATEUR / ADMIN : compte en attente, à valider par un ADMIN
	 */
	public Utilisateur createDemandeAcces(RegisterRequest request) {
		if (utilisateurRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Un utilisateur avec cet email existe déjà");
		}
		if (request.getRole() == null) {
			throw new BadRequestException("Le rôle est obligatoire");
		}
		// Inscription publique: STAGIAIRE (accès direct) ou FORMATEUR (validation ADMIN). ADMIN interdit.
		if (request.getRole() != Role.STAGIAIRE && request.getRole() != Role.FORMATEUR) {
			throw new BadRequestException("Rôle non autorisé pour l'inscription publique");
		}

		Utilisateur utilisateur = new Utilisateur();
		utilisateur.setNom(request.getNom());
		utilisateur.setPrenom(request.getPrenom());
		utilisateur.setEmail(request.getEmail());
		utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
		utilisateur.setRole(request.getRole());
		utilisateur.setTypeFormateur(request.getTypeFormateur());
		utilisateur.setTelephone(request.getTelephone());
		utilisateur.setAdresse(request.getAdresse());
		// Photo de profil ajoutée/modifiée depuis la page Profil via upload
		utilisateur.setPhotoProfil(null);

		// STAGIAIRE => accès direct, FORMATEUR/ADMIN => PENDING
		utilisateur.setStatutValidation(request.getRole() == Role.STAGIAIRE);
		utilisateur.setActif(true);

		Utilisateur saved = utilisateurRepository.save(utilisateur);
		log.info("Demande d'accès créée: {} {} ({})", saved.getPrenom(), saved.getNom(), saved.getRole());
		return saved;
	}

	public Utilisateur updateUtilisateur(Long id, Utilisateur utilisateurDetails) {
		Utilisateur utilisateur = findById(id);

		utilisateur.setNom(utilisateurDetails.getNom());
		utilisateur.setPrenom(utilisateurDetails.getPrenom());

		// Champs optionnels / administratifs
		if (utilisateurDetails.getEmail() != null && !utilisateurDetails.getEmail().isBlank()) {
			utilisateur.setEmail(utilisateurDetails.getEmail());
		}
		if (utilisateurDetails.getRole() != null) {
			utilisateur.setRole(utilisateurDetails.getRole());
		}
		utilisateur.setTypeFormateur(utilisateurDetails.getTypeFormateur());

		// Contact (si fourni)
		utilisateur.setTelephone(utilisateurDetails.getTelephone());
		utilisateur.setAdresse(utilisateurDetails.getAdresse());

		if (utilisateurDetails.getMotDePasse() != null && !utilisateurDetails.getMotDePasse().isEmpty()) {
			utilisateur.setMotDePasse(passwordEncoder.encode(utilisateurDetails.getMotDePasse()));
		}

		return utilisateurRepository.save(utilisateur);
	}

    /**
     * Mise à jour d'un utilisateur depuis la page "Utilisateurs".
     * Règles:
     * - ADMIN: peut modifier n'importe qui (y compris rôle)
     * - FORMATEUR: peut modifier uniquement un STAGIAIRE ou lui-même (pas de changement de rôle)
     */
    public Utilisateur updateUtilisateurByActor(Long actorId, Role actorRole, Long targetId, UtilisateurManageRequest req) {
        findById(actorId);
        Utilisateur target = findById(targetId);

        if (actorRole == Role.ADMIN) {
            // OK
        } else if (actorRole == Role.FORMATEUR) {
            boolean isSelf = actorId.equals(targetId);
            boolean isStagiaire = target.getRole() == Role.STAGIAIRE;
            if (!isSelf && !isStagiaire) {
                throw new AccessDeniedException("Accès interdit");
            }
        } else {
            throw new AccessDeniedException("Accès interdit");
        }

        target.setNom(req.getNom());
        target.setPrenom(req.getPrenom());
        target.setEmail(req.getEmail());
        target.setTelephone(req.getTelephone());
        target.setAdresse(req.getAdresse());

        // ADMIN: peut changer rôle / typeFormateur
        if (actorRole == Role.ADMIN && req.getRole() != null) {
            target.setRole(req.getRole());
            // TypeFormateur seulement si FORMATEUR
            if (req.getRole() == Role.FORMATEUR) {
                target.setTypeFormateur(req.getTypeFormateur());
            } else {
                target.setTypeFormateur(null);
            }
        }

        // FORMATEUR: ne touche jamais au rôle
        if (actorRole == Role.FORMATEUR) {
            // typeFormateur uniquement si self
            if (actorId.equals(targetId) && target.getRole() == Role.FORMATEUR) {
                // pas obligatoire, mais on l'accepte si fourni
                // (req.getTypeFormateur() ignoré car non exposé au formateur)
            }
        }

        if (req.getMotDePasse() != null && !req.getMotDePasse().isBlank()) {
            target.setMotDePasse(passwordEncoder.encode(req.getMotDePasse()));
        }

        return utilisateurRepository.save(target);
    }

    /**
     * Suppression utilisateur avec règles métier.
     * - ADMIN: peut supprimer n'importe qui (sauf le dernier admin)
     * - FORMATEUR: peut supprimer uniquement un STAGIAIRE ou lui-même
     */
    public void deleteUtilisateurByActor(Long actorId, Role actorRole, Long targetId) {
        Utilisateur target = findById(targetId);

        if (actorRole == Role.ADMIN) {
            // Empêcher de supprimer le dernier admin
            if (target.getRole() == Role.ADMIN && countByRole(Role.ADMIN) <= 1) {
                throw new BadRequestException("Impossible de supprimer le dernier administrateur");
            }
            utilisateurRepository.delete(target);
            return;
        }

        if (actorRole == Role.FORMATEUR) {
            boolean isSelf = actorId.equals(targetId);
            boolean isStagiaire = target.getRole() == Role.STAGIAIRE;
            if (!isSelf && !isStagiaire) {
                throw new AccessDeniedException("Accès interdit");
            }
            utilisateurRepository.delete(target);
            return;
        }

        throw new AccessDeniedException("Accès interdit");
    }

	public void validerUtilisateur(Long id) {
		Utilisateur utilisateur = findById(id);
		// Compatible avec le flux "Demande + validation": un STAGIAIRE peut être en attente.
		utilisateur.setStatutValidation(true);
		utilisateurRepository.save(utilisateur);

		// Envoyer notification
		notificationService.envoyerNotificationValidationCompte(utilisateur);

		log.info("Utilisateur validé: {} {} ({})", utilisateur.getPrenom(), utilisateur.getNom(),
				utilisateur.getRole());
	}

	public void deleteUtilisateur(Long id) {
		Utilisateur utilisateur = findById(id);
		utilisateurRepository.delete(utilisateur);
		log.info("Utilisateur supprimé: {} {}", utilisateur.getPrenom(), utilisateur.getNom());
	}

	public long countByRole(Role role) {
		return utilisateurRepository.countByRole(role);
	}

	/**
	 * Mise à jour du profil par l'utilisateur connecté.
	 * On autorise uniquement les champs personnels (pas de changement de rôle).
	 */
	public Utilisateur updateSelf(Long userId, ProfileUpdateRequest req) {
		Utilisateur u = findById(userId);

		if (req.getNom() != null && !req.getNom().isBlank()) {
			u.setNom(req.getNom().trim());
		}
		if (req.getPrenom() != null && !req.getPrenom().isBlank()) {
			u.setPrenom(req.getPrenom().trim());
		}
		// Contact
		u.setTelephone(req.getTelephone());
		u.setAdresse(req.getAdresse());

		// Changement de mot de passe (optionnel)
		if (req.getNouveauMotDePasse() != null && !req.getNouveauMotDePasse().isBlank()) {
			u.setMotDePasse(passwordEncoder.encode(req.getNouveauMotDePasse()));
		}

		return utilisateurRepository.save(u);
	}

	public Utilisateur updatePhotoProfil(Long userId, String photoPath) {
		Utilisateur u = findById(userId);
		u.setPhotoProfil(photoPath);
		return utilisateurRepository.save(u);
	}

	/**
	 * Gestion utilisateur depuis la page Utilisateurs.
	 * Règles:
	 * - ADMIN : peut modifier tout le monde + changer rôle
	 * - FORMATEUR : peut modifier/supprimer uniquement un STAGIAIRE ou lui-même
	 */
	public Utilisateur updateUtilisateurManaged(Long actorId, Role actorRole, Long targetId, UtilisateurManageRequest req) {
		Utilisateur target = findById(targetId);

		if (actorRole == null) {
			throw new AccessDeniedException("Accès interdit");
		}

		if (actorRole == Role.ADMIN) {
			// OK
		} else if (actorRole == Role.FORMATEUR) {
			boolean isSelf = actorId != null && actorId.equals(targetId);
			boolean isStagiaireTarget = target.getRole() == Role.STAGIAIRE;
			if (!isSelf && !isStagiaireTarget) {
				throw new AccessDeniedException("Vous pouvez modifier uniquement votre profil ou un stagiaire");
			}
		} else if (actorRole == Role.STAGIAIRE) {
			boolean isSelf = actorId != null && actorId.equals(targetId);
			if (!isSelf) {
				throw new AccessDeniedException("Accès interdit");
			}
		} else {
			throw new AccessDeniedException("Accès interdit");
		}

		// Vérifier email unique si changement
		if (req.getEmail() != null && !req.getEmail().isBlank()) {
			String newEmail = req.getEmail().trim();
			if (!newEmail.equalsIgnoreCase(target.getEmail())) {
				if (utilisateurRepository.existsByEmailAndIdNot(newEmail, target.getId())) {
					throw new BadRequestException("Cet email est déjà utilisé par un autre utilisateur");
				}
			}
		}

		// Admin : OK sur tout
		// Formateur : self + stagiaire
		// Stagiaire : self
		target.setNom(req.getNom());
		target.setPrenom(req.getPrenom());
		target.setEmail(req.getEmail().trim());
		target.setTelephone(req.getTelephone());
		target.setAdresse(req.getAdresse());

		// Changement rôle uniquement ADMIN
		if (actorRole == Role.ADMIN && req.getRole() != null) {
			target.setRole(req.getRole());
			// Type formateur uniquement si role FORMATEUR
			if (req.getRole() == Role.FORMATEUR) {
				target.setTypeFormateur(req.getTypeFormateur());
			} else {
				target.setTypeFormateur(null);
			}
		}

		// Mot de passe optionnel
		if (req.getMotDePasse() != null && !req.getMotDePasse().isBlank()) {
			target.setMotDePasse(passwordEncoder.encode(req.getMotDePasse()));
		}

		return utilisateurRepository.save(target);
	}

	public void deleteUtilisateurManaged(Long actorId, Role actorRole, Long targetId) {
		Utilisateur target = findById(targetId);
		if (actorRole == null) {
			throw new AccessDeniedException("Accès interdit");
		}
		if (actorRole == Role.ADMIN) {
			if (actorId != null && actorId.equals(targetId)) {
				throw new BadRequestException("Impossible de supprimer votre propre compte");
			}
			// Empêcher la suppression du dernier ADMIN (évite de casser l'appli)
			if (target.getRole() == Role.ADMIN) {
				long admins = utilisateurRepository.countByRole(Role.ADMIN);
				if (admins <= 1) {
					throw new BadRequestException("Impossible de supprimer le dernier compte ADMIN");
				}
			}
			utilisateurRepository.delete(target);
			log.info("Utilisateur supprimé par {} (role={}): {} {}", actorId, actorRole, target.getPrenom(), target.getNom());
			return;
		}
		if (actorRole == Role.FORMATEUR) {
			boolean isSelf = actorId != null && actorId.equals(targetId);
			boolean isStagiaireTarget = target.getRole() == Role.STAGIAIRE;
			if (!isSelf && !isStagiaireTarget) {
				throw new AccessDeniedException("Accès interdit");
			}
			utilisateurRepository.delete(target);
			log.info("Utilisateur supprimé par {} (role={}): {} {}", actorId, actorRole, target.getPrenom(), target.getNom());
			return;
		}
		// STAGIAIRE: suppression interdite via cette page
		throw new AccessDeniedException("Accès interdit");
	}
}
