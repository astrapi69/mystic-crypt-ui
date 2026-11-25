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

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.util.Arrays;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Pause;

/**
 * Controls how fast AssertJ-Swing UI tests in this package step through their interactions.
 * <p>
 * Two modes, selected via the {@code mystic.crypt.ui.test.mode} system property:
 * <ul>
 * <li><b>fast</b> (default) - runs as fast as this shared, live desktop display allows. Pauses are
 * cut to the minimum that still keeps window-manager focus races from causing flaky clicks
 * <li><b>demo</b> - paces interactions like an unhurried real user, so a run can be watched on
 * screen (pass {@code -Dmystic.crypt.ui.test.mode=demo} to Gradle, e.g.
 * {@code ./gradlew test -Dmystic.crypt.ui.test.mode=demo})
 * </ul>
 */
final class UiTestSpeed
{

	static final String MODE_PROPERTY = "mystic.crypt.ui.test.mode";

	private static final boolean DEMO_MODE = "demo"
		.equalsIgnoreCase(System.getProperty(MODE_PROPERTY, "fast"));

	private static final int STABLE_READS = 4;

	private static final int STABLE_READ_INTERVAL_MILLIS = 50;

	private static final long PLACED_TIMEOUT_MILLIS = 5000;

	private UiTestSpeed()
	{
	}

	/**
	 * Pause between two user-visible interactions (e.g. after checking a box, before typing into
	 * the next field). No-op in fast mode
	 */
	static void step()
	{
		if (DEMO_MODE)
		{
			Pause.pause(600);
		}
	}

	/**
	 * Short pause purely to let the window manager hand focus to a just-raised window before the
	 * next click. Kept nonzero even in fast mode - this is a stability wait, not a user-pacing
	 * simulation, and this shared X display (no isolated Xvfb here) needs it either way
	 */
	static void windowManagerSettle()
	{
		Pause.pause(DEMO_MODE ? 400 : 80);
	}

	/**
	 * Waits until the given windows are showing, the event queue is empty and their bounds on
	 * screen have stayed the same in {@value #STABLE_READS} consecutive reads,
	 * {@value #STABLE_READ_INTERVAL_MILLIS} ms apart, and returns those bounds. The window manager
	 * of the test display (fluxbox on Xvfb) can report a window that was just shown at another
	 * place for a few milliseconds and then move it back, and can move the main window after it
	 * appears; a reading taken in that moment measures where the window was not (#962, #1130). A
	 * window that does not settle within {@value #PLACED_TIMEOUT_MILLIS} ms fails the test.
	 *
	 * @param windows
	 *            the windows to read, all of them at the same moment
	 * @return their bounds on screen, in the order given
	 * @throws AssertionError
	 *             if the windows keep moving, naming their last bounds
	 */
	static Rectangle[] awaitSettled(final Window... windows)
	{
		Rectangle[] last = null;
		int sameReads = 0;
		long deadline = System.currentTimeMillis() + PLACED_TIMEOUT_MILLIS;
		while (System.currentTimeMillis() < deadline)
		{
			IdleRobot.ROBOT.waitForIdle();
			Rectangle[] current = GuiActionRunner.execute(() -> boundsOnScreen(windows));
			sameReads = current != null && Arrays.equals(current, last) ? sameReads + 1 : 1;
			if (current != null && sameReads >= STABLE_READS)
			{
				return current;
			}
			last = current;
			Pause.pause(STABLE_READ_INTERVAL_MILLIS);
		}
		throw new AssertionError("the windows did not settle within " + PLACED_TIMEOUT_MILLIS
			+ " ms, last bounds " + Arrays.toString(last) + " (#962, #1130)");
	}

	/**
	 * Waits until the given window is no longer showing, which is what a click on its confirming
	 * button must cause. A window that stays within {@value #PLACED_TIMEOUT_MILLIS} ms fails the
	 * test, so a click that did not arrive is reported as that and not as a model that stayed empty
	 * (#962).
	 *
	 * @param window
	 *            the window the click is meant to close
	 * @throws AssertionError
	 *             if the window is still showing in time, naming it
	 */
	static void awaitClosed(final Window window)
	{
		long deadline = System.currentTimeMillis() + PLACED_TIMEOUT_MILLIS;
		while (System.currentTimeMillis() < deadline)
		{
			IdleRobot.ROBOT.waitForIdle();
			if (!GuiActionRunner.execute(window::isShowing))
			{
				return;
			}
			Pause.pause(STABLE_READ_INTERVAL_MILLIS);
		}
		String name = GuiActionRunner.execute(window::getName);
		throw new AssertionError("the click did not close the window " + name + " within "
			+ PLACED_TIMEOUT_MILLIS + " ms, it is still showing (#962)");
	}

	private static Rectangle[] boundsOnScreen(final Window... windows)
	{
		Rectangle[] bounds = new Rectangle[windows.length];
		for (int index = 0; index < windows.length; index++)
		{
			if (!windows[index].isShowing())
			{
				return null;
			}
			bounds[index] = new Rectangle(windows[index].getLocationOnScreen(),
				windows[index].getSize());
		}
		return bounds;
	}

	/** The robot that empties the event queue, made once, when a wait first needs it */
	private static final class IdleRobot
	{
		private static final Robot ROBOT = create();

		private static Robot create()
		{
			try
			{
				return new Robot();
			}
			catch (final AWTException exception)
			{
				throw new IllegalStateException("no robot on the test display", exception);
			}
		}
	}
}
