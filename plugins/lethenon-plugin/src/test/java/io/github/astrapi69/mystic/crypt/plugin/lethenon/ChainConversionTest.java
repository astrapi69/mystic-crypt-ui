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

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainRejected;

/**
 * A window asks before an old chain file is converted, and goes on only with a file it can read
 * (#581): the person's answer decides, and a chain that does not verify is refused before anyone is
 * asked
 */
class ChainConversionTest
{

	private final Bytes miner = Bytes.of(new byte[] { 9 });

	private final List<Path> asked = new ArrayList<>();

	@TempDir
	Path directory;

	@Test
	@DisplayName("a database needs no conversion: the window goes on and nobody is asked")
	void convertedIfAgreed_goesOnWithADatabase_withoutAsking() throws Exception
	{
		Path database = LethenonFixtures.written(directory.resolve("chain.lethenon"),
			LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner));

		assertTrue(ChainConversion.convertedIfAgreed(null, answering(false), database));

		assertEquals(List.of(), asked);
	}

	@Test
	@DisplayName("a file that does not exist yet needs no conversion: the window goes on and nobody is asked")
	void convertedIfAgreed_goesOnWithANewFile_withoutAsking() throws Exception
	{
		assertTrue(ChainConversion.convertedIfAgreed(null, answering(false),
			directory.resolve("new.lethenon")));

		assertEquals(List.of(), asked);
	}

	@Test
	@DisplayName("an old chain file is converted when the person agrees, and the window goes on with the database")
	void convertedIfAgreed_convertsAnOldChain_whenThePersonAgrees() throws Exception
	{
		List<BlockBody> chain = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner);
		Path old = directory.resolve("old.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(chain);
		Files.write(old, before);

		assertTrue(ChainConversion.convertedIfAgreed(null, answering(true), old));

		assertEquals(List.of(old), asked);
		assertEquals(chain, ChainFiles.read(old));
		assertArrayEquals(before, Files.readAllBytes(ChainFiles.legacyOf(old)));
	}

	@Test
	@DisplayName("an old chain file stays as it was when the person declines, and the window does not go on")
	void convertedIfAgreed_leavesAnOldChain_whenThePersonDeclines() throws Exception
	{
		Path old = directory.resolve("old.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner));
		Files.write(old, before);

		assertFalse(ChainConversion.convertedIfAgreed(null, answering(false), old));

		assertEquals(List.of(old), asked);
		assertArrayEquals(before, Files.readAllBytes(old));
		assertFalse(Files.exists(ChainFiles.legacyOf(old)));
	}

	@Test
	@DisplayName("an old chain that does not verify is refused before anyone is asked, and stays as it was")
	void convertedIfAgreed_refusesAnOldChainThatDoesNotVerify_beforeAsking() throws Exception
	{
		Path retired = directory.resolve("retired.lethenon");
		byte[] before = CanonicalEncoding.encodeChain(LethenonFixtures.aChainUnderLethenonOne());
		Files.write(retired, before);

		assertThrows(ChainRejected.class,
			() -> ChainConversion.convertedIfAgreed(null, answering(true), retired));

		assertEquals(List.of(), asked);
		assertArrayEquals(before, Files.readAllBytes(retired));
		assertFalse(Files.exists(ChainFiles.legacyOf(retired)));
	}

	private ChainConversionConsent answering(final boolean answer)
	{
		return (final Component parent, final Path chainFile) -> {
			asked.add(chainFile);
			return answer;
		};
	}
}
