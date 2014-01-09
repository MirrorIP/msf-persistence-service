package de.imc.mirror.persistenceservice;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.AbstractComponent;
import org.xmpp.component.ComponentException;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

import de.imc.mirror.persistenceservice.Space.PersistenceType;
import de.imc.mirror.persistenceservice.config.NamespaceConfig;
import de.imc.mirror.persistenceservice.config.TaskConfig;
import de.imc.mirror.persistenceservice.exceptions.DBAccessException;
import de.imc.mirror.persistenceservice.exceptions.RequestFailureExeption;
import de.imc.mirror.persistenceservice.filters.DataModelFilter;
import de.imc.mirror.persistenceservice.filters.NamespaceFilter;
import de.imc.mirror.persistenceservice.filters.PeriodFilter;
import de.imc.mirror.persistenceservice.filters.PublisherFilter;
import de.imc.mirror.persistenceservice.filters.ReferencesFilter;

/**
 * XMPP component for the MIRROR Persistence Service.
 * @author simon.schwantzer(at)im-c.de
 */
public class PersistenceService extends AbstractComponent {
	private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);
	
	private DatabaseConnector dbConnector;
	private SpaceManager spaceManager;
	
	public PersistenceService(DatabaseConnector dbConnector) {
		this.dbConnector = dbConnector;
		// A cleanup is performed 5 seconds after initialization and repeated with a fix frequency.
		TaskEngine.getInstance().scheduleAtFixedRate(new DataExpirationTask(dbConnector), 5000, TaskConfig.DATA_CLEANUP_PERIOD);
	}
	
	/**
	 * Creates an error IQ response for an IQ request.
	 * @param requestIq IQ to reply error for.
	 * @param packetError Error to reply.
	 */
	private IQ createErrorIQ(IQ requestIq, PacketError packetError) {
		IQ errorIq = new IQ();
		errorIq.setType(Type.error);
		errorIq.setFrom(new JID(requestIq.getTo().getDomain()));
		errorIq.setTo(requestIq.getFrom());
		errorIq.setID(requestIq.getID());
		errorIq.setError(packetError);
		return errorIq;
	}
	
	/**
	 * Creates a simple bad request IQ without details.
	 * @param requestIq IQ to reply error for.
	 * @return Error to reply.
	 */
	private IQ createBadRequestIQ(IQ requestIq) {
		PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.modify);
		return createErrorIQ(requestIq, error);
	}
	
	@Override
	public String getName() {
		return "MIRROR Persistence Service";
	}

	@Override
	public String getDescription() {
		return "Persists data published on pubsub nodes managed by MIRROR spaces.";
	}
	
	@Override
	public void preComponentStart() {
		log.info("Starting MIRROR Persistence Service.");
		spaceManager = new SpaceManager(this);
	}
	
	@Override
	public void postComponentShutdown() {
		log.info("The MIRROR Persistence Service was shutted down.");
	}
	
	@Override
	protected String[] discoInfoFeatureNamespaces() {
		String[] namespaces = {NamespaceConfig.SERVICE};
		return namespaces;
	}
	
	/**
	 * Handles IQs of type set if received from the MIRROR Spaces Service component.
	 * The service sends data objects to store.
	 * @param requestIq IQ package received.
	 */
	@Override
	protected IQ handleIQSet(IQ requestIq) {
		Element childElement = requestIq.getChildElement();
		if (childElement == null) {
			return createBadRequestIQ(requestIq);
		}
		IQSetType setType = IQSetType.getTypeForElementName(childElement.getName());
		switch (setType) {
		case INSERT:
			if (!requestIq.getFrom().equals(spaceManager.getSpacesServiceJID())) {
				// we only accept inserts from the space service
				log.warn("Forbidden access blocked: " + requestIq.getFrom());
				PacketError error = new PacketError(PacketError.Condition.not_allowed, PacketError.Type.cancel);
				return createErrorIQ(requestIq, error);
			} else {
				return handleInsert(requestIq);
			}
		case DELETE:
			return handleDelete(requestIq);
		default:
			return createBadRequestIQ(requestIq);
		}	
	}
	
	/**
	 * Handles a IQ package requesting a data object insert. 
	 * @param insertIq IQ packet containing the request.
	 * @return IQ response.
	 */
	private IQ handleInsert(IQ insertIq) {
		Element childElement = insertIq.getChildElement();
		String spaceId = childElement.attributeValue("spaceId");
		if (spaceId == null || spaceId.trim().isEmpty()) {
			return createBadRequestIQ(insertIq);
		}
		
		Iterator<?> dataObjectIterator = childElement.elementIterator();
		while (dataObjectIterator.hasNext()) {
			DataObject dataObject = new DataObject((Element) dataObjectIterator.next(), spaceId);
			Space space;
			try {
				space = spaceManager.getSpace(spaceId);
				if (space.getPersistenceType() == PersistenceType.DURATION) {
					Date expirationDate = new Date();
					space.getPersistenceDuration().addTo(expirationDate);
					dataObject.setExpirationDate(expirationDate);
				}
				dbConnector.storeDataObject(dataObject);
			} catch (DBAccessException e) {
				PacketError error = new PacketError(PacketError.Condition.internal_server_error, PacketError.Type.wait);
				return createErrorIQ(insertIq, error);
			} catch (DocumentException e) {
				return createBadRequestIQ(insertIq);
			} catch (RequestFailureExeption e) {
				// space not found
				return createErrorIQ(insertIq, e.getPacketError());
			} catch (ComponentException e) {
				PacketError error = new PacketError(PacketError.Condition.internal_server_error, PacketError.Type.wait);
				return createErrorIQ(insertIq, error);
			}
		}
		
		return IQ.createResultIQ(insertIq);
	}
	
	/**
	 * Handles a IQ requesting the deletion of data objects.
	 * @param deleteIq IQ packet containing the request.
	 * @return IQ response.
	 */
	private IQ handleDelete(IQ deleteIq) {
		Iterator<?> childElementIterator = deleteIq.getChildElement().elementIterator();
		if (!childElementIterator.hasNext()) {
			return createBadRequestIQ(deleteIq);
		}
		
		// identify targets
		Element queryTargetElement = (Element) childElementIterator.next();
		QueryTargetType queryTargetType = QueryTargetType.getTypeForElementName(queryTargetElement.getName());
		Set<String> objectIds = new HashSet<String>();
		String objectId;
		switch (queryTargetType) {
		case OBJECT:
			objectId = queryTargetElement.attributeValue("id");
			if (objectId == null || objectId.trim().isEmpty()) {
				return createBadRequestIQ(deleteIq);
			} else {
				objectIds.add(objectId);
			}
			break;
		case MULTIPLE_OBJECTS:
			Iterator<?> objectElementIterator = queryTargetElement.elementIterator();
			while (objectElementIterator.hasNext()) {
				Element objectElement = (Element) objectElementIterator.next();
				objectId = objectElement.attributeValue("id");
				if (objectId == null || objectId.isEmpty() || !objectElement.getName().equals("object")) {
					return createBadRequestIQ(deleteIq);
				}
				objectIds.add(objectId);
			}
			break;
		default:
			return createBadRequestIQ(deleteIq);
		}
		
		// request space identifiers
		int numberOfDeletedObjects;
		try {
			Set<String> spaceIds = dbConnector.retrieveSpacesForObjects(objectIds);
			// check requester authorization to delete all objects
			String requesterBareJID = deleteIq.getFrom().toBareJID();
			for (String spaceId : spaceIds) {
				Space space = spaceManager.getSpace(spaceId);
				if (!space.isModerator(requesterBareJID)) {
					String errorDescription = "Only moderators of a space may delete its data objects.";
					PacketError packetError = new PacketError(PacketError.Condition.not_allowed, PacketError.Type.cancel, errorDescription);
					return createErrorIQ(deleteIq, packetError);
				}
			}
			numberOfDeletedObjects = dbConnector.deleteObjects(objectIds);
		} catch (DBAccessException e) {
			log.warn("Failed to retrieve data from the database.", e);
			PacketError packetError = new PacketError(
					PacketError.Condition.internal_server_error,
					PacketError.Type.wait,
					"Failed to retrieve data.");
			return createErrorIQ(deleteIq, packetError);
		} catch (RequestFailureExeption e) {
			if (e.getPacketError() != null) {
				return createErrorIQ(deleteIq, e.getPacketError());
			} else {
				return createBadRequestIQ(deleteIq);
			}
		} catch (ComponentException e) {
			log.warn("Failed to request space from Spaces Service.", e);
			PacketError packetError = new PacketError(
					PacketError.Condition.internal_server_error,
					PacketError.Type.wait,
					"Failed to request space from Spaces Service.");
			return createErrorIQ(deleteIq, packetError);
		}
		
		IQ response = IQ.createResultIQ(deleteIq);
		Element deleteResultElement = response.setChildElement("delete", NamespaceConfig.SERVICE);
		deleteResultElement.addAttribute("objectsDeleted", Integer.toString(numberOfDeletedObjects));
		response.setChildElement(deleteResultElement);
		return response;
	}

	@Override
	protected IQ handleIQGet(IQ requestIq) {
		Element rootElement = requestIq.getChildElement();
		if (rootElement == null) {
			return createBadRequestIQ(requestIq);
		}
		IQGetType iqType = IQGetType.getTypeForElementName(rootElement.getName()); 
		switch (iqType) {
		case QUERY:
			return handleQuery(requestIq);
		default:
			return createBadRequestIQ(requestIq);
		}
		
	}
	
	/**
	 * Handles a IQ querying data objects.
	 * @param queryIq IQ packet containing the query.
	 * @return Response IQ packet.
	 */
	private IQ handleQuery(IQ queryIq) {
		
		Iterator<?> childElementIterator = queryIq.getChildElement().elementIterator();
		if (!childElementIterator.hasNext()) {
			return createBadRequestIQ(queryIq);
		}
		
		// identify targets
		Element queryTargetElement = (Element) childElementIterator.next();
		QueryTargetType queryTargetType = QueryTargetType.getTypeForElementName(queryTargetElement.getName());
		Set<String> queriedItemIds = new HashSet<String>();
		String itemId, itemElementName;
		switch (queryTargetType) {
		case SPACE:
			itemId = queryTargetElement.attributeValue("id");
			if (itemId == null || itemId.isEmpty()) {
				return createBadRequestIQ(queryIq);
			}
			queriedItemIds.add(itemId);
			break;
		case MULTIPLE_SPACES:
			Iterator<?> spaceElementIterator = queryTargetElement.elementIterator();
			while (spaceElementIterator.hasNext()) {
				Element spaceElement = (Element) spaceElementIterator.next();
				itemElementName = spaceElement.getName();
				itemId = spaceElement.attributeValue("id");
				if (itemId == null || itemId.isEmpty() || !itemElementName.equals("space")) {
					return createBadRequestIQ(queryIq);
				}
				queriedItemIds.add(itemId);
			}
			break;
		case OBJECT:
			itemId = queryTargetElement.attributeValue("id");
			if (itemId == null || itemId.isEmpty()) {
				return createBadRequestIQ(queryIq);
			}
			queriedItemIds.add(itemId);
			break;
		case MULTIPLE_OBJECTS:
			Iterator<?> objectElementIterator = queryTargetElement.elementIterator();
			while (objectElementIterator.hasNext()) {
				Element objectElement = (Element) objectElementIterator.next();
				itemElementName = objectElement.getName();
				itemId = objectElement.attributeValue("id");
				if (itemId == null || itemId.isEmpty() || !itemElementName.equals("object")) {
					return createBadRequestIQ(queryIq);
				}
				queriedItemIds.add(itemId);
			}
			break;
		default:
			return createBadRequestIQ(queryIq);
		}
		
		// generating filters
		FilterSet filterSet;
		if (childElementIterator.hasNext()) {
			Element filterRootElement = (Element) childElementIterator.next();
			try {
				filterSet = createFilterSet(filterRootElement);
			} catch (IllegalArgumentException e) {
				PacketError packetError = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel, e.getMessage());
				return createErrorIQ(queryIq, packetError);
			}
		} else {
			filterSet = new FilterSet();
		}
		
		// request data
		List<DataObject> dataObjects;
		try {
			switch (queryTargetType) {
			case SPACE:
			case MULTIPLE_SPACES:
				dataObjects = handleQueryBySpaces(queryIq.getFrom(), queriedItemIds, filterSet);
				break;
			case OBJECT:
			case MULTIPLE_OBJECTS:
				dataObjects = handleQueryByObjects(queryIq.getFrom(), queriedItemIds, filterSet);
				break;
			default:
				return createBadRequestIQ(queryIq);
			}
		} catch (ComponentException e) {
			log.warn("Failed to request space from Spaces Service.", e);
			PacketError packetError = new PacketError(
					PacketError.Condition.internal_server_error,
					PacketError.Type.wait,
					"Failed to request space from Spaces Service.");
			return createErrorIQ(queryIq, packetError);
		} catch (RequestFailureExeption e) {
			if (e.getPacketError() != null) {
				return createErrorIQ(queryIq, e.getPacketError());
			} else {
				return createBadRequestIQ(queryIq);
			}
		} catch (DBAccessException e) {
			log.warn("Failed to retrieve data from the database.", e);
			PacketError packetError = new PacketError(
					PacketError.Condition.internal_server_error,
					PacketError.Type.wait,
					"Failed to retrieve data.");
			return createErrorIQ(queryIq, packetError);
		}
		
		// create response
		IQ response = IQ.createResultIQ(queryIq);
		Element queryElement = response.setChildElement("query", NamespaceConfig.SERVICE);
		Element resultElement = queryElement.addElement("result");
		for (DataObject dataObject : dataObjects) {
			if (dataObject.getXMLElement() == null) {
				try {
					dataObject.parseElementString();
				} catch (DocumentException e) {
					PacketError packetError = new PacketError(
							PacketError.Condition.internal_server_error,
							PacketError.Type.wait,
							"Failed to parse data object.");
					return createErrorIQ(queryIq, packetError);
				}
			}
			resultElement.add(dataObject.getXMLElement());
		}
		return response;
	}
	
	/**
	 * Creates a filter set based on the query filter element
	 * @param filterRootElement Filter element received with the query.
	 * @return FilterSet containing all filters.
	 * @throws IllegalArgumentException Failed to parse filter.
	 */
	private FilterSet createFilterSet(Element filterRootElement) throws IllegalArgumentException {
		FilterSet filterSet = new FilterSet();
		Iterator<?> filterIterator = filterRootElement.elementIterator();
		while (filterIterator.hasNext()) {
			Element filterElement = (Element) filterIterator.next();
			FilterType filterType = FilterType.getValueForElementName(filterElement.getName());
			switch (filterType) {
			case PERIOD:
				filterSet.addFilter(new PeriodFilter(filterElement));
				break;
			case DATAMODEL:
				filterSet.addFilter(new DataModelFilter(filterElement));
				break;
			case NAMESPACE:
				filterSet.addFilter(new NamespaceFilter(filterElement));
				break;
			case PUBLISHER:
				filterSet.addFilter(new PublisherFilter(filterElement));
				break;
			case REFERENCES:
				filterSet.addFilter(new ReferencesFilter(filterElement));
				break;
			default:
				throw new IllegalArgumentException("Invalid filter definition.");
			}
		}
		return filterSet;
	}
	
	/**
	 * Handles a query for data objects of spaces.
	 * @param requester JID of the requester. Used to check authorization.
	 * @param spaceIds Set of space identifiers to retrieve data objects of.
	 * @param filterSet Filter set to apply.
	 * @return List of all data objects of the spaces which satisfy the filter criteria.
	 * @throws ComponentException The communication with the Spaces Service failed.
	 * @throws RequestFailureExeption The space request returned an error, e.g., the user is not member of all spaces.
	 * @throws DBAccessException Failed to retrieve data from the database.
	 */
	private List<DataObject> handleQueryBySpaces(JID requester, Set<String> spaceIds, FilterSet filterSet) throws ComponentException, RequestFailureExeption, DBAccessException {
		List<DataObject> dataObjects = new ArrayList<DataObject>();
		String requesterBareJID = requester.toBareJID();
		for (String spaceId : spaceIds) {
			Space space = spaceManager.getSpace(spaceId);
			// check if the requester has the permission to access all requested spaces
			if (!space.isMember(requesterBareJID)) {
				String errorDescription = "Only members of a space may access published data objects.";
				PacketError packetError = new PacketError(PacketError.Condition.not_allowed, PacketError.Type.cancel, errorDescription);
				throw new RequestFailureExeption(errorDescription, packetError);
			}
			// request data objects matching the filter set
			dataObjects.addAll(dbConnector.retrieveObjectsForSpace(space, filterSet));
		}
		return dataObjects;
	}
	
	/**
	 * Handles a query for data objects.
	 * @param requester JID of the requester. Used to check authorization.
	 * @param objectIds Set of identifiers for the data objects to retrieve.  
	 * @param filterSet Filter set to apply.
	 * @return List of all data objects which satisfy the filter criteria.
	 * @throws ComponentException The communication with the Spaces Service failed.
	 * @throws RequestFailureExeption The space request returned an error, e.g., the user is not member of all spaces.
	 * @throws DBAccessException Failed to retrieve data from the database.
	 */
	private List<DataObject> handleQueryByObjects(JID requester, Set<String> objectIds, FilterSet filterSet) throws ComponentException, RequestFailureExeption, DBAccessException {
		// request data objects
		List<DataObject> dataObjects = dbConnector.retrieveObjects(objectIds, filterSet);
		// check requester authorization to access these objects
		Set<String> spaceIds = new HashSet<String>();
		for (DataObject dataObject : dataObjects) {
			spaceIds.add(dataObject.getSpaceId());
		}
		String requesterBareJID = requester.toBareJID();
		for (String spaceId : spaceIds) {
			Space space = spaceManager.getSpace(spaceId);
			if (!space.isMember(requesterBareJID)) {
				String errorDescription = "Only members of a space may access published data objects.";
				PacketError packetError = new PacketError(PacketError.Condition.not_allowed, PacketError.Type.cancel, errorDescription);
				throw new RequestFailureExeption(errorDescription, packetError);
			}
		}
		return dataObjects;
	}

	/**
	 * Listening for message containing space creation and configuration events.
	 * @param message XMPP message received.
	 */
	@Override
	protected void handleMessage(Message message) {
		if (!message.getFrom().equals(spaceManager.getSpacesServiceJID())) {
			// we only accept notifications from the spaces service
			return;
		}
		
		if (message.getType() != Message.Type.headline) {
			// we only consume notifications
			return;
		}
		
		Element eventElement = message.getChildElement("event", NamespaceConfig.SPACES_EVENT);
		if (eventElement == null) {
			// we're not interested
			return;
		}
		Iterator<?> eventIterator = eventElement.elementIterator();
		while (eventIterator.hasNext()) {
			Element singleEventElement = (Element) eventIterator.next();
			String spaceId = singleEventElement.attributeValue("space");
			SpacesEventType singleEventType = SpacesEventType.getTypeForElementName(singleEventElement.getName());
			switch (singleEventType) {
			case CREATE:
			case CONFIGURE:
				DataForm configurationForm = new DataForm(singleEventElement.element("x"));
				Space space = new Space(spaceId, configurationForm);
				switch (space.getPersistenceType()) {
				case OFF:
					try {
						// delete all data objects stored for this space
						dbConnector.deleteObjectsForSpace(spaceId);
					} catch (DBAccessException e) {
						log.warn("Failed to delete data objects of space.", e);
					}
					spaceManager.deleteSpace(spaceId);
					break;
				default:
					spaceManager.setSpace(space);
				}
				break;
			case DELETE:
				try {
					// delete all data objects stored for this space
					dbConnector.deleteObjectsForSpace(spaceId);
				} catch (DBAccessException e) {
					log.warn("Failed to delete data objects of space.", e);
				}
				spaceManager.deleteSpace(spaceId);
			default:
				return;
			}
		}
	}
	
}
