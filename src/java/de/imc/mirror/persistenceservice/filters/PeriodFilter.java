package de.imc.mirror.persistenceservice.filters;

import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.dom4j.DocumentException;
import org.dom4j.Element;

import de.imc.mirror.persistenceservice.DataObject;
import de.imc.mirror.persistenceservice.Filter;


/**
 * Restricts the period of publishing.
 * @author simon.schwantzer(at)im-c.de
 */
public class PeriodFilter implements Filter {
	private Date from, to;

	/**
	 * Generates a filter based on the related XML element of a query. 
	 * @param periodElement XML element.
	 * @throws IllegalArgumentException The given element is not valid.
	 */
	public PeriodFilter(Element xmlElement) throws IllegalArgumentException {
		String fromString = xmlElement.attributeValue("from");
		String toString = xmlElement.attributeValue("to");
		if (fromString == null && toString == null) {
			throw new IllegalArgumentException("Failed to create filter: Attributes are missing.");
		}
		if (fromString != null) {
			from = DatatypeConverter.parseDateTime(fromString).getTime();
		}
		if (toString != null) {
			to = DatatypeConverter.parseDateTime(toString).getTime();
		}
	}

	@Override
	public boolean isDataObjectValid(DataObject object) throws DocumentException {
		Date objectTimestamp = object.getTimestamp();
		if (objectTimestamp == null) {
			return false;
		}
		if (from != null && objectTimestamp.before(from)) {
			return false;
		}
		if (to != null && objectTimestamp.after(to)) {
			return false;
		}
		return true;
	}
}
