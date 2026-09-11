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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rule about what can be written to a checksum file, decided without a display (#320).
 */
class ChecksumSaveDecisionTest
{

	@Test
	@DisplayName("a checksum over typed text cannot be saved, and the reason names the way out")
	void refusalFor_namesTheCheckbox_whenTheChecksumIsOverText()
	{
		String refusal = ChecksumSaveDecision.refusalFor(false, "abc123");

		assertNotNull(refusal, "a checksum file has two columns and typed text fills only one");
		assertTrue(refusal.contains("use the file instead of the text"),
			"a refusal that does not say what to do about it is a refusal the user reads twice");
	}

	@Test
	@DisplayName("over a file with nothing computed, the refusal is the other one")
	void refusalFor_asksForAComputation_whenThereIsNoChecksumYet()
	{
		assertNotNull(ChecksumSaveDecision.refusalFor(true, null));
		assertNotNull(ChecksumSaveDecision.refusalFor(true, "   "),
			"blank is nothing computed, not something computed that happens to be empty");
		assertTrue(ChecksumSaveDecision.refusalFor(true, "").contains("compute the checksum first"));
	}

	@Test
	@DisplayName("over a file with a checksum, nothing is in the way")
	void refusalFor_isNull_whenEverythingIsThere()
	{
		assertNull(ChecksumSaveDecision.refusalFor(true, "abc123"));
	}

	@Test
	@DisplayName("only the text case switches the button off")
	void savingIsImpossible_isTrue_onlyWhileComputingOverText()
	{
		assertTrue(ChecksumSaveDecision.savingIsImpossible(false));
		assertFalse(ChecksumSaveDecision.savingIsImpossible(true),
			"nothing computed yet is fixed by the button next to it, so that state keeps an "
				+ "enabled button and an answer rather than a dead one");
	}
}
