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
package io.github.astrapi69.mystic.crypt.panel.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests run from exploded test classes, not a packaged jar - {@code getImplementationVersion()} is
 * {@code null} there, which is exactly the case that has to fall back to something readable rather
 * than showing a hand-maintained constant that drifts (the "8.1-SNAPSHOT" bug this replaces).
 */
class ApplicationInfoTest
{

	@Test
	void versionFallsBackWhenNoJarManifestIsOnTheClasspath()
	{
		ApplicationInfo info = ApplicationInfo.current("mystic-crypt-ui", "2016 Asterios Raptis",
			"MIT License");

		assertNotNull(info.version(), "the dialog must never show no version at all");
		assertTrue(!info.version().isBlank());
	}

	@Test
	void passesTheGivenTextThrough()
	{
		ApplicationInfo info = ApplicationInfo.current("mystic-crypt-ui", "2016 Asterios Raptis",
			"MIT License");

		assertEquals("mystic-crypt-ui", info.applicationName());
		assertEquals("2016 Asterios Raptis", info.copyright());
		assertEquals("MIT License", info.licenseSummary());
	}

	@Test
	void linksPointAtTheRealProjectNotAPlaceholder()
	{
		ApplicationInfo info = ApplicationInfo.current("mystic-crypt-ui", "", "");

		assertEquals("https://github.com/astrapi69/mystic-crypt-ui", info.githubUrl());
		assertEquals("https://github.com/astrapi69/mystic-crypt-ui/wiki", info.wikiUrl());
	}

}
