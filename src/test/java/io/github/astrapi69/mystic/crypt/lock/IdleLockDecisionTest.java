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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * When an idle workspace locks itself, and when it must not (#241).
 */
class IdleLockDecisionTest
{

	private static final long FIFTEEN_MINUTES = TimeUnit.MINUTES.toMillis(15);

	@Test
	@DisplayName("a workspace idle past the timeout locks itself")
	void shouldLock_isTrue_whenTheUserHasBeenAwayLongerThanTheTimeout()
	{
		assertTrue(IdleLockDecision.shouldLock(true, FIFTEEN_MINUTES, 15),
			"the realistic case for a password manager is the one where nobody remembers to lock "
				+ "it, because the user walked away");
	}

	@ParameterizedTest(name = "signedIn={0}, idle={1}ms, timeout={2}min does not lock")
	@CsvSource({ "false, 3600000, 15", "true, 60000, 15", "true, 3600000, 0", "true, 3600000, -1" })
	@DisplayName("the cases that must not lock")
	void shouldLock_isFalse_forTheCasesThatMustNotLock(final boolean signedIn,
		final long idleMillis, final int timeoutMinutes)
	{
		assertFalse(IdleLockDecision.shouldLock(signedIn, idleMillis, timeoutMinutes));
	}

	@ParameterizedTest(name = "idle {0} of a 15 minute timeout")
	@CsvSource({ "899999, false", "900000, true", "900001, true" })
	@DisplayName("the boundary is the timeout itself, and it is inclusive")
	void shouldLock_atTheBoundary(final long idleMillis, final boolean expected)
	{
		assertEquals(expected, IdleLockDecision.shouldLock(true, idleMillis, 15));
	}

	@Test
	@DisplayName("nothing locks while the user is there")
	void theWatchdogDoesNotLock_whileThereIsActivity()
	{
		AtomicInteger locks = new AtomicInteger();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(() -> true, () -> 15,
			locks::incrementAndGet);

		watchdog.noteActivity();

		assertFalse(watchdog.checkNow(), "the user was here a moment ago");
		assertTrue(watchdog.idleMillis() < FIFTEEN_MINUTES,
			"activity resets the clock, which is the whole mechanism");
		assertEquals(0, locks.get(), "so nothing was locked");
	}

	@Test
	@DisplayName("the watchdog locks once and does not keep asking while the prompt is up")
	void theWatchdogLocksOnce_andResetsItsOwnClock()
	{
		AtomicInteger locks = new AtomicInteger();
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(() -> true, () -> 15,
			locks::incrementAndGet, clock::get);

		clock.set(FIFTEEN_MINUTES);

		assertTrue(watchdog.checkNow(), "fifteen minutes of nothing is what this is for");
		assertEquals(1, locks.get(), "and the workspace is actually locked, not merely decided on");
		assertFalse(watchdog.checkNow(),
			"the second tick must not lock again: locking puts the unlock prompt up, and until "
				+ "somebody answers it the idle time keeps growing, so a watchdog that does not "
				+ "reset its own clock stacks prompts");
		assertEquals(1, locks.get());
	}

	@Test
	@DisplayName("a timeout of zero never locks, however long the user is away")
	void theWatchdogNeverLocks_whenTheTimeoutIsOff()
	{
		AtomicInteger locks = new AtomicInteger();
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(() -> true, () -> IdleLockDecision.OFF,
			locks::incrementAndGet, clock::get);

		clock.set(FIFTEEN_MINUTES * 4);

		assertFalse(watchdog.checkNow(),
			"switched off has to mean switched off where the decision is taken, not only in the "
				+ "dialog that offers the value");
		assertEquals(0, locks.get());
	}
}
