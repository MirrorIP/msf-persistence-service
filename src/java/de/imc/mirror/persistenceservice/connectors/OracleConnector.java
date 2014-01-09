package de.imc.mirror.persistenceservice.connectors;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.DocumentException;
import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.FilterSet;
import de.imc.mirror.persistenceservice.Space;
import de.imc.mirror.persistenceservice.exceptions.DBAccessException;

/**
 * Connector for an IBM DB2 database.
 * @author simon.schwantzer(at)im-c.de
 */
public class OracleConnector extends AbstractSQLConnector {
	private static final Logger log = LoggerFactory.getLogger(OracleConnector.class);

	private static final String SQL_INSERT_DATA_OBJECT = "INSERT INTO " + TABLE_DATA + " (" + COLUMN_OBJECTID + "," + COLUMN_SPACEID + "," + COLUMN_EXPIRATIONDATE + "," + COLUMN_XMLELEMENT + ") VALUES (?,?,?,XMLType(?))";
	private static final String SQL_INSERT_DEPENDENCIES = "INSERT INTO " + TABLE_DEPENDENCIES + " (" + COLUMN_REFERRER + "," + COLUMN_REFERENCE + "," + COLUMN_SPACEID + ") VALUES (?,?,?)";
	private static final String SQL_SELECT_BY_OBJECT_IDS = "SELECT " + COLUMN_OBJECTID + "," + COLUMN_SPACEID + "," + COLUMN_EXPIRATIONDATE + ", to_clob(" + COLUMN_XMLELEMENT + ") AS " + COLUMN_XMLELEMENT + " from " + TABLE_DATA + " WHERE " + COLUMN_OBJECTID + " IN ";
	private static final String SQL_SELECT_BY_SPACE = "SELECT * from " + TABLE_DATA + " WHERE " + COLUMN_SPACEID + " = ?";

	@Override
	public void initialize() {
		log.warn("DB2 connector initialized. The support for this database engine is EXPERIMENTAL!");
	}

	@Override
	public List<DataObject> retrieveObjectsForSpace(Space space, FilterSet filterSet) throws DBAccessException {
		List<DataObject> dataObjects = new ArrayList<DataObject>();
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_SELECT_BY_SPACE);
			stmt.setString(1, space.getId());
			result = stmt.executeQuery();
			while (result.next()) {
				Timestamp expirationDate = result.getTimestamp(COLUMN_EXPIRATIONDATE);
				
				Clob xmlElementClob = result.getClob(COLUMN_XMLELEMENT);
				String xmlElementString = xmlElementClob.getSubString(1, (int) xmlElementClob.length()); // This is ok as MIRROR data models are restricted to 64k.
				DataObject dataObject = new DataObject(xmlElementString, space.getId());
				if (expirationDate != null) {
					dataObject.setExpirationDate(new java.util.Date(expirationDate.getTime()));
				}
				// TODO Move filtering to SQL request.
				if (filterSet.isValid(dataObject)) {
					dataObjects.add(dataObject);
				}
			}
		} catch (SQLException e) {
			throw new DBAccessException("Failed to retrieve data objects from database.", e);
		} finally {
			DbConnectionManager.closeConnection(result, stmt, connection);
		}
		return dataObjects;
	}

	@Override
	public List<DataObject> retrieveObjects(Set<String> objectIds, FilterSet filterSet) throws DBAccessException {
		List<DataObject> dataObjects = new ArrayList<DataObject>();
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_SELECT_BY_OBJECT_IDS + createPlaceHolderTuple(objectIds.size()));
			Iterator<String> objectIdIterator = objectIds.iterator();
			int i = 1;
			while (objectIdIterator.hasNext()) {
				stmt.setString(i, objectIdIterator.next());
				i++;
			}
			result = stmt.executeQuery();
			while (result.next()) {
				String spaceId = result.getString(COLUMN_SPACEID);
				Timestamp expirationDate = result.getTimestamp(COLUMN_EXPIRATIONDATE);
				Clob xmlElementClob = result.getClob(COLUMN_XMLELEMENT);
				String xmlElementString = xmlElementClob.getSubString(1, (int) xmlElementClob.length()); // This is ok as MIRROR data models are restricted to 64k.
				DataObject dataObject = new DataObject(xmlElementString, spaceId);
				if (expirationDate != null) {
					dataObject.setExpirationDate(new java.util.Date(expirationDate.getTime()));
				}
				// TODO Move filtering to SQL request.
				if (filterSet.isValid(dataObject)) {
					dataObjects.add(dataObject);
				}
			}
		} catch (SQLException e) {
			throw new DBAccessException("Failed to retrieve data objects from database.", e);
		} finally {
			DbConnectionManager.closeConnection(result, stmt, connection);
		}
		return dataObjects;
	}

	@Override
	public void storeDataObject(DataObject dataObject) throws DBAccessException, DocumentException {
		java.util.Date expirationDate = dataObject.getExpirationDate();
		String objectId = dataObject.getId();
		Connection connection = null;
		PreparedStatement stmt = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_INSERT_DATA_OBJECT);
			stmt.setString(1, objectId);
			stmt.setString(2, dataObject.getSpaceId());
			
			if (expirationDate != null) {
				stmt.setTimestamp(3, new java.sql.Timestamp(expirationDate.getTime()));
			} else {
				stmt.setTimestamp(3, null);
			}
			Clob xmlClob = connection.createClob();
			xmlClob.setString(1, dataObject.toString());
			stmt.setClob(4, xmlClob);
			stmt.executeUpdate();
			
			Set<String> references = dataObject.getAllReferences();
			if (!references.isEmpty()) {
				// Store dependencies in separate table.
				stmt.close();
				stmt = connection.prepareStatement(SQL_INSERT_DEPENDENCIES);
				for (String reference : references) {
					stmt.setString(1, objectId);
					stmt.setString(2, reference);
					stmt.setString(3, dataObject.getSpaceId());
					stmt.executeUpdate();
				}
			}
		} catch (SQLException e) {
			throw new DBAccessException("Failed to insert data object.", e);
		} finally {
			DbConnectionManager.closeConnection(stmt, connection);
		}
	}
}
