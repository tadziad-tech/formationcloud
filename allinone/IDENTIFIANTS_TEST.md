# 🔐 Identifiants de Test - FormationCloud Platform

## Comptes de Test

Tous les utilisateurs utilisent le même mot de passe pour faciliter les tests : **`password`**

### 1. Administrateur
- **Email:** `admin@formationcloud.com`
- **Mot de passe:** `password`
- **Rôle:** ADMIN
- **Accès:** Toutes les fonctionnalités de la plateforme

### 2. Formateur Interne
- **Email:** `formateur@formationcloud.com`
- **Mot de passe:** `password`
- **Rôle:** FORMATEUR (Interne)
- **Accès:** Gestion de ses formations, validation des inscriptions, création d'évaluations

### 3. Formateur Externe
- **Email:** `formateur2@formationcloud.com`
- **Mot de passe:** `password`
- **Rôle:** FORMATEUR (Externe)
- **Accès:** Gestion de ses formations, validation des inscriptions, création d'évaluations

### 4. Stagiaire 1
- **Email:** `stagiaire@formationcloud.com`
- **Mot de passe:** `password`
- **Rôle:** STAGIAIRE
- **Accès:** Inscription aux formations, passage des évaluations, consultation des certificats

### 5. Stagiaire 2
- **Email:** `stagiaire2@formationcloud.com`
- **Mot de passe:** `password`
- **Rôle:** STAGIAIRE
- **Accès:** Inscription aux formations, passage des évaluations, consultation des certificats

## Comment se connecter

1. Démarrez l'application : `mvn spring-boot:run`
2. Ouvrez votre navigateur : `http://localhost:8080`
3. Cliquez sur "Se connecter"
4. Entrez l'email et le mot de passe
5. Vous serez redirigé vers le dashboard correspondant à votre rôle

## Données de Test Disponibles

### Formations
- Java Débutant (Présentielle)
- Java Avancé (Présentielle - Prérequis: Java Débutant)
- Python Data Science (À distance)
- DevOps Essentials (Présentielle)

### Catégories
- Java
- Python
- DevOps
- UML

### Inscriptions
- Marie Martin (stagiaire@formationcloud.com) est inscrite à Java Débutant et Python Data Science
- Sophie Dubois (stagiaire2@formationcloud.com) est inscrite à Java Débutant et en attente pour DevOps

### Certificats
- Marie Martin a obtenu un certificat pour Java Débutant (note: 15.50/20)
- Sophie Dubois a obtenu un certificat pour Java Débutant (note: 14.00/20)

### Tâches
- Marie Martin a 2 tâches (1 terminée, 1 en cours)
- Sophie Dubois a 1 tâche en cours

## Notes Importantes

⚠️ **Mot de passe encodé en BCrypt**
Le mot de passe "password" est stocké en BCrypt dans la base de données pour la sécurité.

✅ **Validation automatique**
- Les stagiaires sont validés automatiquement lors de l'inscription
- Les formateurs et admins nécessitent une validation par un administrateur

🔔 **Notifications**
Chaque utilisateur a des notifications de test pour tester le système de notifications.

## Support

Pour toute question, contactez: support@formationcloud.com
