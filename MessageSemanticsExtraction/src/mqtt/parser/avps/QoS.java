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

public enum QoS
{
	AT_MOST_ONCE((byte) 0), AT_LEAST_ONCE((byte) 1), EXACTLY_ONCE((byte) 2);

	private byte value;

	private static final Map<Integer, QoS> intToTypeMap = new HashMap<Integer, QoS>();
	private static final Map<String, QoS> strToTypeMap = new HashMap<String, QoS>();

	static
	{
		for (QoS type : QoS.values())
		{
			intToTypeMap.put((int) type.value, type);
			strToTypeMap.put(type.name(), type);
		}
	}

    @JsonValue
	public int getValue()
	{
		return value;
	}

	private QoS(final byte leg)
	{
		value = leg;
	}

	@JsonCreator
	public static QoS valueOf(int type) throws MalformedMessageException
	{
		return intToTypeMap.get(type);
	}

	public static QoS calculate(final QoS subscriberQos, final QoS publisherQos)
	{
		if (subscriberQos.getValue() == publisherQos.getValue())
			return subscriberQos;

		if (subscriberQos.getValue() > publisherQos.getValue())
			return publisherQos;
		else
			return subscriberQos;
	}
}
