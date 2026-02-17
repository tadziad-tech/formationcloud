-- Purge des notifications et données liées au module Tache (pas de DROP TABLE).
DELETE FROM notification WHERE type IN ('TACHE_ASSIGNEE', 'RAPPEL_DEADLINE');

-- Purge des lignes tache (exécuté seulement si la table existe encore)
DELIMITER //
CREATE PROCEDURE purge_tache_if_exists()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tache') > 0 THEN
    DELETE FROM tache;
  END IF;
END //
DELIMITER ;
CALL purge_tache_if_exists();
DROP PROCEDURE IF EXISTS purge_tache_if_exists;
