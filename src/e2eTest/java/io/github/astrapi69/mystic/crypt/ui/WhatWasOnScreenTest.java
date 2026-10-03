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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The harness says what was on screen when a test failed - which is what AssertJ-Swing cannot do
 * when its own component printer dies (#484).
 * <p>
 * Driven directly rather than by failing a real test on purpose: a failing test would have to stay
 * red to prove anything.
 */
class WhatWasOnScreenTest
{

	@Test
	@DisplayName("the report names the visible windows, their titles and every named component")
	void theReport_namesWhatWasThere() throws Exception
	{
		JFrame frame = onTheEventThread();
		try
		{
			String report = WhatWasOnScreen.report("a test that failed",
				new IllegalStateException("something went wrong"));

			assertTrue(report.startsWith(WhatWasOnScreen.MARKER), report);
			assertTrue(report.contains("a test that failed"), report);
			assertTrue(report.contains("IllegalStateException: something went wrong"), report);
			assertTrue(report.contains("\"a window a lookup would search\""),
				"the title is how a reader recognises the window: " + report);
			assertTrue(report.contains("btnTheOneThatWasThere"),
				"a lookup names what it wants, so the report has to name what was there: "
					+ report);
			assertFalse(report.contains("AssertJ-Swing's own component printer"),
				"this failure is not that one, so the note must not appear: " + report);
		}
		finally
		{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

	@Test
	@DisplayName("and it says so when the failure is the library's own printer dying")
	void theReport_namesTheLibrarysOwnPrinter() throws Exception
	{
		JFrame frame = onTheEventThread();
		try
		{
			NullPointerException insideThePrinter = new NullPointerException();
			insideThePrinter.setStackTrace(new StackTraceElement[] {
					new StackTraceElement("org.assertj.core.util.Preconditions", "checkNotNull",
						"Preconditions.java", 82),
					new StackTraceElement("org.assertj.swing.core.BasicComponentPrinter",
						"lambda$print$0", "BasicComponentPrinter.java", 137) });

			String report = WhatWasOnScreen.report("a test that failed", insideThePrinter);

			assertTrue(report.contains("AssertJ-Swing's own component printer dying"),
				"the note is what tells a reader that the stack above is not the lookup: "
					+ report);
			assertTrue(report.contains("btnTheOneThatWasThere"),
				"and the harness's own answer still follows it: " + report);
		}
		finally
		{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

	@Test
	@DisplayName("with nothing on screen it says that, which is itself the answer")
	void theReport_saysWhenNothingWasShowing() throws Exception
	{
		JFrame frame = onTheEventThread();
		SwingUtilities.invokeAndWait(() -> frame.setVisible(false));
		try
		{
			String report = WhatWasOnScreen.report("a test that failed",
				new IllegalStateException("nothing to see"));

			assertTrue(report.contains("NONE was showing") || report.contains("no windows at all"),
				"a lookup that finds nothing on an empty screen has been answered: " + report);
		}
		finally
		{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

	private static JFrame onTheEventThread() throws Exception
	{
		JFrame[] created = new JFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			JFrame frame = new JFrame("a window a lookup would search");
			JPanel panel = new JPanel();
			JButton button = new JButton("ok");
			button.setName("btnTheOneThatWasThere");
			panel.add(button);
			frame.add(panel);
			frame.pack();
			frame.setVisible(true);
			created[0] = frame;
		});
		return created[0];
	}
}
