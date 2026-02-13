-- Table tp_ressource (TP et ressources de cours liées à une formation)
CREATE TABLE tp_ressource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    formation_id BIGINT NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    fichier_url VARCHAR(500),
    date_limite DATETIME,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE,
    INDEX idx_tp_ressource_formation (formation_id),
    INDEX idx_tp_ressource_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table tp_soumission (soumissions de TP par les stagiaires)
CREATE TABLE tp_soumission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tp_id BIGINT NOT NULL,
    stagiaire_id BIGINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'SOUMIS',
    fichier_soumis_url VARCHAR(500),
    commentaire TEXT,
    note DECIMAL(10,2),
    date_soumission TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_tp_soumission_tp_stagiaire UNIQUE (tp_id, stagiaire_id),
    FOREIGN KEY (tp_id) REFERENCES tp_ressource(id) ON DELETE CASCADE,
    FOREIGN KEY (stagiaire_id) REFERENCES utilisateur(id),
    INDEX idx_tp_soumission_tp (tp_id),
    INDEX idx_tp_soumission_stagiaire (stagiaire_id),
    INDEX idx_tp_soumission_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
