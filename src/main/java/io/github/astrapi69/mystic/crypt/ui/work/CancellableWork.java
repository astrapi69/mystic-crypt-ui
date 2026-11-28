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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.swing.SwingUtilities;

/**
 * One piece of work that runs off the event dispatch thread, answers on it, and can be cancelled
 * while it runs (#1053).
 * <p>
 * Deriving a key from a password takes a noticeable moment and cannot be interrupted. On the event
 * dispatch thread that moment freezes the window: it does not repaint, does not react and offers no
 * way out. Here the work runs on the {@code work} executor and its answer is handed to the
 * {@code screen} executor, the event dispatch thread in the application.
 * <p>
 * {@link #start(Callable, Answer)}, {@link #cancel()} and every answer happen on the screen
 * executor, so they see each other in order without any locking. A cancel answers at once with
 * {@link Answer#cancelled()} and gives the window back; the work itself goes on until it ends, and
 * whatever it brings then, a result or a failure, is dropped. That is why this is the shape for
 * work whose result only matters once it is shown, such as reading a database at sign-in, and not
 * for work that writes something on its way: a cancel here cannot stop a write that has begun.
 *
 * @param <T>
 *            what the work brings
 */
public final class CancellableWork<T>
{

	/**
	 * What a piece of work answers with, always on the screen executor and exactly once per start
	 *
	 * @param <T>
	 *            what the work brings
	 */
	public interface Answer<T>
	{

		/**
		 * The work ended with a result and nobody cancelled it
		 *
		 * @param value
		 *            what the work brought
		 */
		void done(T value);

		/**
		 * The work ended with an exception or an error and nobody cancelled it. An error comes in
		 * an {@link ExecutionException} whose cause it is, as {@code FutureTask} hands one over
		 *
		 * @param failure
		 *            what the work threw, or the error it ended with in an ExecutionException
		 */
		void failed(Exception failure);

		/** The user cancelled the work; whatever it brings afterwards is dropped */
		void cancelled();
	}

	private final Executor work;
	private final Executor screen;
	/**
	 * Counts the starts and the cancels, so an answer can tell whether it is still the current one
	 */
	private long generation;
	private Answer<T> pending;

	/**
	 * Work over the given executors
	 *
	 * @param work
	 *            where the work runs
	 * @param screen
	 *            where its answer is given, the event dispatch thread in the application
	 */
	public CancellableWork(final Executor work, final Executor screen)
	{
		this.work = work;
		this.screen = screen;
	}

	/**
	 * The work of the application: it runs on a daemon thread of its own, which does not keep the
	 * application from ending, and answers on the event dispatch thread
	 *
	 * @param <T>
	 *            what the work brings
	 * @param threadName
	 *            the name of the thread the work runs on, as a thread dump shows it
	 * @return the work
	 */
	public static <T> CancellableWork<T> ofTheApplication(final String threadName)
	{
		return new CancellableWork<>(
			operation -> Thread.ofPlatform().daemon().name(threadName).start(operation),
			SwingUtilities::invokeLater);
	}

	/**
	 * Starts the work. Called on the screen executor; the call returns before the work has run
	 *
	 * @param task
	 *            the work
	 * @param answer
	 *            who is told how it ended
	 * @throws IllegalStateException
	 *             if a work is already running, so a second press cannot read a second time
	 */
	public void start(final Callable<T> task, final Answer<T> answer)
	{
		if (pending != null)
		{
			throw new IllegalStateException(
				"a work is already running; cancel it or wait for its answer first");
		}
		pending = answer;
		long started = ++generation;
		work.execute(() -> {
			try
			{
				T value = task.call();
				screen.execute(() -> answer(started, () -> answer.done(value)));
			}
			catch (Exception failure)
			{
				screen.execute(() -> answer(started, () -> answer.failed(failure)));
			}
			catch (Error error)
			{
				// an error ends the work as an exception does, so the answer still comes exactly
				// once; it is handed over the way FutureTask hands one over, in an
				// ExecutionException
				ExecutionException failure = new ExecutionException(error);
				screen.execute(() -> answer(started, () -> answer.failed(failure)));
			}
		});
	}

	/**
	 * Cancels the work that runs and answers {@link Answer#cancelled()} at once. Called on the
	 * screen executor. Does nothing when nothing runs
	 */
	public void cancel()
	{
		if (pending == null)
		{
			return;
		}
		Answer<T> cancelledAnswer = pending;
		pending = null;
		generation++;
		cancelledAnswer.cancelled();
	}

	/**
	 * Whether a work was started and has neither answered nor been cancelled
	 *
	 * @return true while a work runs
	 */
	public boolean isRunning()
	{
		return pending != null;
	}

	/** Gives the answer of the work that was started as {@code started}, unless it was cancelled */
	private void answer(final long started, final Runnable answering)
	{
		if (started != generation)
		{
			return;
		}
		pending = null;
		answering.run();
	}
}
