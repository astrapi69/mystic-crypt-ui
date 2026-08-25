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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.compound.CompoundAlgorithm;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.aead.KeyCommittingAeadEncryptor;
import io.github.astrapi69.mystic.crypt.file.PBEFileDecryptor;

/**
 * The on-disk format of a database that is protected by a master password alone, without a key
 * file.
 * <p>
 * A file written today looks like this:
 *
 * <pre>
 * "MCRDB2"      6 bytes   the marker that says which format follows
 * salt         16 bytes   drawn fresh for every single save
 * iterations    4 bytes   how much work the key derivation costs, big endian
 * payload                 AES-GCM, key-committing, over the xml
 * </pre>
 *
 * The key is derived from the master password with PBKDF2-HMAC-SHA256 over that salt and that many
 * iterations. Marker, salt and iteration count are fed to the cipher as associated data, so none of
 * them can be changed without the file failing to open.
 * <p>
 * Three things follow from this. A wrong password fails as a wrong password, immediately and
 * always, because AES-GCM checks its tag - it can no longer hand out plausible looking rubbish. A
 * file that was tampered with fails to open instead of opening with changed content. And the cost
 * of the key derivation is written into the file, so it can be raised later without making today's
 * files unreadable.
 * <p>
 * <b>Why this exists.</b> Until now this file was encrypted with {@code PBEWithMD5AndDES}, with
 * {@link CompoundAlgorithm#SALT} as its salt and {@link CompoundAlgorithm#ITERATIONCOUNT}, which is
 * 19, as its iteration count. That salt is a constant inside a library published on Maven Central,
 * so it was the same eight bytes on every installation in the world - which is exactly what a salt
 * must never be, because one precomputation then works against everybody's file. Together with a 56
 * bit cipher and 19 iterations there was very little standing between the file and whoever holds
 * it, and because that cipher authenticates nothing, a wrong password produced rubbish rather than
 * an error.
 * <p>
 * Files in the old format are still read, and the next save writes them in the new one, so a
 * database migrates by being used. There is deliberately no way to write the old format again.
 */
public final class PasswordVaultFormat
{

	/** The marker at the start of a file in the current format */
	public static final byte[] MAGIC = "MCRDB2".getBytes(StandardCharsets.US_ASCII);

	/** The length of the salt that goes into the key derivation */
	public static final int SALT_LENGTH = 16;

	/**
	 * How many rounds the key derivation costs for a file written today. The current OWASP
	 * recommendation for PBKDF2-HMAC-SHA256; measured at about a tenth of a second here, which is
	 * nothing next to opening a database and everything next to guessing a password
	 */
	public static final int ITERATIONS = 600_000;

	/** The length of the derived key in bits */
	public static final int KEY_LENGTH_BITS = 256;

	private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";

	private static final int HEADER_LENGTH = MAGIC.length + SALT_LENGTH + Integer.BYTES;

	private PasswordVaultFormat()
	{
	}

	/**
	 * Whether the given file content is in the current format
	 *
	 * @param fileContent
	 *            the bytes of the file
	 * @return true if the marker is there
	 */
	public static boolean isCurrentFormat(final byte[] fileContent)
	{
		return fileContent != null && fileContent.length >= MAGIC.length
			&& Arrays.equals(Arrays.copyOf(fileContent, MAGIC.length), MAGIC);
	}

