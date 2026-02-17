-- Migrate task-related notification types to AUTRE (enum change)
UPDATE notification SET type = 'AUTRE' WHERE type IN ('TACHE_ASSIGNEE', 'RAPPEL_DEADLINE');

-- Optional: clear links pointing to removed /taches routes
UPDATE notification SET lien = NULL WHERE lien LIKE '/taches/%';

-- Drop tache table (FK from utilisateur is on tache.stagiaire_id, so drop table first)
DROP TABLE IF EXISTS tache;
