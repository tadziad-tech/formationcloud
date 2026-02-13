-- Ajout gestion des absences sur les résultats d'évaluation.
-- Un stagiaire peut être ABSENT (pas de note) et sera traité comme "non réussi".

ALTER TABLE resultat_evaluation
    ADD COLUMN absent BOOLEAN NOT NULL DEFAULT FALSE;

-- La note devient nullable (null si ABSENT)
ALTER TABLE resultat_evaluation
    MODIFY note DECIMAL(4,2) NULL;
