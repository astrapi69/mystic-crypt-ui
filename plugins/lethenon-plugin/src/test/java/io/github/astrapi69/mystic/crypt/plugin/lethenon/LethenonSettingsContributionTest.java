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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The plugin's one setting: visible in the settings page, read by the panel, and described well
 * enough to be edited. Both halves are rules rather than taste - a setting that changes behaviour
 * without being editable, and a setting nobody reads, are each forbidden (architecture.md).
 */
class LethenonSettingsContributionTest
{

	private static final PluginSettingsContribution CONTRIBUTION = new LethenonSettingsContribution();

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void releaseTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the contribution names the plugin it belongs to, and names it the same way twice")
	void theContribution_namesItsPlugin()
	{
		assertEquals("lethenon-plugin", CONTRIBUTION.getPluginId(),
			"the id has to be the one in plugin.properties, or the settings page writes into a "
				+ "file the plugin never reads");
		assertEquals("Lethenon", CONTRIBUTION.getDisplayName());
	}

	@Test
	@DisplayName("the chain file is a setting with a default and a description")
	void theChainFile_isEditable()
	{
		Map<String, String> defaults = CONTRIBUTION.getDefaults();

		assertTrue(defaults.containsKey(LethenonSettingsContribution.KEY_CHAIN_FILE));
		assertEquals("", defaults.get(LethenonSettingsContribution.KEY_CHAIN_FILE),
			"no chain file is configured until somebody configures one");
		assertNotNull(CONTRIBUTION.getDescription(LethenonSettingsContribution.KEY_CHAIN_FILE),
			"a setting without a description cannot be edited by anybody who did not write it");
	}

	@ParameterizedTest(name = "no description is invented for \"{0}\"")
	@ValueSource(strings = { "chain.files", "", "report.lines" })
	@DisplayName("a key this plugin does not have gets no description")
	void anUnknownKey_hasNoDescription(final String key)
	{
		assertNull(CONTRIBUTION.getDescription(key));
	}

	@Test
	@DisplayName("the panel reads the configured chain file, and the default when there is none")
	void theChainFile_isWhatThePanelReads() throws Exception
	{
		assertEquals("", LethenonSettingsContribution.chainFile(),
			"nothing is stored yet, so the default answers");

		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			CONTRIBUTION.getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/chain.lethenon"));

		assertEquals("/tmp/chain.lethenon", LethenonSettingsContribution.chainFile(),
			"what the settings page wrote is what the panel starts with - otherwise the setting "
				+ "is dead");
	}
}
