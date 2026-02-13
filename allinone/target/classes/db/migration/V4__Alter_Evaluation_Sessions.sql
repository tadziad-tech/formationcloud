-- Ajout de la gestion des sessions d'évaluation (normale / rattrapage) + état

ALTER TABLE evaluation
    ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN etat VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    ADD COLUMN parent_evaluation_id BIGINT NULL,
    ADD COLUMN date_publication_notes DATETIME NULL;

CREATE INDEX idx_evaluation_parent ON evaluation(parent_evaluation_id);

ALTER TABLE evaluation
    ADD CONSTRAINT fk_evaluation_parent
        FOREIGN KEY (parent_evaluation_id) REFERENCES evaluation(id)
        ON DELETE SET NULL;
