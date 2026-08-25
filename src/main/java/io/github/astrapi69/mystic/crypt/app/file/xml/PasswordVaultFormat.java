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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.compound.CompoundAlgorithm;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.crypto.PassphraseBox;
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
	public static final int SALT_LENGTH = PassphraseBox.SALT_LENGTH;

	/** How many rounds the key derivation costs for a file written today */
	public static final int ITERATIONS = PassphraseBox.ITERATIONS;

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
		return PassphraseBox.hasMagic(fileContent, MAGIC);
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
		return PassphraseBox.deriveKey(password, salt, iterations);
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
		return PassphraseBox.encrypt(MAGIC, xml.getBytes(StandardCharsets.UTF_8), password);
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
		return new String(PassphraseBox.decrypt(MAGIC, fileContent, password),
			StandardCharsets.UTF_8);
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
}
