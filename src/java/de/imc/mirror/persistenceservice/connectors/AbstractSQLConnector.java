package de.imc.mirror.persistenceservice.connectors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentException;
import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.DatabaseConnector;
import de.imc.mirror.persistenceservice.FilterSet;
import de.imc.mirror.persistenceservice.Space;
import de.imc.mirror.persistenceservice.exceptions.DBAccessException;

public abstract class AbstractSQLConnector implements DatabaseConnector {
	private static final Logger log = LoggerFactory.getLogger(AbstractSQLConnector.class);

	public static final String TABLE_DATA = "ofSpacePersistenceData";
	public static final String TABLE_DEPENDENCIES = "ofSpacePersistenceDependencies";
	
	public static final String COLUMN_OBJECTID = "objectId";
	public static final String COLUMN_SPACEID = "spaceId";
	public static final String COLUMN_EXPIRATIONDATE = "expirationDate";
	public static final String COLUMN_XMLELEMENT = "xmlElement";
	
	public static final String COLUMN_REFERRER = "referrer";
	public static final String COLUMN_REFERENCE = "reference";
	
	private static final String SQL_SELECT_BY_SPACE = "SELECT * from " + TABLE_DATA + " WHERE " + COLUMN_SPACEID + " = ?";
	private static final String SQL_SELECT_BY_OBJECT_IDS = "SELECT * from " + TABLE_DATA + " WHERE " + COLUMN_OBJECTID + " IN ";
	private static final String SQL_SELECT_BY_OBJECT_ID = "SELECT * from " + TABLE_DATA + " WHERE " + COLUMN_OBJECTID + " = ?";
	private static final String SQL_SELECT_REFERRING_OBJECTS = "SELECT " + COLUMN_REFERRER + "," + COLUMN_REFERENCE + " FROM " + TABLE_DEPENDENCIES;
	private static final String SQL_INSERT_DATA_OBJECT = "INSERT INTO " + TABLE_DATA + " (" + COLUMN_OBJECTID + "," + COLUMN_SPACEID + "," + COLUMN_EXPIRATIONDATE + "," + COLUMN_XMLELEMENT + ") VALUES (?,?,?,?)";
	private static final String SQL_INSERT_DEPENDENCIES = "INSERT INTO " + TABLE_DEPENDENCIES + " (" + COLUMN_REFERRER + "," + COLUMN_REFERENCE + "," + COLUMN_SPACEID + ") VALUES (?,?,?)";
	private static final String SQL_DELETE_OBJECTS_OF_SPACE = "DELETE FROM " + TABLE_DATA + " WHERE " + COLUMN_SPACEID + " = ?";
	private static final String SQL_DELETE_DEPENDENCIES_BY_SPACE = "DELETE FROM " + TABLE_DEPENDENCIES + " WHERE " + COLUMN_SPACEID + " = ?";
	private static final String SQL_DELETE_OBJECTS = "DELETE FROM " + TABLE_DATA + " WHERE " + COLUMN_OBJECTID + " IN ";
	private static final String SQL_DELETE_DEPENDENCIES_OF_OBJECTS = "DELETE FROM " + TABLE_DEPENDENCIES + " WHERE " + COLUMN_REFERRER + " IN ";
	private static final String SQL_SPACE_BY_OBJECT = "SELECT " + COLUMN_SPACEID + " FROM " + TABLE_DATA + " WHERE " + COLUMN_OBJECTID + " = ?";
	private static final String SQL_SELECT_EXPIRED_OBJECTS = "SELECT " + COLUMN_OBJECTID + " FROM " + TABLE_DATA + " WHERE " + COLUMN_EXPIRATIONDATE + " < ?";
	
