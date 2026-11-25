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
package io.github.astrapi69.mystic.crypt.ui.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Work that runs off the event dispatch thread, answers on it, and can be cancelled while it runs
 * (#1053). The work runs on an executor the test holds, so each state between the start and the
 * answer can be looked at; the screen executor runs at once, as the event dispatch thread would
 * when it gets to it.
 */
class CancellableWorkTest
{

	/** A worker that keeps what it was given until the test lets it run */
	private static final class HeldWork implements Executor
	{
		private final List<Runnable> held = new ArrayList<>();

		@Override
		public void execute(final Runnable command)
		{
			held.add(command);
		}

		void runAll()
		{
			List<Runnable> waiting = new ArrayList<>(held);
			held.clear();
			waiting.forEach(Runnable::run);
		}
	}

	/** What the answer said, in the order it said it */
	private static final class RecordedAnswer implements CancellableWork.Answer<String>
	{
		private final List<String> calls = new ArrayList<>();

		@Override
		public void done(final String value)
		{
			calls.add("done:" + value);
		}

		@Override
		public void failed(final Exception failure)
		{
			calls.add("failed:" + failure.getMessage());
		}

		@Override
		public void cancelled()
		{
			calls.add("cancelled");
		}
	}

	private final HeldWork work = new HeldWork();
	private final CancellableWork<String> cancellableWork = new CancellableWork<>(work,
		Runnable::run);
	private final RecordedAnswer answer = new RecordedAnswer();

	@Test
	@DisplayName("the start returns before the work ran, and the answer comes once it has")
	void theStartReturnsBeforeTheWork_andTheAnswerComesAfterIt()
	{
		cancellableWork.start(() -> "the database", answer);

		assertTrue(answer.calls.isEmpty(), "the answer came before the work ran: " + answer.calls);
		assertTrue(cancellableWork.isRunning(), "the work does not count as running");

		work.runAll();

		assertEquals(List.of("done:the database"), answer.calls);
		assertFalse(cancellableWork.isRunning(), "the work still counts as running");
	}

	@Test
	@DisplayName("a failure of the work is the answer, not an exception on the worker")
	void aFailureOfTheWork_isTheAnswer()
	{
		cancellableWork.start(() -> {
			throw new IllegalStateException("the password is wrong");
		}, answer);

		work.runAll();

		assertEquals(List.of("failed:the password is wrong"), answer.calls);
		assertFalse(cancellableWork.isRunning(), "the work still counts as running");
	}

	@Test
	@DisplayName("a cancel answers at once, and what the work brings afterwards is dropped (#1053)")
	void aCancel_answersAtOnce_andTheLateResultIsDropped()
	{
		cancellableWork.start(() -> "the database", answer);

		cancellableWork.cancel();

		assertEquals(List.of("cancelled"), answer.calls, "the cancel did not answer at once");
		assertFalse(cancellableWork.isRunning(), "a cancelled work still counts as running");

		work.runAll();

		assertEquals(List.of("cancelled"), answer.calls, "the cancelled work still answered");
	}

	@Test
	@DisplayName("a failure that arrives after a cancel is dropped as well (#1053)")
	void aFailureAfterACancel_isDropped()
	{
		cancellableWork.start(() -> {
			throw new IllegalStateException("the password is wrong");
		}, answer);

		cancellableWork.cancel();
		work.runAll();

		assertEquals(List.of("cancelled"), answer.calls);
	}

	@Test
	@DisplayName("after a cancel the next start answers with its own result, not the cancelled one")
	void afterACancel_theNextStartAnswersWithItsOwnResult()
	{
		cancellableWork.start(() -> "the cancelled read", answer);
		cancellableWork.cancel();
		RecordedAnswer secondAnswer = new RecordedAnswer();
		cancellableWork.start(() -> "the second read", secondAnswer);

		work.runAll();

		assertEquals(List.of("cancelled"), answer.calls);
		assertEquals(List.of("done:the second read"), secondAnswer.calls);
	}

	@Test
	@DisplayName("a cancel with nothing running says nothing")
	void aCancelWithNothingRunning_saysNothing()
	{
		cancellableWork.cancel();

		cancellableWork.start(() -> "the database", answer);
		work.runAll();

		assertEquals(List.of("done:the database"), answer.calls);
	}

	@Test
	@DisplayName("a second start while the work runs is refused, so one press cannot read twice")
	void aSecondStartWhileTheWorkRuns_isRefused()
	{
		cancellableWork.start(() -> "the database", answer);

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> cancellableWork.start(() -> "a second read", new RecordedAnswer()));

		assertTrue(refused.getMessage().contains("already running"), refused.getMessage());
	}

	@Test
	@DisplayName("the application's work runs on a thread of its own and answers on the event dispatch thread")
	void theApplicationsWork_runsOffTheEventThread_andAnswersOnIt() throws Exception
	{
		AtomicReference<Thread> workThread = new AtomicReference<>();
		AtomicReference<Boolean> answeredOnTheEventThread = new AtomicReference<>();
		CountDownLatch answered = new CountDownLatch(1);
		CancellableWork<String> applicationWork = CancellableWork.ofTheApplication("test-work");

		SwingUtilities.invokeAndWait(() -> applicationWork.start(() -> {
			workThread.set(Thread.currentThread());
			return "the database";
		}, new CancellableWork.Answer<>()
		{
			@Override
			public void done(final String value)
			{
				answeredOnTheEventThread.set(SwingUtilities.isEventDispatchThread());
				answered.countDown();
			}

			@Override
			public void failed(final Exception failure)
			{
				answered.countDown();
			}

			@Override
			public void cancelled()
			{
				answered.countDown();
			}
		}));

		assertTrue(answered.await(30, TimeUnit.SECONDS), "no answer came");
		assertTrue(answeredOnTheEventThread.get(), "the answer was not given on the event thread");
		assertNotEquals(true, workThread.get().getName().startsWith("AWT-EventQueue"),
			"the work ran on the event dispatch thread");
		assertTrue(workThread.get().isDaemon(), "the work thread keeps the application alive");
		assertEquals("test-work", workThread.get().getName());
	}
}
