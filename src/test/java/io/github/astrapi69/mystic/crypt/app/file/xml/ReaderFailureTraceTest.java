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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;

/**
 * A failure of the xml reader, told by the type of each exception in its chain, the line the parser
 * names, and its frames ({@link ReaderFailureTrace})
 */
class ReaderFailureTraceTest
{

	/** The text each message and each entry of a chain holds */
	private static final String TEXT = "text-" + UUID.randomUUID();

	/**
	 * An exception that names a position the way the pull parser's exception does, through public
	 * getters without parameters
	 */
	static class PositionedFailure extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		private final int line;
		private final int column;

		PositionedFailure(final int line, final int column)
		{
			super(TEXT);
			this.line = line;
			this.column = column;
		}

		public int getLineNumber()
		{
			return line;
		}

		public int getColumnNumber()
		{
			return column;
		}
	}

	@Test
	void aChain_isToldByItsTypesPositionsAndFrames()
	{
		PositionedFailure parser = new PositionedFailure(12, 34);
		ConversionException reader = new ConversionException(TEXT, new StreamException(parser));
		reader.add("line number", "12");
		reader.add("path", "/" + TEXT);

		ReaderFailureTrace told = ReaderFailureTrace.of(reader);

		assertEquals(
			List.of(ConversionException.class.getName() + " at line 12",
				StreamException.class.getName(), PositionedFailure.class.getName() + " at line 12"),
			messagesOf(told));
		assertArrayEquals(reader.getStackTrace(), told.getStackTrace());
		assertArrayEquals(parser.getStackTrace(), told.getCause().getCause().getStackTrace());
		assertFalse(printed(told).contains(TEXT), "told by types and positions");
	}

	@Test
	void aChainWithASuppressedException_isToldCauseByCause()
	{
		IllegalStateException failure = new IllegalStateException(TEXT);
		failure.addSuppressed(new IllegalArgumentException(TEXT));

		ReaderFailureTrace told = ReaderFailureTrace.of(failure);

		assertEquals(0, told.getSuppressed().length);
		assertEquals(List.of(IllegalStateException.class.getName()), messagesOf(told));
		assertFalse(printed(told).contains(TEXT), "told by types and positions");
	}

	static Stream<Arguments> exceptionsAndHowTheyAreTold()
	{
		return Stream.of(
			Arguments.of("an exception without a position",
				(Supplier<Throwable>)() -> new IllegalStateException(TEXT),
				IllegalStateException.class.getName()),
			Arguments.of("a position the parser does not know (-1)",
				(Supplier<Throwable>)() -> new PositionedFailure(-1, -1),
				PositionedFailure.class.getName()),
			Arguments.of("a line, and a column the parser does not know (-1)",
				(Supplier<Throwable>)() -> new PositionedFailure(1, -1),
				PositionedFailure.class.getName() + " at line 1"),
			Arguments.of("line 0, the first the parser can name, with a column",
				(Supplier<Throwable>)() -> new PositionedFailure(0, 0),
				PositionedFailure.class.getName() + " at line 0"),
			Arguments.of("an exception of XStream without a recorded line",
				(Supplier<Throwable>)() -> new ConversionException(TEXT),
				ConversionException.class.getName()),
			Arguments.of("an exception of XStream whose recorded line is no number",
				(Supplier<Throwable>)() -> recorded("line number", TEXT),
				ConversionException.class.getName()),
			Arguments.of("an exception of XStream with line 0 recorded",
				(Supplier<Throwable>)() -> recorded("line number", "0"),
				ConversionException.class.getName() + " at line 0"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("exceptionsAndHowTheyAreTold")
	void anException_isToldByItsTypeAndPosition(final String caseName,
		final Supplier<Throwable> exception, final String expected)
	{
		ReaderFailureTrace told = ReaderFailureTrace.of(exception.get());

		assertEquals(List.of(expected), messagesOf(told));
		assertFalse(printed(told).contains(TEXT), "told by types and positions");
	}

	private static ConversionException recorded(final String entry, final String value)
	{
		ConversionException recorded = new ConversionException(TEXT);
		recorded.add(entry, value);
		return recorded;
	}

	private static List<String> messagesOf(final Throwable told)
	{
		List<String> messages = new ArrayList<>();
		for (Throwable link = told; link != null; link = link.getCause())
		{
			messages.add(link.getMessage());
		}
		return messages;
	}

	private static String printed(final Throwable told)
	{
		StringWriter printed = new StringWriter();
		told.printStackTrace(new PrintWriter(printed));
		return printed.toString();
	}
}
