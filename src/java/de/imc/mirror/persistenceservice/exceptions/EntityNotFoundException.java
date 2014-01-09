package de.imc.mirror.persistenceservice.exceptions;

/**
 * Exception thrown if a required entity cannot be found.
 * @author simon.schwantzer(at)im-c.de
 */
public class EntityNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public EntityNotFoundException() {
		super();
	}
	
	public EntityNotFoundException(String message) {
		super(message);
	}
	
	public EntityNotFoundException(String message, Throwable e) {
		super(message, e);
	}
}
