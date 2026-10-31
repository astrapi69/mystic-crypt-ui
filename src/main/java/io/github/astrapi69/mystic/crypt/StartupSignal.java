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
package io.github.astrapi69.mystic.crypt;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Says how far the start of the application has come, for a harness that drives it (#522).
 * <p>
 * The start counts three steps: it has begun, its sign-in dialog has opened, and it has completed -
 * the last statement of the frame's initialization has run. A harness waits for the next count
 * instead of for a window that happens to be showing: whether a window shows within some seconds
 * depends on the machine, whether a step has been reached does not.
 * <p>
 * Only in test mode, when the system property {@value #PROPERTY} is {@code true}. A real start
 * never sets it, so there nothing is counted and nothing reads the counts.
 */
public final class StartupSignal
{

	/** The system property that switches the signal on */
	public static final String PROPERTY = "mystic.crypt.ui.startup.signal";

	private static final AtomicInteger STARTS_BEGUN = new AtomicInteger();

	private static final AtomicInteger SIGN_IN_DIALOGS_SHOWN = new AtomicInteger();

	private static final AtomicInteger STARTS_COMPLETED = new AtomicInteger();

	private StartupSignal()
	{
	}

	/** The start has begun: the frame's initialization has started */
	public static void startBegun()
	{
		countInTestMode(STARTS_BEGUN);
	}

	/** The sign-in dialog has opened on the screen and takes input */
	public static void signInDialogShown()
	{
		countInTestMode(SIGN_IN_DIALOGS_SHOWN);
	}

	/** The start has completed: the last statement of the frame's initialization has run */
	public static void startCompleted()
	{
		countInTestMode(STARTS_COMPLETED);
	}

	/**
	 * How many starts have begun since the last reset
	 *
	 * @return the count
	 */
	public static int startsBegun()
	{
		return STARTS_BEGUN.get();
	}

	/**
	 * How many sign-in dialogs have opened since the last reset
	 *
	 * @return the count
	 */
	public static int signInDialogsShown()
	{
		return SIGN_IN_DIALOGS_SHOWN.get();
	}

	/**
	 * How many starts have completed since the last reset
	 *
	 * @return the count
	 */
	public static int startsCompleted()
	{
		return STARTS_COMPLETED.get();
	}

	/**
	 * Whether a start has begun and not yet completed
	 *
	 * @return true while a start is running
	 */
	public static boolean startInProgress()
	{
		return STARTS_COMPLETED.get() < STARTS_BEGUN.get();
	}

	/** Sets every count back to zero, for the next test in the same JVM */
	public static void reset()
	{
		STARTS_BEGUN.set(0);
		SIGN_IN_DIALOGS_SHOWN.set(0);
		STARTS_COMPLETED.set(0);
	}

	private static void countInTestMode(final AtomicInteger count)
	{
		if (Boolean.getBoolean(PROPERTY))
		{
			count.incrementAndGet();
		}
	}
}
