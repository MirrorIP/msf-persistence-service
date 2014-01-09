package de.imc.mirror.persistenceservice.filters;

import org.dom4j.DocumentException;
import org.dom4j.Element;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.Filter;

/**
 * Filters data objects by their publisher.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class PublisherFilter implements Filter {
	
	private String publisher;
	
	/**
	 * Creates the filter based on the properties given with a query request.
	 * @param xmlElement XML element specifying the filter properties.
	 * @throws IllegalArgumentException Required properties are missing.
	 */
	public PublisherFilter(Element xmlElement) throws IllegalArgumentException {
		publisher = xmlElement.getText();
		
		if (publisher == null || publisher.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing bare-JID or full-JID of the data object publisher.");
		}
	}

	@Override
	public boolean isDataObjectValid(DataObject object) throws DocumentException {
		String objectPublisher = object.getPublisher();
		if (objectPublisher == null || !objectPublisher.startsWith(publisher)) {
			return false;
		}
		return true;
	}
}
