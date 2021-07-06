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

import mqtt.parser.avps.ConnackCode;
import mqtt.parser.avps.MessageType;
import mqtt.parser.header.api.MQDevice;
import mqtt.parser.header.api.MQMessage;

public class Connack extends MQMessage
{
	private boolean sessionPresent;
	private ConnackCode returnCode;

	public Connack()
	{
	}

	public Connack(boolean sessionPresent, ConnackCode returnCode)
	{
		this.sessionPresent = sessionPresent;
		this.returnCode = returnCode;
	}

	public Connack reInit(boolean sessionPresent, ConnackCode returnCode)
	{
		this.sessionPresent = sessionPresent;
		this.returnCode = returnCode;
		return this;
	}

	@Override
	public int getLength()
	{
		return 2;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((returnCode == null) ? 0 : returnCode.hashCode());
		result = prime * result + (sessionPresent ? 1231 : 1237);
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
		Connack other = (Connack) obj;
		if (returnCode != other.returnCode)
			return false;
		if (sessionPresent != other.sessionPresent)
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Connack [sessionPresent=" + sessionPresent + ", returnCode=" + returnCode + "]";
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processConnack(returnCode, sessionPresent);
	}

	@Override
	public MessageType getType()
	{
		return MessageType.CONNACK;
	}

	public boolean isSessionPresent()
	{
		return sessionPresent;
	}

	public void setSessionPresent(boolean sessionPresent)
	{
		this.sessionPresent = sessionPresent;
	}

	public ConnackCode getReturnCode()
	{
		return returnCode;
	}

	public void setReturnCode(ConnackCode returnCode)
	{
		this.returnCode = returnCode;
	}
}
