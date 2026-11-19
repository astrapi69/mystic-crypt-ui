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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the caller of {@link EventThreadTask#runAndWait(Runnable)} gets back, on its own thread: the
 * start thread must not go on without a sign-in when it is interrupted while the dialog is up
 * (#1072).
 */
class EventThreadTaskTest
{

	@Test
	@DisplayName("a task runs on the event thread and the caller returns after it finished")
	void theTaskRunsOnTheEventThreadBeforeTheCallerReturns() throws Exception
	{
		AtomicReference<Boolean> onEventThread = new AtomicReference<>();

		EventThreadTask.runAndWait(() -> onEventThread.set(SwingUtilities.isEventDispatchThread()));

		assertEquals(Boolean.TRUE, onEventThread.get());
	}

	@Test
	@DisplayName("a caller that is interrupted while it waits gets an IllegalStateException and keeps its interrupt flag")
	void anInterruptedCallerFailsAndKeepsItsInterruptFlag() throws Exception
	{
		CountDownLatch eventThreadBusy = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		SwingUtilities.invokeLater(() -> {
			eventThreadBusy.countDown();
			awaitQuietly(release);
		});
		assertTrue(eventThreadBusy.await(30, TimeUnit.SECONDS), "the event thread is busy");
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		AtomicReference<Boolean> flagAfter = new AtomicReference<>();
		Thread caller = new Thread(() -> {
			try
			{
				EventThreadTask.runAndWait(() -> {
				});
			}
			catch (Throwable failure)
			{
				thrown.set(failure);
			}
			flagAfter.set(Thread.currentThread().isInterrupted());
		}, "interrupted-caller");
		caller.start();
		long deadline = System.currentTimeMillis() + 30_000;
		while (caller.getState() != Thread.State.WAITING && System.currentTimeMillis() < deadline)
		{
			Thread.sleep(10);
		}
		assertEquals(Thread.State.WAITING, caller.getState(), "the caller waits for the task");

		caller.interrupt();
		caller.join(30_000);
		release.countDown();

		assertTrue(thrown.get() instanceof IllegalStateException,
			"the caller does not go on as if the task had finished: " + thrown.get());
		assertEquals(Boolean.TRUE, flagAfter.get(),
			"the interrupt flag is restored for the caller");
	}

	private static void awaitQuietly(final CountDownLatch latch)
	{
		try
		{
			latch.await(60, TimeUnit.SECONDS);
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}

	@Test
	@DisplayName("a RuntimeException and an Error of the task come out as they are")
	void aRuntimeExceptionAndAnErrorComeOutAsTheyAre()
	{
		RuntimeException failure = new IllegalArgumentException("task failed");
		Error error = new AssertionError("task errored");

		assertSame(failure,
			assertThrows(RuntimeException.class, () -> EventThreadTask.runAndWait(() -> {
				throw failure;
			})));
		assertSame(error, assertThrows(Error.class, () -> EventThreadTask.runAndWait(() -> {
			throw error;
		})));
	}
}
