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

public enum ConnackCode
{
	ACCEPTED(0), UNACCEPTABLE_PROTOCOL_VERSION(1), IDENTIFIER_REJECTED(2), SERVER_UNUVALIABLE(3), BAD_USER_OR_PASS(4), NOT_AUTHORIZED(5);

	private int num;

	private static Map<Integer, ConnackCode> map = new HashMap<Integer, ConnackCode>();

	static
	{
		for (ConnackCode legEnum : ConnackCode.values())
		{
			map.put(legEnum.num, legEnum);
		}
	}

    @JsonValue
	public byte getNum()
	{
		return (byte) num;
	}

	private ConnackCode(final int leg)
	{
		num = leg;
	}

	@JsonCreator
	public static ConnackCode valueOf(int type)
	{
		return map.get(type);
	}

}
