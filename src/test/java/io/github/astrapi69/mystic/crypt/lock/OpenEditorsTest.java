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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The counter the lock and the close ask about work in flight (#303).
 */
class OpenEditorsTest
{

	@BeforeEach
	void startFromAKnownState()
	{
		OpenEditors.forgetAll();
	}

	@Test
	@DisplayName("nothing open is the starting state")
	void nothingIsOpenToBeginWith()
	{
		assertFalse(OpenEditors.anyOpen());
	}

	@Test
	@DisplayName("an open editor is work in flight until it closes")
	void oneEditorOpensAndCloses()
	{
		OpenEditors.opened();
		assertTrue(OpenEditors.anyOpen(), "an open editor holds what the user typed");

		OpenEditors.closed();
		assertFalse(OpenEditors.anyOpen());
	}

	@Test
	@DisplayName("the last editor closing is what ends the state, not the first")
	void aSecondEditorOverTheFirstKeepsTheStateUntilBothAreClosed()
	{
		OpenEditors.opened();
		OpenEditors.opened();

		OpenEditors.closed();
		assertTrue(OpenEditors.anyOpen(),
			"one of two closed leaves work in flight - a counter, not a flag, is why");

		OpenEditors.closed();
		assertFalse(OpenEditors.anyOpen());
	}

	@Test
	@DisplayName("closing more often than opening does not go negative")
	void anUnbalancedCloseCannotDriveTheCounterBelowZero()
	{
		OpenEditors.closed();
		OpenEditors.closed();

		assertFalse(OpenEditors.anyOpen());

		OpenEditors.opened();
		assertTrue(OpenEditors.anyOpen(),
			"a negative counter would swallow the next real editor, which is the failure this "
				+ "guards against");
	}

	@Test
	@DisplayName("forgetting all of them clears the state")
	void forgetAllClearsEverything()
	{
		OpenEditors.opened();
		OpenEditors.opened();

		OpenEditors.forgetAll();

		assertFalse(OpenEditors.anyOpen(),
			"an editor over a vault that is gone counts for nothing");
	}
}
