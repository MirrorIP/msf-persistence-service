package de.imc.mirror.persistenceservice;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Wrapper for data objects.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class DataObject {
	private Element element;
	private String elementString;
	private String spaceId;
	private Date expirationDate;
	
	/**
	 * Creates a data object based on the given string.
	 * The string is NOT parsed during initialization.
	 * @param elementString XML element string.
	 * @param spaceId Identifier of the space the object was published on.
	 */
	public DataObject(String elementString, String spaceId) {
		this.element = null;
		this.elementString = elementString;
		this.spaceId = spaceId;
		this.expirationDate = null;
	}
	
	/**
	 * Creates an data object based on the given XML element.
	 * @param element XML element representing the data object.
	 * @param spaceId Identifier of the space the object was published on.
	 */
	public DataObject(Element element, String spaceId) {
		this.element = element;
		this.spaceId = spaceId;
		this.expirationDate = null;
	}
	
	/**
	 * Parses the element string given during initialization.
	 * If the string is already parsed, nothing happens.
	 * @throws DocumentException The element could not be parsed.
	 */
	public void parseElementString() throws DocumentException {
		if (element != null) {
			// Element was already parsed.
			return;
		}
		element = DocumentHelper.parseText(elementString).getRootElement();
	}
	
	/**
	 * Returns the identifier for the space the data object was published on.
	 * @return Space identifier.
	 */
	public String getSpaceId() {
		return spaceId;
	}
	
	/**
	 * Sets the date and time the data object expires.
	 * @param expirationDate Expiration date or <code>null</code> if the data object does not expire.
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	
	/**
	 * Returns the expiration date and time for this data object.
	 * @return Expiration date or <code>null</code> if the data object does not expire.
	 */
	public Date getExpirationDate() {
		return this.expirationDate;
	}
	
	/**
	 * Returns the id of the data object.
	 * @return Data object id.
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public String getId() throws DocumentException {
		parseElementString();
		return element.attributeValue("id");
	}
	
	/**
	 * Returns the namespace of the data object.
	 * @return Namespace URI string.
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public String getNamespace() throws DocumentException {
		parseElementString();
		return element.getNamespaceURI();
	}
	
	/**
	 * Returns the value of the CDM attribute <code>modelVersion</code>.
	 * @return Model version string or <code>null</code> if not set.
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public String getModelVersion() throws DocumentException {
		parseElementString();
		return element.attributeValue("modelVersion");
	}
	
	/**
	 * Returns the value of the CDM attribute <code>timestamp</code>. 
	 * @return Date object for the timestamp or <code>null</code> if not set or invalid.  
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public Date getTimestamp() throws DocumentException {
		parseElementString();
		String dateTimeString = element.attributeValue("timestamp");
		if (dateTimeString != null) {
			try {
				Calendar calendar = DatatypeConverter.parseDateTime(dateTimeString);
				return calendar.getTime();
			} catch (IllegalArgumentException e) {
				throw new DocumentException("Failed to convert timestamp.", e);
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the value of the CDM attribute <code>publisher</publisher>.
	 * @return Full JID of the publisher or <code>null</code> if not set.
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public String getPublisher() throws DocumentException {
		parseElementString();
		String publisher = element.attributeValue("publisher");
		return publisher;
	}
	
	/**
	 * Returns the value of the CDM attribute <code>ref</code>.
	 * @return Object id of the object referenced with the attribute or <code>null</code> if not set.
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public String getRef() throws DocumentException {
		parseElementString();
		String ref = element.attributeValue("ref");
		return ref;
	}
	
	/**
	 * Returns all data objects which are references by this data object.
	 * @return Set of data object identifiers.
	 * @throws DocumentException The element string was parsed and a parsing error occurred.
	 */
	public Set<String> getAllReferences() throws DocumentException {
		Set<String> references = new HashSet<String>();
		String ref = this.getRef();
		if (ref != null) {
			references.add(ref);
		}
		return references;
	}
	
	/**
	 * Returns the XML element wrapped by this object.
	 * Changes applied to this element will also affect this object.
	 * @return XML element representing the data object.
	 */
	public Element getXMLElement() {
		return element;
	}
	
	/**
	 * Returns the XML element as string.
	 * @return String representation of the XML element.
	 */
	@Override
	public String toString() {
		if (element != null) {
			return element.asXML();
		} else {
			return elementString;
		}
	}
}
