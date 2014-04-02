package de.imc.mirror.persistenceservice;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentException;

import de.imc.mirror.persistenceservice.exceptions.DBAccessException;

/**
 * Interface to be implemented for all supported database engines.
 * @author simon.schwantzer(at)im-c.de
 */
public interface DatabaseConnector {
	/**
	 * Initialize the data base, e.g., create tables if not existent.
	 */
	public void initialize();
	
	/**
	 * Stores a data object in the database.
	 * @param dataObject Data object to store.
	 * @throws DBAccessException The database access failed.
	 * @throws DocumentException Failed to parse data object when trying to retrieve id.
	 */
	public void storeDataObject(DataObject dataObject) throws DBAccessException, DocumentException;
	
	/**
	 * Returns all objects from a space which fit the given filter set.
	 * @param space Space where the data objects were published.
	 * @param filterSet Filter set to apply.
	 * @return List of data objects which fits the filter set.
	 * @throws DBAccessException The database access failed.
	 */
	public List<DataObject> retrieveObjectsForSpace(Space space, FilterSet filterSet) throws DBAccessException;
	
	/**
	 * Returns all objects with the given ids which fit the given filter set.
	 * @param objectIds Set of object identifiers.
	 * @param filterSet Filter set to apply.
	 * @return List of data objects which fits the filter set.
	 * @throws DBAccessException The database access failed.
	 */
	public List<DataObject> retrieveObjects(Set<String> objectIds, FilterSet filterSet) throws DBAccessException;
	
	/**
	 * Returns the data object with the given identifier.
	 * @param objectId Data object identifier.
	 * @return Data object or <code>null</code> if no data object with the given identifier is stored.
	 * @throws DBAccessException
	 */
	public DataObject retrieveObject(String objectId) throws DBAccessException;
	
	/**
	 * Deletes all data objects published on the given space.
	 * @param space Identifier for the space to delete related data objects. 
	 * @return Number of objects deleted.
	 * @throws DBAccessException The database access failed.
	 */
	public int deleteObjectsForSpace(String spaceId) throws DBAccessException;
	
	/**
	 * Deletes data objects.
	 * Invalid object IDs will be ignored.
	 * @param objectIds Set of identifiers for the objects to delete.
	 * @return Number of objects deleted.
	 * @throws DBAccessException The database access failed.
	 */
	public int deleteObjects(Set<String> objectIds) throws DBAccessException;
	
	/**
	 * Returns the spaces the given objects are stored in.
	 * @param objectIds Identifiers for the objects to return related space. 
	 * @return Map with object identifiers as keys and space identifiers as values. If a data object is not found, the value of the entry is <code>null</code>.
	 * @throws DBAccessException The database access failed.
	 */
	public Map<String, String> retrieveSpacesForObjects(Set<String> objectIds) throws DBAccessException;
	
	/**
	 * Deletes all data objects which expiration date lies in the past.
	 * @return Number of data objects deleted. 
	 * @throws DBAccessException The database access failed.
	 */
	public int deleteExpiredDataObjects() throws DBAccessException;
}
