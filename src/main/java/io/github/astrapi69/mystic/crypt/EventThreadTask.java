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

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

/**
 * Runs a task on the event dispatch thread for a caller that waits for it, such as the start thread
 * that waits for the modal sign-in dialog (#1072).
 */
final class EventThreadTask
{

	private EventThreadTask()
	{
	}

	/**
	 * Runs the task on the event dispatch thread and returns when it has finished, which for the
	 * modal sign-in dialog is when the dialog is closed. Called on the event dispatch thread
	 * already (the "Open Database" action), it just runs.
	 * <p>
	 * It does not return before the task has finished: a caller that is interrupted while it waits
	 * gets an {@link IllegalStateException} and keeps its interrupt flag, so a start does not go on
	 * without a sign-in while the dialog is still up. What the task throws is thrown here, as it is
	 * when it is a {@link RuntimeException} or an {@link Error}, and wrapped otherwise.
	 *
	 * @param task
	 *            what to run
	 */
	static void runAndWait(final Runnable task)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			task.run();
			return;
		}
		try
		{
			SwingUtilities.invokeAndWait(task);
		}
		catch (final InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException(
				"interrupted while the task was still running on the event thread (the start "
					+ "was interrupted while the sign-in dialog was open)",
				interrupted);
		}
		catch (final InvocationTargetException failed)
		{
			Throwable cause = failed.getCause();
			if (cause instanceof RuntimeException runtimeException)
			{
				throw runtimeException;
			}
			if (cause instanceof Error error)
			{
				throw error;
			}
			throw new IllegalStateException("the task failed on the event thread", cause);
		}
	}
}
