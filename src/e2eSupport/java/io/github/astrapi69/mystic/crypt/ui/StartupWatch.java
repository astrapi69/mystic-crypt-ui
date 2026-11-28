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

import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.UIManager;

/**
 * Watches what a start of the application reports: what it writes to standard error, which
 * exceptions no thread caught, and how often it switches the look and feel.
 * <p>
 * Begin it before the application is launched and close it after the start, so standard error and
 * the default uncaught exception handler of the test JVM are put back. Standard error is still
 * written to the real stream as well, so the log of the run keeps what the watch captured.
 */
final class StartupWatch implements AutoCloseable
{

	private final PrintStream originalStandardError = System.err;
	private final Thread.UncaughtExceptionHandler originalHandler = Thread
		.getDefaultUncaughtExceptionHandler();
	private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
	private final List<String> uncaught = new CopyOnWriteArrayList<>();
	private final List<String> lookAndFeelChanges = new CopyOnWriteArrayList<>();
	private final PropertyChangeListener lookAndFeelListener = event -> {
		if ("lookAndFeel".equals(event.getPropertyName()))
		{
			lookAndFeelChanges.add(String.valueOf(event.getNewValue()));
		}
	};

	private StartupWatch()
	{
		System.setErr(new PrintStream(new Tee(originalStandardError, captured), true,
			StandardCharsets.UTF_8));
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			uncaught.add(thread.getName() + ": " + throwable);
			originalStandardError.println("uncaught on " + thread.getName() + ": " + throwable);
		});
		UIManager.addPropertyChangeListener(lookAndFeelListener);
	}

	/**
	 * Starts watching
	 *
	 * @return the running watch
	 */
	static StartupWatch begin()
	{
		return new StartupWatch();
	}

	/**
	 * Everything written to standard error since the watch began
	 *
	 * @return the text
	 */
	String standardError()
	{
		return captured.toString(StandardCharsets.UTF_8);
	}

	/**
	 * The exceptions no thread caught, as "thread: exception"
	 *
	 * @return the reports, in the order they came
	 */
	List<String> uncaught()
	{
		return List.copyOf(uncaught);
	}

	/**
	 * The look and feels the application switched to since the watch began
	 *
	 * @return their descriptions, in the order they were set
	 */
	List<String> lookAndFeelChanges()
	{
		return List.copyOf(lookAndFeelChanges);
	}

	/**
	 * The stack traces the JDK's keyboard manager wrote to standard error
	 *
	 * @return how many there are
	 */
	int keyboardManagerStackTraces()
	{
		return occurrences("javax.swing.KeyboardManager.unregisterKeyStroke");
	}

	/**
	 * How often a text occurs in standard error
	 *
	 * @param text
	 *            the text to count
	 * @return the number of occurrences
	 */
	int occurrences(final String text)
	{
		String standardError = standardError();
		int count = 0;
		for (int at = standardError.indexOf(text); at >= 0; at = standardError.indexOf(text,
			at + text.length()))
		{
			count++;
		}
		return count;
	}

	/** Puts standard error and the uncaught exception handler back */
	@Override
	public void close()
	{
		UIManager.removePropertyChangeListener(lookAndFeelListener);
		Thread.setDefaultUncaughtExceptionHandler(originalHandler);
		System.setErr(originalStandardError);
	}

	/** Writes to two streams, so the log of the run still shows what the watch captured */
	private static final class Tee extends OutputStream
	{
		private final OutputStream first;
		private final OutputStream second;

		Tee(final OutputStream first, final OutputStream second)
		{
			this.first = first;
			this.second = second;
		}

		@Override
		public void write(final int value) throws IOException
		{
			first.write(value);
			second.write(value);
		}

		@Override
		public void write(final byte[] bytes, final int offset, final int length) throws IOException
		{
			first.write(bytes, offset, length);
			second.write(bytes, offset, length);
		}

		@Override
		public void flush() throws IOException
		{
			first.flush();
			second.flush();
		}
	}
}
