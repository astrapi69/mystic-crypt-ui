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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The sweep window keeps its state in its model, sweeps through {@link SweepSupport}, says what
 * sweeping costs before anything is signed and again afterwards, and uses the wallet's password
 * once.
 */
class LethenonSweepPanelBindingTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File configurationDirectory;

	@TempDir
	File directory;

	private Path chainFile;

	private Path walletFile;

	private Wallet wallet;

	@BeforeEach
	void writeAChainAndAWallet() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		wallet = Wallet.create();
		walletFile = new File(directory, "wallet.lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = new File(directory, "chain.lethenon").toPath();
		Files.write(chainFile,
			CanonicalEncoding.encodeChain(LethenonFixtures.aChainPayingTheWallet(payer, wallet)));
	}

	@AfterEach
	void releaseTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the sweep window starts with the configured chain file, no fee and the command line's memo")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonSweepPanel panel = new LethenonSweepPanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
		assertEquals("0", panel.getModelObject().getFee());
		assertEquals("swept from a one-time destination", panel.getModelObject().getMemo());
	}

	@Test
	@DisplayName("before anything is signed, the window says what sweeping costs")
	void thePanel_saysWhatSweepingCosts_beforeAnythingIsSigned()
	{
		LethenonSweepPanel panel = new LethenonSweepPanel();

		String cost = panel.getModelObject().getCostStatement();
		assertTrue(cost.contains("together"), cost);
		assertTrue(cost.contains("Receiving is unlinkable; spending is the moment that ends"),
			cost);
	}

	@Test
	@DisplayName("sweeping leaves one transfer per destination waiting, and repeats what it costs")
	void sweeping_writesTheTransfersAndRepeatsTheCost() throws Exception
	{
		LethenonSweepPanel panel = filledPanel(PASSWORD, "0.1");

		panel.onSweep();

		List<SignedTransaction> pending = new ChainFile(chainFile).readPending();
		assertEquals(1, pending.size());
		assertEquals("swept from a one-time destination", pending.getFirst().body().memo());
		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("1 transfer"), report);
		assertTrue(report.contains("4.90000000 LETH"), report);
		assertTrue(report.contains(wallet.spendKey(SignatureSuite.ED25519).toString()), report);
		assertTrue(report.contains(panel.getModelObject().getCostStatement()), report);
		assertEquals("the sweep was signed and waits for the next block",
			panel.getModelObject().getResultText());
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	@Test
	@DisplayName("with nothing worth sweeping the window says so and writes nothing")
	void sweeping_nothing_saysSoAndWritesNothing() throws Exception
	{
		LethenonSweepPanel panel = filledPanel(PASSWORD, "5");

		panel.onSweep();

		assertTrue(panel.getModelObject().getResultText().startsWith("nothing to sweep"),
			panel.getModelObject().getResultText());
		assertFalse(Files.exists(Path.of(chainFile + ".pending")));
	}

	@Test
	@DisplayName("a wrong password leaves no report and does not repeat the password")
	void sweeping_withAWrongPassword_reportsTheReasonOnly() throws Exception
	{
		LethenonSweepPanel panel = filledPanel("not the password at all", "0");

		panel.onSweep();

		assertEquals("", panel.getModelObject().getReport());
		String result = panel.getModelObject().getResultText();
		assertTrue(result.startsWith("nothing was swept"), result);
		assertTrue(result.contains("wallet.lethenon-wallet"), result);
		assertFalse(result.contains("not the password at all"), result);
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	private LethenonSweepPanel filledPanel(final String password, final String fee)
	{
		LethenonSweepPanel panel = new LethenonSweepPanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(password.toCharArray());
		panel.getModelObject().setFee(fee);
		return panel;
	}
}
