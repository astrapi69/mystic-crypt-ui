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
package io.github.astrapi69.mystic.crypt.lock;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import io.github.astrapi69.crypt.api.algorithm.HashAlgorithm;
import io.github.astrapi69.crypt.data.hash.HashExtensions;

/**
 * What a locked workspace keeps of the private key of its key file instead of the key itself
 * (#880): a salted SHA-256 digest of the key's encoding, enough to recognise the key when the key
 * file is read from the disk again at unlock, nothing the key could be rebuilt from.
 * <p>
 * The counterpart of {@link MasterPasswordVerifier} for the second factor. A key is not a password
 * - it has the full entropy of its size - so a single digest is enough and no key stretching is
 * needed; the salt keeps two locks of the same key from leaving the same value behind.
 */
public final class KeyFileVerifier
{

	private static final int SALT_LENGTH = 16;

	private final byte[] salt;

	private final byte[] digest;

	private KeyFileVerifier(final byte[] salt, final byte[] digest)
	{
		this.salt = salt;
		this.digest = digest;
	}

	/**
	 * Makes the verifier of a key
	 *
	 * @param encodedKey
	 *            the encoding of the private key, as the key model holds it
	 * @return the verifier
	 * @throws IllegalArgumentException
	 *             when there is no key to make it from
	 */
	public static KeyFileVerifier of(final byte[] encodedKey)
	{
		if (encodedKey == null || encodedKey.length == 0)
		{
			throw new IllegalArgumentException("a key file verifier needs a key to be made from");
		}
		byte[] salt = new byte[SALT_LENGTH];
		new SecureRandom().nextBytes(salt);
		return new KeyFileVerifier(salt, digestOf(salt, encodedKey));
	}

	/**
	 * Whether a key is the key this verifier was made from, compared in constant time
	 *
	 * @param candidateEncodedKey
	 *            the encoding of the key read from a key file
	 * @return true for the same key
	 */
	public boolean matches(final byte[] candidateEncodedKey)
	{
		if (candidateEncodedKey == null || candidateEncodedKey.length == 0)
		{
			return false;
		}
		byte[] candidateDigest = digestOf(salt, candidateEncodedKey);
		try
		{
			return MessageDigest.isEqual(digest, candidateDigest);
		}
		finally
		{
			Arrays.fill(candidateDigest, (byte)0);
		}
	}

	/**
	 * Overwrites the salt and the digest, after which nothing matches any more
	 */
	public void wipe()
	{
		Arrays.fill(salt, (byte)0);
		Arrays.fill(digest, (byte)0);
	}

	/**
	 * Whether two verifiers hold the same digest, for the test that each lock salts on its own
	 *
	 * @param other
	 *            the other verifier
	 * @return true for the same digest
	 */
	boolean holdsTheSameDigestAs(final KeyFileVerifier other)
	{
		return MessageDigest.isEqual(digest, other.digest);
	}

	private static byte[] digestOf(final byte[] salt, final byte[] encodedKey)
	{
		byte[] salted = new byte[salt.length + encodedKey.length];
		System.arraycopy(salt, 0, salted, 0, salt.length);
		System.arraycopy(encodedKey, 0, salted, salt.length, encodedKey.length);
		try
		{
			return HashExtensions.hash(salted, HashAlgorithm.SHA_256);
		}
		catch (NoSuchAlgorithmException never)
		{
			// SHA-256 is required of every Java platform; the name comes from crypt-api
			throw new IllegalStateException(
				HashAlgorithm.SHA_256.getAlgorithm() + " is missing from this Java runtime", never);
		}
		finally
		{
			Arrays.fill(salted, (byte)0);
		}
	}
}
