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
import mqtt.parser.avps.Topic;
import mqtt.parser.avps.Topic.TopicExt;
import mqtt.parser.header.api.CountableMessage;
import mqtt.parser.header.api.MQDevice;

public class Subscribe extends CountableMessage
{
	private Topic[] topics;

	public Subscribe()
	{
		super();
	}

	public Subscribe(Topic[] topics)
	{
		this(null, topics);
	}

	public Subscribe(Integer packetID, Topic[] topics)
	{
		super(packetID);
		this.topics = topics;
	}

	public Subscribe reInit(Integer packetID, Topic[] topics)
	{
		super.reInit(packetID);
		this.topics = topics;
		return this;
	}

	@Override
	public int getLength()
	{
		int length = 0;
		length += getPacketID() != null ? 2 : 0;
		for (Topic s : this.topics)
			length += s.getName().length() + 3;
		return length;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		int topicsHash = 0;
		if (topics != null)
		{
			TopicExt[] topicsExt = new TopicExt[topics.length];
			for (int i = 0; i < topics.length; i++)
				topicsExt[i] = topics[i].ext();
			topicsHash = Arrays.hashCode(topicsExt);
		}
		result = prime * result + topicsHash;
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
		Subscribe other = (Subscribe) obj;
		TopicExt[] topicsExt = null;
		if (topics != null)
		{
			topicsExt = new TopicExt[topics.length];
			for (int i = 0; i < topics.length; i++)
				topicsExt[i] = topics[i].ext();
		}
		TopicExt[] otherTopicsExt = null;
		if (other.topics != null)
		{
			otherTopicsExt = new TopicExt[other.topics.length];
			for (int i = 0; i < other.topics.length; i++)
				otherTopicsExt[i] = other.topics[i].ext();
		}
		if (!Arrays.equals(topicsExt, otherTopicsExt))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Subscribe [topics=" + Arrays.toString(topics) + ", getPacketID()=" + getPacketID() + "]";
	}

	@Override
	public MessageType getType()
	{
		return MessageType.SUBSCRIBE;
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processSubscribe(getPacketID(), topics);
	}

	public Topic[] getTopics()
	{
		return topics;
	}

	public void setTopics(Topic[] topics)
	{
		this.topics = topics;
	}
}
