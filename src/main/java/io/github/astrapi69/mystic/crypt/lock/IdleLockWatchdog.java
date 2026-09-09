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
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

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

	private final BooleanSupplier signedIn;

	private final IntSupplier timeoutMinutes;

	private final Runnable lockWorkspace;

	private final Timer timer;

	private volatile long lastActivityMillis = System.currentTimeMillis();

	/** Kept so {@link #stop()} can take it off the shared toolkit again */
	private AWTEventListener activityListener;

	/**
	 * Instantiates a watchdog over the state it needs, as suppliers rather than as values: the
	 * timeout is read again on every check, so changing it in the settings takes effect without
	 * restarting anything
	 *
	 * @param signedIn
	 *            answers whether a vault is open and unlocked
	 * @param timeoutMinutes
	 *            answers the configured timeout in minutes
	 * @param lockWorkspace
	 *            what to run when the workspace should lock
	 */
	public IdleLockWatchdog(final BooleanSupplier signedIn, final IntSupplier timeoutMinutes,
		final Runnable lockWorkspace)
	{
		this.signedIn = signedIn;
		this.timeoutMinutes = timeoutMinutes;
		this.lockWorkspace = lockWorkspace;
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
		lastActivityMillis = System.currentTimeMillis();
	}

	/**
	 * How long the user has been away, in milliseconds
	 *
	 * @return the idle time
	 */
	public long idleMillis()
	{
		return System.currentTimeMillis() - lastActivityMillis;
	}

	private void onCheck(final ActionEvent actionEvent)
	{
		if (IdleLockDecision.shouldLock(signedIn.getAsBoolean(), idleMillis(),
			timeoutMinutes.getAsInt()))
		{
			// the clock is reset here rather than by the lock itself: locking puts up the unlock
			// prompt, and until somebody answers it the idle time keeps growing, so a second tick
			// would ask to lock an already locked workspace over and over
			noteActivity();
			lockWorkspace.run();
		}
	}
}
