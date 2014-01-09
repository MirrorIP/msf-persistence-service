package de.imc.mirror.persistenceservice;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.imc.mirror.persistenceservice.exceptions.DBAccessException;

/**
 * Task for the deletion of all expired data objects.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class DataExpirationTask extends TimerTask {
	private static final Logger log = LoggerFactory.getLogger(DataExpirationTask.class);

	private final DatabaseConnector dbConnector;
	
	public DataExpirationTask(DatabaseConnector dbConnector) {
		this.dbConnector = dbConnector;
	}
	
	@Override
	public void run() {
		try {
			int objectsDeleted = dbConnector.deleteExpiredDataObjects();
			log.info("Deleted " + objectsDeleted + " expired data object(s).");
		} catch (DBAccessException e) {
			log.warn("Failed to delete expired data objects.", e);
		}
	}

}
