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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The wallet file field stays only for use without a vault wallet, and says so (#718, the
 * maintainer's decision A of 31 October 2026): while no wallet of the vault is chosen, a sentence
 * under the field says the wallet comes from this file; with one chosen, both are gone.
 */
class WalletFileNoteTest
{

	private static final String NOTE = "No wallet of the vault is chosen in \"Wallet\", so the "
		+ "wallet comes from this file.";

	@TempDir
	Path configuration;

	private LethenonVault vault;

	@BeforeEach
	void aVaultWithAWalletNotChosen()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		vault = new LethenonVault();
		vault.connect(new InMemoryVaultAccess());
		vault.restore(Wallet.create().phrase().toCharArray(), ThrowawayPasswords.next(),
			WalletNetwork.TESTNET);
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	static Stream<Arguments> windows()
	{
		return Stream.of(
			Arguments.of("Send LETH", (BiFunction<LethenonVault, Locale, JPanel>)(vault,
				locale) -> new LethenonSendPanel(ChainConversionConsent.ASK, vault,
					SendConfirmation.ASK, locale)),
			Arguments.of("Mine a Pun", (BiFunction<LethenonVault, Locale, JPanel>)(vault,
				locale) -> new LethenonMinePanel(ChainConversionConsent.ASK, vault, locale)),
			Arguments.of("Sweep", (BiFunction<LethenonVault, Locale, JPanel>)(vault,
				locale) -> new LethenonSweepPanel(ChainConversionConsent.ASK, vault, locale)),
			Arguments.of("Run a Node", (BiFunction<LethenonVault, Locale, JPanel>)(vault,
				locale) -> new LethenonNodePanel(ChainConversionConsent.ASK, vault, locale)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("windows")
	@DisplayName("the note shows with the wallet file field and goes with it once a vault wallet is chosen")
	void theNote_followsTheFileField(final String window,
		final BiFunction<LethenonVault, Locale, JPanel> open)
	{
		JPanel panel = open.apply(vault, Locale.ENGLISH);

		assertTrue(component(panel, "txtWalletFile").isVisible());
		assertTrue(component(panel, "txtWalletFileNote").isVisible(), window);
		assertEquals(NOTE, textOf(panel, "txtWalletFileNote"));

		vault.choose(vault.wallets().get(0));

		assertFalse(component(panel, "txtWalletFile").isVisible());
		assertFalse(component(panel, "txtWalletFileNote").isVisible(),
			window + ": no note while the vault's wallet is used");
	}

	@Test
	@DisplayName("the note in German")
	void theNote_inGerman()
	{
		JPanel panel = new LethenonMinePanel(ChainConversionConsent.ASK, vault, Locale.GERMAN);

		assertEquals("Im Fenster \"Wallet\" ist keine Wallet aus dem Tresor gewählt, darum kommt "
			+ "sie aus dieser Datei.", textOf(panel, "txtWalletFileNote"));
	}

	private static Component component(final JPanel panel, final String name)
	{
		return Windows.component(panel, name, Component.class);
	}
}
