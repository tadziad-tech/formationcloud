# Tests manuels – Autorisation formation-level (403)

Vérifier qu’un **stagiaire non inscrit** (ou inscrit avec statut EN_ATTENTE/REFUSEE/ABANDONNEE) reçoit **403 Forbidden** sur les endpoints de données sensibles d’une formation à laquelle il n’a pas accès.

## Prérequis

- Backend démarré (ex. `mvn spring-boot:run`).
- Compte **stagiaire** (ex. `stagiaire@test.com`) **non inscrit** à une formation donnée, ou inscrit avec statut EN_ATTENTE/REFUSEE/ABANDONNEE.
- IDs connus : `formationId`, `seanceId`, `tpId` d’une formation existante.

## Scénario : stagiaire non inscrit → 403

1. Se connecter en tant que stagiaire (récupérer le token JWT / cookie de session selon votre auth).
2. Appeler les endpoints suivants avec un `formationId` / `seanceId` / `tpId` d’une formation à laquelle ce stagiaire n’est **pas** inscrit (ou pas confirmé).

### Endpoints à tester (attendu : 403)

| Méthode | URL | Description |
|--------|-----|-------------|
| GET | `/api/formations/{formationId}/seances` | Liste des séances |
| GET | `/api/formations/{formationId}/tp-ressources` | Liste des TP/ressources |
| GET | `/api/formations/{formationId}/tp-ressources/type/{type}` | Liste des TP par type |
| GET | `/api/inscriptions/formation/{formationId}` | Liste des inscriptions de la formation |
| GET | `/api/formations/{formationId}/progression/me` | Ma progression |
| GET | `/api/seances/{seanceId}` | Détail d’une séance (formation déduite) |
| GET | `/api/seances/{seanceId}/presences` | Présences d’une séance |
| GET | `/api/seances/{seanceId}/presences/me` | Ma présence pour la séance |
| GET | `/api/tp-ressources/{tpId}` | Détail d’un TP |
| GET | `/api/tp-ressources/{tpId}/fichier` | Téléchargement fichier TP |
| POST | `/api/tp-ressources/{tpId}/soumissions` | Soumettre un TP (body selon API) |
| POST | `/api/tp-ressources/{tpId}/soumissions/upload` | Soumettre un TP avec fichier |

Exemple (remplacer `BASE_URL`, `formationId`, token) :

```bash
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer TOKEN" "http://localhost:8080/api/formations/1/seances"
# Attendu : 403
```

3. **Résultat attendu** : réponse HTTP **403 Forbidden** (et pas 200 avec des données).

## Récapitulatif des règles

- **ADMIN** : accès total.
- **FORMATEUR** : accès uniquement si `formation.formateur.id == currentUser.id`.
- **STAGIAIRE** : accès uniquement si inscription à cette formation avec statut **CONFIRMEE**, **EN_COURS** ou **TERMINEE** (pas EN_ATTENTE/DEMANDEE/REFUSEE/ABANDONNEE).
