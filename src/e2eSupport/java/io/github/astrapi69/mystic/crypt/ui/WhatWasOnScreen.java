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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JInternalFrame;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
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
 * <p>
 * The report is taken when the test method fails, not when JUnit tells a {@link TestWatcher} about
 * it: a watcher is called after the teardown, and the teardown disposes every window. A report read
 * then said "4 windows existed and NONE was showing" whatever had been on screen at the failure
 * (#522). The watcher stays for failures outside the test method - in a setup or a teardown - and
 * reports a failure only once.
 * <p>
 * And it carries the threads (#522): the start thread, the event threads, and every thread that is
 * blocked or deadlocked, each with its stack - because a test that times out waiting for the start
 * to finish says that it waited, not where the start stood. A start thread that died says what it
 * died of: a start that ends with an exception opens no window and no dialog, and the exception is
 * the only trace it leaves.
 */
class WhatWasOnScreen implements TestExecutionExceptionHandler, TestWatcher
{

	/** The name of the thread the end-to-end tests start the application on */
	static final String START_THREAD = "mystic-crypt-app-under-test";

	/** How many frames of one thread's stack the report prints */
	private static final int FRAMES_PER_THREAD = 60;

	/** What the current start thread died of, or {@code null} while it has not died */
	private static volatile Throwable startThreadDeath;

	/** The failures already reported, by the unique id of the test that failed */
	private final Set<String> reported = ConcurrentHashMap.newKeySet();

	/**
	 * Every line of the report carries this, so the build can forward exactly these lines to the
	 * console - a report that only reaches the XML is a report nobody reads after a CI failure
	 */
	static final String TAG = "[on-screen] ";

	/** The marker a reader can grep for in a CI log */
	static final String MARKER = TAG + "=== what was on screen when the test failed ===";

	@Override
	public void handleTestExecutionException(final ExtensionContext context,
		final Throwable throwable) throws Throwable
	{
		reported.add(context.getUniqueId());
		print(report(context.getDisplayName(), throwable));
		throw throwable;
	}

	@Override
	public void testFailed(final ExtensionContext context, final Throwable cause)
	{
		if (reported.remove(context.getUniqueId()))
		{
			return;
		}
		print(report(context.getDisplayName(), cause));
	}

	/**
	 * Keeps what the start thread died of for the report, and prints it to standard error as the
	 * default handler would have
	 *
	 * @param thread
	 *            the start thread
	 * @param death
	 *            what it died of
	 */
	static void startThreadDied(final Thread thread, final Throwable death)
	{
		startThreadDeath = death;
		System.err.print("Exception in thread \"" + thread.getName() + "\" ");
		death.printStackTrace(System.err);
	}

	/** Forgets the death of an earlier start thread, when a new one is launched */
	static void startThreadLaunched()
	{
		startThreadDeath = null;
	}

	private static void print(final String report)
	{
		for (String line : report.split("\n"))
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
		long showing = Arrays.stream(Window.getWindows()).filter(Window::isShowing).count();
		if (showing == 0)
		{
			report.append(Window.getWindows().length == 0
				? "no windows at all - the application had not opened one, or they were disposed "
					+ "before the failure was reported\n"
				: Window.getWindows().length + " windows existed and NONE was showing - which is "
					+ "itself the answer when a lookup could not find anything\n");
		}
		report.append(threads());
		return report.toString();
	}

	/**
	 * The start thread, the event threads, and every blocked or deadlocked thread, each with its
	 * stack; and what the start thread died of when it is not running
	 */
	static String threads()
	{
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		Set<Long> deadlocked = new HashSet<>();
		long[] deadlockedIds = threadBean.findDeadlockedThreads();
		if (deadlockedIds != null)
		{
			Arrays.stream(deadlockedIds).forEach(deadlocked::add);
		}
		StringBuilder threads = new StringBuilder("threads at the failure (#522):\n");
		boolean startThreadRunning = false;
		for (ThreadInfo thread : threadBean.dumpAllThreads(true, true))
		{
			boolean start = START_THREAD.equals(thread.getThreadName());
			startThreadRunning |= start;
			if (start || thread.getThreadName().startsWith("AWT-EventQueue")
				|| thread.getThreadState() == Thread.State.BLOCKED
				|| deadlocked.contains(thread.getThreadId()))
			{
				threads.append(describe(thread, deadlocked.contains(thread.getThreadId())));
			}
		}
		if (!startThreadRunning)
		{
			threads.append("the start thread is not running (\"").append(START_THREAD)
				.append("\": it ended, or it was never started)\n");
		}
		Throwable death = startThreadDeath;
		if (death != null)
		{
			threads.append("the start thread died of: ").append(death).append('\n');
			for (StackTraceElement frame : death.getStackTrace())
			{
				threads.append("    at ").append(frame).append('\n');
			}
		}
		threads.append(deadlocked.isEmpty()
			? "no deadlocked threads\n"
			: deadlocked.size() + " deadlocked threads, marked above\n");
		return threads.toString();
	}

	private static String describe(final ThreadInfo thread, final boolean deadlocked)
	{
		StringBuilder described = new StringBuilder("thread \"").append(thread.getThreadName())
			.append("\" ").append(thread.getThreadState());
		if (thread.getLockName() != null)
		{
			described.append(" on ").append(thread.getLockName());
		}
		if (thread.getLockOwnerName() != null)
		{
			described.append(" held by \"").append(thread.getLockOwnerName()).append('"');
		}
		if (deadlocked)
		{
			described.append(" DEADLOCKED");
		}
		described.append('\n');
		StackTraceElement[] frames = thread.getStackTrace();
		for (int index = 0; index < Math.min(frames.length, FRAMES_PER_THREAD); index++)
		{
			described.append("    at ").append(frames[index]).append('\n');
		}
		if (frames.length > FRAMES_PER_THREAD)
		{
			described.append("    ... ").append(frames.length - FRAMES_PER_THREAD)
				.append(" more\n");
		}
		return described.toString();
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
