package mqtt.parser.header.impl;

import java.util.Arrays;

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
import mqtt.parser.avps.Text;
import mqtt.parser.header.api.CountableMessage;
import mqtt.parser.header.api.MQDevice;

public class Unsubscribe extends CountableMessage
{
	private Text[] topics;

	public Unsubscribe()
	{
		super();
	}

	public Unsubscribe(Text[] topics)
	{
		this(null, topics);
	}

	public Unsubscribe(Integer packetID, Text[] topics)
	{
		super(packetID);
		this.topics = topics;
	}

	public Unsubscribe reInit(Integer packetID, Text[] topics)
	{
		super.reInit(packetID);
		this.topics = topics;
		return this;
	}

	@Override
	public int getLength()
	{
		int length = 2;
		for (Text topic : topics)
			length += topic.length() + 2;
		return length;
	}

	@Override
	public MessageType getType()
	{
		return MessageType.UNSUBSCRIBE;
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processUnsubscribe(getPacketID(), topics);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(topics);
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
		Unsubscribe other = (Unsubscribe) obj;
		if (!Arrays.equals(topics, other.topics))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Unsubscribe [topics=" + Arrays.toString(topics) + ", getPacketID()=" + getPacketID() + "]";
	}

	public Text[] getTopics()
	{
		return topics;
	}

	public void setTopics(Text[] topics)
	{
		this.topics = topics;
	}
}
