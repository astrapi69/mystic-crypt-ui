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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every key the checksum plugin asks its bundle for is in the bundle (#896).
 * <p>
 * A key that is missing does not fail anything: {@link ChecksumMessages} shows the default written
 * next to the lookup, so the text appears in English whatever the bundle holds, and nothing says it
 * is missing. The keys are read from the plugin's own sources, the way a reader finds them, so a new
 * lookup is checked without being listed here.
 */
class ChecksumBundleHoldsEveryKeyTest
{

	/** The plugin's main sources, relative to the plugin build's directory the tests run in */
	private static final Path MAIN_SOURCES = Path.of("src/main/java");

	/** A lookup with a literal key, on one line or with the key on the next */
	private static final Pattern LOOKUP = Pattern
		.compile("ChecksumMessages\\s*\\.\\s*getString\\(\\s*\"([^\"]+)\"");

	@Test
	@DisplayName("every key the plugin's sources ask for is in its bundle (#896)")
	void everyKeyTheSourcesAskForIsInTheBundle() throws IOException
	{
		Set<String> asked = keysTheSourcesAskFor();
		ResourceBundle bundle = ResourceBundle.getBundle("checksum.messages");
		Set<String> missing = new TreeSet<>(asked);
		missing.removeIf(bundle::containsKey);

		assertTrue(asked.size() > 1, "the sources were read: " + asked.size() + " keys found");
		assertEquals(Set.of(), missing,
			"a key missing from the bundle shows only the Java default, in English whatever the "
				+ "bundle holds; " + missing.size() + " of " + asked.size() + " are missing");
	}

	@Test
	@DisplayName("the disabled Save checksum button's tooltip comes from the bundle, apostrophes kept (#896)")
	void theDisabledSaveTooltipComesFromTheBundle()
	{
		assertEquals(
			"a checksum file names the file it belongs to - tick 'use the file instead of the text' "
				+ "and compute, then this can be saved",
			ChecksumMessages.getString("checksum.and.mac.tooltip.checksum.save.button.disabled",
				"the default, which must not be what is shown"));
	}

	private static Set<String> keysTheSourcesAskFor() throws IOException
	{
		assertTrue(Files.isDirectory(MAIN_SOURCES),
			"the plugin's sources are not where the test looks: " + MAIN_SOURCES.toAbsolutePath());
		Set<String> keys = new TreeSet<>();
		try (Stream<Path> files = Files.walk(MAIN_SOURCES))
		{
			for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList())
			{
				Matcher lookup = LOOKUP.matcher(Files.readString(source));
				while (lookup.find())
				{
					keys.add(lookup.group(1));
				}
			}
		}
		return keys;
	}
}
