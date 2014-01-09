package de.imc.mirror.persistenceservice;

/**
 * Model for space members.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class SpaceMember {
	public enum Role {
		MEMBER,
		MODERATOR;
	}
	
	private String jid;
	private Role role;
	
	/**
	 * Creates a space member.
	 * @param jid JID of the member as string.
	 * @param role Role of the member, i.e., <code>Role.MEMBER</code> or <code>Role.MODERATOR</code>.
	 */
	public SpaceMember(String jid, Role role) {
		this.jid = jid;
		this.role = role;
	}
	
	/**
	 * Returns the JID of the member.
	 * @return JID of the member as string.
	 */
	public String getJIDString() {
		return this.jid;
	}
	
	/**
	 * Returns the role of the member.
	 * @return <code>Role.MEMBER</code> or <code>Role.MODERATOR</code>.
	 */
	public Role getRole() {
		return this.role;
	}

}
