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
package io.github.astrapi69.mystic.crypt.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Where a test JVM writes its temporary files. On Linux the JDK sets {@code java.io.tmpdir} to
 * {@code /tmp} whatever {@code TMPDIR} says, and {@code @TempDir}, {@code File.createTempFile} and
 * the libraries follow {@code java.io.tmpdir}, not the variable. The runs of {@code gradle.yml} and
 * {@code e2e.yml}, the release gate among them, start every step with {@code TMPDIR} on the
 * runner's work disk; this test reads what the test JVM got. It runs only where the build was
 * started with {@code TMPDIR}.
 */
class TestJvmTemporaryDirectoryTest
{

	@Test
	@DisplayName("a test JVM writes its temporary files to /tmp, whatever TMPDIR the build was started with")
	void temporaryDirectory_isTmp_whenTheBuildWasStartedWithTmpdir()
	{
		String temporaryDirectory = System.getenv("TMPDIR");
		assumeTrue(temporaryDirectory != null && !temporaryDirectory.isBlank(),
			"the build was started without TMPDIR");

		// today's behaviour; the next commit forwards TMPDIR and turns this assertion
		assertEquals("/tmp", System.getProperty("java.io.tmpdir"));
	}

}
