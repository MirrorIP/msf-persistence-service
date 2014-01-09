package de.imc.mirror.persistenceservice.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for a MySQL database.
 * @author simon.schwantzer(at)im-c.de
 */
public class MySQLConnector extends AbstractSQLConnector {
	private static final Logger log = LoggerFactory.getLogger(MySQLConnector.class);

	@Override
	public void initialize() {
		log.debug("MySQL connector initialized.");
	}
}
