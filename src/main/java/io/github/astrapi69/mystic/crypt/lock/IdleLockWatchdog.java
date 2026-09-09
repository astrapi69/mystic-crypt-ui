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

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import javax.swing.Timer;

/**
 * Locks the workspace once the user has been away long enough (#241).
 * <p>
 * WHAT COUNTS AS ACTIVITY is defined here and read from one place, rather than per panel: a key
 * press, a mouse button, a mouse wheel or a mouse move anywhere in this application's windows,
 * observed on the shared event queue. A panel that forgets to report itself cannot make the
 * application think its user has left, and a panel added later needs no wiring at all.
 * <p>
 * Deliberately NOT counted: the timer's own ticks and the repaints they cause. A clock that resets
 * the clock never fires - which is the failure mode that makes an idle timeout look like it works
 * while it protects nothing.
 * <p>
 * Whether to lock is {@link IdleLockDecision}'s answer. This class only measures the time, asks,
 * and runs what the answer says.
 */
public final class IdleLockWatchdog
{

	/**
	 * How often the idle time is checked. Well below any sensible timeout, so the vault is locked
	 * within a few seconds of the timeout rather than up to a whole timeout late
	 */
	private static final int CHECK_INTERVAL_MILLIS = (int)TimeUnit.SECONDS.toMillis(10);

	private static final long ACTIVITY_EVENT_MASK = AWTEvent.KEY_EVENT_MASK
		| AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
		| AWTEvent.MOUSE_WHEEL_EVENT_MASK;

	private final LockableWorkspace workspace;

	private final IntSupplier idleTimeoutMinutes;

	private final IntSupplier closeLockedTimeoutMinutes;

	private final Timer timer;

	/**
	 * Where "now" comes from. A parameter rather than a direct call to the system clock so a test
	 * can place the last activity fifteen minutes in the past instead of waiting fifteen minutes
	 */
	private final LongSupplier clock;

	private volatile long lastActivityMillis;

	/**
	 * When the workspace was seen locked with a vault still open, or null while it is not in that
	 * state. Measured here rather than by the lock action, so a workspace locked before this
	 * watchdog existed - or locked by any path that does not report to it - still starts its clock
	 * on the first check that sees it.
	 * <p>
	 * A {@code Long} rather than a {@code long} with 0 for "not locked": 0 is a perfectly good
	 * instant, and a clock that starts there - a test's does - would read "locked since 0" as "not
	 * locked" and never close anything
	 */
	private volatile Long lockedSinceMillis;

	/** Kept so {@link #stop()} can take it off the shared toolkit again */
	private AWTEventListener activityListener;

	/**
	 * Instantiates a watchdog over the workspace it guards. The timeouts are suppliers rather than
	 * values, so changing either in the settings takes effect on the next check instead of after a
	 * restart
	 *
	 * @param workspace
	 *            the workspace to watch and, when it comes to it, to lock and close
	 * @param idleTimeoutMinutes
	 *            after how many idle minutes an unlocked workspace locks itself
	 * @param closeLockedTimeoutMinutes
	 *            after how many further minutes a locked vault is closed altogether
	 */
	public IdleLockWatchdog(final LockableWorkspace workspace, final IntSupplier idleTimeoutMinutes,
		final IntSupplier closeLockedTimeoutMinutes)
	{
		this(workspace, idleTimeoutMinutes, closeLockedTimeoutMinutes, System::currentTimeMillis);
	}

	/**
	 * Instantiates a watchdog over an explicit clock, so a test can measure what happens after
	 * fifteen idle minutes without spending fifteen minutes
	 *
	 * @param workspace
	 *            the workspace to watch and, when it comes to it, to lock and close
	 * @param idleTimeoutMinutes
	 *            after how many idle minutes an unlocked workspace locks itself
	 * @param closeLockedTimeoutMinutes
	 *            after how many further minutes a locked vault is closed altogether
	 * @param clock
	 *            where "now" comes from, in milliseconds
	 */
	public IdleLockWatchdog(final LockableWorkspace workspace, final IntSupplier idleTimeoutMinutes,
		final IntSupplier closeLockedTimeoutMinutes, final LongSupplier clock)
	{
		this.workspace = workspace;
		this.idleTimeoutMinutes = idleTimeoutMinutes;
		this.closeLockedTimeoutMinutes = closeLockedTimeoutMinutes;
		this.clock = clock;
		this.lastActivityMillis = clock.getAsLong();
		this.timer = new Timer(CHECK_INTERVAL_MILLIS, this::onCheck);
		this.timer.setRepeats(true);
	}

