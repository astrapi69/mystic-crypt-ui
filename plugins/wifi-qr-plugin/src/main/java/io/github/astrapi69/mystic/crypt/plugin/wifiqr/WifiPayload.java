/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.wifiqr;

import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * The text of a Wi-Fi QR code, {@code WIFI:T:<type>;S:<ssid>;P:<password>;H:true;;}, as the ZXing
 * wiki "Barcode Contents" describes it (#1120): {@code \}, {@code ;}, {@code ,}, {@code "} and
 * {@code :} are escaped with a backslash, and a name or password that could be read as hex is
 * enclosed in double quotes. It is built into a character array, not a String, so the password can
 * be overwritten once the code is drawn.
 */
final class WifiPayload
{

	private static final String SPECIAL = "\\;,\":";

	private WifiPayload()
	{
	}

	/**
	 * The payload of a network
	 *
	 * @param network
	 *            the network
	 * @return the payload; the caller overwrites it when done
	 */
	static char[] of(final WifiNetwork network)
	{
		CharBuffer payload = CharBuffer
			.allocate(64 + 4 * network.ssid().length() + 4 * network.password().length);
		payload.put("WIFI:T:").put(network.type()).put(";S:");
		value(payload, network.ssid().toCharArray());
		payload.put(';');
		if (!WifiNetwork.OPEN.equals(network.type()))
		{
			payload.put("P:");
			value(payload, network.password());
			payload.put(';');
		}
		if (network.hidden())
		{
			payload.put("H:true;");
		}
		payload.put(';');
		char[] written = Arrays.copyOf(payload.array(), payload.position());
		Arrays.fill(payload.array(), '\0');
		return written;
	}

	private static void value(final CharBuffer payload, final char[] value)
	{
		boolean quoted = looksLikeHex(value);
		if (quoted)
		{
			payload.put('"');
		}
		for (char character : value)
		{
			if (SPECIAL.indexOf(character) >= 0)
			{
				payload.put('\\');
			}
			payload.put(character);
		}
		if (quoted)
		{
			payload.put('"');
		}
	}

	/** Whether a value consists of hex digits only, which a reader could take for a hex key */
	private static boolean looksLikeHex(final char[] value)
	{
		if (value.length == 0)
		{
			return false;
		}
		for (char character : value)
		{
			boolean hexDigit = character >= '0' && character <= '9'
				|| character >= 'a' && character <= 'f' || character >= 'A' && character <= 'F';
			if (!hexDigit)
			{
				return false;
			}
		}
		return true;
	}
}
