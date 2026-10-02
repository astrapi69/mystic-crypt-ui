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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The balance window keeps its state in its model, the password included - as a char array that is
 * wiped when it is replaced and dropped once it was used, and that never reaches the report or the
 * result line.
 */
class LethenonBalancePanelBindingTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File configurationDirectory;

	@TempDir
	File directory;

	private Path chainFile;

	private Path walletFile;

	@BeforeEach
	void writeAChainAndAWallet() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		Wallet wallet = Wallet.create();
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
	@DisplayName("the balance window starts with the configured chain file")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonBalancePanel panel = new LethenonBalancePanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
	}

	@Test
	@DisplayName("the spendable accounts and the one-time payments are shown apart")
	void showing_theBalance_separatesWhatCanBeSpent() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);

		panel.onShow();

		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("ed25519 account"), report);
		assertTrue(report.contains("3.00000000 LETH, spendable"), report);
		assertTrue(report.contains("1 one-time payments holding 5.00000000 LETH"), report);
		assertTrue(report.contains("not spendable yet"), report);
		assertFalse(report.contains("8.00000000"),
			"the one-time payment must not be added to what can be spent: " + report);
		assertEquals("the balance was read from the replayed chain",
			panel.getModelObject().getResultText());
	}

	@Test
	@DisplayName("the password is gone from the model once it was used, and was in no text")
	void showing_theBalance_dropsThePassword() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);

		panel.onShow();

		assertEquals(0, panel.getModelObject().getPassword().length,
			"a password that opened the wallet once has no reason to stay in memory");
		assertFalse(panel.getModelObject().getReport().contains(PASSWORD));
		assertFalse(panel.getModelObject().getResultText().contains(PASSWORD));
	}

	@Test
	@DisplayName("a wrong password leaves no report and does not repeat the password")
	void showing_withAWrongPassword_reportsTheReasonOnly() throws Exception
	{
		LethenonBalancePanel panel = filledPanel("not the password at all");

		panel.onShow();

		assertEquals("", panel.getModelObject().getReport());
		String result = panel.getModelObject().getResultText();
		assertTrue(result.startsWith("the balance could not be read"), result);
		assertTrue(result.contains("wallet.lethenon-wallet"), result);
		assertFalse(result.contains("not the password at all"), result);
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	@Test
	@DisplayName("replacing the password overwrites the array it replaces")
	void setPassword_wipesTheReplacedArray()
	{
		LethenonBalancePanelModel model = new LethenonBalancePanelModel();
		char[] first = "first".toCharArray();
		model.setPassword(first);

		model.setPassword("second".toCharArray());

		assertArrayEquals(new char[5], first);
	}

	private LethenonBalancePanel filledPanel(final String password)
	{
		LethenonBalancePanel panel = new LethenonBalancePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(password.toCharArray());
		return panel;
	}
}