	/**
	 * Derives the key a file is encrypted with
	 *
	 * @param password
	 *            the master password
	 * @param salt
	 *            the salt of that file
	 * @param iterations
	 *            the iteration count of that file
	 * @return the derived key
	 * @throws Exception
	 *             if the key cannot be derived
	 */
	public static SecretKey deriveKey(final String password, final byte[] salt,
		final int iterations) throws Exception
	{
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations,
			KEY_LENGTH_BITS);
		try
		{
			byte[] keyBytes = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
				.generateSecret(keySpec).getEncoded();
			return new SecretKeySpec(keyBytes, "AES");
		}
		finally
		{
			// the spec holds a copy of the password; there is no reason to leave it lying around
			keySpec.clearPassword();
		}
	}

	/**
	 * Encrypts the given xml with the master password
	 *
	 * @param xml
	 *            the xml of the database
	 * @param password
	 *            the master password
	 * @return the bytes to write, header included
	 * @throws Exception
	 *             if encrypting fails
	 */
	public static byte[] encrypt(final String xml, final String password) throws Exception
	{
		byte[] salt = new byte[SALT_LENGTH];
		// deliberately not SecureRandom.getInstanceStrong(): on Linux that can resolve to the
		// blocking source, and a save must never hang waiting for entropy. The default instance
		// reads from the non-blocking pool, which is the right source for a salt
		new SecureRandom().nextBytes(salt);
		byte[] header = newHeader(salt, ITERATIONS);
		byte[] payload = new KeyCommittingAeadEncryptor(deriveKey(password, salt, ITERATIONS))
			.encrypt(xml.getBytes(StandardCharsets.UTF_8), header);
		return ByteBuffer.allocate(header.length + payload.length).put(header).put(payload).array();
	}

	/**
	 * Reads a database file that is protected by a master password, in whichever of the two formats
	 * it happens to be in
	 *
	 * @param applicationFile
	 *            the database file
	 * @param password
	 *            the master password
	 * @return the xml of the database
	 * @throws Exception
	 *             if the password is wrong, the file was tampered with, or it cannot be read
	 */
	public static String decrypt(final File applicationFile, final String password) throws Exception
	{
		byte[] fileContent = Files.readAllBytes(applicationFile.toPath());
		if (!isCurrentFormat(fileContent))
		{
			return decryptLegacy(applicationFile, password);
		}
		if (fileContent.length <= HEADER_LENGTH)
		{
			throw new IllegalArgumentException(
				"the database file is truncated: it carries the " + "marker but no content");
		}
		byte[] header = Arrays.copyOf(fileContent, HEADER_LENGTH);
		byte[] salt = Arrays.copyOfRange(header, MAGIC.length, MAGIC.length + SALT_LENGTH);
		int iterations = ByteBuffer.wrap(header, MAGIC.length + SALT_LENGTH, Integer.BYTES)
			.getInt();
		byte[] payload = Arrays.copyOfRange(fileContent, HEADER_LENGTH, fileContent.length);
		// the header is the associated data, so a changed salt or iteration count breaks the tag
		byte[] xml = new KeyCommittingAeadEncryptor(deriveKey(password, salt, iterations))
			.decrypt(payload, header);
		return new String(xml, StandardCharsets.UTF_8);
	}

	/**
	 * Reads a file written before the format carried a marker: {@code PBEWithMD5AndDES} with the
	 * fixed salt and iteration count that every installation shared
	 *
	 * @param applicationFile
	 *            the database file
	 * @param password
	 *            the master password
	 * @return the xml of the database
	 * @throws Exception
	 *             if the file cannot be read
	 */
	public static String decryptLegacy(final File applicationFile, final String password)
		throws Exception
	{
		// salt and iteration count MUST be pinned exactly as they were when the file was written:
		// without them the decryptor draws its own random salt, which can never match
		CryptModel<Cipher, String, String> legacyModel = CryptModel
			.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).salt(CompoundAlgorithm.SALT)
			.iterationCount(CompoundAlgorithm.ITERATIONCOUNT).build();
		// this decryptor only works on files, so it puts the decrypted database next to the
		// encrypted one for a moment; it is removed again right away, and a save afterwards writes
		// the new format, so this path is walked at most once per database
		File decrypted = new PBEFileDecryptor(legacyModel).decrypt(applicationFile);
		try
		{
			return ReadFileExtensions.fromFile(decrypted);
		}
		finally
		{
			DeleteFileExtensions.delete(decrypted);
		}
	}

	private static byte[] newHeader(final byte[] salt, final int iterations)
	{
		return ByteBuffer.allocate(HEADER_LENGTH).put(MAGIC).put(salt).putInt(iterations).array();
	}
}
