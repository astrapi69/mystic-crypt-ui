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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Metal and Ocean share one class, {@link MetalLookAndFeel}, and differ only in the theme. The
 * settings hold the theme beside the look's name, so the start shows what was picked (#1065, option
 * 1 of the maintainer's decision of 2026-11-19): an optional {@code metalTheme} in settings.json,
 * which a file written before leaves out and loads without
 */
class MetalThemeIsSavedTest
{

	@TempDir
	File configurationDirectory;

	private LookAndFeel originalLook;

	private MetalTheme originalTheme;

	@BeforeEach
	void rememberTheLookAndTheTheme()
	{
		originalLook = UIManager.getLookAndFeel();
		originalTheme = MetalLookAndFeel.getCurrentTheme();
	}

	@AfterEach
	void restoreTheLookAndTheTheme() throws Exception
	{
		MetalLookAndFeel.setCurrentTheme(originalTheme);
		UIManager.setLookAndFeel(originalLook);
	}

	@Test
	@DisplayName("the Steel theme of Metal is written with the look")
	void theSteelTheme_isWrittenWithTheLook() throws Exception
	{
		metalWith(new DefaultMetalTheme());

		assertTrue(LookAndFeelChoice.persistActive(configurationDirectory));

		MysticCryptSettings saved = MysticCryptSettings.load(configurationDirectory);
		assertEquals("Metal", saved.getLookAndFeel());
		assertEquals(MetalThemeChoice.STEEL, saved.getMetalTheme());
	}

	@Test
	@DisplayName("a switch from Ocean to Steel is written, although the look's name stays Metal")
	void aSwitchFromOceanToSteel_isWritten() throws Exception
	{
		metalWith(new OceanTheme());
		LookAndFeelChoice.persistActive(configurationDirectory);
		metalWith(new DefaultMetalTheme());

		assertTrue(LookAndFeelChoice.persistActive(configurationDirectory),
			"the theme changed and nothing was written");

		assertEquals(MetalThemeChoice.STEEL,
			MysticCryptSettings.load(configurationDirectory).getMetalTheme());
	}

	@Test
	@DisplayName("a saved Steel is what the start shows, though the runtime's default is Ocean")
	void aSavedSteel_isWhatTheStartShows() throws Exception
	{
		metalWith(new OceanTheme());
		UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());

		GeneralSettingsPanel.applyLookAndFeel("Metal", MetalThemeChoice.STEEL);

		assertInstanceOf(MetalLookAndFeel.class, UIManager.getLookAndFeel());
		assertInstanceOf(DefaultMetalTheme.class, MetalLookAndFeel.getCurrentTheme());
		assertTrue(!(MetalLookAndFeel.getCurrentTheme() instanceof OceanTheme),
			"the start shows Ocean although Steel was saved");
	}

	@Test
	@DisplayName("a saved Ocean is applied over a Metal that shows Steel")
	void aSavedOcean_isAppliedOverASteelMetal() throws Exception
	{
		metalWith(new DefaultMetalTheme());

		GeneralSettingsPanel.applyLookAndFeel("Metal", MetalThemeChoice.OCEAN);

		assertInstanceOf(OceanTheme.class, MetalLookAndFeel.getCurrentTheme(),
			"Metal was already active, so the theme was not applied");
	}

	@Test
	@DisplayName("a settings file written before leaves the theme out and loads without it")
	void aSettingsFileWrittenBefore_loadsWithoutATheme() throws Exception
	{
		Files.writeString(
			new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME).toPath(),
			"{\"lookAndFeel\":\"Metal\"}", StandardCharsets.UTF_8);

		MysticCryptSettings loaded = MysticCryptSettings.load(configurationDirectory);

		assertEquals("Metal", loaded.getLookAndFeel());
		assertNull(loaded.getMetalTheme(), "a file without a theme got one");
	}

	private static void metalWith(final MetalTheme theme) throws Exception
	{
		MetalLookAndFeel.setCurrentTheme(theme);
		UIManager.setLookAndFeel(new MetalLookAndFeel());
	}
}
