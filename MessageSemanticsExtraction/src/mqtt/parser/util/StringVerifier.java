package mqtt.parser.util;

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

public class StringVerifier
{
	private static final String NULL_CHARACTER = "\u0000";

	public static boolean verify(String topic)
	{
		if (topic.length() > 0)
		{
			if (topic.contains(NULL_CHARACTER))
				return false;

			for (int i = 0; i < topic.length(); i++)
			{
				char c = topic.charAt(i);
				if (Character.isSurrogate(c))
					return false;
			}
		}

		return true;
	}
}
