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
import io.github.astrapi69.mystic.crypt.settings.PluginSettingsPanel;

/**
 * The plugin's settings: the chain file, the wallet file, the node and the proxy that worked last
 * (#605), visible and editable in the settings page, read by the windows, and described well
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

	@ParameterizedTest(name = "\"{0}\" is a setting with an empty default and a description")
	@ValueSource(strings = { LethenonSettingsContribution.KEY_CHAIN_FILE,
			LethenonSettingsContribution.KEY_WALLET_FILE, LethenonSettingsContribution.KEY_NODE,
			LethenonSettingsContribution.KEY_PROXY })
	@DisplayName("the chain, the wallet, the node and the proxy are settings (#605)")
	void everyRememberedValue_isASetting(final String key)
	{
		Map<String, String> defaults = CONTRIBUTION.getDefaults();

		assertEquals("", defaults.get(key), "nothing is remembered until something worked");
		assertNotNull(CONTRIBUTION.getDescription(key));
	}

	@Test
	@DisplayName("the settings page shows all four values and saves what is typed there; a cleared field forgets (#605)")
	void theSettingsPage_showsAndSavesAllFour() throws Exception
	{
		LethenonSettingsContribution.remember(Map.of(LethenonSettingsContribution.KEY_PROXY,
			"127.0.0.1:9050"));
		PluginSettingsPanel page = new PluginSettingsPanel(configurationDirectory,
			java.util.List.of(CONTRIBUTION));

		assertEquals(java.util.List.of(LethenonSettingsContribution.KEY_CHAIN_FILE,
			LethenonSettingsContribution.KEY_WALLET_FILE, LethenonSettingsContribution.KEY_NODE,
			LethenonSettingsContribution.KEY_PROXY), java.util.List.copyOf(page.getShownValues().keySet()));
		assertEquals("127.0.0.1:9050", page.getShownValues().get(LethenonSettingsContribution.KEY_PROXY));

		page.setShownValue(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/a.lethenon");
		page.setShownValue(LethenonSettingsContribution.KEY_WALLET_FILE, "/tmp/home.wallet");
		page.setShownValue(LethenonSettingsContribution.KEY_NODE, "198.51.100.7:18480");
		page.setShownValue(LethenonSettingsContribution.KEY_PROXY, "");
		applyButtonOf(page).doClick();

		assertEquals("/tmp/a.lethenon", LethenonSettingsContribution.chainFile());
		assertEquals("/tmp/home.wallet", LethenonSettingsContribution.walletFile());
		assertEquals("198.51.100.7:18480", LethenonSettingsContribution.node());
		assertEquals("", LethenonSettingsContribution.proxy(), "a cleared field forgets the proxy");
	}

	@Test
	@DisplayName("remembering one value keeps the others as they were")
	void remember_keepsTheOtherValues()
	{
		LethenonSettingsContribution.remember(Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE,
			"/tmp/chain.lethenon", LethenonSettingsContribution.KEY_NODE, "203.0.113.5:18480"));

		LethenonSettingsContribution.remember(Map.of(LethenonSettingsContribution.KEY_WALLET_FILE,
			"/tmp/home.wallet"));

		assertEquals("/tmp/chain.lethenon", LethenonSettingsContribution.chainFile());
		assertEquals("/tmp/home.wallet", LethenonSettingsContribution.walletFile());
		assertEquals("203.0.113.5:18480", LethenonSettingsContribution.node());
	}

	@ParameterizedTest(name = "\"{0}\" is refused")
	@ValueSource(strings = { "password", "wallet.password", "chain.files" })
	@DisplayName("only the four values can be remembered: no password, in any form")
	void remember_refusesAnyOtherKey(final String key)
	{
		IllegalArgumentException refused = org.junit.jupiter.api.Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> LethenonSettingsContribution.remember(Map.of(key, "secret")));

		assertTrue(refused.getMessage().contains(key), refused.getMessage());
		assertTrue(PluginSettings.load(LethenonSettingsContribution.PLUGIN_ID,
			CONTRIBUTION.getDefaults()).values().stream().noneMatch("secret"::equals));
	}

	private static javax.swing.AbstractButton applyButtonOf(final java.awt.Container container)
	{
		for (java.awt.Component child : container.getComponents())
		{
			if (child instanceof javax.swing.AbstractButton button
				&& "btnApplyPluginSettings".equals(button.getName()))
			{
				return button;
			}
			if (child instanceof java.awt.Container nested)
			{
				javax.swing.AbstractButton found = applyButtonOf(nested);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