	/**
	 * Starts watching: from here on every user event resets the idle clock, and the workspace locks
	 * itself once the clock passes the configured timeout
	 */
	public void start()
	{
		noteActivity();
		if (activityListener == null)
		{
			activityListener = event -> noteActivity();
			Toolkit.getDefaultToolkit().addAWTEventListener(activityListener, ACTIVITY_EVENT_MASK);
		}
		timer.start();
	}

	/**
	 * Stops watching and takes the listener off the shared toolkit again.
	 * <p>
	 * The listener is registered on the DEFAULT toolkit, which outlives any one frame, and it holds
	 * this watchdog, which holds the frame's state through its suppliers. Leaving it there keeps
	 * every closed window's watchdog alive and asking a disposed frame for its model - which is
	 * exactly what a suite that builds a hundred frames would do.
	 */
	public void stop()
	{
		timer.stop();
		if (activityListener != null)
		{
			Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
			activityListener = null;
		}
	}

	/** Records that the user did something just now */
	public void noteActivity()
	{
		lastActivityMillis = clock.getAsLong();
	}

	/**
	 * How long the user has been away, in milliseconds
	 *
	 * @return the idle time
	 */
	public long idleMillis()
	{
		return clock.getAsLong() - lastActivityMillis;
	}

	private void onCheck(final ActionEvent actionEvent)
	{
		checkNow();
	}

	/**
	 * Asks both decisions once and acts on their answers. This is what the timer's tick does; it is
	 * public so it can be asked outside the timer's rhythm - a test measures what happens after
	 * fifteen idle minutes without spending fifteen of them.
	 * <p>
	 * Locking is asked first and closing second, deliberately: a workspace that has just locked
	 * itself starts its locked clock on this same tick, so it cannot be locked and closed in one
	 * breath
	 *
	 * @return what this check did to the workspace
	 */
	public Outcome checkNow()
	{
		if (IdleLockDecision.shouldLock(workspace.isSignedIn(), idleMillis(),
			idleTimeoutMinutes.getAsInt()))
		{
			// the clock is reset here rather than by the lock itself: locking puts up the unlock
			// prompt, and until somebody answers it the idle time keeps growing, so a second tick
			// would ask to lock an already locked workspace over and over
			noteActivity();
			lockedSinceMillis = clock.getAsLong();
			workspace.lock();
			return Outcome.LOCKED;
		}
		noteWhetherItIsLocked();
		if (IdleLockDecision.shouldCloseLockedVault(workspace.aVaultIsOpen(),
			workspace.isSignedIn(), workspace.hasUnsavedChanges(), lockedMillis(),
			closeLockedTimeoutMinutes.getAsInt()))
		{
			lockedSinceMillis = null;
			workspace.closeVault();
			return Outcome.CLOSED;
		}
		return Outcome.NOTHING;
	}

	/**
	 * Starts or clears the locked clock from what the workspace looks like now, so a workspace
	 * locked by any path - the menu entry, a plugin, this watchdog - is measured from the first
	 * check that sees it locked
	 */
	private void noteWhetherItIsLocked()
	{
		boolean locked = workspace.aVaultIsOpen() && !workspace.isSignedIn();
		if (!locked)
		{
			lockedSinceMillis = null;
			return;
		}
		if (lockedSinceMillis == null)
		{
			lockedSinceMillis = clock.getAsLong();
		}
	}

	/**
	 * How long the workspace has been locked with a vault open, in milliseconds; 0 when it is not
	 * in that state
	 *
	 * @return the locked time
	 */
	public long lockedMillis()
	{
		return lockedSinceMillis == null ? 0L : clock.getAsLong() - lockedSinceMillis;
	}

	/** What one check did to the workspace */
	public enum Outcome
	{
		/** the workspace was locked because its user had been away too long */
		LOCKED,

		/** the locked vault was closed, so its decrypted content left memory */
		CLOSED,

		/** nothing was due */
		NOTHING
	}
}
