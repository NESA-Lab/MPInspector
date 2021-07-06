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

import mqtt.parser.avps.MessageType;
import mqtt.parser.header.api.CountableMessage;
import mqtt.parser.header.api.MQDevice;

public class Pubrec extends CountableMessage
{
	public Pubrec()
	{
		super();
	}

	public Pubrec(Integer packetID)
	{
		super(packetID);
	}

	@Override
	public Pubrec reInit(Integer packetID)
	{
		super.reInit(packetID);
		return this;
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processPubrec(getPacketID());
	}

	@Override
	public int getLength()
	{
		return 2;
	}

	@Override
	public MessageType getType()
	{
		return MessageType.PUBREC;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return super.equals(obj);
	}
	
	@Override
	public String toString()
	{
		return getType() + " [getPacketID()=" + getPacketID() + "]";
	}
}
