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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checksums and message authentication codes, without any user interface.
 * <p>
 * A checksum says whether a file arrived unchanged. It says nothing about who produced it: anyone
 * who can change the file can compute a new checksum for it. A message authentication code answers
 * the other question - it takes a key, so only someone who has that key can produce a code that
 * checks out. Both belong in a tool about integrity, and they are not the same thing.
 * <p>
 * The digests offered here go beyond the six the application knew: SHA-3 and BLAKE2 were reachable
 * from the command line but not from a window.
 */
public final class ChecksumSupport
{

	/** The digests offered, in the order they are shown */
	public static final List<String> DIGESTS = List.of("SHA-256", "SHA-512", "SHA-384", "SHA-1",
		"MD5", "SHA3-256", "SHA3-512", "BLAKE2B-256", "BLAKE2B-512", "BLAKE2S-256");

	/** The message authentication codes offered */
	public static final List<String> MACS = List.of("HmacSHA256", "HmacSHA512", "HmacSHA3-256",
		"HmacSHA3-512");

	private ChecksumSupport()
	{
	}

	/**
	 * The checksum of a piece of text, over its UTF-8 bytes
	 *
	 * @param text
	 *            the text
	 * @param algorithm
	 *            the digest, one of {@link #DIGESTS}
	 * @return the checksum as lower case hex
	 * @throws Exception
	 *             if this machine does not know that digest
	 */
	public static String checksumOfText(final String text, final String algorithm) throws Exception
	{
		return toHex(MessageDigest.getInstance(algorithm)
			.digest(text.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * The checksum of a file, read in blocks so a large file does not have to fit in memory
	 *
	 * @param file
	 *            the file
	 * @param algorithm
	 *            the digest, one of {@link #DIGESTS}
	 * @return the checksum as lower case hex
	 * @throws Exception
	 *             if this machine does not know that digest or the file cannot be read
	 */
	public static String checksumOfFile(final File file, final String algorithm) throws Exception
	{
		requireFile(file);
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		try (java.io.InputStream in = Files.newInputStream(file.toPath()))
		{
			byte[] block = new byte[8192];
			int read;
			while ((read = in.read(block)) != -1)
			{
				digest.update(block, 0, read);
			}
		}
		return toHex(digest.digest());
	}

	/**
	 * The message authentication code of a piece of text
	 *
	 * @param text
	 *            the text
	 * @param key
	 *            the key, as text; its UTF-8 bytes are the key
	 * @param algorithm
	 *            the code, one of {@link #MACS}
	 * @return the code as lower case hex
	 * @throws Exception
	 *             if this machine does not know that code, or no key was given
	 */
	public static String macOfText(final String text, final String key, final String algorithm)
		throws Exception
	{
		return toHex(mac(text.getBytes(StandardCharsets.UTF_8), key, algorithm));
	}

	/**
	 * The message authentication code of a file
	 *
	 * @param file
	 *            the file
	 * @param key
	 *            the key, as text
	 * @param algorithm
	 *            the code, one of {@link #MACS}
	 * @return the code as lower case hex
	 * @throws Exception
	 *             if this machine does not know that code, no key was given, or the file cannot be
	 *             read
	 */
	public static String macOfFile(final File file, final String key, final String algorithm)
		throws Exception
	{
		requireFile(file);
		return toHex(mac(Files.readAllBytes(file.toPath()), key, algorithm));
	}

	/**
	 * Whether a checksum or a code is the one that was expected.
	 * <p>
	 * Compared without letting the time it takes depend on how much of it matches: a comparison
	 * that stops at the first difference tells an attacker, one attempt at a time, how far they
	 * got.
	 *
	 * @param expected
	 *            what it should be, as hex, in whatever case and with whatever spaces
	 * @param actual
	 *            what it is, as hex
	 * @return true if the two are the same
	 */
	public static boolean matches(final String expected, final String actual)
	{
		if (expected == null || actual == null)
		{
			return false;
		}
		return MessageDigest.isEqual(normalise(expected).getBytes(StandardCharsets.US_ASCII),
			normalise(actual).getBytes(StandardCharsets.US_ASCII));
	}

	/**
	 * The digest a checksum of this length was made with, so a value pasted from a download page
	 * does not have to be identified by hand
	 *
	 * @param checksum
	 *            the checksum as hex
	 * @return the digest, or {@code null} when its length fits none or several
	 */
	public static String digestByLength(final String checksum)
	{
		return switch (normalise(checksum).length())
		{
			case 32 -> "MD5";
			case 40 -> "SHA-1";
			case 96 -> "SHA-384";
			// 64 and 128 hex digits fit SHA-256 and SHA-512 as well as their SHA-3 and BLAKE2
			// counterparts, so the length alone does not say which one it was
			default -> null;
		};
	}

	private static byte[] mac(final byte[] data, final String key, final String algorithm)
		throws Exception
	{
		if (key == null || key.isEmpty())
		{
			throw new IllegalArgumentException(
				"a message authentication code needs a key - that is what makes it one");
		}
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
		return mac.doFinal(data);
	}

	private static void requireFile(final File file)
	{
		if (file == null || !file.isFile())
		{
			throw new IllegalArgumentException("'" + file + "' is not a file that could be read");
		}
	}

	private static String normalise(final String hex)
	{
		return hex.trim().replace(" ", "").replace(":", "").toLowerCase(java.util.Locale.ROOT);
	}

	private static String toHex(final byte[] bytes)
	{
		return HexFormat.of().formatHex(bytes);
	}
}
