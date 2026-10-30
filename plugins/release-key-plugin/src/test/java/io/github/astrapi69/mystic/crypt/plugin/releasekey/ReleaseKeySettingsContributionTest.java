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
package io.github.astrapi69.mystic.crypt.plugin.releasekey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

import io.github.astrapi69.mystic.crypt.gnupg.GnuPgHome;

/**
 * The plugin's two settings, the GnuPG home and the release key, are declared, described and read
 * back from the configuration directory
 */
class ReleaseKeySettingsContributionTest
{

	private static final PluginSettingsContribution CONTRIBUTION = new ReleaseKeySettingsContribution();

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

	private void store(final String key, final String value) throws Exception
	{
		PluginSettings.save(configurationDirectory, CONTRIBUTION.getPluginId(),
			CONTRIBUTION.getDefaults(), Map.of(key, value));
	}

	@Test
	@DisplayName("both settings are declared blank, with a description each")
	void declaresItsPluginIdAndItsDefaults()
	{
		assertEquals("release-key-plugin", CONTRIBUTION.getPluginId());
		assertEquals(Map.of(ReleaseKeySettingsContribution.KEY_GNUPG_HOME, "",
			ReleaseKeySettingsContribution.KEY_RELEASE_KEY, ""), CONTRIBUTION.getDefaults());
		for (String key : CONTRIBUTION.getDefaults().keySet())
		{
			assertNotNull(CONTRIBUTION.getDescription(key), key);
		}
	}

	@Test
	@DisplayName("without a stored home the plugin reads the home gpg itself uses")
	void gnuPgHome_isTheDefaultHome_whenNoneIsStored()
	{
		assertEquals(GnuPgHome.defaultHome(System.getenv(), System.getProperty("user.home")),
			ReleaseKeySettingsContribution.gnuPgHome());
	}

	@Test
	@DisplayName("a stored home is read back")
	void gnuPgHome_isTheStoredOne() throws Exception
	{
		store(ReleaseKeySettingsContribution.KEY_GNUPG_HOME, "  /somewhere/gnupg  ");

		assertEquals(Path.of("/somewhere/gnupg"), ReleaseKeySettingsContribution.gnuPgHome());
	}

	@ParameterizedTest(name = "stored \"{0}\": \"{1}\"")
	@CsvSource({ "'', ''", "'  5B2FA6B11E298BC0  ', 5B2FA6B11E298BC0" })
	@DisplayName("the stored release key is read back, blank when none is stored")
	void releaseKey_isTheStoredOne(final String stored, final String expected) throws Exception
	{
		store(ReleaseKeySettingsContribution.KEY_RELEASE_KEY, stored);

		assertEquals(expected, ReleaseKeySettingsContribution.releaseKey());
	}
}
