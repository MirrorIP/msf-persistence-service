package de.imc.mirror.persistenceservice.exceptions;

/**
 * Exception thrown when a database access fails.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class DBAccessException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public DBAccessException(String message, Throwable e) {
		super(message, e);
	}

}
