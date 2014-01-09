package de.imc.mirror.persistenceservice;

import org.dom4j.DocumentException;

/**
 * Interface for data object filters.
 * @author simon.schwantzer(at)im-c.de
 */
public interface Filter {
	/**
	 * Checks if the given data objects passes the filter.
	 * @param object Data object to validate.
	 * @return <code>true</code> if the data object applies to conditions of the filter, otherwise <code>false</code>.
	 * @throws DocumentException An error occurred when parsing the data object XML element.
	 */
	public boolean isDataObjectValid(DataObject object) throws DocumentException;
}
