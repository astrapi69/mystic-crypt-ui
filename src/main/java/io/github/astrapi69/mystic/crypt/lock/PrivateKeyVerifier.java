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
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Recognises the private key a locked workspace was opened with, without holding the key: locking
 * derives this verifier and wipes the key, unlocking reads the key file from disk again and asks
 * whether it is the same key. The verifier is a salted SHA-256 over the key's encoding - unlike a
 * password, the key is drawn with its full entropy, so there is no candidate space a fast hash
 * would open to guessing, and the digest cannot be turned back into the key.
 */
public final class PrivateKeyVerifier
{

	private static final int SALT_LENGTH = 16;

	private final byte[] salt;

	private final byte[] digest;

	private PrivateKeyVerifier(final byte[] salt, final byte[] digest)
	{
		this.salt = salt;
		this.digest = digest;
	}

	/**
	 * Derives a verifier for the given private key encoding, over a salt drawn fresh for it
	 *
	 * @param encoded
	 *            the private key's encoding, as {@code PrivateKey.getEncoded()} answers it
	 * @return the verifier
	 * @throws IllegalArgumentException
	 *             if there is no key to derive from
	 * @throws Exception
	 *             if the digest cannot be computed
	 */
	public static PrivateKeyVerifier of(final byte[] encoded) throws Exception
	{
		if (encoded == null || encoded.length == 0)
		{
			throw new IllegalArgumentException("a private key verifier needs a key to derive from");
		}
		byte[] salt = new byte[SALT_LENGTH];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		return new PrivateKeyVerifier(salt, digestOf(salt, encoded));
	}

	/**
	 * Whether the given encoding is the key this verifier was derived from. The comparison is
	 * {@link MessageDigest#isEqual(byte[], byte[])}, which does not return early on the first
	 * differing byte
	 *
	 * @param candidate
	 *            the encoding of the key that was read from the chosen file
	 * @return true if it digests to the same bytes
	 */
	public boolean matches(final byte[] candidate)
	{
		if (candidate == null || candidate.length == 0)
		{
			return false;
		}
		try
		{
			return MessageDigest.isEqual(digest, digestOf(salt, candidate));
		}
		catch (Exception cannotDigest)
		{
			return false;
		}
	}

	/**
	 * Overwrites what this verifier holds, for the moment it stops being needed - unlocking, or the
	 * close that follows a lock nobody came back to. Dropping the reference would leave the digest
	 * in the heap for the collector to get to eventually; this does not (#242)
	 */
	public void wipe()
	{
		Arrays.fill(salt, (byte)0);
		Arrays.fill(digest, (byte)0);
	}

	private static byte[] digestOf(final byte[] salt, final byte[] encoded) throws Exception
	{
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		sha256.update(salt);
		sha256.update(encoded);
		return sha256.digest();
	}
}
