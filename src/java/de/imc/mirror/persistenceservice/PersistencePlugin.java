package de.imc.mirror.persistenceservice;

import java.io.File;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

import de.imc.mirror.persistenceservice.config.ComponentConfig;
import de.imc.mirror.persistenceservice.connectors.DB2Connector;
import de.imc.mirror.persistenceservice.connectors.HSQLDBConnector;
import de.imc.mirror.persistenceservice.connectors.MySQLConnector;
import de.imc.mirror.persistenceservice.connectors.OracleConnector;
import de.imc.mirror.persistenceservice.connectors.PostgreSQLConnector;
import de.imc.mirror.persistenceservice.connectors.SQLServerConnector;

/**
 * Openfire plugin implementation for the MIRROR Persistence Service. 
 * @author simon.schwantzer(at)im-c.de
 */
public class PersistencePlugin implements Plugin {
	private static final Logger log = LoggerFactory.getLogger(PersistencePlugin.class);
	
	private ComponentManager componentManager;
	private PersistenceService persistenceServiceComponent;
	
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		DatabaseConnector dbConnector;
		switch (DbConnectionManager.getDatabaseType()) {
		case postgresql:
			dbConnector = new PostgreSQLConnector();
			break;
		case hsqldb:
			dbConnector = new HSQLDBConnector();
			break;
		case db2:
			dbConnector = new DB2Connector();
			break;
		case mysql:
			dbConnector = new MySQLConnector();
			break;
		case oracle:
			dbConnector = new OracleConnector();
			break;
		case sqlserver:
			dbConnector = new SQLServerConnector();
			break;
		default:
			log.error("The database engine used by Openfire (" + DbConnectionManager.getDatabaseType() + ") is not supported by the MIRROR Persistence Service. Initialization cancelled!");
			return;
		}
		dbConnector.initialize();
		
		persistenceServiceComponent = new PersistenceService(dbConnector);
		
		// Register as a component.
		componentManager = ComponentManagerFactory.getComponentManager();
		try {
			componentManager.addComponent(ComponentConfig.SUBDOMAIN, persistenceServiceComponent);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void destroyPlugin() {
		if (componentManager != null) {
			try {
				componentManager.removeComponent("persistence");
			}
			catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
