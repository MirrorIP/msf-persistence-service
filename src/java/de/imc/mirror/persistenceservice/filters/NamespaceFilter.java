package de.imc.mirror.persistenceservice.filters;

import org.dom4j.DocumentException;
import org.dom4j.Element;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.Filter;

/**
 * Filter for specific namespaces.
 * @author simon.schwantzer(at)im-c.de
 */
public class NamespaceFilter implements Filter {
	/**
	 * Compare type for the namespace filter.
	 * @author simon.schwantzer(at)im-c.de
	 *
	 */
	public enum CompareType {
		/**
		 * The namespace has to be equal the given compare string.
		 */
		STRICT,
		/**
		 * The namespace must contain the given compare string.
		 */
		CONTAINS,
		/**
		 * The namespace must match the regex given with the compare string.
		 */
		REGEX;
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}
	
	private String compareString;
	private CompareType compareType;
	
	/**
	 * Creates a namespace filter based on the properties given in the query.
	 * @param xmlElement XML element specifying the filter properties.
	 * @throws IllegalArgumentException Failed to retrieve required properties from the element.
	 */
	public NamespaceFilter(Element xmlElement) throws IllegalArgumentException {
		String compareTypeString = xmlElement.attributeValue("compareType");
		if (compareTypeString == null) {
			compareType = CompareType.STRICT;
		} else {
			for (CompareType type : CompareType.values()) {
				if (type.toString().equals(compareTypeString)) {
					compareType = type;
					break;
				}
			}
			if (compareType == null) {
				throw new IllegalArgumentException("Compare type for namespace filter has to be either \"strict\" (default), \"contains\", or \"regex\".");
			}
		}
		
		compareString = xmlElement.getText();
		if (compareString == null || compareString.isEmpty()) {
			throw new IllegalArgumentException("Compare string is missing.");
		}
	}

	@Override
	public boolean isDataObjectValid(DataObject object) throws DocumentException {
		String objectNamespace = object.getNamespace();
		switch (compareType) {
		case STRICT:
			if (!objectNamespace.equals(compareString)) {
				return false;
			}
			break;
		case CONTAINS:
			if (!objectNamespace.contains(compareString)) {
				return false;
			}
			break;
		case REGEX:
			if (!objectNamespace.matches(compareString)) {
				return false;
			}
			break;
		}
		return true;
	}
}
