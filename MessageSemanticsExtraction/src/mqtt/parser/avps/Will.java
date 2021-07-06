package mqtt.parser.avps;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

public class Will
{
	private Topic topic;
	private byte[] content;
	private boolean retain;

	public Will()
	{

	}

	public Will(Topic topic, byte[] content, boolean retain)
	{
		this.topic = topic;
		this.content = content;
		this.retain = retain;
	}

	public int retrieveLength()
	{
		return topic.length() + content.length + 4;
	}

	public Topic getTopic()
	{
		return topic;
	}

	public void setTopic(Topic topic)
	{
		this.topic = topic;
	}

	public byte[] getContent()
	{
		return content;
	}

	public void setContent(byte[] content)
	{
		this.content = content;
	}

	public boolean isRetain()
	{
		return retain;
	}

	public void setRetain(boolean retain)
	{
		this.retain = retain;
	}

	@JsonIgnore
	public boolean isValid()
	{
		return this.topic != null && this.topic.length() > 0 && this.content != null && this.topic.getQos() != null;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(content);
		result = prime * result + (retain ? 1231 : 1237);
		result = prime * result + ((topic == null) ? 0 : topic.ext().hashCode());
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
		Will other = (Will) obj;
		if (!Arrays.equals(content, other.content))
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
}
