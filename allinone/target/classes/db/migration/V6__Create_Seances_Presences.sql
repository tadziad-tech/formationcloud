-- Table Seance (liée à formation)
CREATE TABLE seance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    formation_id BIGINT NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'PRESENTIEL',
    zoom_link VARCHAR(500),
    lieu VARCHAR(255),
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE',
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE,
    INDEX idx_seance_formation (formation_id),
    INDEX idx_seance_dates (date_debut, date_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table Presence (une ligne par stagiaire et par séance)
CREATE TABLE presence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seance_id BIGINT NOT NULL,
    stagiaire_id BIGINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'NON_MARQUE',
    remarque TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_presence_seance_stagiaire UNIQUE (seance_id, stagiaire_id),
    FOREIGN KEY (seance_id) REFERENCES seance(id) ON DELETE CASCADE,
    FOREIGN KEY (stagiaire_id) REFERENCES utilisateur(id),
    INDEX idx_presence_seance (seance_id),
    INDEX idx_presence_stagiaire (stagiaire_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
