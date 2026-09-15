/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.clipboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClipboardClearDecisionTest
{

	@Test
	@DisplayName("before the interval has passed, nothing is cleared")
	void shouldClear_isFalse_beforeTheIntervalElapses()
	{
		long armedAt = 1_000_000L;
		long almostThere = armedAt + TimeUnit.SECONDS.toMillis(19);

		assertFalse(ClipboardClearDecision.shouldClear(armedAt, almostThere, 20),
			"one second short of the configured interval must not clear yet");
	}

	@Test
	@DisplayName("once the interval has passed, it clears")
	void shouldClear_isTrue_onceTheIntervalElapses()
	{
		long armedAt = 1_000_000L;
		long exactlyThere = armedAt + TimeUnit.SECONDS.toMillis(20);
		long wellPast = armedAt + TimeUnit.SECONDS.toMillis(45);

		assertTrue(ClipboardClearDecision.shouldClear(armedAt, exactlyThere, 20),
			"the boundary itself counts - a check that runs exactly on time must not wait for "
				+ "the next tick");
		assertTrue(ClipboardClearDecision.shouldClear(armedAt, wellPast, 20));
	}

	@Test
	@DisplayName("0 turns it off, whatever the elapsed time")
	void shouldClear_isFalse_whenTheIntervalIsOff()
	{
		long armedAt = 1_000_000L;
		long farInTheFuture = armedAt + TimeUnit.DAYS.toMillis(1);

		assertFalse(
			ClipboardClearDecision.shouldClear(armedAt, farInTheFuture, ClipboardClearDecision.OFF),
			"0 is the documented off switch, matching the two lock "
				+ "timeouts this decision is modelled on");
	}

	@Test
	@DisplayName("a negative interval is treated as off too, not as an error")
	void shouldClear_isFalse_forANegativeInterval()
	{
		assertFalse(ClipboardClearDecision.shouldClear(0L, TimeUnit.DAYS.toMillis(1), -5),
			"a corrupted settings file with a negative number must not clear on every tick - "
				+ "off is the safe reading of a value that cannot mean anything else");
	}
}
