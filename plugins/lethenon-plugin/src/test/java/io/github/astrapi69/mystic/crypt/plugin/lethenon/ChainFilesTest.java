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
 * (#581, lethenon#202): a file in the format of 0.5.0 or earlier is read and verified as it is
 * before the library migrates it, so a chain the plugin refuses is never migrated
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
	@DisplayName("a chain file of 0.5.0 that verifies is read, migrated into a database, and keeps its blocks")
	void read_migratesAVerifiedChainOfTheOldFormat() throws IOException
	{
		List<BlockBody> chain = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner);
		Path old = directory.resolve("old.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(chain);
		Files.write(old, before);

		assertEquals(chain, ChainFiles.read(old));

		assertFalse(ChainFiles.isLegacy(old), "the file is a database now");
		assertArrayEquals(before, Files.readAllBytes(directory.resolve("old.lethenon.legacy")),
			"the old file stays beside it, byte for byte");
	}

	@Test
	@DisplayName("a chain file of 0.5.0 that does not verify is refused with the file's name, and is not migrated")
	void read_refusesAnOldChainThatDoesNotVerify_beforeTheMigration() throws IOException
	{
		Path retired = directory.resolve("retired.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(LethenonFixtures.aChainUnderLethenonOne());
		Files.write(retired, before);

		ChainRejected refused = assertThrows(ChainRejected.class, () -> ChainFiles.read(retired));

		assertTrue(refused.getMessage().startsWith(retired.toAbsolutePath().toString()), refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(retired), "the file stays as it was");
		assertFalse(Files.exists(directory.resolve("retired.lethenon.legacy")), "nothing was migrated");
		assertFalse(Files.exists(directory.resolve("retired.lethenon.migrating")), "nothing was migrated");
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
