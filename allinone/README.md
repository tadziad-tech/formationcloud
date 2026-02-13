# 🎓 FormationCloud Platform

## Description
Plateforme cloud complète de gestion et de suivi des formations professionnelles avec système de certification, prérequis, et gestion avancée des inscriptions.

## 🚀 Fonctionnalités Principales

### Pour les Administrateurs
- Gestion complète des formations (CRUD)
- Gestion des utilisateurs et validation des inscriptions
- Assignation des formateurs aux formations
- Assignation des tâches aux stagiaires
- Dashboard avec statistiques complètes
- Gestion des catégories de formations

### Pour les Formateurs
- Gestion de leurs formations assignées
- Validation des inscriptions des stagiaires
- Création et gestion des évaluations
- Gestion des participants (ajout/suppression)
- Calendrier personnel des formations
- Dashboard personnalisé

### Pour les Stagiaires
- Consultation du catalogue des formations
- Inscription aux formations (avec vérification des prérequis)
- Suivi des formations en cours
- Passage des évaluations
- Consultation des certificats obtenus
- Gestion des tâches assignées
- Dashboard personnel

## 🛠️ Technologies Utilisées

- **Backend**: Spring Boot 3.2.0, Java 17
- **Base de données**: MySQL 8.0
- **Sécurité**: Spring Security + JWT
- **Frontend**: HTML5, CSS3, JavaScript moderne
- **Migrations**: Flyway
- **Build**: Maven

## 📦 Installation

### Prérequis
- Java 17 ou supérieur
- MySQL 8.0 ou supérieur
- Maven 3.6+

### Étapes d'installation

1. **Cloner le projet**
```bash
cd formationcloud-platform
```

2. **Configurer la base de données**
- Créer une base de données MySQL nommée `formationcloud_db`
- Ou laisser Spring Boot la créer automatiquement

3. **Configurer application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/formationcloud_db
spring.datasource.username=root
spring.datasource.password=votre_mot_de_passe
```

4. **Lancer l'application**
```bash
mvn clean install
mvn spring-boot:run
```

5. **Accéder à l'application**
```
http://localhost:8080
```

## 🔐 Comptes de Test

### Administrateur
- Email: admin@formationcloud.com
- Mot de passe: Admin@123

### Formateur
- Email: formateur@formationcloud.com
- Mot de passe: Formateur@123

### Stagiaire
- Email: stagiaire@formationcloud.com
- Mot de passe: Stagiaire@123

## 📊 Structure de la Base de Données

- **utilisateur**: Gestion des utilisateurs (Admin, Formateur, Stagiaire)
- **formation**: Gestion des formations avec prérequis
- **categorie**: Catégories des formations (Java, Python, etc.)
- **inscription**: Inscriptions des stagiaires aux formations
- **evaluation**: Évaluations créées par les formateurs
- **resultat_evaluation**: Résultats des évaluations des stagiaires
- **certificat**: Certificats générés automatiquement
- **tache**: Tâches assignées aux stagiaires
- **notification**: Système de notifications

## 🎯 Workflow Métier

1. **Création de Formation**: Admin crée une formation et assigne un formateur
2. **Inscription**: Stagiaire s'inscrit (vérification des prérequis automatique)
3. **Validation**: Formateur valide l'inscription
4. **Formation**: Stagiaire suit la formation
5. **Évaluation**: Stagiaire passe l'évaluation
6. **Certification**: Si réussite, certificat généré automatiquement
7. **Déblocage**: Le certificat débloque les formations supérieures

## 📧 Support

Pour toute question ou problème, contactez: support@formationcloud.com

## 📄 Licence

© 2024 FormationCloud Platform - Tous droits réservés

## Sécurité (JWT)
- Connexion: `POST /api/auth/login` ⇒ renvoie `{ token, id, email, nom, prenom, role, statutValidation }`
- Toutes les routes `/api/**` (sauf `/api/auth/**` et les `GET /api/formations/**`) nécessitent un header:
  `Authorization: Bearer <token>`
- Contrôle d'accès:
  - Administration (utilisateurs, stats, etc.) : `ADMIN`
  - Fonctions formateur : `FORMATEUR`
  - Fonctions stagiaire : `STAGIAIRE`
  - Les endpoints `.../utilisateur/{id}` sont protégés : accès seulement pour `ADMIN` ou l’utilisateur lui-même.
