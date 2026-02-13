# Hotfix v24

- frontend-react/src/pages/Activites.jsx: markAllRead uses PUT /api/notifications/utilisateur/{id}/lire-tout
- frontend-react/src/pages/Login.jsx: removed ADMIN from public signup roles; clarified message
- UtilisateurService.createDemandeAcces: blocks ADMIN role in public registration
- NotificationDTO: added titre, lien, utilisateurId/nom/prenom
- NotificationController: added titre/lien/user info in DTO mapping
