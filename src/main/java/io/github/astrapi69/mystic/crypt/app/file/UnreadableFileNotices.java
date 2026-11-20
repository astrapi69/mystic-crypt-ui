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
package io.github.astrapi69.mystic.crypt.app.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tells the user about a file that could not be read and was kept aside instead of written over
 * (#1082).
 * <p>
 * {@link JsonFileStore} sets the file aside wherever it is saved, which can be before the
 * application window exists, during the sign-in. A notice published then waits until a listener
 * takes it, and every later notice goes to that listener at once. There is no Swing here: the
 * application window is the listener and shows the message.
 */
public final class UnreadableFileNotices
{

	/**
	 * A file that could not be read, and where it was kept
	 *
	 * @param original
	 *            the file as the application knows it
	 * @param keptAs
	 *            where its bytes lie now
	 */
	public record SetAside(File original, File keptAs) {
	}

	private static final List<SetAside> PENDING = new ArrayList<>();

	private static Consumer<SetAside> listener;

	private UnreadableFileNotices()
	{
	}

	/**
	 * Hands the notice to the listener, or keeps it until there is one
	 *
	 * @param setAside
	 *            the file that was set aside
	 */
	public static synchronized void publish(final SetAside setAside)
	{
		if (listener == null)
		{
			PENDING.add(setAside);
			return;
		}
		listener.accept(setAside);
	}

	/**
	 * Makes the given listener the one that takes the notices, and hands it those that waited
	 *
	 * @param newListener
	 *            the listener, or {@code null} to keep the notices from now on
	 */
	public static synchronized void listen(final Consumer<SetAside> newListener)
	{
		listener = newListener;
		if (newListener == null)
		{
			return;
		}
		List<SetAside> waiting = new ArrayList<>(PENDING);
		PENDING.clear();
		waiting.forEach(newListener);
	}
}
