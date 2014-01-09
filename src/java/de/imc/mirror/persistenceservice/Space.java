package de.imc.mirror.persistenceservice;

import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

import de.imc.mirror.persistenceservice.SpaceMember.Role;

/**
 * Data model for the service relevant space information.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class Space {
	public enum PersistenceType {
		ON, OFF, DURATION;
	}
	
	private final String id;
	private final Set<SpaceMember> members;
	private PersistenceType persistenceType;
	private Duration persistenceDuration;

	/**
	 * Creates the model based on a response from the spaces service.
	 * @param id Identifier for the space.
	 * @param dataForm XMPP data form containing the space configuration.
	 */
	public Space(String id, DataForm dataForm) {
		this.id = id;
		this.members = new HashSet<SpaceMember>();
		FormField membersFormField = dataForm.getField("spaces#members");
		FormField moderatorsFormField = dataForm.getField("spaces#moderators");
		
		for (String membersFormFieldValue : membersFormField.getValues()) {
			SpaceMember newMember;
			if (moderatorsFormField.getValues().contains(membersFormFieldValue)) {
				newMember = new SpaceMember(membersFormFieldValue, SpaceMember.Role.MODERATOR);
			} else {
				newMember = new SpaceMember(membersFormFieldValue, SpaceMember.Role.MEMBER);
			}
			this.members.add(newMember);
		}
		moderatorsFormField.getValues();
		
		persistenceDuration = null; // default
		persistenceType = PersistenceType.OFF; // default
		FormField persistentField = dataForm.getField("spaces#persistent");
		if (persistentField != null) {
			String persistentValue = persistentField.getFirstValue();
			if (persistentValue.equalsIgnoreCase("true") || persistentValue.equals("1")) {
				persistenceType = PersistenceType.ON;
			} else {
				try {
					persistenceDuration = DatatypeFactory.newInstance().newDuration(persistentValue);
					persistenceType = PersistenceType.DURATION;
				} catch (Exception e) {
					// Failed to parse duration.
				}
			}
		}
	}
	
	/**
	 * Returns the ID of this space.
	 * @return Space identifier.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Checks if the given user is member of the space.
	 * @param bareJIDString Bare-JID of a xmpp user as string.
	 * @return <code>true</code> is the user is member of the space, otherwise <code>false</code>.
	 */
	public boolean isMember(String bareJIDString) {
		for (SpaceMember member : members) {
			if (member.getJIDString().equals(bareJIDString)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if the given user is moderator of the space.
	 * @param bareJIDString Bare-JID of a xmpp user as string.
	 * @return <code>true</code> if the user is member and moderator of the space, otherwise <code>false</code>.
	 */
	public boolean isModerator(String bareJIDString) {
		for (SpaceMember member : members) {
			if (member.getJIDString().equals(bareJIDString) && member.getRole() == Role.MODERATOR) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the persistence type of the space.
	 * @return <code>Space.PersistenceType.ON</code>, <code>Space.PersistenceType.OFF</code>, or <code>Space.PersistenceType.DURATION</code>. 
	 */
	public PersistenceType getPersistenceType() {
		return persistenceType;
	}
	
	/**
	 * Returns the persistence duration.
	 * @return Persistence duration or <code>null</code> if the persistence type is not <code>Space.PersistenceType.DURATION</code>.
	 */
	public Duration getPersistenceDuration() {
		return persistenceDuration;
	}
}
