package de.imc.mirror.persistenceservice;

/**
 * Enumeration for IQ requests.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum IQGetType {
	QUERY,
	OTHER;
	
	/**
	 * Returns the type for the give element name.
	 * @param elementName Element name.
	 * @return Related type or {@link IQGetType#OTHER} if the mapping failed.
	 */
	public static IQGetType getTypeForElementName(String elementName) {
		for (IQGetType type : IQGetType.values()) {
			if (type.toString().equalsIgnoreCase(elementName)) {
				return type;
			}
		}
		return OTHER; 
	}
}
