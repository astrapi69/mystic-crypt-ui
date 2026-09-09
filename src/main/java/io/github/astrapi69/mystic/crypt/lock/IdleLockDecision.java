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

import java.util.concurrent.TimeUnit;

/**
 * Whether an idle workspace has been idle long enough to lock itself (#241).
 * <p>
 * Locking was a deliberate user action and nothing else: no idle timeout, no lock on screensaver,
 * no lock on suspend. #237 made locking actually lock, but only when somebody remembers to trigger
 * it - and the realistic case for a password manager is the one where nobody does, because the user
 * walked away.
 * <p>
 * The decision is a function of state rather than something a Swing timer decides for itself, for
 * the same reason {@link WorkspaceLockDecision} is: it can be unit tested and mutated here, and the
 * timer only wires its answer.
 */
public final class IdleLockDecision
{

	/** The timeout a fresh installation gets, in minutes */
	public static final int DEFAULT_TIMEOUT_MINUTES = 15;

	/** The value that turns the automatic lock off */
	public static final int OFF = 0;

	private IdleLockDecision()
	{
	}

	/**
	 * Whether the workspace should lock itself now.
	 * <p>
	 * A workspace that is not signed in is not locked again: it is already locked, or there is no
	 * vault at all, and locking either would put a second unlock prompt on screen over the first. A
	 * timeout of {@link #OFF} - or a negative one out of a hand-edited settings file - never locks,
	 * which is what "configurable and switchable off" has to mean at the deciding end rather than
	 * only in the dialog.
	 *
	 * @param signedIn
	 *            whether a vault is open and unlocked
	 * @param idleMillis
	 *            how long ago the last real user activity was
	 * @param timeoutMinutes
	 *            the configured timeout in minutes; {@link #OFF} or less turns it off
	 * @return true when the workspace should be locked now
	 */
	public static boolean shouldLock(final boolean signedIn, final long idleMillis,
		final int timeoutMinutes)
	{
		if (!signedIn || timeoutMinutes <= OFF)
		{
			return false;
		}
		return TimeUnit.MINUTES.toMillis(timeoutMinutes) <= idleMillis;
	}
}
