package de.imc.mirror.persistenceservice;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for a set of filters. 
 * @author simon.schwantzer(at)im-c.de
 */
public class FilterSet {
	private static final Logger log = LoggerFactory.getLogger(FilterSet.class);
	
	Set<Filter> filters;
	
	/**
	 * Creates a new filter set.
	 */
	public FilterSet() {
		filters = new HashSet<Filter>();
	}
	
	/**
	 * Adds a filter to the set.
	 * @param filter Filter to add.
	 */
	public void addFilter(Filter filter) {
		this.filters.add(filter);
	}
	
	/**
	 * Returns all filters.
	 * @return Unmodifiable set of filters.
	 */
	public Set<Filter> getFilters() {
		return Collections.unmodifiableSet(filters);
	}
	
	/**
	 * Applies all filters to the given data object.
	 * @param dataObject Data object to apply filters to.
	 * @return <code>true</code> if the data object validates against all filters, otherwise <code>false</code>.
	 */
	public boolean isValid(DataObject dataObject) {
		try {
			for (Filter filter : filters) {
				if (!filter.isDataObjectValid(dataObject)) return false;
			}
		} catch (DocumentException e) {
			log.warn("Failed to parse XML element of object.", e);
			return false;
		}
		return true;
	}
}
