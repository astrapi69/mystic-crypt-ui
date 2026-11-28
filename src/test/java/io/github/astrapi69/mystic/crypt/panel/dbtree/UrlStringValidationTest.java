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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Which addresses the tree takes as a URL of an entry: the ones a browser can be sent to. The cases
 * are the ones {@code new URL(String).toURI()} decided before the constructor was deprecated (JDK
 * 20), so a replacement is held to the same answers
 */
class UrlStringValidationTest
{

	@ParameterizedTest(name = "[{0}] is a URL: {1}")
	@CsvSource(delimiter = '|', nullValues = "NULL", value = { "NULL|false",
			"https://example.com|true", "http://example.com/a/b?c=d#e|true",
			"ftp://example.com/file|true", "mailto:someone@example.com|true", "file:///tmp/x|true",
			"example.com|false", "foo://example.com|false", "http://example.com/a b|false",
			"https://|false", "''|false", "' '|false" })
	void validateUrlString_answersAsTheUrlConstructorDid(final String address,
		final boolean expected)
	{
		assertEquals(expected, SecretKeyTreeWithContentPanel.validateUrlString(address));
	}
}
