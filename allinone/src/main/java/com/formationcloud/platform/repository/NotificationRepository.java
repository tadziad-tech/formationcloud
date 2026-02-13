package com.formationcloud.platform.repository;

import com.formationcloud.platform.model.Notification;
import com.formationcloud.platform.model.TypeNotification;
import com.formationcloud.platform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByDestinataire(Utilisateur destinataire);

	List<Notification> findByDestinataireAndLu(Utilisateur destinataire, Boolean lu);

	List<Notification> findByDestinataireOrderByDateCreationDesc(Utilisateur destinataire);

	@Query("SELECT n FROM Notification n WHERE n.destinataire.id = :destinataireId AND n.lu = false ORDER BY n.dateCreation DESC")
	List<Notification> findNotificationsNonLuesByDestinataire(Long destinataireId);

	@Query("SELECT COUNT(n) FROM Notification n WHERE n.destinataire.id = :destinataireId AND n.lu = false")
	long countNotificationsNonLuesByDestinataire(Long destinataireId);

	List<Notification> findByType(TypeNotification type);

	@Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.destinataire.id = :userId AND n.type = :type AND n.lien = :lien AND n.dateCreation >= :after")
	boolean existsByDestinataire_IdAndTypeAndLienAndDateCreationAfter(@Param("userId") Long userId, @Param("type") TypeNotification type, @Param("lien") String lien, @Param("after") LocalDateTime after);
}
