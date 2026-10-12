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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

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

	@Test
	@DisplayName("the report is printed when the test fails, before its teardown disposes the windows")
	void theReport_isPrintedAtTheFailure_notAfterTheTeardown() throws Exception
	{
		JFrame frame = onTheEventThread();
		WhatWasOnScreen watcher = new WhatWasOnScreen();
		ExtensionContext context = contextOf("[engine:junit-jupiter]/[method:aTest()]", "a test");
		IllegalStateException failure = new IllegalStateException("timed out");
		String printed;
		try
		{
			printed = printedBy(() -> assertSame(failure,
				assertThrows(IllegalStateException.class,
					() -> watcher.handleTestExecutionException(context, failure)),
				"the failure goes on unchanged; the report is only said on the way"));
		}
		finally
		{
			SwingUtilities.invokeAndWait(frame::dispose);
		}

		assertTrue(printed.contains(WhatWasOnScreen.MARKER), printed);
		assertTrue(printed.contains("\"a window a lookup would search\""),
			"the window was showing when the test failed; the teardown disposes it before a "
				+ "TestWatcher is called, and a report read then says NONE was showing (#522): "
				+ printed);

		String afterTheTeardown = printedBy(() -> watcher.testFailed(context, failure));
		assertEquals("", afterTheTeardown,
			"the same failure is reported once, from the moment it happened: " + afterTheTeardown);
	}

	@Test
	@DisplayName("the report carries the stacks of the start thread and of the event thread")
	void theReport_carriesTheStacksOfTheStartThreadAndTheEventThread() throws Exception
	{
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch waiting = new CountDownLatch(1);
		Thread start = new Thread(() -> waitInARecognisableFrame(waiting, release),
			WhatWasOnScreen.START_THREAD);
		start.setDaemon(true);
		start.start();
		JFrame frame = onTheEventThread();
		try
		{
			assertTrue(waiting.await(5, TimeUnit.SECONDS));

			String report = WhatWasOnScreen.report("a test that failed",
				new IllegalStateException("timed out waiting for the application"));

			assertTrue(
				report.contains("thread \"" + WhatWasOnScreen.START_THREAD + "\" TIMED_WAITING"),
				"where the start is when a test gives up waiting for it is the answer #522 "
					+ "could not give: " + report);
			assertTrue(report.contains("waitInARecognisableFrame"),
				"with its stack, not only its state: " + report);
			assertTrue(report.contains("thread \"AWT-EventQueue"),
				"and the event thread's, which the start competes with: " + report);
		}
		finally
		{
			release.countDown();
			start.join(5000);
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

	@Test
	@DisplayName("a blocked thread is in the report with the thread that holds its lock")
	void theReport_namesABlockedThreadAndWhoHoldsItsLock() throws Exception
	{
		Object monitor = new Object();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread holder = new Thread(() -> {
			synchronized (monitor)
			{
				held.countDown();
				awaitQuietly(release);
			}
		}, "a thread holding the lock");
		Thread blocked = new Thread(() -> {
			synchronized (monitor)
			{
				monitor.notifyAll();
			}
		}, "a thread waiting for the lock");
		holder.setDaemon(true);
		blocked.setDaemon(true);
		holder.start();
		try
		{
			assertTrue(held.await(5, TimeUnit.SECONDS));
			blocked.start();
			awaitState(blocked, Thread.State.BLOCKED);

			String report = WhatWasOnScreen.report("a test that failed",
				new IllegalStateException("timed out"));

			assertTrue(report.contains("thread \"a thread waiting for the lock\" BLOCKED"), report);
			assertTrue(report.contains("held by \"a thread holding the lock\""),
				"a blocked thread without its lock's owner is half a deadlock: " + report);
		}
		finally
		{
			release.countDown();
			holder.join(5000);
			blocked.join(5000);
		}
	}

	@Test
	@DisplayName("a start thread that died says what it died of")
	void theReport_namesWhatTheStartThreadDiedOf() throws Exception
	{
		Thread start = new Thread(() -> {
			throw new IllegalStateException("the plugins directory could not be written");
		}, WhatWasOnScreen.START_THREAD);
		start.setUncaughtExceptionHandler(WhatWasOnScreen::startThreadDied);
		String printed = printedTo(true, () -> {
			start.start();
			start.join(5000);
		});
		try
		{
			String report = WhatWasOnScreen.report("a test that failed",
				new IllegalStateException("timed out waiting for application model is signed in"));

			assertTrue(report.contains("the start thread is not running"), report);
			assertTrue(report.contains("the plugins directory could not be written"),
				"a start that ends with an exception shows no window and no dialog; the "
					+ "exception is the only trace it leaves (#522): " + report);
			assertTrue(printed.contains("the plugins directory could not be written"),
				"and it still reaches standard error, as it did without the handler: " + printed);
		}
		finally
		{
			WhatWasOnScreen.startThreadLaunched();
		}
	}

	private static void waitInARecognisableFrame(CountDownLatch waiting, CountDownLatch release)
	{
		waiting.countDown();
		awaitQuietly(release);
	}

	private static void awaitQuietly(CountDownLatch latch)
	{
		try
		{
			latch.await(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}

	private static void awaitState(Thread thread, Thread.State state) throws InterruptedException
	{
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (thread.getState() != state && System.nanoTime() < deadline)
		{
			Thread.sleep(10);
		}
		assertEquals(state, thread.getState());
	}

	/** An extension context that answers what the report reads from it, and nothing else */
	private static ExtensionContext contextOf(String uniqueId, String displayName)
	{
		return (ExtensionContext)Proxy.newProxyInstance(ExtensionContext.class.getClassLoader(),
			new Class<?>[] { ExtensionContext.class },
			(proxy, method, arguments) -> switch (method.getName())
			{
			case "getUniqueId" -> uniqueId;
			case "getDisplayName" -> displayName;
			case "toString" -> "context of " + displayName;
			default -> throw new UnsupportedOperationException(method.getName());
			});
	}

	private interface Action
	{
		void run() throws Throwable;
	}

	private static String printedBy(Action action) throws Exception
	{
		return printedTo(false, action);
	}

	private static String printedTo(boolean standardError, Action action) throws Exception
	{
		PrintStream original = standardError ? System.err : System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		PrintStream capturing = new PrintStream(captured, true, StandardCharsets.UTF_8);
		if (standardError)
		{
			System.setErr(capturing);
		}
		else
		{
			System.setOut(capturing);
		}
		try
		{
			action.run();
		}
		catch (Throwable unexpected)
		{
			throw new AssertionError(unexpected);
		}
		finally
		{
			if (standardError)
			{
				System.setErr(original);
			}
			else
			{
				System.setOut(original);
			}
		}
		return captured.toString(StandardCharsets.UTF_8);
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
