-- Purge des données liées au module Tache (notifications + lignes tache).
-- La table tache est conservée pour ne pas casser JPA/Hibernate.
-- Idempotent : DELETE sans ligne concernée ne modifie rien ; si la table tache
-- a déjà été supprimée (ex. par une autre migration), on ne fait rien.

DELETE FROM notification WHERE type IN ('TACHE_ASSIGNEE', 'RAPPEL_DEADLINE');

-- Purge tache seulement si la table existe encore
DELIMITER //
CREATE PROCEDURE purge_tache_if_table_exists()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tache') > 0 THEN
    DELETE FROM tache;
  END IF;
END //
DELIMITER ;
CALL purge_tache_if_table_exists();
DROP PROCEDURE IF EXISTS purge_tache_if_table_exists;
