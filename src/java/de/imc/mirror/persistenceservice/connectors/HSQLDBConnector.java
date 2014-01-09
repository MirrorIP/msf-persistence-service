package de.imc.mirror.persistenceservice.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for a HSQLDB database.
 * @author simon.schwantzer(at)im-c.de
 */
public class HSQLDBConnector extends AbstractSQLConnector {
	private static final Logger log = LoggerFactory.getLogger(HSQLDBConnector.class);
	
	@Override
	public void initialize() {
		log.warn("HSQLDB connector initialized. Use a regular database for better performance!");
	}
}
