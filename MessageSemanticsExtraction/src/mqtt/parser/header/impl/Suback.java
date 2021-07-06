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

import java.util.List;

import mqtt.parser.avps.MessageType;
import mqtt.parser.avps.SubackCode;
import mqtt.parser.header.api.CountableMessage;
import mqtt.parser.header.api.MQDevice;

public class Suback extends CountableMessage
{
	private List<SubackCode> returnCodes;

	public Suback()
	{
		super();
	}

	public Suback(Integer packetID, List<SubackCode> returnCodes)
	{
		super(packetID);
		this.returnCodes = returnCodes;
	}

	public Suback reInit(Integer packetID, List<SubackCode> returnCodes)
	{
		super.reInit(packetID);
		this.returnCodes = returnCodes;
		return this;
	}

	@Override
	public int getLength()
	{
		return 2 + returnCodes.size();
	}

	@Override
	public MessageType getType()
	{
		return MessageType.SUBACK;
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processSuback(getPacketID(), returnCodes);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((returnCodes == null) ? 0 : returnCodes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Suback other = (Suback) obj;
		if (returnCodes == null)
		{
			if (other.returnCodes != null)
				return false;
		}
		else if (!returnCodes.equals(other.returnCodes))
			return false;
		return true;
	}

	public List<SubackCode> getReturnCodes()
	{
		return returnCodes;
	}

	@Override
	public String toString()
	{
		return "Suback [returnCodes=" + returnCodes + ", getPacketID()=" + getPacketID() + "]";
	}

	public void setReturnCodes(List<SubackCode> returnCodes)
	{
		this.returnCodes = returnCodes;
	}
}
