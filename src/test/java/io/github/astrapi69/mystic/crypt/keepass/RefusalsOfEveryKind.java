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
package io.github.astrapi69.mystic.crypt.keepass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Refusals of the kinds an import or an export can end with, each with the given text in one place
 * of it: its message, a cause's message, only a grandchild cause's, only a suppressed exception's,
 * or only the text its getLocalizedMessage gives
 */
final class RefusalsOfEveryKind
{

	private RefusalsOfEveryKind()
	{
	}

	/**
	 * The refusals, as the case name and the exception
	 *
	 * @param text
	 *            the text each refusal carries in one place
	 * @return the arguments of a parameterized test
	 */
	static Stream<Arguments> carrying(final String text)
	{
		return Stream.of(
			Arguments.of("a state the library refuses", new IllegalStateException(text)),
			Arguments.of("an argument, with a cause",
				new IllegalArgumentException(text, new IOException(text))),
			Arguments.of("a parser failure, wrapped",
				new RuntimeException(new XMLStreamException(text))),
			Arguments.of("an input failure", new IOException(text)),
			Arguments.of("an unchecked input failure, with a cause",
				new UncheckedIOException(text, new IOException(text))),
			Arguments.of("the text only on a grandchild cause",
				new IllegalStateException("a refusal",
					new RuntimeException("a cause", new IOException(text)))),
			Arguments.of("the text only on a suppressed exception", withASuppressed(text)),
			Arguments.of("the text only in an overridden getLocalizedMessage",
				withALocalizedMessage(text)));
	}

	private static Exception withASuppressed(final String text)
	{
		IllegalStateException refusal = new IllegalStateException("a refusal");
		refusal.addSuppressed(new IOException(text));
		return refusal;
	}

	private static Exception withALocalizedMessage(final String text)
	{
		return new IllegalStateException("a refusal")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getLocalizedMessage()
			{
				return text;
			}
		};
	}
}
