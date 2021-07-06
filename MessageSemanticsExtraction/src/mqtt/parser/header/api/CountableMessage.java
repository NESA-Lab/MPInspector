package mqtt.parser.header.api;

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

public abstract class CountableMessage extends MQMessage
{
	private Integer packetID;

	public CountableMessage()
	{

	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((packetID == null) ? 0 : packetID.hashCode());
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
		CountableMessage other = (CountableMessage) obj;
		if (packetID == null)
		{
			if (other.packetID != null)
				return false;
		}
		else if (!packetID.equals(other.packetID))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "packetID=" + packetID;
	}

	public CountableMessage(Integer packetID)
	{
		this.packetID = packetID;
	}

	public CountableMessage reInit(Integer packetID)
	{
		this.packetID = packetID;
		return this;
	}

	public Integer getPacketID()
	{
		return packetID;
	}

	public void setPacketID(Integer packetID)
	{
		this.packetID = packetID;
	}
}
