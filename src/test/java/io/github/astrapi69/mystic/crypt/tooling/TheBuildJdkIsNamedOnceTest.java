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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The JDK a build runs on is named once, in .sdkmanrc, which `sdk env` reads to set the shell, and
 * the Makefile pins what .sdkmanrc names instead of a version of its own (#586). Two places
 * drifted: the Makefile stayed on JDK 25 as released while the shell had moved on to 25.0.4, and
 * make and ./gradlew could not share a daemon (#580).
 */
class TheBuildJdkIsNamedOnceTest
{

	/**
	 * An sdkman Temurin JDK with its full version, which a deliberate pull request raises (#586)
	 */
	private static final Pattern PINNED_JDK = Pattern.compile("java=\\d+\\.\\d+\\.\\d+-tem");

	@Test
	@DisplayName(".sdkmanrc names one Temurin JDK with its full version")
	void sdkmanrc_namesTheJdk() throws IOException
	{
		List<String> lines = Files.readAllLines(Path.of(".sdkmanrc"), StandardCharsets.UTF_8)
			.stream().filter(line -> line.startsWith("java=")).toList();

		assertEquals(1, lines.size(), ".sdkmanrc names the JDK once: " + lines);
		assertTrue(PINNED_JDK.matcher(lines.getFirst()).matches(), lines.getFirst());
	}

	@Test
	@DisplayName("the Makefile pins the JDK .sdkmanrc names, and no version of its own")
	void theMakefile_pinsTheJdkOfSdkmanrc() throws IOException
	{
		List<String> makefile = Files.readAllLines(Path.of("Makefile"), StandardCharsets.UTF_8);
		String pin = makefile.stream().filter(line -> line.startsWith("PINNED_JAVA_HOME :="))
			.findFirst().orElse("");

		assertTrue(pin.contains(".sdkmanrc"), "the pin reads .sdkmanrc: " + pin);
		assertFalse(makefile.stream().anyMatch(line -> line.contains("candidates/java/2")),
			"no JDK version written into the Makefile");
	}
}
