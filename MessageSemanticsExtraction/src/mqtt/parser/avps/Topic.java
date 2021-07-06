package mqtt.parser.avps;

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

public class Topic
{
	private static final String SEPARATOR = ":";

	protected Text name;
	protected QoS qos;

	public Topic()
	{

	}

	public Topic(Text name, QoS qos)
	{
		this.name = name;
		this.qos = qos;
	}

	public TopicExt ext() 
	{
		return new TopicExt(name, qos);
	}
	
	public String toString()
	{
		return name.toString() + SEPARATOR + qos;
	}

	public Text getName()
	{
		return name;
	}

	public void setName(Text name)
	{
		this.name = name;
	}

	public QoS getQos()
	{
		return qos;
	}

	public void setQos(QoS qos)
	{
		this.qos = qos;
	}

	public int length()
	{
		return name.length();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Topic other = (Topic) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		return true;
	}

	public static Topic valueOf(Text topic, QoS qos)
	{
		return new Topic(topic, qos);
	}

	public static class TopicExt extends Topic
	{
		public TopicExt()
		{
			super();
		}

		public TopicExt(Text name, QoS qos)
		{
			super(name, qos);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((qos == null) ? 0 : qos.hashCode());
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
			Topic other = (Topic) obj;
			if (name == null)
			{
				if (other.name != null)
					return false;
			}
			else if (!name.equals(other.name))
				return false;
			if (qos != other.qos)
				return false;
			return true;
		}
	}
}
