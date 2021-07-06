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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import mqtt.parser.exceptions.MalformedMessageException;

public enum MessageType
{
	CONNECT(1), CONNACK(2), PUBLISH(3), PUBACK(4), PUBREC(5), PUBREL(6), PUBCOMP(7), SUBSCRIBE(8), SUBACK(9), UNSUBSCRIBE(10), UNSUBACK(11), PINGREQ(12), PINGRESP(13), DISCONNECT(14);

	private int num;

	private static Map<Integer, MessageType> map = new HashMap<Integer, MessageType>();

	static
	{
		for (MessageType legEnum : MessageType.values())
		{
			map.put(legEnum.num, legEnum);
		}
	}

    @JsonValue
	public int getNum()
	{
		return num;
	}

	private MessageType(final int leg)
	{
		num = leg;
	}

	@JsonCreator
	public static MessageType valueOf(int type) throws MalformedMessageException
	{
		MessageType result = map.get(type);
		if (result == null)
			throw new MalformedMessageException(String.format("Header code undefined: %d", type));
		return result;
	}
}