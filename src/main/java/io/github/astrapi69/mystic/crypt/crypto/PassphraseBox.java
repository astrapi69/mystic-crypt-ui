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
package io.github.astrapi69.mystic.crypt.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.astrapi69.mystic.crypt.aead.KeyCommittingAeadEncryptor;

/**
 * Encrypts something with a passphrase, so that a wrong passphrase fails as a wrong passphrase.
 * <p>
 * What comes out looks like this:
 *
 * <pre>
 * magic                   whatever the caller uses to recognise its own files
 * salt         16 bytes   drawn fresh every time
 * iterations    4 bytes   the cost of the key derivation, big endian
 * payload                 AES-GCM, key-committing
 * </pre>
 *
 * The key comes from PBKDF2-HMAC-SHA256 over that salt. Magic, salt and iteration count are the
 * cipher's associated data, so none of them can be changed without the result refusing to open, and
 * the recorded cost can be raised later without making today's files unreadable.
 * <p>
 * Two users so far: the database that is protected by a master password, and the tool that encrypts
 * a file or a piece of text. They differ only in their magic - the construction is the same, and it
 * should stay in one place.
 */
public final class PassphraseBox
{

	/** The length of the salt that goes into the key derivation */
	public static final int SALT_LENGTH = 16;

	/**
	 * How many rounds the key derivation costs for something written today. The current OWASP
	 * recommendation for PBKDF2-HMAC-SHA256; measured at about a tenth of a second
	 */
	public static final int ITERATIONS = 600_000;

	/** The length of the derived key in bits */
	public static final int KEY_LENGTH_BITS = 256;

	private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";

	private PassphraseBox()
	{
	}

	/**
	 * The length of the header in front of the payload
	 *
	 * @param magic
	 *            the marker of the format
	 * @return the number of bytes before the payload starts
	 */
	public static int headerLength(final byte[] magic)
	{
		return magic.length + SALT_LENGTH + Integer.BYTES;
	}

	/**
	 * Whether the given content starts with the given marker
	 *
	 * @param content
	 *            the bytes to look at
	 * @param magic
	 *            the marker to look for
	 * @return true if the marker is there
	 */
	public static boolean hasMagic(final byte[] content, final byte[] magic)
	{
		return content != null && content.length >= magic.length
			&& Arrays.equals(Arrays.copyOf(content, magic.length), magic);
	}

	/**
	 * Derives the key something is encrypted with
	 *
	 * @param passphrase
	 *            the passphrase
	 * @param salt
	 *            the salt
	 * @param iterations
	 *            the iteration count
	 * @return the derived key
	 * @throws Exception
	 *             if the key cannot be derived
	 */
	public static SecretKey deriveKey(final String passphrase, final byte[] salt,
		final int iterations) throws Exception
	{
		PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations,
			KEY_LENGTH_BITS);
		try
		{
			byte[] keyBytes = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
				.generateSecret(keySpec).getEncoded();
			return new SecretKeySpec(keyBytes, "AES");
		}
		finally
		{
			// the spec holds a copy of the passphrase; there is no reason to leave it lying around
			keySpec.clearPassword();
		}
	}

	/**
	 * Encrypts the given bytes with the given passphrase
	 *
	 * @param magic
	 *            the marker to put in front
	 * @param plaintext
	 *            what to encrypt
	 * @param passphrase
	 *            the passphrase
	 * @return the encrypted result, header included
	 * @throws Exception
	 *             if encrypting fails
	 */
	public static byte[] encrypt(final byte[] magic, final byte[] plaintext,
		final String passphrase) throws Exception
	{
		byte[] salt = new byte[SALT_LENGTH];
		// deliberately not SecureRandom.getInstanceStrong(): on Linux that can resolve to the
		// blocking source, and encrypting must never hang waiting for entropy. The default instance
		// reads from the non-blocking pool, which is the right source for a salt
		new SecureRandom().nextBytes(salt);
		byte[] header = ByteBuffer.allocate(headerLength(magic)).put(magic).put(salt)
			.putInt(ITERATIONS).array();
		byte[] payload = new KeyCommittingAeadEncryptor(deriveKey(passphrase, salt, ITERATIONS))
			.encrypt(plaintext, header);
		return ByteBuffer.allocate(header.length + payload.length).put(header).put(payload).array();
	}

	/**
	 * Decrypts what {@link #encrypt(byte[], byte[], String)} produced
	 *
	 * @param magic
	 *            the marker the content must start with
	 * @param content
	 *            the encrypted bytes
	 * @param passphrase
	 *            the passphrase
	 * @return the decrypted bytes
	 * @throws Exception
	 *             if the passphrase is wrong, the content was tampered with, or it is not of this
	 *             format at all
	 */
	public static byte[] decrypt(final byte[] magic, final byte[] content, final String passphrase)
		throws Exception
	{
		if (!hasMagic(content, magic))
		{
			throw new IllegalArgumentException(
				"this is not something this application encrypted: the marker is missing");
		}
		int headerLength = headerLength(magic);
		if (content.length <= headerLength)
		{
			throw new IllegalArgumentException(
				"truncated: the marker is there but there is no content behind it");
		}
		byte[] header = Arrays.copyOf(content, headerLength);
		byte[] salt = Arrays.copyOfRange(header, magic.length, magic.length + SALT_LENGTH);
		int iterations = ByteBuffer.wrap(header, magic.length + SALT_LENGTH, Integer.BYTES)
			.getInt();
		byte[] payload = Arrays.copyOfRange(content, headerLength, content.length);
		// the header is the associated data, so a changed salt or iteration count breaks the tag
		return new KeyCommittingAeadEncryptor(deriveKey(passphrase, salt, iterations))
			.decrypt(payload, header);
	}
}
