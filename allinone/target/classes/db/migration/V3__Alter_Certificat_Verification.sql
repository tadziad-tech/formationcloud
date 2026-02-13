-- Add fields to make certificates verifiable and revocable

ALTER TABLE certificat
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'VALIDE',
    ADD COLUMN date_revocation DATE NULL;

CREATE INDEX idx_certificat_statut ON certificat(statut);
