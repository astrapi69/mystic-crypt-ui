/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.random;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * The random values the application draws itself, from one {@link SecureRandom}: the characters of
 * a generated password and the serial number of a new certificate. Both used to come from
 * randomizer, a library for test data (#658, GHSA-qxx5-r8qx-vj98).
 * <p>
 * Both belong in mystic-crypt and are taken from there once it ships them. Until then they live
 * here, each with the issue that moves it.
 */
public final class SecureRandomValues
{

	/**
	 * 159 bits fill the 20 octets RFC 5280 allows a serial number with the top bit clear, so the
	 * DER encoding needs no sign byte
	 */
	private static final int SERIAL_NUMBER_BITS = 159;

	private static final SecureRandom SOURCE = new SecureRandom();

	private SecureRandomValues()
	{
	}

	/**
	 * Draws characters from an alphabet, each position independently and every character of the
	 * alphabet equally likely. {@link SecureRandom#nextInt(int)} rejects the range that would
	 * favour the first characters, so there is no modulo bias. A character that appears twice in
	 * the alphabet is drawn twice as often.
	 * <p>
	 * TODO(astrapi69/mystic-crypt#252): replace with SecretRandom.characters once mystic-crypt
	 * 13.10 is out
	 *
	 * @param length
	 *            how many characters, 0 or more
	 * @param alphabet
	 *            the characters to draw from, not empty
	 * @return the drawn characters, for the caller to overwrite when it is done with them
	 */
	public static char[] characters(final int length, final CharSequence alphabet)
	{
		if (length < 0)
		{
			throw new IllegalArgumentException("length must be 0 or more, was " + length);
		}
		if (alphabet == null || alphabet.isEmpty())
		{
			throw new IllegalArgumentException("the alphabet to draw from is empty");
		}
		char[] drawn = new char[length];
		for (int i = 0; i < length; i++)
		{
			drawn[i] = alphabet.charAt(SOURCE.nextInt(alphabet.length()));
		}
		return drawn;
	}

	/**
	 * A serial number for a new X.509 certificate as RFC 5280 section 4.1.2.2 asks for it:
	 * positive, at most 20 octets, and with 159 bits from {@link SecureRandom} well above the 64
	 * random bits the CA/Browser Forum baseline requires.
	 * <p>
	 * TODO(astrapi69/mystic-crypt#253): replace with the library's serial number generator once
	 * mystic-crypt 13.10 is out
	 *
	 * @return the serial number
	 */
	public static BigInteger serialNumber()
	{
		BigInteger serial;
		do
		{
			serial = new BigInteger(SERIAL_NUMBER_BITS, SOURCE);
		}
		while (serial.signum() == 0);
		return serial;
	}
}
