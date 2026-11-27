/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.lang.reflect.Method;

import com.thoughtworks.xstream.converters.ErrorWritingException;

/**
 * A failure of the xml reader, told by the type of each exception in its chain, the line the parser
 * names, and its frames.
 * <p>
 * Each exception of the chain, cause by cause, becomes one of these, in the same order and with the
 * same frames, whose message is the exception's class name and its line: the line the pull parser's
 * exception names, or the line XStream records on its own exceptions.
 */
final class ReaderFailureTrace extends Exception
{

	private static final long serialVersionUID = 1L;

	/** The entry XStream records the reader's line under, as a number */
	private static final String LINE_NUMBER = "line number";

	private ReaderFailureTrace(final Throwable link, final ReaderFailureTrace cause)
	{
		super(link.getClass().getName() + lineOf(link), cause);
		setStackTrace(link.getStackTrace());
	}

	/**
	 * The given failure and its causes, told by types, lines and frames
	 *
	 * @param failure
	 *            what the reader threw
	 * @return the same chain, told that way
	 */
	static ReaderFailureTrace of(final Throwable failure)
	{
		Throwable cause = failure.getCause();
		return new ReaderFailureTrace(failure, cause == null ? null : of(cause));
	}

	/**
	 * The line where reading stopped, as {@code " at line L"}, when the exception names it
	 *
	 * @param link
	 *            one exception of the chain
	 * @return the line, or an empty text when the exception names none
	 */
	private static String lineOf(final Throwable link)
	{
		if (link instanceof ErrorWritingException recorded)
		{
			return recordedLineOf(recorded);
		}
		int line = parserLineOf(link);
		return line < 0 ? "" : " at line " + line;
	}

	/**
	 * The line XStream recorded on its own exception, under its entry for the line number
	 *
	 * @param recorded
	 *            an exception of XStream that carries entries
	 * @return the line, or an empty text when the entry is missing or not a number
	 */
	private static String recordedLineOf(final ErrorWritingException recorded)
	{
		String line = recorded.get(LINE_NUMBER);
		return line != null && line.matches("\\d+") ? " at line " + line : "";
	}

	/**
	 * The line a public {@code getLineNumber()} of the exception returns, as the pull parser's
	 * exception names it. Its type comes with XStream and is not declared by this build; looking
	 * the getter up by name keeps it undeclared. The price: a parser that renames or drops it
	 * yields no line, and {@code UnreadableContentLogTest}, which expects the line, then fails
	 *
	 * @param link
	 *            one exception of the chain
	 * @return the line, or -1 when the exception has no such getter, as the parser itself says for
	 *         a line it does not know
	 */
	private static int parserLineOf(final Throwable link)
	{
		try
		{
			Method getter = link.getClass().getMethod("getLineNumber");
			return getter.getReturnType() == int.class ? (int)getter.invoke(link) : -1;
		}
		catch (ReflectiveOperationException | SecurityException noUsableGetter)
		{
			// no such getter (NoSuchMethodException, the usual case), one this class may not call
			// (IllegalAccessException, SecurityException), or one that threw
			// (InvocationTargetException): in each the exception names no line
			return -1;
		}
	}
}
