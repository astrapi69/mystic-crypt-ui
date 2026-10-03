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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JInternalFrame;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Prints what was on screen when an end-to-end test failed, from this harness rather than from the
 * library.
 * <p>
 * AssertJ-Swing answers a failed lookup by printing the component hierarchy into the exception's
 * message - and that path itself dies with a {@link NullPointerException} in
 * {@code BasicComponentPrinter.lambda$print$0}, measured three times in three different tests
 * (#484). When it does, the real failure is replaced by the failure of its own report: nothing says
 * which component was not found, and a rerun is the only move left. That is the shape this project
 * treats as a defect rather than as noise.
 * <p>
 * So the diagnosis comes from here instead. On every failure this prints the visible windows with
 * their titles and, inside each, every component that carries a name - because a lookup names what
 * it wants, and "what was there" is the other half of the answer. When the throwable is that NPE it
 * says so, so the next red run is read rather than restarted.
 */
class WhatWasOnScreen implements TestWatcher
{

	/**
	 * Every line of the report carries this, so the build can forward exactly these lines to the
	 * console - a report that only reaches the XML is a report nobody reads after a CI failure
	 */
	static final String TAG = "[on-screen] ";

	/** The marker a reader can grep for in a CI log */
	static final String MARKER = TAG + "=== what was on screen when the test failed ===";

	@Override
	public void testFailed(final ExtensionContext context, final Throwable cause)
	{
		for (String line : report(context.getDisplayName(), cause).split("\n"))
		{
			System.out.println(line.startsWith(TAG) ? line : TAG + line);
		}
	}

	/**
	 * The report as text, so it can be asserted rather than only read in a log
	 *
	 * @param displayName
	 *            the test that failed
	 * @param cause
	 *            what it failed with
	 * @return the report
	 */
	static String report(final String displayName, final Throwable cause)
	{
		StringBuilder report = new StringBuilder(MARKER).append('\n');
		report.append("test: ").append(displayName).append('\n');
		report.append("failure: ").append(describe(cause)).append('\n');
		if (isTheLibrarysOwnPrinterFailing(cause))
		{
			report.append("NOTE: this is AssertJ-Swing's own component printer dying while it "
				+ "built the failure message (#484), so the lookup that failed is NOT in the "
				+ "stack above. What follows is this harness's own answer to it.\n");
		}
		for (Window window : Window.getWindows())
		{
			if (!window.isShowing())
			{
				continue;
			}
			report.append("window: ").append(titleOf(window)).append(" [")
				.append(window.getClass().getSimpleName()).append("]\n");
			for (String named : namedComponentsOf(window))
			{
				report.append("    ").append(named).append('\n');
			}
		}
		long showing = java.util.Arrays.stream(Window.getWindows()).filter(Window::isShowing)
			.count();
		if (showing == 0)
		{
			report.append(Window.getWindows().length == 0
				? "no windows at all - the application had not opened one, or they were disposed "
					+ "before the failure was reported\n"
				: Window.getWindows().length + " windows existed and NONE was showing - which is "
					+ "itself the answer when a lookup could not find anything\n");
		}
		return report.toString();
	}

	private static boolean isTheLibrarysOwnPrinterFailing(final Throwable cause)
	{
		for (Throwable each = cause; each != null; each = each.getCause())
		{
			if (each instanceof NullPointerException)
			{
				for (StackTraceElement frame : each.getStackTrace())
				{
					if (frame.getClassName().contains("BasicComponentPrinter"))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private static String describe(final Throwable cause)
	{
		if (cause == null)
		{
			return "(none)";
		}
		return cause.getClass().getName() + ": "
			+ (cause.getMessage() == null ? "(no message)" : firstLineOf(cause.getMessage()));
	}

	private static String firstLineOf(final String message)
	{
		int newline = message.indexOf('\n');
		return newline < 0 ? message : message.substring(0, newline) + " [...]";
	}

	private static String titleOf(final Window window)
	{
		if (window instanceof Frame frame)
		{
			return quoted(frame.getTitle());
		}
		if (window instanceof Dialog dialog)
		{
			return quoted(dialog.getTitle());
		}
		return "(untitled)";
	}

	private static String quoted(final String title)
	{
		return title == null || title.isBlank() ? "(untitled)" : '"' + title + '"';
	}

	/**
	 * Every named component inside a window, internal frames included, because a tool window is
	 * where the names a lookup asks for live
	 */
	private static List<String> namedComponentsOf(final Container container)
	{
		List<String> named = new ArrayList<>();
		collectNames(container, named);
		return named;
	}

	private static void collectNames(final Container container, final List<String> named)
	{
		for (Component component : container.getComponents())
		{
			if (component.getName() != null && !component.getName().isBlank())
			{
				named.add(component.getName() + " [" + component.getClass().getSimpleName()
					+ (component.isShowing() ? "" : ", not showing") + "]");
			}
			if (component instanceof JInternalFrame internalFrame)
			{
				named.add("internal frame " + quoted(internalFrame.getTitle()));
			}
			if (component instanceof Container child)
			{
				collectNames(child, named);
			}
		}
	}
}
