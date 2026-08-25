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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;

/**
 * Tests of the menu designer's own configuration.
 */
class MenuDesignerSettingsContributionTest
{

	private static final PluginSettingsContribution CONTRIBUTION = new MenuDesignerSettingsContribution();

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the contribution reads through the installed configuration directory; pointing it at a
		// temporary one keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private void store(String key, String value) throws Exception
	{
		PluginSettings.save(configurationDirectory, CONTRIBUTION.getPluginId(),
			CONTRIBUTION.getDefaults(), Map.of(key, value));
	}

	@Test
	void declaresItsPluginIdAndItsDefault()
	{
		assertEquals("menu-designer-plugin", CONTRIBUTION.getPluginId());
		assertEquals("true",
			CONTRIBUTION.getDefaults().get(MenuDesignerSettingsContribution.KEY_EXPORT_ON_OPEN));
	}

	@Test
	void withoutAStoredValueTheEditorIsFilledOnOpen()
	{
		assertTrue(MenuDesignerSettingsContribution.exportOnOpen());
	}

	@ParameterizedTest
	@CsvSource({ "true,true", "false,false", "FALSE,false", "anything else,true" })
	void theStoredValueDecides(String stored, boolean expected) throws Exception
	{
		store(MenuDesignerSettingsContribution.KEY_EXPORT_ON_OPEN, stored);

		assertEquals(expected, MenuDesignerSettingsContribution.exportOnOpen());
	}
}
