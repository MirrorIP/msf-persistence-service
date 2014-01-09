package de.imc.mirror.persistenceservice;

/**
 * Enumeration for events send from the spaces service.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum SpacesEventType {
	CREATE,
	CONFIGURE,
	DELETE,
	OTHER;
	
	/**
	 * Return the space event type for the given XML element name.
	 * @param elementName Name of the XML element, which is child element of the root event element.
	 * @return Type or {@link SpacesEventType#OTHER} if the element name cannot be mapped.
	 */
	public static SpacesEventType getTypeForElementName(String elementName) {
		for (SpacesEventType type : SpacesEventType.values()) {
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
