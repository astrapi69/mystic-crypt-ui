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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

/**
 * What a failure of reading a decrypted vault says, and how it keeps the reader's failure
 * ({@link VaultContentUnreadableException})
 */
class VaultContentUnreadableExceptionTest
{

	/** The text the messages of the reader's failure hold */
	private static final String TEXT = "text-" + UUID.randomUUID();

	private static final String UNKNOWN_TYPE = "io.github.astrapi69.mystic.crypt.NoSuchTypeInAnyVersion";

	private static final String FAMILY = "io.github.astrapi69.";

	static Stream<Arguments> readerFailures()
	{
		return Stream.of(
			Arguments.of("the parser refused the document, inside a conversion",
				new ConversionException(TEXT, new StreamException(new IllegalStateException(TEXT))),
				true, "the reader failed with IllegalStateException"),
			Arguments.of("the parser refused the document at its root",
				new StreamException(new IllegalStateException(TEXT)), true,
				"the reader failed with IllegalStateException"),
			Arguments.of("a field of a well-formed document", new ConversionException(TEXT), false,
				"the reader failed with ConversionException"),
			Arguments.of("a type of this family this version does not know",
				new ConversionException(TEXT, new CannotResolveClassException(UNKNOWN_TYPE)), false,
				"it names the type " + UNKNOWN_TYPE + ", which this version does not know"),
			Arguments.of("a type of another form",
				new ConversionException(TEXT, new CannotResolveClassException(TEXT)), false,
				"it names a type this version does not know"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("readerFailures")
	void aReaderFailure_isNamedByItsKind_andToldByTypesAndPositions(final String caseName,
		final XStreamException failure, final boolean noXml, final String reason)
	{
		VaultContentUnreadableException unreadable = new VaultContentUnreadableException(failure,
			false);

		assertEquals(noXml, unreadable.isNoXml());
		assertEquals(reason, unreadable.getReason());
		assertInstanceOf(ReaderFailureTrace.class, unreadable.getCause());
		StringWriter printed = new StringWriter();
		unreadable.printStackTrace(new PrintWriter(printed));
		assertFalse(printed.toString().contains(TEXT), "told by kinds, types and positions");
	}

	static Stream<Arguments> namesAContentGives()
	{
		int longest = VaultContentUnreadableException.LONGEST_NAMED_TYPE;
		return Stream.of(Arguments.of("a class of this family", UNKNOWN_TYPE, true),
			Arguments.of("a nested class by its binary name", FAMILY + "crypt.Outer$Inner", true),
			Arguments.of("exactly the longest named length",
				FAMILY + "a".repeat(longest - FAMILY.length()), true),
			Arguments.of("one character longer", FAMILY + "a".repeat(longest - FAMILY.length() + 1),
				false),
			Arguments.of("a class outside the family", "java.lang.String", false),
			Arguments.of("the family's package without a class", FAMILY, false),
			Arguments.of("the family's name without its dot", "io.github.astrapi69", false),
			Arguments.of("an empty segment", FAMILY + "crypt..Type", false),
			Arguments.of("a segment that starts with a digit", FAMILY + "crypt.1Type", false),
			Arguments.of("a space", FAMILY + "crypt.Some Type", false),
			Arguments.of("an ignorable character", FAMILY + "crypt.Some\u0001Type", false),
			Arguments.of("a trailing dot", FAMILY + "crypt.Type.", false),
			Arguments.of("a letter beyond ASCII", FAMILY + "crypt.Grüße", true),
			Arguments.of("a supplementary character", FAMILY + "crypt.Type\uD835\uDC00", false),
			Arguments.of("a format character, zero width space", FAMILY + "crypt.Some\u200BType",
				false),
			Arguments.of("a format character, right-to-left override",
				FAMILY + "crypt.Some\u202EType", false),
			Arguments.of("no name", null, false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("namesAContentGives")
	void aName_isAClassNameOfThisFamily_onlyByItsForm(final String caseName, final String name,
		final boolean expected)
	{
		assertEquals(expected, VaultContentUnreadableException.isAClassNameOfThisFamily(name));
	}
}
