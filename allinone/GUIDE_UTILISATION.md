# Guide d'Utilisation - FormationCloud

## 📋 Table des Matières
1. [Introduction](#introduction)
2. [Installation et Démarrage](#installation-et-démarrage)
3. [Comptes de Test](#comptes-de-test)
4. [Guide par Rôle](#guide-par-rôle)
5. [Fonctionnalités Principales](#fonctionnalités-principales)

---

## 🎯 Introduction

FormationCloud est une plateforme complète de gestion de formations professionnelles développée avec Spring Boot et une interface web moderne. Elle permet de gérer l'ensemble du cycle de vie des formations, des inscriptions aux certifications.

### Technologies Utilisées
- **Backend**: Spring Boot 3.x, Java 17
- **Base de données**: MySQL 8.0
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **Sécurité**: Spring Security + JWT
- **Migration**: Flyway

---

## 🚀 Installation et Démarrage

### Prérequis
- Java 17 ou supérieur
- MySQL 8.0 ou supérieur
- Maven 3.6+

### Étapes d'Installation

1. **Configurer la base de données MySQL**
   ```sql
   CREATE DATABASE formationcloud;
   CREATE USER 'formationcloud_user'@'localhost' IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON formationcloud.* TO 'formationcloud_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configurer application.properties**
   Le fichier `src/main/resources/application.properties` est déjà configuré :
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/formationcloud
   spring.datasource.username=formationcloud_user
   spring.datasource.password=password
   ```

3. **Compiler et démarrer l'application**
   ```bash
   cd formationcloud-v2
   mvn clean install
   mvn spring-boot:run
   ```

4. **Accéder à l'application**
   Ouvrez votre navigateur et accédez à : `http://localhost:8080`

---

## 🔑 Comptes de Test

L'application est livrée avec des comptes de test pré-configurés :

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| **Administrateur** | admin@formationcloud.com | password |
| **Formateur 1** (Interne) | formateur@formationcloud.com | password |
| **Formateur 2** (Externe) | formateur2@formationcloud.com | password |
| **Stagiaire 1** | stagiaire@formationcloud.com | password |
| **Stagiaire 2** | stagiaire2@formationcloud.com | password |

---

## 👥 Guide par Rôle

### 🔴 ADMINISTRATEUR

#### Tableau de Bord
- Vue d'ensemble des statistiques globales
- Graphiques de répartition des utilisateurs et formations
- Gestion des inscriptions en attente
- Validation des nouveaux utilisateurs

#### Fonctionnalités Principales
1. **Gestion des Utilisateurs**
   - Créer, modifier, supprimer des utilisateurs
   - Valider les comptes en attente
   - Filtrer par rôle et statut

2. **Gestion des Formations**
   - Créer et configurer des formations
   - Assigner des formateurs
   - Définir les prérequis
   - Gérer la capacité (max 30 participants)

3. **Gestion des Tâches**
   - Assigner des tâches aux stagiaires
   - Suivre la progression
   - Définir les échéances

4. **Validation des Inscriptions**
   - Approuver ou refuser les demandes
   - Gérer les listes d'attente

---

### 🔵 FORMATEUR

#### Tableau de Bord
- Statistiques personnelles
- Liste des formations assignées
- Demandes d'inscription en attente
- Calendrier des formations

#### Fonctionnalités Principales
1. **Mes Formations**
   - Consulter les formations assignées
   - Voir la liste des participants
   - Gérer les inscriptions

2. **Gestion des Participants**
   - Valider ou refuser les inscriptions
   - Retirer des participants si nécessaire
   - Suivre la progression

3. **Évaluations**
   - Créer des évaluations pour les formations
   - Définir le seuil de réussite
   - Consulter les résultats des stagiaires

4. **Calendrier**
   - Vue d'ensemble des formations planifiées
   - Dates de début et fin

---

### 🟢 STAGIAIRE

#### Tableau de Bord
- Formations en cours
- Évaluations disponibles
- Certificats récents
- Tâches à faire

#### Fonctionnalités Principales
1. **Catalogue de Formations**
   - Explorer les formations disponibles
   - Filtrer par catégorie et type
   - S'inscrire aux formations

2. **Mes Inscriptions**
   - Suivre le statut des inscriptions
   - Voir la progression
   - Accéder aux détails

3. **Évaluations**
   - Passer les évaluations disponibles
   - Soumettre les notes
   - Obtenir automatiquement les certificats (si note ≥ seuil)

4. **Mes Certificats**
   - Consulter les certificats obtenus
   - Voir les détails (numéro, date, note)
   - Utiliser comme prérequis pour d'autres formations

5. **Mes Tâches**
   - Voir les tâches assignées
   - Mettre à jour la progression
   - Respecter les échéances

---

## 🎓 Fonctionnalités Principales

### 1. Système d'Inscription
- **Workflow**: Demande → Validation Formateur → Confirmation
- **Vérification automatique** des prérequis (certificats requis)
- **Gestion de la capacité**: Maximum 30 participants par formation
- **Liste d'attente** automatique si capacité atteinte

### 2. Évaluations et Certificats
- Les formateurs créent des évaluations avec un seuil de réussite
- Les stagiaires passent les évaluations
- **Génération automatique de certificats** si note ≥ seuil
- Numérotation unique des certificats
- Les certificats débloquent les formations de niveau supérieur

### 3. Système de Prérequis
- Les formations peuvent avoir des prérequis
- Exemple: "Java Avancé" nécessite le certificat "Java Débutant"
- Vérification automatique lors de l'inscription

### 4. Gestion des Tâches
- L'administrateur assigne des tâches aux stagiaires
- Suivi de la progression (0-100%)
- Statuts: Assignée, En cours, Terminée, En retard
- Alertes pour les échéances

### 5. Notifications en Temps Réel
- Nouvelle formation disponible
- Évaluation disponible
- Tâche assignée
- Rappel d'échéance
- Inscription validée/refusée
- Certificat obtenu
- Nouvelle demande d'inscription (pour formateurs)

### 6. Catégories de Formations
- Java (Débutant, Intermédiaire, Avancé)
- Python (Débutant, Avancé)
- UML
- DevOps

### 7. Types de Formation
- **À distance**: Formation en ligne
- **En présentiel**: Formation sur site

---

## 📊 Données de Démonstration

L'application inclut des données de test :
- 5 utilisateurs (1 admin, 2 formateurs, 2 stagiaires)
- 4 formations (Java Débutant, Java Avancé, Python Débutant, UML)
- Inscriptions avec différents statuts
- Évaluations et résultats
- Certificats obtenus
- Tâches assignées
- Notifications

---

## 🔒 Sécurité

- **Authentification**: JWT (JSON Web Tokens)
- **Mots de passe**: Chiffrés avec BCrypt
- **Autorisations**: Basées sur les rôles (RBAC)
- **Sessions**: Gestion sécurisée des tokens
- **Validation**: Comptes formateurs/admin nécessitent validation

---

## 📱 Interface Utilisateur

### Design
- Interface moderne et responsive
- Navigation intuitive par rôle
- Tableaux de bord personnalisés
- Notifications en temps réel
- Filtres et recherche avancée

### Composants
- Cartes statistiques
- Graphiques (utilisateurs, formations)
- Tableaux interactifs
- Modales pour les formulaires
- Badges de statut colorés
- Barres de progression

---

## 🛠️ Structure du Projet

```
formationcloud-v2/
├── src/
│   ├── main/
│   │   ├── java/com/formationcloud/
│   │   │   ├── config/          # Configuration (Security, JWT)
│   │   │   ├── controller/      # REST Controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Exception Handling
│   │   │   ├── model/           # Entities JPA
│   │   │   ├── repository/      # JPA Repositories
│   │   │   ├── service/         # Business Logic
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/    # Flyway migrations
│   │       └── static/          # Frontend
│   │           ├── css/
│   │           ├── js/
│   │           └── *.html
│   └── test/                    # Tests unitaires
├── pom.xml
└── README.md
```

---

## 🎯 Cas d'Usage Typiques

### Scénario 1: Inscription à une Formation
1. Le stagiaire parcourt le catalogue
2. Il sélectionne "Java Avancé"
3. Le système vérifie qu'il possède le certificat "Java Débutant"
4. Il soumet sa demande d'inscription
5. Le formateur reçoit une notification
6. Le formateur valide l'inscription
7. Le stagiaire reçoit une confirmation

### Scénario 2: Obtention d'un Certificat
1. Le formateur crée une évaluation (seuil: 70%)
2. Le stagiaire reçoit une notification
3. Le stagiaire passe l'évaluation et obtient 85%
4. Le système génère automatiquement un certificat
5. Le stagiaire reçoit une notification
6. Le certificat est disponible dans "Mes Certificats"
7. Le certificat débloque les formations de niveau supérieur

### Scénario 3: Gestion d'une Tâche
1. L'admin assigne une tâche à un stagiaire
2. Le stagiaire reçoit une notification
3. Il consulte la tâche dans "Mes Tâches"
4. Il met à jour la progression (50%)
5. Il continue et atteint 100%
6. Le statut passe automatiquement à "Terminée"

---

## 📞 Support

Pour toute question ou problème :
- Consultez la documentation technique dans README.md
- Vérifiez les logs de l'application
- Contactez l'équipe de développement

---

## 📝 Notes Importantes

1. **Capacité maximale**: 30 participants par formation
2. **Validation requise**: Les comptes formateur et admin nécessitent une validation
3. **Prérequis**: Vérifiés automatiquement lors de l'inscription
4. **Certificats**: Générés automatiquement si note ≥ seuil de réussite
5. **Notifications**: Rafraîchies toutes les 30 secondes

---

## 🎓 Projet de Fin d'Études (PFE)

Ce projet a été développé dans le cadre d'un Master en Informatique comme projet de fin d'études. Il démontre :
- Architecture Spring Boot complète
- Gestion de la sécurité avec JWT
- Interface utilisateur moderne
- Gestion de workflows complexes
- Système de notifications
- Génération automatique de certificats

---

**Version**: 2.0  
**Date**: Novembre 2024  
**Auteur**: Projet de Fin d'Études Master