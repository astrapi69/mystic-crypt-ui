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

	/**
	 * A clock that does not start at zero. At zero, "now minus the last activity" and "now plus the
	 * last activity" are the same number, and a test that starts there cannot tell the idle time
	 * from its own mirror image
	 */
	private static final long A_NON_ZERO_INSTANT = TimeUnit.HOURS.toMillis(9);

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
		WorkspaceDouble workspace = new WorkspaceDouble();
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 15, clock::get);

		watchdog.noteActivity();

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"the user was here a moment ago");
		assertEquals(0, workspace.locks, "so nothing was locked");
		assertEquals(0, workspace.closes, "and nothing was closed");
	}

	@Test
	@DisplayName("the watchdog locks once and does not keep asking while the prompt is up")
	void theWatchdogLocksOnce_andResetsItsOwnClock()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 0, clock::get);

		clock.addAndGet(FIFTEEN_MINUTES);

		assertEquals(IdleLockWatchdog.Outcome.LOCKED, watchdog.checkNow(),
			"fifteen minutes of nothing is what this is for");
		assertEquals(1, workspace.locks,
			"and the workspace is actually locked, not merely decided on");
		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"the second tick must not lock again: locking puts the unlock prompt up, and until "
				+ "somebody answers it the idle time keeps growing, so a watchdog that does not "
				+ "reset its own clock stacks prompts");
		assertEquals(1, workspace.locks);
	}

	@Test
	@DisplayName("the clock is reset by the lock itself, not by the workspace happening to change")
	void theWatchdogResetsItsIdleClock_evenWhenLockingDidNotTake()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		// lock() deliberately leaves the workspace signed in: the real one locks asynchronously,
		// and a watchdog that only stops asking because the state changed under it would stack
		// unlock prompts the moment locking is slow or refused
		workspace.lockingTakesEffect = false;
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 0, clock::get);

		clock.addAndGet(FIFTEEN_MINUTES);
		assertEquals(IdleLockWatchdog.Outcome.LOCKED, watchdog.checkNow());
		assertEquals(0L, watchdog.idleMillis(),
			"locking resets the idle clock, which is what stops the next tick from locking again");

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow());
		assertEquals(1, workspace.locks, "one lock, one prompt");
	}

	@Test
	@DisplayName("a timeout of zero never locks, however long the user is away")
	void theWatchdogNeverLocks_whenTheTimeoutIsOff()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> IdleLockDecision.OFF,
			() -> IdleLockDecision.OFF, clock::get);

		clock.addAndGet(FIFTEEN_MINUTES * 4);

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"switched off has to mean switched off where the decision is taken, not only in the "
				+ "dialog that offers the value");
		assertEquals(0, workspace.locks);
	}

	@Test
	@DisplayName("a vault stays locked for the close timeout, then it is closed")
	void theWatchdogClosesTheVault_afterItHasBeenLockedLongEnough()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 15, clock::get);

		clock.addAndGet(FIFTEEN_MINUTES);
		assertEquals(IdleLockWatchdog.Outcome.LOCKED, watchdog.checkNow());
		assertEquals(0, workspace.closes,
			"locking and closing must not happen in one breath: the locked clock starts on the "
				+ "tick that locks");

		clock.addAndGet(FIFTEEN_MINUTES - 1);
		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"one millisecond short of the timeout is still short of it");

		clock.addAndGet(1);
		assertEquals(IdleLockWatchdog.Outcome.CLOSED, watchdog.checkNow(),
			"and now the decrypted vault leaves memory, which locking alone never did (#242)");
		assertEquals(1, workspace.closes);
	}

	@Test
	@DisplayName("a vault locked by hand is measured from the first check that sees it")
	void theWatchdogClosesAVault_thatSomebodyElseLocked()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		workspace.signedIn = false;
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 15, clock::get);

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"the first check only starts the clock - the workspace was already locked when this "
				+ "watchdog first looked, and it cannot know for how long");

		clock.addAndGet(FIFTEEN_MINUTES);

		assertEquals(IdleLockWatchdog.Outcome.CLOSED, watchdog.checkNow(),
			"the menu entry locks too, and a vault locked that way has to be bounded the same way");
	}

	@Test
	@DisplayName("unsaved changes keep a locked vault open, whatever the timeout says")
	void theWatchdogDoesNotClose_whenThereAreUnsavedChanges()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		workspace.signedIn = false;
		workspace.unsavedChanges = true;
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 15, clock::get);

		watchdog.checkNow();
		clock.addAndGet(FIFTEEN_MINUTES * 4);

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"a locked workspace has no master password, so pending changes cannot be written and "
				+ "closing would drop them. Locking saves first; this is what happens when that "
				+ "save failed, and losing somebody's entries is never the cheaper trade");
		assertEquals(0, workspace.closes);
	}

	@Test
	@DisplayName("with nothing open there is nothing to close")
	void theWatchdogDoesNotClose_whenNoVaultIsOpen()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		workspace.signedIn = false;
		workspace.aVaultIsOpen = false;
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 15, clock::get);

		watchdog.checkNow();
		clock.addAndGet(FIFTEEN_MINUTES * 4);

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow());
		assertEquals(0, workspace.closes);
	}

	@Test
	@DisplayName("unlocking clears the locked clock, so the next lock starts a fresh one")
	void theWatchdogForgetsTheLockedClock_whenTheWorkspaceIsUnlockedAgain()
	{
		WorkspaceDouble workspace = new WorkspaceDouble();
		workspace.signedIn = false;
		AtomicLong clock = new AtomicLong(A_NON_ZERO_INSTANT);
		IdleLockWatchdog watchdog = new IdleLockWatchdog(workspace, () -> 15, () -> 15, clock::get);

		watchdog.checkNow();
		clock.addAndGet(FIFTEEN_MINUTES - 1);
		workspace.signedIn = true;
		watchdog.noteActivity();
		watchdog.checkNow();
		workspace.signedIn = false;
		watchdog.checkNow();
		clock.addAndGet(FIFTEEN_MINUTES - 1);

		assertEquals(IdleLockWatchdog.Outcome.NOTHING, watchdog.checkNow(),
			"the fourteen minutes it was locked before somebody unlocked it must not count "
				+ "towards this lock - otherwise unlocking and locking again closes the vault "
				+ "under the user's hands");
	}

	/**
	 * A workspace that counts what was done to it, so the watchdog's behaviour can be measured
	 * without a display and without a real application frame
	 */
	private static final class WorkspaceDouble implements LockableWorkspace
	{
		private boolean signedIn = true;

		private boolean aVaultIsOpen = true;

		private boolean unsavedChanges;

		private boolean lockingTakesEffect = true;

		private int locks;

		private int closes;

		@Override
		public boolean isSignedIn()
		{
			return signedIn;
		}

		@Override
		public boolean aVaultIsOpen()
		{
			return aVaultIsOpen;
		}

		@Override
		public boolean hasUnsavedChanges()
		{
			return unsavedChanges;
		}

		@Override
		public void lock()
		{
			locks++;
			if (lockingTakesEffect)
			{
				signedIn = false;
			}
		}

		@Override
		public void closeVault()
		{
			closes++;
			aVaultIsOpen = false;
		}
	}
}
