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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import io.github.astrapi69.mystic.crypt.crypto.PassphraseBox;

/**
 * Encrypting and decrypting a file or a piece of text with a passphrase, without any user
 * interface.
 * <p>
 * The construction is the one the application uses for a database that is protected by a master
 * password: AES-GCM with a key that commits to itself, over a key derived from the passphrase with
 * PBKDF2-HMAC-SHA256 and a fresh salt. That has three consequences worth knowing. A wrong
 * passphrase fails immediately and says so, instead of producing rubbish. A file that was changed
 * afterwards, by so much as a bit, refuses to open rather than opening with changed content. And
 * the same file encrypted twice looks different both times.
 */
public final class FileCryptSupport
{

	/** The marker every file this tool writes starts with */
	public static final byte[] MAGIC = "MCFILE1".getBytes(StandardCharsets.US_ASCII);

	/** What is appended to the name of an encrypted file when no other name was given */
	public static final String ENCRYPTED_EXTENSION = ".mcenc";

	private FileCryptSupport()
	{
	}

	/**
	 * Whether the given file is one this tool wrote
	 *
	 * @param file
	 *            the file to look at
	 * @return true if it carries the marker
	 * @throws Exception
	 *             if the file cannot be read
	 */
	public static boolean isEncrypted(final File file) throws Exception
	{
		if (!file.isFile() || file.length() < MAGIC.length)
		{
			return false;
		}
		try (java.io.InputStream in = Files.newInputStream(file.toPath()))
		{
			return PassphraseBox.hasMagic(in.readNBytes(MAGIC.length), MAGIC);
		}
	}

	/**
	 * The name an encrypted file gets when none was given: the original name plus
	 * {@value #ENCRYPTED_EXTENSION}
	 *
	 * @param source
	 *            the file that is encrypted
	 * @return the file to write
	 */
	public static File defaultEncryptedFile(final File source)
	{
		return new File(source.getParentFile(), source.getName() + ENCRYPTED_EXTENSION);
	}

	/**
	 * The name a decrypted file gets when none was given: the encrypted name without
	 * {@value #ENCRYPTED_EXTENSION}, or with {@code .decrypted} added when it did not end in it
	 *
	 * @param source
	 *            the encrypted file
	 * @return the file to write
	 */
	public static File defaultDecryptedFile(final File source)
	{
		String name = source.getName();
		String decryptedName = name.endsWith(ENCRYPTED_EXTENSION)
			? name.substring(0, name.length() - ENCRYPTED_EXTENSION.length())
			: name + ".decrypted";
		return new File(source.getParentFile(), decryptedName);
	}

	/**
	 * Encrypts a file with a passphrase
	 *
	 * @param source
	 *            the file to encrypt, which is left as it is
	 * @param target
	 *            the file to write, or {@code null} for {@link #defaultEncryptedFile(File)}
	 * @param passphrase
	 *            the passphrase
	 * @return the file that was written
	 * @throws Exception
	 *             if the file cannot be read or written
	 */
	public static File encryptFile(final File source, final File target, final String passphrase)
		throws Exception
	{
		requireUsable(source, passphrase);
		File encryptedFile = target != null ? target : defaultEncryptedFile(source);
		byte[] encrypted = PassphraseBox.encrypt(MAGIC, Files.readAllBytes(source.toPath()),
			passphrase);
		writeNewFile(encryptedFile.toPath(), encrypted);
		return encryptedFile;
	}

	/**
	 * Decrypts a file that was encrypted with {@link #encryptFile(File, File, String)}
	 *
	 * @param source
	 *            the encrypted file, which is left as it is
	 * @param target
	 *            the file to write, or {@code null} for {@link #defaultDecryptedFile(File)}
	 * @param passphrase
	 *            the passphrase
	 * @return the file that was written
	 * @throws Exception
	 *             if the passphrase is wrong, the file was tampered with, or it cannot be read or
	 *             written
	 */
	public static File decryptFile(final File source, final File target, final String passphrase)
		throws Exception
	{
		requireUsable(source, passphrase);
		File decryptedFile = target != null ? target : defaultDecryptedFile(source);
		byte[] decrypted = PassphraseBox.decrypt(MAGIC, Files.readAllBytes(source.toPath()),
			passphrase);
		writeNewFile(decryptedFile.toPath(), decrypted);
		return decryptedFile;
	}

	/**
	 * Encrypts a piece of text with a passphrase
	 *
	 * @param text
	 *            the text to encrypt
	 * @param passphrase
	 *            the passphrase
	 * @return the encrypted text as Base64, which can be pasted anywhere text can go
	 * @throws Exception
	 *             if encrypting fails
	 */
	public static String encryptText(final String text, final String passphrase) throws Exception
	{
		requirePassphrase(passphrase);
		return Base64.getEncoder().encodeToString(
			PassphraseBox.encrypt(MAGIC, text.getBytes(StandardCharsets.UTF_8), passphrase));
	}

	/**
	 * Decrypts what {@link #encryptText(String, String)} produced
	 *
	 * @param encrypted
	 *            the encrypted text as Base64
	 * @param passphrase
	 *            the passphrase
	 * @return the text
	 * @throws Exception
	 *             if the passphrase is wrong, the text was changed, or it is not Base64 at all
	 */
	public static String decryptText(final String encrypted, final String passphrase)
		throws Exception
	{
		requirePassphrase(passphrase);
		byte[] content;
		try
		{
			content = Base64.getDecoder().decode(encrypted.trim());
		}
		catch (IllegalArgumentException exception)
		{
			throw new IllegalArgumentException(
				"this is not something this tool encrypted: it is not even Base64", exception);
		}
		return new String(PassphraseBox.decrypt(MAGIC, content, passphrase), StandardCharsets.UTF_8);
	}

	private static void requireUsable(final File source, final String passphrase)
	{
		requirePassphrase(passphrase);
		if (source == null || !source.isFile())
		{
			throw new IllegalArgumentException(
				"'" + source + "' is not a file that could be read");
		}
	}

	private static void requirePassphrase(final String passphrase)
	{
		if (passphrase == null || passphrase.isEmpty())
		{
			throw new IllegalArgumentException("a passphrase is required");
		}
	}

	/**
	 * Writes a file that does not exist yet - overwriting the wrong file with ciphertext is not
	 * something anybody recovers from
	 */
	private static void writeNewFile(final Path target, final byte[] content) throws Exception
	{
		if (Files.exists(target))
		{
			throw new IllegalArgumentException(
				"'" + target + "' already exists - pick another name or remove it first");
		}
		Files.write(target, content);
	}
}
