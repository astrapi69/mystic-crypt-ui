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

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import javax.swing.Timer;

import io.github.astrapi69.awt.extension.ClipboardExtensions;
import io.github.astrapi69.mystic.crypt.vault.SecretBuffers;

/**
 * Clears the system clipboard some time after this application put a secret on it (#352).
 * <p>
 * Whether to clear at all is {@link ClipboardClearDecision}'s answer; this class only measures the
 * time, asks, and acts - the same split {@code IdleLockWatchdog} makes for the lock decision.
 * <p>
 * CLEARING IS CONDITIONAL, not unconditional: this remembers what it put on the clipboard and, once
 * the interval has passed, clears only if the clipboard still holds exactly that. A user who copied
 * something else in the meantime is left alone - an unconditional clear would destroy that instead,
 * which is worse than leaving a stale secret a few seconds too long. Comparing means holding a
 * second copy of the secret to compare against; it is wiped the moment the check runs, whether or
 * not it matched.
 */
public final class ClipboardClearWatchdog
{

	/**
	 * How often the clipboard is checked. Deliberately much finer than {@code IdleLockWatchdog}'s
	 * ten seconds: the whole interval this guards is measured in tens of seconds, not minutes, so a
	 * coarse check would arrive visibly late
	 */
	private static final int CHECK_INTERVAL_MILLIS = 1000;

	private final IntSupplier clearAfterSeconds;

	/**
	 * Where "now" comes from. A parameter rather than a direct call to the system clock so a test
	 * can place the arming moment twenty seconds in the past instead of waiting twenty seconds
	 */
	private final LongSupplier clock;

	private final Timer timer;

	private volatile char[] armedContent;

	private volatile long armedAtMillis;

	/**
	 * Instantiates a watchdog reading its interval from the given supplier, so changing it in the
	 * settings takes effect on the next check instead of after a restart
	 *
	 * @param clearAfterSeconds
	 *            how many seconds after arming the clipboard is cleared;
	 *            {@link ClipboardClearDecision#OFF} turns it off
	 */
	public ClipboardClearWatchdog(final IntSupplier clearAfterSeconds)
	{
		this(clearAfterSeconds, System::currentTimeMillis);
	}

	/**
	 * Instantiates a watchdog over an explicit clock, so a test can measure what happens after the
	 * interval without spending it
	 *
	 * @param clearAfterSeconds
	 *            how many seconds after arming the clipboard is cleared
	 * @param clock
	 *            where "now" comes from, in milliseconds
	 */
	public ClipboardClearWatchdog(final IntSupplier clearAfterSeconds, final LongSupplier clock)
	{
		this.clearAfterSeconds = clearAfterSeconds;
		this.clock = clock;
		this.timer = new Timer(CHECK_INTERVAL_MILLIS, this::onCheck);
		this.timer.setRepeats(true);
	}

	/** Starts the periodic check */
	public void start()
	{
		timer.start();
	}

	/** Stops the periodic check and wipes whatever was still armed */
	public void stop()
	{
		timer.stop();
		forget();
	}

	/**
	 * Records that this content was just put on the clipboard, so it can be cleared again once the
	 * configured interval has passed. A second call before the first has fired replaces the armed
	 * content - the earlier copy is what was actually left on the clipboard, and this method wipes
	 * its own held copy of it before taking the new one, the same discipline
	 * {@code MasterPwFileModelBean.setMasterPw} keeps for the same reason (#351)
	 *
	 * @param copiedContent
	 *            exactly what was written to the clipboard; read, not modified, and cloned before
	 *            being held
	 */
	public void arm(final char[] copiedContent)
	{
		forget();
		this.armedContent = copiedContent.clone();
		this.armedAtMillis = clock.getAsLong();
	}

	private void forget()
	{
		SecretBuffers.wipe(armedContent);
		armedContent = null;
	}

	private void onCheck(final ActionEvent actionEvent)
	{
		checkNow();
	}

	/**
	 * Asks the decision once and acts on it. This is what the timer's tick does; it is public so a
	 * test can ask it outside the timer's rhythm
	 *
	 * @return what this check did
	 */
	public Outcome checkNow()
	{
		char[] expected = armedContent;
		if (expected == null)
		{
			return Outcome.NOTHING;
		}
		if (!ClipboardClearDecision.shouldClear(armedAtMillis, clock.getAsLong(),
			clearAfterSeconds.getAsInt()))
		{
			return Outcome.NOTHING;
		}
		Outcome outcome = clipboardStillHolds(expected) ? clear() : Outcome.SKIPPED;
		forget();
		return outcome;
	}

	private Outcome clear()
	{
		ClipboardExtensions.copyToClipboard("");
		return Outcome.CLEARED;
	}

	/**
	 * Whether the system clipboard still holds exactly what was armed. An unreadable or non-text
	 * clipboard answers false - "cannot tell it is still ours" is not "still ours".
	 * <p>
	 * Compared through a {@link java.nio.CharBuffer} view of the armed array rather than
	 * {@code new String(expected)}: the clipboard API only speaks {@link String}, so the value read
	 * back is one already, and wrapping the array instead of copying it is what keeps this from
	 * adding a second unwiped String of the same secret next to the one already necessary
	 *
	 * @param expected
	 *            what this watchdog put there
	 * @return true if the clipboard is unchanged since arming
	 */
	private static boolean clipboardStillHolds(final char[] expected)
	{
		try
		{
			Object current = Toolkit.getDefaultToolkit().getSystemClipboard()
				.getData(DataFlavor.stringFlavor);
			return current instanceof String currentText
				&& currentText.contentEquals(java.nio.CharBuffer.wrap(expected));
		}
		catch (Exception unreadable)
		{
			return false;
		}
	}

	/** What one check did to the clipboard */
	public enum Outcome
	{
		/** nothing was armed, or the interval has not passed yet */
		NOTHING,
		/** the interval passed and the clipboard still held what was armed, so it was cleared */
		CLEARED,
		/** the interval passed but the clipboard held something else, so nothing was touched */
		SKIPPED
	}
}
