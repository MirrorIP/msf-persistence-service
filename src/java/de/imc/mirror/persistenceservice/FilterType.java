package de.imc.mirror.persistenceservice;

/**
 * Enumeration for filter types as specified in a query.
 * @author simon.schwantzer(at)im-c.de
 */
public enum FilterType {
	PERIOD,
	PUBLISHER,
	NAMESPACE,
	DATAMODEL,
	REFERENCES,
	OTHER;
	
	/**
	 * Returns the filter type for a given element name.
	 * @param elementName Name of the XML element specifying the filter properties.
	 * @return Filter type or <code>OTHER</code> if no type matches.
	 */
	public static FilterType getValueForElementName(String elementName) {
		for (FilterType type : FilterType.values()) {
			if (type.toString().equalsIgnoreCase(elementName)) {
				return type;
			}
		}
		return OTHER;
	}
	
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
