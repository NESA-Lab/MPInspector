package mqtt.parser.avps;

import io.netty.buffer.ByteBuf;

import mqtt.parser.exceptions.MalformedMessageException;

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

public class LengthDetails
{
	private int length;
	private int size;

	public LengthDetails(int length, int size)
	{
		this.length = length;
		this.size = size;
	}

	public int getLength()
	{
		return length;
	}

	public int getSize()
	{
		return size;
	}

	public static LengthDetails decode(ByteBuf buf) throws MalformedMessageException
	{
		int length = 0, multiplier = 1;
		int bytesUsed = 0;
		byte enc = 0;
		do
		{
			if (multiplier > 128 * 128 * 128)
				throw new MalformedMessageException("Encoded length exceeds maximum of 268435455 bytes");

			if (!buf.isReadable())
				return new LengthDetails(0, 0);

			enc = buf.readByte();
			length += (enc & 0x7f) * multiplier;
			multiplier *= 128;
			bytesUsed++;
		}
		while ((enc & 0x80) != 0);

		return new LengthDetails(length, bytesUsed);
	}
}
