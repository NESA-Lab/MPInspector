package mqtt.parser.header.impl;

/**
 * Mobius Software LTD
 * Copyright 2015-2016, Mobius Software LTD
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import mqtt.parser.avps.MessageType;
import mqtt.parser.avps.Will;
import mqtt.parser.header.api.MQDevice;
import mqtt.parser.header.api.MQMessage;

public class Connect extends MQMessage
{
	public static final byte defaultProtocolLevel = 4;
	public static final String PROTOCOL_NAME = "MQTT";

	private String username;
	private String password;
	private String clientID;

	private byte protocolLevel = defaultProtocolLevel;
	private boolean cleanSession;
	private int keepalive;

	private Will will;

	public Connect()
	{

	}

	public Connect(String username, String password, String clientID, boolean isClean, int keepalive, Will will)
	{
		this.username = username;
		this.password = password;
		this.clientID = clientID;
		this.cleanSession = isClean;
		this.keepalive = keepalive;
		this.will = will;
		this.protocolLevel = defaultProtocolLevel;
	}

	public Connect reInit(String username, String password, String clientID, boolean isClean, int keepalive, Will will)
	{
		this.username = username;
		this.password = password;
		this.clientID = clientID;
		this.cleanSession = isClean;
		this.keepalive = keepalive;
		this.will = will;
		this.protocolLevel = defaultProtocolLevel;
		return this;
	}

	@Override
	public MessageType getType()
	{
		return MessageType.CONNECT;
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processConnect(cleanSession, keepalive, will);
	}

	@Override
	public int getLength()
	{
		int length = 10;
		length += clientID.length() + 2;
		length += isWillFlag() ? will.retrieveLength() : 0;
		length += username != null ? username.length() + 2 : 0;
		length += password != null ? password.length() + 2 : 0;
		return length;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (cleanSession ? 1231 : 1237);
		result = prime * result + ((clientID == null) ? 0 : clientID.hashCode());
		result = prime * result + keepalive;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + protocolLevel;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		result = prime * result + ((will == null) ? 0 : will.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connect other = (Connect) obj;
		if (cleanSession != other.cleanSession)
			return false;
		if (clientID == null)
		{
			if (other.clientID != null)
				return false;
		}
		else if (!clientID.equals(other.clientID))
			return false;
		if (keepalive != other.keepalive)
			return false;
		if (password == null)
		{
			if (other.password != null)
				return false;
		}
		else if (!password.equals(other.password))
			return false;
		if (protocolLevel != other.protocolLevel)
			return false;
		if (username == null)
		{
			if (other.username != null)
				return false;
		}
		else if (!username.equals(other.username))
			return false;
		if (will == null)
		{
			if (other.will != null)
				return false;
		}
		else if (!will.equals(other.will))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Connect [username=" + username + ", password=" + password + ", clientID=" + clientID + ", protocolLevel=" + protocolLevel + ", cleanSession=" + cleanSession + ", keepalive=" + keepalive + ", will=" + will + "]";
	}

	public int getProtocolLevel()
	{
		return protocolLevel;
	}

	public void setProtocolLevel(int protocolLevel)
	{
		this.protocolLevel = (byte) protocolLevel;
	}

	public boolean isCleanSession()
	{
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession)
	{
		this.cleanSession = cleanSession;
	}

	public boolean isWillFlag()
	{
		return will != null;
	}

	public Will getWill()
	{
		return will;
	}

	public void setWill(Will will)
	{
		this.will = will;
	}

	public int getKeepalive()
	{
		return keepalive;
	}

	public void setKeepalive(int keepalive)
	{
		this.keepalive = keepalive;
	}

	public String getClientID()
	{
		return clientID;
	}

	public void setClientID(String clientID)
	{
		this.clientID = clientID;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public boolean isUsernameFlag()
	{
		return username != null;
	}

	public boolean isPasswordFlag()
	{
		return password != null;
	}

	@JsonProperty("protocolName")
	public String getName()
	{
		return PROTOCOL_NAME;
	}
}
