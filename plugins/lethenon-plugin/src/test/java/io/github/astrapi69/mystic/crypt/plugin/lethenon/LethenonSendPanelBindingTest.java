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

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The send window keeps its state in its model, sends through {@link TransferSupport}, and uses the
 * wallet's password once: it is dropped after the transfer was signed or refused, and it reaches no
 * text the window shows.
 */
class LethenonSendPanelBindingTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File configurationDirectory;

	@TempDir
	File directory;

	private Path chainFile;

	private Path walletFile;

	private Bytes recipient;

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
		recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
	}

	@AfterEach
	void releaseTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the choice of recipient says what a one-time destination hides and what stays on the chain")
	void theRecipientKind_saysWhatStaysOnTheChain()
	{
		LethenonSendPanel panel = new LethenonSendPanel();

		String tooltip = namedIn(panel, "cbxRecipientKind").getToolTipText();

		assertTrue(tooltip.contains("the sender and the amount are on the chain"),
			"no text of the plugin may let a payment to a published address look hidden as a "
				+ "whole (lethenon#167, #550): " + tooltip);
		assertFalse(tooltip.contains("nobody but its holder"),
			"the sender derived the one-time destination and knows it: " + tooltip);
	}

	private static JComponent namedIn(Container container, String name)
	{
		JComponent found = searchIn(container, name);
		if (found == null)
		{
			throw new IllegalStateException("no component named " + name);
		}
		return found;
	}

	private static JComponent searchIn(Container container, String name)
	{
		for (Component component : container.getComponents())
		{
			if (name.equals(component.getName()) && component instanceof JComponent named)
			{
				return named;
			}
			if (component instanceof Container child)
			{
				JComponent found = searchIn(child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	@Test
	@DisplayName("the send window starts with the configured chain file, the Ed25519 account and no fee")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonSendPanel panel = new LethenonSendPanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
		assertEquals(SignatureSuite.ED25519, panel.getModelObject().getSuite());
		assertEquals(RecipientKind.ACCOUNT_KEY, panel.getModelObject().getRecipientKind());
		assertEquals("0", panel.getModelObject().getFee());
	}

	@Test
	@DisplayName("sending leaves the signed transfer with its memo waiting next to the chain file")
	void sending_writesTheTransferToThePendingFile() throws Exception
	{
		LethenonSendPanel panel = filledPanel(PASSWORD, "1.25");

		panel.onSend();

		List<SignedTransaction> pending = new ChainFile(chainFile).readPending();
		assertEquals(1, pending.size());
		assertEquals("watching is not protecting", pending.getFirst().body().memo());
		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("1.25000000 LETH"), report);
		assertTrue(report.contains("nonce 0"), report);
		assertTrue(report.contains("1 waiting"), report);
		assertEquals("the transfer was signed and waits for the next block",
			panel.getModelObject().getResultText());
	}

	@Test
	@DisplayName("sending through a node reports the node, and the transfer waits in its pool")
	void sending_throughANode_reportsTheNodeAndItsPool() throws Exception
	{
		Path testChain = new File(directory, "test-chain.lethenon").toPath();
		new ChainFile(testChain).write(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2,
			wallet.spendKey(SignatureSuite.ED25519)));
		try (Node node = Node.serving(new ChainFile(testChain)))
		{
			String address = "127.0.0.1:" + node.listen(0);
			LethenonSendPanel panel = filledPanel(PASSWORD, "2");
			panel.getModelObject().setChainFile(testChain.toString());
			panel.getModelObject().setNode(address);

			panel.onSend();

			assertEquals(1, node.pending().size(), panel.getModelObject().getResultText());
			String report = panel.getModelObject().getReport();
			assertTrue(report.contains("handed it to the node at " + address), report);
			assertTrue(report.contains("1 waiting"), report);
			assertEquals("the transfer was signed and handed to the node",
				panel.getModelObject().getResultText());
		}
	}

	@Test
	@DisplayName("sending to a published address reports the address, never the one-time destination it derived")
	void sending_toAPublishedAddress_reportsTheAddressOnly() throws Exception
	{
		String address = Wallet.create().address().toText();
		LethenonSendPanel panel = filledPanel(PASSWORD, "1");
		panel.getModelObject().setRecipientKind(RecipientKind.PUBLISHED_ADDRESS);
		panel.getModelObject().setRecipient(address);

		panel.onSend();

		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("a one-time destination of " + address), report);
		Bytes oneTimeKey = new ChainFile(chainFile).readPending().getFirst().body().recipient()
			.key();
		assertFalse(report.contains(oneTimeKey.toString()),
			"the sender's own screen is a place where that link would be written down");
		assertFalse(panel.getModelObject().getResultText().contains(oneTimeKey.toString()));
	}

	@Test
	@DisplayName("the password is gone from the model once the transfer was signed, and was in no text")
	void sending_dropsThePassword() throws Exception
	{
		LethenonSendPanel panel = filledPanel(PASSWORD, "1");

		panel.onSend();

		assertEquals(0, panel.getModelObject().getPassword().length);
		assertFalse(panel.getModelObject().getReport().contains(PASSWORD));
		assertFalse(panel.getModelObject().getResultText().contains(PASSWORD));
	}

	@Test
	@DisplayName("a transfer the account cannot cover leaves no report and no pending file")
	void sending_tooMuch_reportsTheReasonOnly() throws Exception
	{
		LethenonSendPanel panel = filledPanel(PASSWORD, "300");

		panel.onSend();

		assertEquals("", panel.getModelObject().getReport());
		String result = panel.getModelObject().getResultText();
		assertTrue(result.startsWith("the transfer was not signed"), result);
		assertTrue(result.contains("holds 3.00000000"), result);
		assertFalse(Files.exists(Path.of(chainFile + ".pending")));
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	@Test
	@DisplayName("a wrong password leaves no report and does not repeat the password")
	void sending_withAWrongPassword_reportsTheReasonOnly() throws Exception
	{
		LethenonSendPanel panel = filledPanel("not the password at all", "1");

		panel.onSend();

		assertEquals("", panel.getModelObject().getReport());
		String result = panel.getModelObject().getResultText();
		assertTrue(result.contains("wallet.lethenon-wallet"), result);
		assertFalse(result.contains("not the password at all"), result);
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	@Test
	@DisplayName("replacing the password overwrites the array it replaces")
	void setPassword_wipesTheReplacedArray()
	{
		LethenonSendPanelModel model = new LethenonSendPanelModel();
		char[] first = "first".toCharArray();
		model.setPassword(first);

		model.setPassword("second".toCharArray());

		assertArrayEquals(new char[5], first);
	}

	private LethenonSendPanel filledPanel(final String password, final String amount)
	{
		LethenonSendPanel panel = new LethenonSendPanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(password.toCharArray());
		panel.getModelObject().setRecipient(recipient.toString());
		panel.getModelObject().setAmount(amount);
		panel.getModelObject().setMemo("watching is not protecting");
		return panel;
	}
}
