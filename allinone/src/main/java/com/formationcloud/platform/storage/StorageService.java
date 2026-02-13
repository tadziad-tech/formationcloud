package com.formationcloud.platform.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service de stockage de fichiers (upload local).
 * Les téléchargements se font via des endpoints /api, pas via un dossier public.
 */
public interface StorageService {

	/**
	 * Stocke un fichier dans le sous-dossier donné.
	 *
	 * @param subDir sous-dossier (ex: "tp"), sans slash, nettoyé
	 * @param file   fichier uploadé
	 * @return chemin relatif à utiliser avec loadAsResource / delete (ex: "tp/uuid.pdf")
	 */
	String store(String subDir, MultipartFile file);

	/**
	 * Charge un fichier par son chemin relatif (retourné par store).
	 *
	 * @param relativePath chemin relatif (ex: "tp/uuid.pdf")
	 * @return ressource lisible, ou exception si absent / path traversal
	 */
	Resource loadAsResource(String relativePath);

	/**
	 * Supprime un fichier par son chemin relatif.
	 *
	 * @param relativePath chemin relatif (ex: "tp/uuid.pdf")
	 */
	void delete(String relativePath);
}
