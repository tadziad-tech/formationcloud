package com.formationcloud.platform.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.formationcloud.platform.exception.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implémentation locale du stockage : répertoire configurable, noms sécurisés (UUID + extension),
 * interdiction du path traversal.
 */
@Service
public class LocalStorageService implements StorageService {

	@Value("${app.storage.upload-dir:uploads}")
	private String baseDirName;

	private Path basePath;

	@PostConstruct
	public void init() {
		basePath = Paths.get(baseDirName).toAbsolutePath().normalize();
		try {
			Files.createDirectories(basePath);
		} catch (IOException e) {
			throw new IllegalStateException("Impossible de créer le dossier de stockage: " + basePath, e);
		}
	}

	@Override
	public String store(String subDir, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Fichier vide ou null");
		}
		String safeSubDir = sanitizeSegment(subDir);
		if (safeSubDir.isEmpty()) {
			safeSubDir = "files";
		}
		String ext = getSafeExtension(file.getOriginalFilename());
		String uniqueName = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);

		Path targetDir = basePath.resolve(safeSubDir).normalize();
		if (!targetDir.startsWith(basePath)) {
			throw new SecurityException("Path traversal interdit");
		}
		try {
			Files.createDirectories(targetDir);
		} catch (IOException e) {
			throw new IllegalStateException("Impossible de créer le sous-dossier: " + targetDir, e);
		}

		Path targetFile = targetDir.resolve(uniqueName).normalize();
		if (!targetFile.startsWith(basePath)) {
			throw new SecurityException("Path traversal interdit");
		}
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new IllegalStateException("Erreur lors de l'écriture du fichier: " + targetFile, e);
		}

		return safeSubDir + "/" + uniqueName;
	}

	@Override
	public Resource loadAsResource(String relativePath) {
		Path resolved = resolveAndValidate(relativePath);
		if (!Files.exists(resolved) || !Files.isReadable(resolved)) {
			throw new ResourceNotFoundException("Fichier introuvable ou illisible: " + relativePath);
		}
		try {
			return new UrlResource(resolved.toUri());
		} catch (IOException e) {
			throw new IllegalStateException("Impossible de charger le fichier: " + relativePath, e);
		}
	}

	@Override
	public void delete(String relativePath) {
		Path resolved = resolveAndValidate(relativePath);
		try {
			Files.deleteIfExists(resolved);
		} catch (IOException e) {
			throw new IllegalStateException("Impossible de supprimer le fichier: " + relativePath, e);
		}
	}

	private Path resolveAndValidate(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			throw new IllegalArgumentException("Chemin relatif vide");
		}
		if (relativePath.contains("..")) {
			throw new SecurityException("Path traversal interdit: " + relativePath);
		}
		Path resolved = basePath.resolve(relativePath.trim()).normalize();
		if (!resolved.startsWith(basePath)) {
			throw new SecurityException("Path traversal interdit: " + relativePath);
		}
		return resolved;
	}

	private String sanitizeSegment(String segment) {
		if (segment == null) return "";
		String s = segment.trim().replaceAll("\\.\\.", "").replaceAll("[/\\\\]+", "");
		return s.isEmpty() ? "" : s;
	}

	private String getSafeExtension(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) return "";
		String name = originalFilename.trim();
		int i = name.lastIndexOf('.');
		if (i < 0 || i >= name.length() - 1) return "";
		String ext = name.substring(i + 1);
		return ext.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
	}
}
