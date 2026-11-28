/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins the test vectors of the 9.0 container's byte layout (#862,
 * {@code docs/vault-container-9.0.md}).
 * <p>
 * Each vector under {@code src/test/resources/vault/container-vectors/} names its verdict in a
 * {@code # verdict:} line. A small walker that follows the byte table and nothing else reads every
 * vector and must reach that verdict, so the vectors and the table cannot drift apart before the
 * reader of #865 exists; that reader runs the same vectors and must reach the same verdicts.
 */
class ContainerLayoutVectorsTest
{

	private static final Path VECTORS = Path.of("src/test/resources/vault/container-vectors");

	private static final byte[] MAGIC = "MCVLT1".getBytes(StandardCharsets.US_ASCII);

	private static final int HEADER_LENGTH = 18;

	private static final int SUPPORTED_MAJOR = 9;

	private static final int SUPPORTED_MINOR = 0;

	private static final int LENGTH_PREFIX = Long.BYTES;

	private static final Pattern INDEX_ENTRY = Pattern
		.compile("\"offset\":(\\d+),\"length\":(\\d+)");

	/** The verdict a reader reaches on one container. */
	enum Verdict
	{
		ACCEPT, ACCEPT_READ_ONLY, WRONG_MAGIC, TRUNCATED_HEADER, NEWER_MAJOR, UNKNOWN_MAJOR, JSON_PAST_THE_END, JSON_WITH_BOM, JSON_NOT_UTF8, TRUNCATED_ATTACHMENT, INDEX_MISMATCH
	}

	/** What the walker found: the verdict and, for an accepted container, the offsets it walked. */
	record Walk(Verdict verdict, List<Long> blobOffsets) {
		static Walk refused(final Verdict verdict)
		{
			return new Walk(verdict, List.of());
		}
	}

	/** One vector file: its name, its bytes and what its comment lines promise. */
	record Vector(String name, byte[] bytes, Verdict verdict, List<Long> blobOffsets) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Vector> vectors() throws IOException
	{
		try (Stream<Path> files = Files.list(VECTORS))
		{
			return files.filter(file -> file.toString().endsWith(".hex")).sorted()
				.map(ContainerLayoutVectorsTest::read).toList().stream();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vectors")
	void walkingTheVectorByTheByteTable_reachesTheVerdictItNames(final Vector vector)
	{
		Walk walk = walk(vector.bytes());

		assertEquals(vector.verdict(), walk.verdict(), vector.name());
		assertEquals(vector.blobOffsets(), walk.blobOffsets(), vector.name());
	}

	@Test
	void theVectorSet_holdsEveryVerdictOfTheTableAtLeastOnce() throws IOException
	{
		List<Verdict> named = vectors().map(Vector::verdict).distinct().toList();

		List<Verdict> missing = new ArrayList<>(Arrays.asList(Verdict.values()));
		missing.removeAll(named);
		assertTrue(missing.isEmpty(), "verdicts without a vector: " + missing);
	}

	@Test
	void theHeaderOfTheSmallestContainer_isTheMagicTheVersionAndTheJsonLength()
	{
		byte[] bytes = read(VECTORS.resolve("accept-no-attachments.hex")).bytes();
		ByteBuffer header = ByteBuffer.wrap(bytes, 0, HEADER_LENGTH);

		byte[] magic = new byte[MAGIC.length];
		header.get(magic);
		assertEquals("MCVLT1", new String(magic, StandardCharsets.US_ASCII));
		assertEquals(9, Short.toUnsignedInt(header.getShort()));
		assertEquals(0, Short.toUnsignedInt(header.getShort()));
		assertEquals(bytes.length - HEADER_LENGTH, header.getLong());
	}

	private static Walk walk(final byte[] bytes)
	{
		if (bytes.length < HEADER_LENGTH)
		{
			return Walk.refused(Verdict.TRUNCATED_HEADER);
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		byte[] magic = new byte[MAGIC.length];
		buffer.get(magic);
		if (!Arrays.equals(MAGIC, magic))
		{
			return Walk.refused(Verdict.WRONG_MAGIC);
		}
		int major = Short.toUnsignedInt(buffer.getShort());
		int minor = Short.toUnsignedInt(buffer.getShort());
		if (major > SUPPORTED_MAJOR)
		{
			return Walk.refused(Verdict.NEWER_MAJOR);
		}
		if (major < SUPPORTED_MAJOR)
		{
			return Walk.refused(Verdict.UNKNOWN_MAJOR);
		}
		long jsonLength = buffer.getLong();
		if (jsonLength < 0 || jsonLength > bytes.length - HEADER_LENGTH)
		{
			return Walk.refused(Verdict.JSON_PAST_THE_END);
		}
		return walkBody(bytes, (int)jsonLength, minor > SUPPORTED_MINOR);
	}

	private static Walk walkBody(final byte[] bytes, final int jsonLength, final boolean readOnly)
	{
		byte[] json = Arrays.copyOfRange(bytes, HEADER_LENGTH, HEADER_LENGTH + jsonLength);
		if (json.length >= 3 && (json[0] & 0xff) == 0xef && (json[1] & 0xff) == 0xbb
			&& (json[2] & 0xff) == 0xbf)
		{
			return Walk.refused(Verdict.JSON_WITH_BOM);
		}
		String text;
		try
		{
			text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(json))
				.toString();
		}
		catch (CharacterCodingException exception)
		{
			return Walk.refused(Verdict.JSON_NOT_UTF8);
		}
		List<long[]> area = walkArea(bytes, HEADER_LENGTH + jsonLength);
		if (area == null)
		{
			return Walk.refused(Verdict.TRUNCATED_ATTACHMENT);
		}
		List<long[]> index = index(text);
		if (!sameEntries(index, area))
		{
			return Walk.refused(Verdict.INDEX_MISMATCH);
		}
		return new Walk(readOnly ? Verdict.ACCEPT_READ_ONLY : Verdict.ACCEPT,
			area.stream().map(entry -> entry[0]).toList());
	}

	/**
	 * Each attachment of the area as {offset, length}, offsets from the area's start; null if one
	 * is cut.
	 */
	private static List<long[]> walkArea(final byte[] bytes, final int areaStart)
	{
		List<long[]> entries = new ArrayList<>();
		long offset = 0;
		long remaining = bytes.length - areaStart;
		while (offset < remaining)
		{
			if (remaining - offset < LENGTH_PREFIX)
			{
				return null;
			}
			long length = ByteBuffer.wrap(bytes, (int)(areaStart + offset), LENGTH_PREFIX)
				.getLong();
			if (length < 0 || length > remaining - offset - LENGTH_PREFIX)
			{
				return null;
			}
			entries.add(new long[] { offset, length });
			offset += LENGTH_PREFIX + length;
		}
		return entries;
	}

	/**
	 * The index's entries as {offset, length}, read from the compact JSON the vectors carry. A
	 * pattern is enough for these hand-built documents and keeps a JSON library out of a test that
	 * pins bytes; the reader of #865 parses the JSON properly.
	 */
	private static List<long[]> index(final String json)
	{
		List<long[]> entries = new ArrayList<>();
		Matcher entry = INDEX_ENTRY.matcher(json);
		while (entry.find())
		{
			entries
				.add(new long[] { Long.parseLong(entry.group(1)), Long.parseLong(entry.group(2)) });
		}
		return entries;
	}

	private static boolean sameEntries(final List<long[]> index, final List<long[]> area)
	{
		if (index.size() != area.size())
		{
			return false;
		}
		for (int i = 0; i < index.size(); i++)
		{
			if (!Arrays.equals(index.get(i), area.get(i)))
			{
				return false;
			}
		}
		return true;
	}

	private static Vector read(final Path file)
	{
		try
		{
			List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			Verdict verdict = null;
			List<Long> offsets = List.of();
			for (String line : lines)
			{
				if (line.startsWith("# verdict: "))
				{
					verdict = verdict(line.substring("# verdict: ".length()));
				}
				else if (line.startsWith("# blob offsets: "))
				{
					offsets = offsets(line.substring("# blob offsets: ".length()));
				}
				else if (!line.startsWith("#") && !line.isBlank())
				{
					for (String hex : line.trim().split("\\s+"))
					{
						bytes.write(Integer.parseInt(hex, 16));
					}
				}
			}
			String name = file.getFileName().toString();
			if (verdict == null)
			{
				throw new IllegalStateException(name + " names no verdict");
			}
			return new Vector(name, bytes.toByteArray(), verdict, offsets);
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}

	private static Verdict verdict(final String text)
	{
		return switch (text)
		{
			case "accept" -> Verdict.ACCEPT;
			case "accept read-only" -> Verdict.ACCEPT_READ_ONLY;
			default -> Verdict.valueOf(text.substring("refuse ".length()));
		};
	}

	private static List<Long> offsets(final String text)
	{
		if (text.equals("none"))
		{
			return List.of();
		}
		return Arrays.stream(text.split(" ")).map(Long::valueOf).toList();
	}
}
