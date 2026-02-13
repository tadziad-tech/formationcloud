package com.formationcloud.platform.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String resource, String field, Object value) {
		super(String.format("%s non trouvé(e) avec %s : '%s'", resource, field, value));
	}

	private static final long serialVersionUID = 1L;

}
