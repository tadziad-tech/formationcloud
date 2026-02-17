-- Nettoyage des traces du module Tache (données uniquement, pas de DROP table).
-- Compatible MySQL.

-- 1) Supprimer les notifications liées aux tâches
DELETE FROM notification WHERE type IN ('TACHE_ASSIGNEE', 'RAPPEL_DEADLINE');

-- 2) Optionnel : supprimer les tâches seedées si la table existe encore
-- (ne fait rien si la table a déjà été supprimée par une autre migration, ex. V9)
DELIMITER //
CREATE PROCEDURE cleanup_tache_if_exists()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tache') > 0 THEN
    DELETE FROM tache;
  END IF;
END //
DELIMITER ;
CALL cleanup_tache_if_exists();
DROP PROCEDURE IF EXISTS cleanup_tache_if_exists;
