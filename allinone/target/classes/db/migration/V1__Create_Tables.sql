-- Table Utilisateur
CREATE TABLE utilisateur (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    type_formateur VARCHAR(20),
    statut_validation BOOLEAN DEFAULT FALSE,
    telephone VARCHAR(20),
    adresse VARCHAR(255),
    photo_profil VARCHAR(255),
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Categorie
CREATE TABLE categorie (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icone VARCHAR(50),
    couleur VARCHAR(7),
    INDEX idx_nom (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Formation
CREATE TABLE formation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    categorie_id BIGINT,
    capacite_max INT NOT NULL DEFAULT 30,
    formateur_id BIGINT,
    prerequis_id BIGINT,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    lieu VARCHAR(255),
    duree_heures INT,
    prix DECIMAL(10,2),
    image_url VARCHAR(255),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (categorie_id) REFERENCES categorie(id),
    FOREIGN KEY (formateur_id) REFERENCES utilisateur(id),
    FOREIGN KEY (prerequis_id) REFERENCES formation(id),
    INDEX idx_statut (statut),
    INDEX idx_formateur (formateur_id),
    INDEX idx_dates (date_debut, date_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Inscription
CREATE TABLE inscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stagiaire_id BIGINT NOT NULL,
    formation_id BIGINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_validation TIMESTAMP,
    motif_refus TEXT,
    commentaire TEXT,
    position_liste_attente INT,
    FOREIGN KEY (stagiaire_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE,
    UNIQUE KEY unique_inscription (stagiaire_id, formation_id),
    INDEX idx_statut (statut),
    INDEX idx_stagiaire (stagiaire_id),
    INDEX idx_formation (formation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Evaluation
CREATE TABLE evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    formation_id BIGINT NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    seuil_reussite DECIMAL(4,2) NOT NULL,
    date_evaluation DATE NOT NULL,
    duree_minutes INT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE,
    INDEX idx_formation (formation_id),
    INDEX idx_date (date_evaluation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Resultat Evaluation
CREATE TABLE resultat_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    stagiaire_id BIGINT NOT NULL,
    note DECIMAL(4,2) NOT NULL,
    date_passage TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    commentaire TEXT,
    reussi BOOLEAN,
    FOREIGN KEY (evaluation_id) REFERENCES evaluation(id) ON DELETE CASCADE,
    FOREIGN KEY (stagiaire_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    UNIQUE KEY unique_resultat (evaluation_id, stagiaire_id),
    INDEX idx_evaluation (evaluation_id),
    INDEX idx_stagiaire (stagiaire_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Certificat
CREATE TABLE certificat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_unique VARCHAR(100) NOT NULL UNIQUE,
    stagiaire_id BIGINT NOT NULL,
    formation_id BIGINT NOT NULL,
    date_obtention DATE NOT NULL,
    note_finale DECIMAL(4,2),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    url_pdf VARCHAR(255),
    FOREIGN KEY (stagiaire_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE,
    UNIQUE KEY unique_certificat (stagiaire_id, formation_id),
    INDEX idx_numero (numero_unique),
    INDEX idx_stagiaire (stagiaire_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Tache
CREATE TABLE tache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    stagiaire_id BIGINT NOT NULL,
    formation_id BIGINT,
    pourcentage_accomplissement INT DEFAULT 0,
    statut VARCHAR(20) NOT NULL DEFAULT 'ASSIGNEE',
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (stagiaire_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE SET NULL,
    INDEX idx_stagiaire (stagiaire_id),
    INDEX idx_statut (statut),
    INDEX idx_dates (date_debut, date_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Notification
CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    destinataire_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    lu BOOLEAN DEFAULT FALSE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMP,
    lien VARCHAR(255),
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    INDEX idx_destinataire (destinataire_id),
    INDEX idx_lu (lu),
    INDEX idx_date (date_creation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
