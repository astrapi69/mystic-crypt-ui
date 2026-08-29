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
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.swing.enumeration.FrameMode;

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
		assertEquals(FrameMode.APPLICATION_PANEL, settings.getViewMode(),
			"the default view has to be the one the application always showed after signing in");
	}

	@Test
	void savedSettingsRoundTripThroughTheConfigurationDirectory(
		@TempDir File configurationDirectory)
	{
		MysticCryptSettings settings = new MysticCryptSettings("Metal", "de",
			FrameMode.DESKTOP_PANE);
		settings.save(configurationDirectory);
		assertTrue(new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).exists(),
			"saving must write the settings json");

		MysticCryptSettings loaded = MysticCryptSettings.load(configurationDirectory);
		assertEquals(FrameMode.DESKTOP_PANE, loaded.getViewMode(),
			"the chosen view did not survive the round trip");
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

	@ParameterizedTest(name = "a settings file that says {0} for the view mode opens the default view")
	@ValueSource(strings = { "\"viewMode\": null,", "\"viewMode\": \"NO_SUCH_MODE\",", "" })
	@DisplayName("a view mode this version cannot read is the default view, not a crash")
	void aViewModeThisVersionCannotReadIsTheDefaultView(final String viewModeEntry,
		@TempDir File configurationDirectory) throws Exception
	{
		Files.writeString(
			new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).toPath(),
			"{ " + viewModeEntry + " \"lookAndFeel\": \"Metal\", \"language\": \"de\" }");

		MysticCryptSettings loaded = MysticCryptSettings.load(configurationDirectory);

		assertEquals(FrameMode.APPLICATION_PANEL, loaded.getViewMode(),
			"an unreadable view mode has to fall back to the default view");
		assertEquals("Metal", loaded.getLookAndFeel(),
			"the rest of the file has to survive an unreadable view mode");
	}

}
