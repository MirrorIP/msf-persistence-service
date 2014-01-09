package de.imc.mirror.persistenceservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.AbstractComponent;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.PacketError.Condition;

import de.imc.mirror.persistenceservice.config.ComponentConfig;
import de.imc.mirror.persistenceservice.exceptions.RequestFailureExeption;

/**
 * The space manager is responsible fetching, buffering, and retrieving of spaces.
 * @author simon.schwantzer(at)im-c.de
 */
public class SpaceManager {
	private static final Logger log = LoggerFactory.getLogger(SpaceManager.class);
	
	private AbstractComponent serviceComponent;
	// map of <spaceId, space>
	private Map<String, Space> spaces;
	private JID spacesServiceJID;
	
	/**
	 * Creates a new space manager.
	 * @param serviceComponent Component of the MIRROR Persistence Service.
	 */
	public SpaceManager(AbstractComponent serviceComponent) {
		this.serviceComponent = serviceComponent;
		this.spaces = new HashMap<String, Space>();
		this.spacesServiceJID = new JID(ComponentConfig.SPACES_SERVICE_SUBDOMAIN + "." + XMPPServer.getInstance().getServerInfo().getXMPPDomain());
	}
	
	/*
	private JID requestSpacesServiceJID() throws ComponentException, EntityNotFoundException {
		QName queryElementName = new QName("query", Namespace.get("http://jabber.org/protocol/disco#items"));
		Element rootElement = DocumentHelper.createElement(queryElementName);
		IQ discoItemsIq = new IQ(rootElement);
		discoItemsIq.setType(IQ.Type.get);
		discoItemsIq.setFrom(serviceComponent.getJID());
		discoItemsIq.setTo(serviceComponent.getDomain());
		
		IQ responseIq = ComponentManagerFactory.getComponentManager().query(serviceComponent, discoItemsIq, 1000);
		
		if (responseIq != null) {
			Element resultQueryElement = responseIq.getChildElement();
			List<?> resultItemElements = resultQueryElement.elements("item");
			for (Object resultItemElementObject : resultItemElements) {
				Element resultItemElement = (Element) resultItemElementObject;
				Attribute nameAttr = resultItemElement.attribute("name");
				Attribute jidAttr = resultItemElement.attribute("jid");
				if (nameAttr != null && nameAttr.getValue().equalsIgnoreCase(ComponentConfig.SPACES_SERVICE_NAME)) {
					if (jidAttr != null) {
						JID spacesServiceJID = new JID(jidAttr.getValue());
						log.info("Using MIRROR Spaces Service at " + spacesServiceJID.toString() + ".");
						return spacesServiceJID;
					}
				}
			}
		}
		throw new EntityNotFoundException("Failed to locate the MIRROR Spaces Service component!");
	}
	*/
	
	/**
	 * Returns the JID of the MIRROR Spaces Service.
	 * @return JID of the service component. 
	 */
	protected JID getSpacesServiceJID() {
		return spacesServiceJID;
	}
	
	/**
	 * Requests a space from the spaces service.
	 * @param spaceId Space identifier.
	 * @return Space object.
	 * @throws ComponentException Communication with spaces service failed.
	 * @throws RequestFailureExeption A space with the given identifier is not available.
	 */
	private Space requestSpace(String spaceId) throws ComponentException, RequestFailureExeption {
		Space space;
		Element queryElement = DocumentHelper.createElement(
				new QName("query", Namespace.get("http://jabber.org/protocol/disco#info"))
		);
		queryElement.addAttribute("node", spaceId);
		IQ queryIq = new IQ();
		queryIq.setType(IQ.Type.get);
		queryIq.setFrom(serviceComponent.getJID());
		queryIq.setTo(spacesServiceJID);
		queryIq.setID(UUID.randomUUID().toString());
		queryIq.setChildElement(queryElement);
		IQ responseIq = ComponentManagerFactory.getComponentManager().query(serviceComponent, queryIq, 500);
		switch (responseIq.getType()) {
		case result:
			Element queryResponseElement = responseIq.getChildElement();
			DataForm dataForm = new DataForm(queryResponseElement.element("x"));
			space = new Space(spaceId, dataForm); 
			break;
		case error:
			if (responseIq.getError().getCondition() == Condition.item_not_found) {
				PacketError error = new PacketError(responseIq.getError().getCondition(), responseIq.getError().getType(), "Space '" + spaceId + "' does not exist.");
				throw new RequestFailureExeption("Space not found.", error);				
			}
		default:
			log.warn("Unexpected request error: " + responseIq.getError().toString());
			throw new ComponentException("Unexpected request error: " + responseIq.getError().toString());
		}
		return space;
	}
	
	/**
	 * Returns the space for the given id.
	 * @param spaceId Identifier for the space to get.
	 * @return Space with the given identifier.
	 * @throws RequestFailureExeption Failed to retrieve space.
	 * @throws ComponentException Failed to communicate with the Spaces Service.
	 */
	public Space getSpace(String spaceId) throws RequestFailureExeption, ComponentException {
		Space space = spaces.get(spaceId);
		if (space != null) {
			return space;
		} else {
			// request space from spaces service
			space = requestSpace(spaceId);
			return space;
		}
		
	}
	
	/**
	 * Sets the space by either adding it to the buffer or replacing it.
	 * @param space Space to set.
	 */
	public void setSpace(Space space) {
		spaces.put(space.getId(), space);
	}
	
	/**
	 * Removes the space from the buffer. 
	 * @param spaceId ID of the space to delete.
	 */
	public void deleteSpace(String spaceId) {
		spaces.remove(spaceId);
	}
}
