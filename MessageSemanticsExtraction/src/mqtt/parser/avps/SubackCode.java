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

public enum SubackCode
{
	ACCEPTED_QOS0(0), ACCEPTED_QOS1(1), ACCEPTED_QOS2(2), FAILURE(128);

	private int num;

	private static Map<Integer, SubackCode> map = new HashMap<Integer, SubackCode>();

	static
	{
		for (SubackCode legEnum : SubackCode.values())
		{
			map.put(legEnum.num, legEnum);
		}
	}

    @JsonValue
	public int getNum()
	{
		return num;
	}

	private SubackCode(final int leg)
	{
		num = leg;
	}

	@JsonCreator
	public static SubackCode valueOf(int type)
	{
		return map.get(type);
	}

}
