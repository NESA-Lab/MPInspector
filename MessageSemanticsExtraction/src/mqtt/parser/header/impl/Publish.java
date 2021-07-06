package mqtt.parser.header.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import mqtt.parser.avps.MessageType;
import mqtt.parser.avps.Topic;
import mqtt.parser.header.api.CountableMessage;
import mqtt.parser.header.api.MQDevice;
import mqtt.parser.util.ByteBufDeserializer;
import mqtt.parser.util.ByteBufSerializer;

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

import io.netty.buffer.ByteBuf;

public class Publish extends CountableMessage
{
	private Topic topic;
	private ByteBuf content;
	private boolean retain;
	private boolean dup;

	public Publish()
	{
		super();
	}

	public Publish(Topic topic, ByteBuf content, boolean retain, boolean dup)
	{
		this(null, topic, content, retain, dup);
	}

	public Publish reInit(Integer packetID, Topic topic, ByteBuf content, boolean retain, boolean dup)
	{
		super.reInit(packetID);
		this.topic = topic;
		this.content = content;
		this.retain = retain;
		this.dup = dup;
		return this;
	}

	public Publish(Integer packetID, Topic topic, ByteBuf content, boolean retain, boolean dup)
	{
		super(packetID);
		this.topic = topic;
		this.content = content;
		this.retain = retain;
		this.dup = dup;
	}

	@Override
	public MessageType getType()
	{
		return MessageType.PUBLISH;
	}

	@Override
	public void processBy(MQDevice device)
	{
		device.processPublish(getPacketID(), topic, content, retain, dup);
	}

	@Override
	public int getLength()
	{
		int length = 0;
		length += getPacketID() != null ? 2 : 0;
		length += topic.length() + 2;
		length += content.readableBytes();
		return length;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + (dup ? 1231 : 1237);
		result = prime * result + (retain ? 1231 : 1237);
		result = prime * result + ((topic == null) ? 0 : topic.ext().hashCode());
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
		Publish other = (Publish) obj;
		if (content == null)
		{
			if (other.content != null)
				return false;
		}
		else if (!content.equals(other.content))
			return false;
		if (dup != other.dup)
			return false;
		if (retain != other.retain)
			return false;
		if (topic == null)
		{
			if (other.topic != null)
				return false;
		}
		else if (other.topic == null)
			return false;
		else if (!topic.ext().equals(other.topic.ext()))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Publish [topic=" + topic + ", content=" + content + ", retain=" + retain + ", dup=" + dup + ", getPacketID()=" + getPacketID() + "]";
	}

	public Topic getTopic()
	{
		return topic;
	}

	public void setTopic(Topic topic)
	{
		this.topic = topic;
	}

	@JsonSerialize(using = ByteBufSerializer.class)
	@JsonDeserialize(using = ByteBufDeserializer.class)
	public ByteBuf getContent()
	{
		return content;
	}

	public void setContent(ByteBuf buf)
	{
		this.content = buf;
	}

	public boolean isRetain()
	{
		return retain;
	}

	public void setRetain(boolean retain)
	{
		this.retain = retain;
	}

	public boolean isDup()
	{
		return dup;
	}

	public void setDup(boolean dup)
	{
		this.dup = dup;
	}

}
