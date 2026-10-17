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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.ChainRejected;

/**
 * The one way the plugin reads a chain file since lethenon 0.6.0 keeps the chain in a database
 * (#581, lethenon#202): a file in the format of 0.5.0 or earlier is read and verified as it is, and
 * converted into a database only when asked to, so a chain the plugin refuses is never converted and
 * no chain is converted without the person agreeing to it
 */
class ChainFilesTest
{

	private static final byte[] SQLITE_HEADER = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

	private final Bytes miner = Bytes.of(new byte[] { 9 });

	@TempDir
	Path directory;

	@Test
	@DisplayName("a file in the format of 0.5.0 is recognised as one, a database and a missing file are not")
	void isLegacy_tellsTheOldFormatFromADatabase() throws IOException
	{
		Path old = directory.resolve("old.lethenon");
		Files.write(old, CanonicalEncoding.encodeChain(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner)));
		Path database = directory.resolve("new.lethenon");
		new ChainFile(database).write(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner));

		assertTrue(ChainFiles.isLegacy(old));
		assertFalse(ChainFiles.isLegacy(database));
		assertFalse(ChainFiles.isLegacy(directory.resolve("missing.lethenon")));
		assertArrayEquals(SQLITE_HEADER, Arrays.copyOf(Files.readAllBytes(database), 16));
	}

	@Test
	@DisplayName("a chain file of 0.5.0 that verifies is not read until it is converted, and is left as it was")
	void read_refusesAVerifiedChainOfTheOldFormat_untilItIsConverted() throws IOException
	{
		Path old = directory.resolve("old.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner));
		Files.write(old, before);

		UnconvertedChainFile refused = assertThrows(UnconvertedChainFile.class, () -> ChainFiles.read(old));

		assertTrue(refused.getMessage().startsWith(old.toAbsolutePath().toString()), refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(old), "the file stays as it was");
		assertFalse(Files.exists(directory.resolve("old.lethenon.legacy")), "nothing was converted");
	}

	@Test
	@DisplayName("a chain file of 0.5.0 that verifies is converted into a database, keeps its blocks, and stays beside it as .legacy")
	void convert_turnsAVerifiedChainOfTheOldFormatIntoADatabase() throws IOException
	{
		List<BlockBody> chain = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner);
		Path old = directory.resolve("old.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(chain);
		Files.write(old, before);

		Path legacy = ChainFiles.convert(old);

		assertEquals(directory.resolve("old.lethenon.legacy"), legacy);
		assertArrayEquals(before, Files.readAllBytes(legacy), "the old file stays beside it, byte for byte");
		assertFalse(ChainFiles.isLegacy(old), "the file is a database now");
		assertEquals(chain, ChainFiles.read(old));
	}

	@Test
	@DisplayName("only a chain file of 0.5.0 that verifies needs converting; a database and a missing file do not")
	void needsConversion_onlyForAVerifiedChainOfTheOldFormat() throws IOException
	{
		Path old = directory.resolve("old.lethenon");
		Files.write(old, CanonicalEncoding.encodeChain(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner)));
		Path database = directory.resolve("new.lethenon");
		new ChainFile(database).write(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner));

		assertTrue(ChainFiles.needsConversion(old));
		assertFalse(ChainFiles.needsConversion(database));
		assertFalse(ChainFiles.needsConversion(directory.resolve("missing.lethenon")));
	}

	@Test
	@DisplayName("a database is not converted again: there is nothing to convert, and the file stays as it was")
	void convert_refusesADatabase() throws IOException
	{
		Path database = directory.resolve("new.lethenon");
		new ChainFile(database).write(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner));
		byte[] before = Files.readAllBytes(database);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainFiles.convert(database));

		assertTrue(refused.getMessage().startsWith(database.toAbsolutePath().toString()), refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(database));
	}

	@Test
	@DisplayName("a chain file of 0.5.0 that does not verify is refused with the file's name by every way in, and is not converted")
	void readNeedsConversionAndConvert_refuseAnOldChainThatDoesNotVerify() throws IOException
	{
		Path retired = directory.resolve("retired.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(LethenonFixtures.aChainUnderLethenonOne());
		Files.write(retired, before);

		for (ChainFileCall call : List.<ChainFileCall> of(ChainFiles::read, ChainFiles::needsConversion,
			ChainFiles::convert))
		{
			ChainRejected refused = assertThrows(ChainRejected.class, () -> call.on(retired));
			assertTrue(refused.getMessage().startsWith(retired.toAbsolutePath().toString()), refused.getMessage());
		}
		assertArrayEquals(before, Files.readAllBytes(retired), "the file stays as it was");
		assertFalse(Files.exists(directory.resolve("retired.lethenon.legacy")), "nothing was converted");
		assertFalse(Files.exists(directory.resolve("retired.lethenon.migrating")), "nothing was converted");
	}

	/** One of the ways into a chain file, each of which must refuse a chain that does not verify */
	private interface ChainFileCall
	{
		Object on(Path chainFile) throws IOException;
	}

	@Test
	@DisplayName("bytes that are no chain file are refused with the file's name and size, and stay as they were")
	void read_refusesBytesThatAreNoChainFile() throws IOException
	{
		Path garbage = directory.resolve("garbage.lethenon");
		byte[] before = "not a chain".getBytes(StandardCharsets.US_ASCII);
		Files.write(garbage, before);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainFiles.read(garbage));

		assertTrue(refused.getMessage().startsWith(garbage.toAbsolutePath() + " is 11 bytes"),
			refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(garbage));
	}

	@Test
	@DisplayName("a database is read as the library reads it")
	void read_readsADatabase() throws IOException
	{
		List<BlockBody> chain = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner);
		Path database = directory.resolve("db.lethenon");
		new ChainFile(database).write(chain);

		assertEquals(chain, ChainFiles.read(database));
	}
}
