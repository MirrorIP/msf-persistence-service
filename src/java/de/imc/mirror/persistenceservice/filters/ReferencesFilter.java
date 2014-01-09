package de.imc.mirror.persistenceservice.filters;

import org.dom4j.DocumentException;
import org.dom4j.Element;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.Filter;

/**
 * Filters data objects by the content of their ref-attributes.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class ReferencesFilter implements Filter {
	private String referenceId;
	
	/**
	 * Creates the filter based on the properties given with a query request.
	 * @param xmlElement XML element specifying the properties.
	 * @throws IllegalArgumentException At least one property required to create the filter is not set.
	 */
	public ReferencesFilter(Element xmlElement) throws IllegalArgumentException {
		referenceId = xmlElement.attributeValue("id");
		if (referenceId == null || referenceId.trim().isEmpty()) {
			throw new IllegalArgumentException("The filter requires an reference id.");
		}
	}

	@Override
	public boolean isDataObjectValid(DataObject object) throws DocumentException {
		String objectRefValue = object.getRef();
		if (objectRefValue == null || !objectRefValue.equals(referenceId)) {
			return false;
		}
		return true;
	}
}
