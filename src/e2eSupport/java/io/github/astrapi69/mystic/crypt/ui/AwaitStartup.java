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
package io.github.astrapi69.mystic.crypt.ui;

import java.util.concurrent.TimeUnit;

import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

import io.github.astrapi69.mystic.crypt.StartupSignal;

/**
 * Waits for the steps the application under test reports through {@link StartupSignal} (#522).
 * <p>
 * A UI test used to wait for a window to be showing, 15 seconds at most. Whether a window shows in
 * 15 seconds depends on the machine: on CI the start was still building the sign-in dialog, or the
 * event thread was waiting on the X server, when the 15 seconds ran out, and the next run of the
 * same commit was green (measured in #522). The application now says when it has reached a step, so
 * the wait is for that step. It is generous because it costs nothing when the step comes, and when
 * it does not come the failure names the step the start had reached.
 */
final class AwaitStartup
{

	/** How long a step may take before the test fails */
	static final long TIMEOUT_SECONDS = 120;

	private AwaitStartup()
	{
	}

	/** Switches the signal on and starts its counts from zero, before each test */
	static void switchOn()
	{
		System.setProperty(StartupSignal.PROPERTY, Boolean.TRUE.toString());
		StartupSignal.reset();
	}

	/** Switches the signal off again, after each test */
	static void switchOff()
	{
		StartupSignal.reset();
		System.clearProperty(StartupSignal.PROPERTY);
	}

	/**
	 * Waits until one more sign-in dialog has opened than the given count, and returns the new
	 * count. Stops waiting early when the start thread has ended without it, which a test then
	 * reports instead of a timeout.
	 *
	 * @param alreadySeen
	 *            the number of sign-in dialogs the test has already seen
	 * @param startThread
	 *            the thread that runs the start, or null when the application is already running
	 * @return how many sign-in dialogs have opened now
	 */
	static int signInDialogOpened(final int alreadySeen, final Thread startThread)
	{
		Pause.pause(new Condition("the application reports that its sign-in dialog has opened")
		{
			@Override
			public boolean test()
			{
				return StartupSignal.signInDialogsShown() > alreadySeen
					|| (startThread != null && !startThread.isAlive());
			}

			@Override
			protected String descriptionAddendum()
			{
				return whereTheStartStands();
			}
		}, TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
		int shown = StartupSignal.signInDialogsShown();
		if (shown <= alreadySeen)
		{
			throw new IllegalStateException("the start thread ended before the sign-in dialog "
				+ "opened" + whereTheStartStands());
		}
		return shown;
	}

	/** Waits until no start is running any more: every start that has begun has completed */
	static void startCompleted()
	{
		Pause.pause(new Condition("the application reports that its start has completed")
		{
			@Override
			public boolean test()
			{
				return !StartupSignal.startInProgress();
			}

			@Override
			protected String descriptionAddendum()
			{
				return whereTheStartStands();
			}
		}, TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
	}

	private static String whereTheStartStands()
	{
		return " (starts begun: " + StartupSignal.startsBegun() + ", sign-in dialogs opened: "
			+ StartupSignal.signInDialogsShown() + ", starts completed: "
			+ StartupSignal.startsCompleted() + ")";
	}
}
