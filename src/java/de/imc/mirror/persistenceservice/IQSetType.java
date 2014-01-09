package de.imc.mirror.persistenceservice;

/**
 * Enumeration for IQ set requests.
 * @author simon.schwantzer(at)im-c.de
 */
public enum IQSetType {
	INSERT,
	DELETE,
	OTHER;
	
	/**
	 * Returns the type for the given element name.
	 * @param elementName Name of the XML element.
	 * @return Type for the given element or {@link IQSetType#OTHER} if the mapping fails.
	 */
	public static IQSetType getTypeForElementName(String elementName) {
		for (IQSetType type : IQSetType.values()) {
			if (type.toString().equalsIgnoreCase(elementName)) {
				return type;
			}
		}
		return OTHER;
	}
}
