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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.component;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textsOf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
		LethenonFixtures.written(chainFile, LethenonFixtures.aChainPayingTheWallet(payer, wallet));
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
		assertEquals("Careful: after a sweep, anyone who reads the chain can see that these "
			+ "payments belong to you.", textOf(panel, "txtCost"),
			"the cost stays on the screen before signing, in plain words (#604)");
		assertEquals(cost, component(panel, "txtCost", JComponent.class).getToolTipText(),
			"the whole statement is one look away");
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
		assertEquals("Swept: 4.90000000 LETH from 1 payment onto your account. It counts once the "
			+ "next block is mined.", textOf(panel, "txtAnswer"));
		assertEquals("4.90000000 LETH", textOf(panel, "txtSwept"));
		assertEquals("1 payment", textOf(panel, "txtPayments"));
		assertEquals(wallet.spendKey(SignatureSuite.ED25519).toString(),
			textOf(panel, "txtOntoAccount"));
		assertEquals("next to the chain file, until the next block is mined",
			textOf(panel, "txtWhereItWaits"));
		assertEquals("1 transfer", textOf(panel, "txtWaiting"));
		assertEquals(panel.getModelObject().getCostStatement(), textOf(panel, "txtCostDetail"),
			"what it cost, repeated behind the details");
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

		assertEquals("There was nothing to sweep: no payment to your address holds more than the "
			+ "fee of 5.00000000 LETH.", textOf(panel, "txtAnswer"));
		assertEquals("", textOf(panel, "txtSwept"));
		assertTrue(panel.getModelObject().getResultText().startsWith("nothing to sweep"),
			panel.getModelObject().getResultText());
		assertEquals(List.of(), new ChainFile(chainFile).readPending(), "nothing waits");
	}

	@Test
	@DisplayName("what only a specialist reads - what waits and what a sweep shows the chain - is behind \"Show details\", closed at first (#604)")
	void theDetails_areClosedAtFirst_andOpenOnAClick() throws Exception
	{
		LethenonSweepPanel panel = filledPanel(PASSWORD, "0.1");
		panel.onSweep();
		JPanel details = component(panel, "pnlDetails", JPanel.class);

		assertFalse(details.isVisible(), "the details are closed at first");

		panel.onDetails();

		assertTrue(details.isVisible());
		assertEquals("Hide details", component(panel, "btnDetails", JButton.class).getText());
	}

	@Test
	@DisplayName("every field and button of the window has a tooltip in plain words (#604)")
	void everyFieldAndButton_hasATooltip() throws Exception
	{
		LethenonSweepPanel panel = filledPanel(PASSWORD, "0.1");
		panel.onSweep();

		assertEquals(List.of(), Windows.withoutATooltip(panel),
			"fields and buttons without a tooltip");
	}

	@Test
	@DisplayName("a wrong password after a sweep empties every field, says what to do and does not repeat the password")
	void sweeping_withAWrongPassword_saysWhatToDo() throws Exception
	{
		LethenonSweepPanel panel = filledPanel(PASSWORD, "0.1");
		panel.onSweep();
		panel.getModelObject().setPassword("not the password at all".toCharArray());

		panel.onSweep();

		for (String field : List.of("txtAnswer", "txtSwept", "txtPayments", "txtOntoAccount",
			"txtWhereItWaits", "txtWaiting", "txtCostDetail"))
		{
			assertEquals("", textOf(panel, field), field + " keeps nothing of the sweep before");
		}
		assertFalse(textsOf(panel).stream().anyMatch(text -> text.contains(PASSWORD)));
		String result = panel.getModelObject().getResultText();
		assertTrue(result.startsWith("nothing was swept"), result);
		assertTrue(result.contains("this password does not open wallet.lethenon-wallet; type it again"),
			result);
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
