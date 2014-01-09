package de.imc.mirror.persistenceservice;

/**
 * Enumeration for query targets.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum QueryTargetType {
	SPACE, // a single space
	MULTIPLE_SPACES, // multiple spaces
	OBJECT, // a single object
	MULTIPLE_OBJECTS, // multiple objects
	OTHER; // unknown target type
	
	/**
	 * Returns the type for the given XML element name.
	 * @param elementName Element name string.
	 * @return Query target type or <code>OTHER</other> if type matches.
	 */
	public static QueryTargetType getTypeForElementName(String elementName) {
		if (elementName.equals("objectsForSpace")) {
			return SPACE;
		} else if (elementName.equals("objectsForSpaces")) {
			return MULTIPLE_SPACES;
		} else if (elementName.equals("object")) {
			return OBJECT;
		} else if (elementName.equals("objects")) {
			return MULTIPLE_OBJECTS;
		}
		return OTHER;
	}
	
	@Override
	public String toString() {
		switch (this) {
		case SPACE:
			return "objectsForSpace";
		case MULTIPLE_SPACES:
			return "objectsForSpaces";
		case OBJECT:
			return "object";
		case MULTIPLE_OBJECTS:
			return "objects";
		default:
			return "other";
		}
	}
}
