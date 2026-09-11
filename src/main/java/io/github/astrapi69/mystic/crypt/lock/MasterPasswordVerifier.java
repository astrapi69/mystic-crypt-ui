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
import java.util.HexFormat;

import io.github.astrapi69.mystic.crypt.crypto.PassphraseBox;

/**
 * Recognises the master password without being it.
 * <p>
 * A locked workspace used to keep the master password itself in memory, because unlocking compared
 * the typed characters against it. That made the lock a weaker boundary than it looked: anything
 * that can read the process - a memory dump, a swap file, a core file - could read the password out
 * of a workspace its owner had locked (#242).
 * <p>
 * What stays behind instead is a salt and what PBKDF2-HMAC-SHA256 derives from the password over
 * it, through {@link PassphraseBox}, the same derivation the database itself is protected with.
 * From those two the password cannot be read back; a candidate can only be derived again and
 * compared, which is what {@link #matches(char[])} does.
 */
public final class MasterPasswordVerifier
{

	private final byte[] salt;

	private final byte[] derivedKey;

	private final int iterations;

	private MasterPasswordVerifier(final byte[] salt, final byte[] derivedKey, final int iterations)
	{
		this.salt = salt;
		this.derivedKey = derivedKey;
		this.iterations = iterations;
	}

	/**
	 * Derives a verifier for the given master password, over a salt drawn fresh for it
	 *
	 * @param masterPassword
	 *            the password to recognise later
	 * @return the verifier
	 * @throws IllegalArgumentException
	 *             if there is no password to derive from
	 * @throws Exception
	 *             if the derivation fails
	 */
	public static MasterPasswordVerifier of(final char[] masterPassword) throws Exception
	{
		if (masterPassword == null || masterPassword.length == 0)
		{
			throw new IllegalArgumentException(
				"a master password verifier needs a password to derive from");
		}
		byte[] salt = new byte[PassphraseBox.SALT_LENGTH];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		return new MasterPasswordVerifier(salt,
			PassphraseBox.deriveKey(masterPassword, salt, PassphraseBox.ITERATIONS).getEncoded(),
			PassphraseBox.ITERATIONS);
	}

	/**
	 * Overwrites what this verifier holds, for the moment it stops being needed - unlocking, or the
	 * close that follows a lock nobody came back to.
	 * <p>
	 * It is not the password and the password cannot be read back from it. It is still PBKDF2
	 * output over that password at the same 600,000 iterations the database itself uses, which
	 * makes it the one thing an offline guesser can test a candidate against without the file.
	 * Dropping the reference leaves it in the heap for the collector to get to eventually; this
	 * does not (#242).
	 */
	public void wipe()
	{
		Arrays.fill(salt, (byte)0);
		Arrays.fill(derivedKey, (byte)0);
	}

	/**
	 * Whether the given characters are the password this verifier was derived from. The comparison
	 * is {@link MessageDigest#isEqual(byte[], byte[])}, which does not return early on the first
	 * differing byte
	 *
	 * @param candidate
	 *            the characters that were typed
	 * @return true if they derive to the same bytes
	 */
	public boolean matches(final char[] candidate)
	{
		if (candidate == null || candidate.length == 0)
		{
			return false;
		}
		byte[] candidateKey;
		try
		{
			candidateKey = PassphraseBox.deriveKey(candidate, salt, iterations).getEncoded();
		}
		catch (Exception exception)
		{
			// a derivation that cannot run is not a match, and saying so is the safe answer here -
			// the caller's alternative would be to let an exception unlock nothing at all
			return false;
		}
		try
		{
			return MessageDigest.isEqual(derivedKey, candidateKey);
		}
		finally
		{
			Arrays.fill(candidateKey, (byte)0);
		}
	}

	/**
	 * A short, non-reversible label of the derived bytes, for tests and for telling two verifiers
	 * apart in a log line without printing what they are
	 *
	 * @return the first four bytes of the derived key, in hex
	 */
	public String fingerprint()
	{
		return HexFormat.of().formatHex(Arrays.copyOf(derivedKey, 4));
	}

	/**
	 * @return a description that names the type and the fingerprint, never the password
	 */
	@Override
	public String toString()
	{
		return "MasterPasswordVerifier[" + fingerprint() + "]";
	}
}
