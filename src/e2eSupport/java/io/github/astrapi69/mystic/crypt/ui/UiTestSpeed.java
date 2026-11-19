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

	private static final int STABLE_READS = 3;

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
	 * Waits until a window that has just been shown stays where it is. A click is computed from the
	 * window's location on screen, and the window manager can still move a new window after it is
	 * shown (measured: AWT reported a dialog at (1280,758) and then at (640,390)), so a click
	 * computed before that lands where the window was (#962). The location must be the same in
	 * {@value #STABLE_READS} consecutive reads, {@value #STABLE_READ_INTERVAL_MILLIS} ms apart
	 *
	 * @param window
	 *            the window that was just shown
	 * @throws AssertionError
	 *             if the window keeps moving, naming the last two locations
	 */
	static void awaitWindowPlaced(final java.awt.Window window)
	{
		java.awt.Point previous = null;
		java.awt.Point current = null;
		int sameReads = 0;
		long deadline = System.currentTimeMillis() + PLACED_TIMEOUT_MILLIS;
		while (System.currentTimeMillis() < deadline)
		{
			current = GuiActionRunner
				.execute(() -> window.isShowing() ? window.getLocationOnScreen() : null);
			sameReads = current != null && current.equals(previous) ? sameReads + 1 : 1;
			if (sameReads >= STABLE_READS)
			{
				return;
			}
			previous = current;
			Pause.pause(STABLE_READ_INTERVAL_MILLIS);
		}
		throw new AssertionError("the window was not placed within " + PLACED_TIMEOUT_MILLIS
			+ " ms: it was at " + previous + " and is at " + current + " (#962)");
	}
}
