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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.Replay;

/**
 * The one way this plugin reads a chain file that already exists (#581).
 * <p>
 * Since lethenon 0.6.0 the chain is an SQLite database, and the library migrates a chain file of
 * 0.5.0 or earlier the first time it reads one; lethenon 0.5.0 - the library of mystic-crypt-ui 8.8
 * and 8.9 - cannot read the chain after that (lethenon ADR 0006). So a file in the old format is
 * read and verified as it is first, with the old encoding, and a chain the plugin would refuse - one
 * an earlier lethenon wrote (#535), one that does not verify - is refused as before, with the file's
 * name, and is never converted. A chain that verifies is converted only by {@link #convert}, which a
 * window calls once the person agreed to it; {@link #read} refuses it until then.
 * <p>
 * No chain logic of its own: {@link CanonicalEncoding#readChain} and {@link Replay#verify} decide,
 * {@link ChainFile} migrates and reads. It takes a {@link Path} and is testable without a display
 * (architecture.md layer 2).
 */
final class ChainFiles
{

	/**
	 * The first 16 bytes of every SQLite database (SQLite's documented file format, "The Database
	 * Header"). Checked here because lethenon cannot yet say whether reading a file would migrate it
	 * (lethenon#222); this knows less than the library does, an interrupted migration for one.
	 */
	private static final byte[] SQLITE_HEADER = "SQLite format 3\0"
		.getBytes(StandardCharsets.US_ASCII);

	private ChainFiles()
	{
	}

	/**
	 * Whether the file is a chain file in the format of lethenon 0.5.0 or earlier, which the library
	 * would migrate on its first read
	 *
	 * @param chainFile
	 *            the file
	 * @return true for a file that exists, is not empty and is not an SQLite database
	 * @throws IOException
	 *             when the file cannot be read
	 */
	static boolean isLegacy(final Path chainFile) throws IOException
	{
		if (!Files.isRegularFile(chainFile) || Files.size(chainFile) == 0)
		{
			return false;
		}
		try (InputStream in = Files.newInputStream(chainFile))
		{
			return !Arrays.equals(SQLITE_HEADER, in.readNBytes(SQLITE_HEADER.length));
		}
	}

	/**
	 * Whether the file is a chain file of the old format whose chain verifies, which is read only
	 * once {@link #convert} has turned it into a database
	 *
	 * @param chainFile
	 *            the file
	 * @return true for a verified chain of the old format; false for a database, an empty or a
	 *         missing file
	 * @throws IOException
	 *             when the file cannot be read
	 * @throws IllegalArgumentException
	 *             when the bytes are no chain file, with the file and its size in front of the reason
	 * @throws ChainRejected
	 *             when a chain in the old format does not verify, with the file in front of the reason
	 */
	static boolean needsConversion(final Path chainFile) throws IOException
	{
		if (!isLegacy(chainFile))
		{
			return false;
		}
		ChainReplaySupport.replay(chainFile, decode(chainFile, Files.readAllBytes(chainFile)));
		return true;
	}

	/**
	 * Converts a verified chain file of the old format into a database, through the library's
	 * migration, which keeps the file as it was beside it as {@code <file>.legacy}. Called once the
	 * person agreed to it: lethenon 0.5.0 and earlier cannot read the file afterwards.
	 *
	 * @param chainFile
	 *            the file
	 * @return the old file, kept as {@code <file>.legacy}
	 * @throws IOException
	 *             when the file cannot be read or converted, or the library kept no
	 *             {@code <file>.legacy}, with the file's absolute path in front of the reason
	 * @throws IllegalArgumentException
	 *             when the file is no chain file of the old format: a database, a missing file, bytes
	 *             that are no chain file
	 * @throws ChainRejected
	 *             when the chain does not verify; it is then not converted
	 */
	static Path convert(final Path chainFile) throws IOException
	{
		if (!needsConversion(chainFile))
		{
			throw new IllegalArgumentException(chainFile.toAbsolutePath() + " is no chain file of "
				+ "lethenon 0.5.0 or earlier, so there is nothing to convert");
		}
		fromTheLibrary(chainFile);
		Path legacy = legacyOf(chainFile);
		if (!Files.isRegularFile(legacy))
		{
			throw new IOException(chainFile.toAbsolutePath() + " was converted, and the library "
				+ "kept no " + legacy.getFileName() + " beside it");
		}
		return legacy;
	}

	/**
	 * Where the library keeps a chain file of the old format once it converted it
	 *
	 * @param chainFile
	 *            the chain file
	 * @return {@code <file>.legacy} beside it
	 */
	static Path legacyOf(final Path chainFile)
	{
		return chainFile.resolveSibling(chainFile.getFileName() + ".legacy");
	}

	/**
	 * Reads a chain file: a database through the library; a verified file of the old format is
	 * refused until it is converted, a file of the old format that does not verify is refused for
	 * that
	 *
	 * @param chainFile
	 *            the file, which exists
	 * @return the blocks, genesis first
	 * @throws UnconvertedChainFile
	 *             when the file is a verified chain of the old format, which is then left as it was
	 * @throws IOException
	 *             when the file cannot be read, or the library refuses the database, with the file's
	 *             absolute path in front of its reason
	 * @throws IllegalArgumentException
	 *             when the bytes are no chain file, with the file and its size in front of the reason
	 * @throws ChainRejected
	 *             when a chain in the old format does not verify, with the file in front of the reason;
	 *             it is then not converted
	 */
	static List<BlockBody> read(final Path chainFile) throws IOException
	{
		if (needsConversion(chainFile))
		{
			throw new UnconvertedChainFile(chainFile);
		}
		return fromTheLibrary(chainFile);
	}

	private static List<BlockBody> fromTheLibrary(final Path chainFile) throws IOException
	{
		try
		{
			return new ChainFile(chainFile).read();
		}
		catch (IllegalArgumentException unreadable)
		{
			throw new IllegalArgumentException(chainFile.toAbsolutePath() + " is "
				+ Files.size(chainFile) + " bytes and cannot be read as a lethenon chain file: "
				+ unreadable.getMessage(), unreadable);
		}
		catch (IOException refused)
		{
			// the library names its own working file, <chain>.migrating, not the chain file
			// (lethenon#216); the reader needs the file they picked
			throw new IOException(chainFile.toAbsolutePath() + ": " + refused.getMessage(), refused);
		}
	}

	/**
	 * The old encoding's refusal of bytes it cannot read, with the file and its size in front, which
	 * the library cannot name (lethenon#80, #532)
	 */
	private static List<BlockBody> decode(final Path chainFile, final byte[] encoded)
	{
		try
		{
			return CanonicalEncoding.readChain(encoded);
		}
		catch (IllegalArgumentException unreadable)
		{
			throw new IllegalArgumentException(chainFile.toAbsolutePath() + " is " + encoded.length
				+ " bytes and cannot be read as a lethenon chain file: " + unreadable.getMessage(),
				unreadable);
		}
	}
}