	/**
	 * Creates an tuple of place holders to be used in SQL clauses.
	 * @param size Number of elements.
	 * @return String containing a tube of place holders, e.g. "(?,?,?)"
	 */
	public String createPlaceHolderTuple(int size) {
		StringBuilder builder = new StringBuilder(size * 2 + 2);
		builder.append("(");
		for (int i = 0; i < size; i++) {
			builder.append("?");
			if (i < size - 1) {
				builder.append(",");
			}
		}
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Retrieve a map containing all referenced nodes with a list of their referrers.
	 * @return Map with the referenced objects id as string and a list of all objects referring to this object. 
	 * @throws DBAccessException Failed to retrieve data object information from the database.
	 */
	public Map<String, Set<String>> retrieveReferencedNodes() throws DBAccessException {
		Map<String, Set<String>> referencedObjects = new HashMap<String, Set<String>>();
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_SELECT_REFERRING_OBJECTS);
			result = stmt.executeQuery();
			while (result.next()) {
				String referrer = result.getString(COLUMN_REFERRER);
				String reference = result.getString(COLUMN_REFERENCE);
				if (!referencedObjects.containsKey(reference)) {
					referencedObjects.put(reference, new HashSet<String>());
				}
				referencedObjects.get(reference).add(referrer);
			}
		} catch (SQLException e) {
			log.warn("Crashed while executing SQL: " + SQL_SELECT_REFERRING_OBJECTS);
			throw new DBAccessException("Failed to retrieve data object information from the database.", e);
		} finally {
			DbConnectionManager.closeConnection(result, stmt, connection);
		}
		return referencedObjects;
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
				String xmlElementString = result.getString(COLUMN_XMLELEMENT);
				DataObject dataObject = new DataObject(xmlElementString, space.getId());
				if (expirationDate != null) {
					dataObject.setExpirationDate(new java.util.Date(expirationDate.getTime()));
				}
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
				String xmlElementString = result.getString(COLUMN_XMLELEMENT);
				DataObject dataObject = new DataObject(xmlElementString, spaceId);
				if (expirationDate != null) {
					dataObject.setExpirationDate(new java.util.Date(expirationDate.getTime()));
				}
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
	public DataObject retrieveObject(String objectId) throws DBAccessException {
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		DataObject dataObject;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_SELECT_BY_OBJECT_ID);
			stmt.setString(1, objectId);
			result = stmt.executeQuery();
			if (result.next()) {
				String spaceId = result.getString(COLUMN_SPACEID);
				Timestamp expirationDate = result.getTimestamp(COLUMN_EXPIRATIONDATE);
				String xmlElementString = result.getString(COLUMN_XMLELEMENT);
				dataObject = new DataObject(xmlElementString, spaceId);
				if (expirationDate != null) {
					dataObject.setExpirationDate(new java.util.Date(expirationDate.getTime()));
				}
			} else {
				dataObject = null;
			}
		} catch (SQLException e) {
			throw new DBAccessException("Failed to retrieve data objects from database.", e);
		} finally {
			DbConnectionManager.closeConnection(result, stmt, connection);
		}
		return dataObject;
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
			stmt.setString(4, dataObject.toString());
			stmt.executeUpdate();
			
			Set<String> references = dataObject.getAllReferences();
			if (!references.isEmpty()) {
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
	
	@Override
	public int deleteObjectsForSpace(String spaceId) throws DBAccessException {
		int deletedObjects = 0;
		Connection connection = null;
		PreparedStatement stmt = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_DELETE_OBJECTS_OF_SPACE); 
			stmt.setString(1, spaceId);
			deletedObjects = stmt.executeUpdate();
			stmt.close();
			stmt = connection.prepareStatement(SQL_DELETE_DEPENDENCIES_BY_SPACE);
			stmt.setString(1, spaceId);
			stmt.executeUpdate();			
		} catch (SQLException e) {
			throw new DBAccessException("Failed to delete data objects.", e);
		} finally {
			DbConnectionManager.closeConnection(stmt, connection);
		}
		return deletedObjects;
	}

	@Override
	public int deleteObjects(Set<String> objectIds) throws DBAccessException {
		int deletedObjects = 0;
		Connection connection = null;
		PreparedStatement deleteObjectStmt, deleteDependenciesStmt;
		String inClause = createPlaceHolderTuple(objectIds.size());
		try {
			connection = DbConnectionManager.getConnection();
			deleteObjectStmt = connection.prepareStatement(SQL_DELETE_OBJECTS + inClause);
			deleteDependenciesStmt = connection.prepareStatement(SQL_DELETE_DEPENDENCIES_OF_OBJECTS + inClause);
			Iterator<String> objectIdIterator = objectIds.iterator();
			int i = 1;
			while (objectIdIterator.hasNext()) {
				String objectId = objectIdIterator.next();
				deleteObjectStmt.setString(i, objectId);
				deleteDependenciesStmt.setString(i, objectId);
				i++;
			}
			deletedObjects = deleteObjectStmt.executeUpdate();
			deleteDependenciesStmt.executeUpdate();
			deleteObjectStmt.close();
			deleteDependenciesStmt.close();
		} catch (SQLException e) {
			throw new DBAccessException("Failed to delete data objects.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
		return deletedObjects;
	}

	@Override
	public Map<String, String> retrieveSpacesForObjects(Set<String> objectIds) throws DBAccessException {
		Map<String, String> spaceIds = new HashMap<String, String>();
		
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_SPACE_BY_OBJECT);
			for (String objectId : objectIds) {
				stmt.setString(1, objectId);
				result = stmt.executeQuery();
				spaceIds.put(objectId, result.next() ? result.getString(COLUMN_SPACEID) : null);
				result.close();
			}
		} catch (SQLException e) {
			throw new DBAccessException("Failed to retrieve space identifiers from database.", e);
		} finally {
			DbConnectionManager.closeConnection(result, stmt, connection);
		}
		return spaceIds;
	}

	@Override
	public int deleteExpiredDataObjects() throws DBAccessException {
		int rowsDeleted = 0;
		Set<String> expiredObjects = retrieveExpiredDataObjects();
		int expiredCount = expiredObjects.size();
		Map<String, Set<String>> referencedObjects = retrieveReferencedNodes();
		Set<String> objectsToDelete = new HashSet<String>();
		boolean done = false;
		while (!done) {
			List<String> candidates = new ArrayList<String>();
			for (String expiredObject : expiredObjects) {
				if (!referencedObjects.containsKey(expiredObject)) {
					candidates.add(expiredObject);
				}
			}
			if (candidates.isEmpty()) {
				done = true;
				continue;
			}
			objectsToDelete.addAll(candidates);
			expiredObjects.removeAll(candidates);
			Set<String> referencedObjectsToDelete = new HashSet<String>();
			for (String deletedObject : candidates) {
				for (String referencedObjectId : referencedObjects.keySet()) {
					Set<String> referrers = referencedObjects.get(referencedObjectId);
					if (referrers.contains(deletedObject)) {
						referrers.remove(deletedObject);
						if (referrers.isEmpty()) {
							referencedObjectsToDelete.add(referencedObjectId);
						}
					}
				}
			}
			for (String objectId : referencedObjectsToDelete) {
				referencedObjects.remove(objectId);
			}
		}
		log.info("Kept " + (expiredCount - objectsToDelete.size()) + " data objects due to depenedencies.");
		if (!objectsToDelete.isEmpty()) {
			rowsDeleted = this.deleteObjects(objectsToDelete);
		}
		return rowsDeleted;
	}

	/**
	 * Retrieves all data objects which are expired.
	 * @return List of object identifiers.
	 * @throws DBAccessException Failed to retrieve data object identifiers.
	 */
	private Set<String> retrieveExpiredDataObjects() throws DBAccessException {
		Set<String> objectIds = new HashSet<String>();
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(SQL_SELECT_EXPIRED_OBJECTS);
			stmt.setTimestamp(1, new Timestamp(new java.util.Date().getTime()));
			result = stmt.executeQuery();
			while (result.next()) {
				objectIds.add(result.getString(COLUMN_OBJECTID));
			}
		} catch (SQLException e) {
			throw new DBAccessException("Failed to retrieve data object information from the database.", e);
		} finally {
			DbConnectionManager.closeConnection(result, stmt, connection);
		}
		return objectIds;
	}

}
