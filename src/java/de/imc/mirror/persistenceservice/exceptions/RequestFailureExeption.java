package de.imc.mirror.persistenceservice.exceptions;

import org.xmpp.packet.PacketError;

/**
 * Exception thrown when a communication error occurs.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class RequestFailureExeption extends Exception {
	private static final long serialVersionUID = 1L;
	
	private PacketError packetError;
	
	public RequestFailureExeption(String message) {
		super(message);
	}
	
	public RequestFailureExeption(String message, PacketError packetError) {
		super(message);
		this.packetError = packetError;
	}
	
	public PacketError getPacketError() {
		return packetError;
	}

}
