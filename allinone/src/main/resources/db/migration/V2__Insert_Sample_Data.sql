-- Insertion des catégories
INSERT INTO categorie (nom, description, icone, couleur) VALUES
('Java', 'Programmation Java et frameworks', 'fa-coffee', '#f89820'),
('Python', 'Développement Python et Data Science', 'fa-python', '#3776ab'),
('DevOps', 'DevOps, CI/CD et Cloud', 'fa-server', '#0db7ed'),
('UML', 'Modélisation et conception', 'fa-project-diagram', '#5c4ee5');

-- Insertion des utilisateurs avec mots de passe encodés en BCrypt
-- Mot de passe pour tous: "password"
-- Hash BCrypt: $2b$10$D5UH9Yrb/xXcbv7PJqBXjOC5z.JZxJ7PB8ZlQ/YqFPzWJF6rBxCWa

INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role, statut_validation, actif) VALUES
('Admin', 'Super', 'admin@formationcloud.com', '$2b$10$D5UH9Yrb/xXcbv7PJqBXjOC5z.JZxJ7PB8ZlQ/YqFPzWJF6rBxCWa', 'ADMIN', TRUE, TRUE),
('Dupont', 'Jean', 'formateur@formationcloud.com', '$2b$10$D5UH9Yrb/xXcbv7PJqBXjOC5z.JZxJ7PB8ZlQ/YqFPzWJF6rBxCWa', 'FORMATEUR', TRUE, TRUE),
('Martin', 'Marie', 'stagiaire@formationcloud.com', '$2b$10$D5UH9Yrb/xXcbv7PJqBXjOC5z.JZxJ7PB8ZlQ/YqFPzWJF6rBxCWa', 'STAGIAIRE', TRUE, TRUE),
('Bernard', 'Pierre', 'formateur2@formationcloud.com', '$2b$10$D5UH9Yrb/xXcbv7PJqBXjOC5z.JZxJ7PB8ZlQ/YqFPzWJF6rBxCWa', 'FORMATEUR', TRUE, TRUE),
('Dubois', 'Sophie', 'stagiaire2@formationcloud.com', '$2b$10$D5UH9Yrb/xXcbv7PJqBXjOC5z.JZxJ7PB8ZlQ/YqFPzWJF6rBxCWa', 'STAGIAIRE', TRUE, TRUE);

-- Mise à jour du type de formateur
UPDATE utilisateur SET type_formateur = 'INTERNE' WHERE email = 'formateur@formationcloud.com';
UPDATE utilisateur SET type_formateur = 'EXTERNE' WHERE email = 'formateur2@formationcloud.com';

-- Insertion des formations
INSERT INTO formation (nom, description, type, categorie_id, capacite_max, formateur_id, statut, date_debut, date_fin, lieu, duree_heures, prix) VALUES
('Java Débutant', 'Introduction à la programmation Java', 'PRESENTIELLE', 1, 30, 2, 'ACTIVE', '2024-02-01', '2024-02-15', 'Salle A', 40, 500.00),
('Java Avancé', 'Concepts avancés de Java et Spring Boot', 'PRESENTIELLE', 1, 25, 2, 'ACTIVE', '2024-03-01', '2024-03-20', 'Salle B', 60, 800.00),
('Python Data Science', 'Analyse de données avec Python', 'A_DISTANCE', 2, 30, 4, 'ACTIVE', '2024-02-10', '2024-03-10', 'En ligne', 50, 600.00),
('DevOps Essentials', 'Introduction au DevOps et CI/CD', 'PRESENTIELLE', 3, 20, 4, 'ACTIVE', '2024-02-20', '2024-03-05', 'Salle C', 45, 700.00);

-- Définir les prérequis
UPDATE formation SET prerequis_id = 1 WHERE id = 2;

-- Insertion des inscriptions
INSERT INTO inscription (stagiaire_id, formation_id, statut, date_validation) VALUES
(3, 1, 'CONFIRMEE', NOW()),
(5, 1, 'CONFIRMEE', NOW()),
(3, 3, 'EN_COURS', NOW()),
(5, 4, 'EN_ATTENTE', NULL);

-- Insertion des évaluations
INSERT INTO evaluation (formation_id, titre, description, seuil_reussite, date_evaluation, duree_minutes) VALUES
(1, 'Examen Final Java Débutant', 'Évaluation des connaissances de base en Java', 10.00, '2024-02-14', 120),
(3, 'Projet Data Science', 'Projet pratique d\'analyse de données', 12.00, '2024-03-08', 180);

-- Insertion des résultats d'évaluation
INSERT INTO resultat_evaluation (evaluation_id, stagiaire_id, note, reussi, commentaire) VALUES
(1, 3, 15.50, TRUE, 'Très bonne compréhension des concepts de base'),
(1, 5, 14.00, TRUE, 'Bon travail, continue comme ça');

-- Insertion des certificats (générés automatiquement après réussite)
INSERT INTO certificat (numero_unique, stagiaire_id, formation_id, date_obtention, note_finale) VALUES
('CERT-2024-001-3456', 3, 1, '2024-02-15', 15.50),
('CERT-2024-002-7890', 5, 1, '2024-02-15', 14.00);

-- Insertion des tâches
INSERT INTO tache (titre, description, stagiaire_id, formation_id, pourcentage_accomplissement, statut, date_debut, date_fin) VALUES
('TP Java - Classes et Objets', 'Créer une application de gestion avec POO', 3, 1, 100, 'TERMINEE', '2024-02-05', '2024-02-10'),
('Projet Python - Analyse de données', 'Analyser un dataset avec Pandas', 3, 3, 60, 'EN_COURS', '2024-02-15', '2024-03-05'),
('TP DevOps - Pipeline CI/CD', 'Mettre en place un pipeline Jenkins', 5, 4, 30, 'EN_COURS', '2024-02-25', '2024-03-03');

-- Insertion des notifications
INSERT INTO notification (destinataire_id, type, message, lu, lien) VALUES
(3, 'CERTIFICAT_OBTENU', 'Félicitations ! Vous avez obtenu le certificat pour la formation Java Débutant', FALSE, '/certificats/1'),
(5, 'CERTIFICAT_OBTENU', 'Félicitations ! Vous avez obtenu le certificat pour la formation Java Débutant', FALSE, '/certificats/2'),
(3, 'TACHE_ASSIGNEE', 'Nouvelle tâche assignée: Projet Python - Analyse de données', TRUE, '/taches/2'),
(5, 'INSCRIPTION_VALIDEE', 'Votre inscription à la formation Java Débutant a été validée', TRUE, '/formations/1'),
(2, 'NOUVELLE_INSCRIPTION', 'Sophie Dubois s\'est inscrite à votre formation DevOps Essentials', FALSE, '/inscriptions/4');
