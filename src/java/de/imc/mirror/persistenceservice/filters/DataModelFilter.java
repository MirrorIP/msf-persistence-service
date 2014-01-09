package de.imc.mirror.persistenceservice.filters;

import org.dom4j.DocumentException;
import org.dom4j.Element;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.Filter;

/**
 * Filter for data model information.
 * @author simon.schwantzer(at)im-c.de
 */
public class DataModelFilter implements Filter {
	private String namespace;
	private String version;
	
	/**
	 * Creates a filter based on a filter property entry of a query.
	 * @param xmlElement XML element specifying the filter properties.
	 * @throws IllegalArgumentException Failed to retrieve properties from the given XML element.
	 */
	public DataModelFilter(Element xmlElement) throws IllegalArgumentException {
		namespace = xmlElement.attributeValue("namespace");
		if (namespace == null || namespace.trim().isEmpty()) {
			throw new IllegalArgumentException("A model namespace is required but missing.");
		}
		version = xmlElement.attributeValue("version");
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) throws DocumentException {
		String objectNamespace = dataObject.getNamespace();
		if (!objectNamespace.equals(namespace)) {
			return false;
		}
		if (version != null) {
			String modelVersion = dataObject.getModelVersion();
			if (modelVersion == null || !modelVersion.equals(version)) {
				return false;
			}
		}
		return true;
	}
}
