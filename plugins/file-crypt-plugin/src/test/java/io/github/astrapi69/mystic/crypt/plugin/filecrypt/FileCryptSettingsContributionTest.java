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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Tests of the file encryption plugin's own configuration.
 */
class FileCryptSettingsContributionTest
{

	private static final PluginSettingsContribution CONTRIBUTION = new FileCryptSettingsContribution();

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
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
	void declaresItsPluginIdAndItsDefaults()
	{
		assertEquals("file-crypt-plugin", CONTRIBUTION.getPluginId());
		assertEquals(2, CONTRIBUTION.getDefaults().size());
		assertTrue(CONTRIBUTION.getDescription(FileCryptSettingsContribution.KEY_DELETE_SOURCE)
			.contains("wiping"), "the setting has to say what deleting does not do");
	}

	@Test
	void withoutStoredValuesNothingIsDeletedAndTheFileTabOpens()
	{
		assertFalse(FileCryptSettingsContribution.deleteSourceAfterEncrypt(),
			"removing the original must never be the default");
		assertEquals("file", FileCryptSettingsContribution.startTab());
	}

	@ParameterizedTest
	@CsvSource({ "true,true", "false,false", "yes,false" })
	void theDeleteSettingIsRead(String stored, boolean expected) throws Exception
	{
		store(FileCryptSettingsContribution.KEY_DELETE_SOURCE, stored);

		assertEquals(expected, FileCryptSettingsContribution.deleteSourceAfterEncrypt());
	}

	@ParameterizedTest
	@CsvSource({ "text,text", "TEXT,text", "file,file", "something else,file" })
	void theStartTabIsRead(String stored, String expected) throws Exception
	{
		store(FileCryptSettingsContribution.KEY_START_TAB, stored);

		assertEquals(expected, FileCryptSettingsContribution.startTab());
	}
}
