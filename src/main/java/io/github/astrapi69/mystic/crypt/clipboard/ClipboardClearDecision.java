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

import java.util.concurrent.TimeUnit;

/**
 * Whether enough time has passed to clear what this application put on the clipboard (#352).
 * <p>
 * "Copy Password" and "Copy Username" left whatever they put there readable by every other process
 * on the machine, every clipboard manager, and whatever the desktop environment syncs clipboards
 * to, until the next lock or close - up to the idle timeout, unbounded if the user kept working.
 * Every comparable password manager clears the clipboard on a short timer; this is that timer's
 * decision, asked the same way {@link io.github.astrapi69.mystic.crypt.lock.IdleLockDecision} asks
 * whether to lock - a function of state rather than something a Swing timer decides for itself, so
 * it can be unit tested without a display and mutated.
 * <p>
 * Answering "should I clear" is only half of what makes this safe. WHETHER to actually clear once
 * the time is up is a second question this class does not answer: clearing unconditionally can
 * destroy something the user copied from elsewhere in the meantime, so the caller compares the
 * clipboard's current content against what it put there and clears only a match. That comparison
 * needs the live system clipboard, which is not a decision and does not belong here.
 */
public final class ClipboardClearDecision
{

	/** The interval a fresh installation gets, in seconds */
	public static final int DEFAULT_CLEAR_AFTER_SECONDS = 20;

	/** The value that turns the automatic clear off */
	public static final int OFF = 0;

	private ClipboardClearDecision()
	{
	}

	/**
	 * Whether whatever was armed at {@code armedAtMillis} has been sitting long enough to clear now
	 *
	 * @param armedAtMillis
	 *            when the content was put on the clipboard
	 * @param nowMillis
	 *            the current time, in the same clock as {@code armedAtMillis}
	 * @param clearAfterSeconds
	 *            the configured interval; {@link #OFF} or below never clears
	 * @return true once the interval has elapsed
	 */
	public static boolean shouldClear(final long armedAtMillis, final long nowMillis,
		final int clearAfterSeconds)
	{
		if (clearAfterSeconds <= OFF)
		{
			return false;
		}
		return nowMillis - armedAtMillis >= TimeUnit.SECONDS.toMillis(clearAfterSeconds);
	}
}
