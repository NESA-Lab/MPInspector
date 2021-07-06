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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonValue;

public class Text
{
	//reference on the local buffer
	protected byte[] chars;

	//the start position
	protected int pos;

	//the length of the string
	protected int len;

	private int hash = -1;

	public Text()
	{
	}

	protected Text(Text another)
	{
		this.chars = another.chars;
		this.pos = another.pos;
		this.len = another.len;
	}

	public Text(String s)
	{
		this.chars = s.getBytes();
		this.pos = 0;
		this.len = chars.length;
	}

	public Text(byte[] data, int pos, int len)
	{
		this.chars = data;
		this.pos = pos;
		this.len = len;
	}

	public int length()
	{
		return len;
	}

	public char charAt(int index)
	{
		return (char) chars[pos + index];
	}

	public Collection<Text> split(char separator)
	{
		int pointer = pos;
		int limit = pos + len;
		int mark = pointer;

		ArrayList<Text> tokens = new ArrayList<Text>();
		while (pointer < limit)
		{
			if (chars[pointer] == separator)
			{
				tokens.add(new Text(chars, mark, pointer - mark));
				mark = pointer + 1;
			}
			pointer++;
		}

		tokens.add(new Text(chars, mark, limit - mark));
		return tokens;
	}

	public Collection<Text> split(Text separator)
	{
		ArrayList<Text> tokens = new ArrayList<Text>();
		int pointer = pos;
		int limit = pos + len;
		int mark = pointer;

		if (separator.length() == 0)
		{
			tokens.add(new Text(chars, mark, pointer - mark));
			return tokens;
		}

		int index = 0;
		while (pointer < limit)
		{
			if (chars[pointer] == separator.charAt(index))
				index++;
			else
				index = 0;

			if (index == separator.length())
			{
				tokens.add(new Text(chars, mark, pointer - mark));
				mark = pointer + 1;
				index = 0;
			}

			pointer++;
		}

		tokens.add(new Text(chars, mark, limit - mark));
		return tokens;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null)
		{
			return false;
		}

		if (!(other instanceof Text))
		{
			return false;
		}

		Text t = (Text) other;
		if (this.len != t.len)
		{
			return false;
		}

		return compareChars(t.chars, t.pos);
	}

	private boolean compareChars(byte[] chars, int pos)
	{
		for (int i = 0; i < len; i++)
			if ((char) this.chars[i + this.pos] != (char) chars[i + pos])
				return false;

		return true;
	}

	public int indexOf(Text value)
	{

		int pointer = pos;
		int limit = pos + len;
		int mark = pointer;
		if (value.length() == 0)
			return 0;
		int index = 0;
		while (pointer < limit)
		{
			if (chars[pointer] == value.charAt(index))
				index++;
			else

			{
				index = 0;
				mark++;
				pointer = mark;
			}
			if (index == value.length())
				return mark;
			pointer++;
		}
		return -1;
	}

	public boolean contains(char c)
	{
		for (int k = pos; k < len; k++)
		{
			if (chars[k] == c)
				return true;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		if (hash == -1)
			hash = 67 * 7 + Arrays.hashCode(this.chars);

		return hash;
	}

	@Override
	@JsonValue
	public String toString()
	{
		return new String(chars, pos, len).trim();
	}
}