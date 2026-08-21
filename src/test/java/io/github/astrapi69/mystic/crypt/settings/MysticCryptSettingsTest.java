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
package io.github.astrapi69.mystic.crypt.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test for {@link MysticCryptSettings}: defaults, JSON round-trip and the missing-file
 * fallback
 */
class MysticCryptSettingsTest
{

	@Test
	void freshSettingsHaveSensibleDefaults()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		assertEquals("Nimbus", settings.getLookAndFeel());
		assertEquals("en", settings.getLanguage());
	}

	@Test
	void savedSettingsRoundTripThroughTheConfigurationDirectory(
		@TempDir File configurationDirectory)
	{
		MysticCryptSettings settings = new MysticCryptSettings("Metal", "de");
		settings.save(configurationDirectory);
		assertTrue(new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).exists(),
			"saving must write the settings json");

		MysticCryptSettings loaded = MysticCryptSettings.load(configurationDirectory);
		assertEquals("Metal", loaded.getLookAndFeel());
		assertEquals("de", loaded.getLanguage());
	}

	@Test
	void loadingFromAnEmptyDirectoryReturnsDefaults(@TempDir File configurationDirectory)
	{
		MysticCryptSettings loaded = MysticCryptSettings.load(configurationDirectory);
		assertEquals("Nimbus", loaded.getLookAndFeel());
		assertEquals("en", loaded.getLanguage());
	}
}
