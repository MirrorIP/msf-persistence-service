package de.imc.mirror.persistenceservice.config;

/**
 * Configuration for tasks performed by the persistence service.
 * @author simon.schwantzer(at)im-c.de
 */
public interface TaskConfig {
	/**
	 * Period in milliseconds when a data object cleanup is performed.
	 */
	public long DATA_CLEANUP_PERIOD = 86400000;
}
