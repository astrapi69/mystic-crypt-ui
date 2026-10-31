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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.swing.JComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * "Send LETH" from the wallet in the vault (#704): the window names the wallet instead of asking
 * for a wallet file, asks the wallet password for the send only, and on the main network shows the
 * confirmation T5 before anything is signed; on an unknown network it first asks which network.
 */
class LethenonSendFromVaultTest
{

	@TempDir
	Path configuration;

	@TempDir
	Path directory;

	private LethenonVault vault;

	private Wallet wallet;

	private char[] password;

	private Path chainFile;

	private Bytes recipient;

	private final RecordedConfirmation confirmation = new RecordedConfirmation();

	@BeforeEach
	void aVaultWalletAndAChainPayingIt() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		vault = new LethenonVault();
		vault.connect(new InMemoryVaultAccess());
		wallet = Wallet.create();
		password = ThrowawayPasswords.next();
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = LethenonFixtures.written(directory.resolve("chain.lethenon"),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
		recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private LethenonSendPanel aSendWindowOn(final WalletNetwork network)
	{
		vault.restore(wallet.phrase().toCharArray(), password.clone(), network);
		vault.choose(vault.wallets().get(0));
		return aSendWindow();
	}

	private LethenonSendPanel aSendWindow()
	{
		LethenonSendPanel panel = new LethenonSendPanel(ChainConversionConsent.ASK, vault,
			confirmation, Locale.ENGLISH);
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setRecipient(recipient.toString());
		panel.getModelObject().setAmount("1");
		panel.getModelObject().setFee("0.01");
		return panel;
	}

	private int waiting() throws Exception
	{
		return new ChainFile(chainFile).readPending().size();
	}

	@Test
	@DisplayName("with a wallet in the overview the window names it and shows no wallet file field")
	void theWindow_namesTheVaultWallet()
	{
		LethenonSendPanel panel = aSendWindowOn(WalletNetwork.TESTNET);

		assertEquals("lethenon wallet (testnet), from the vault", textOf(panel, "txtFromWallet"));
		assertFalse(Windows.component(panel, "txtWalletFile", Component.class).isVisible());
	}

	@Test
	@DisplayName("on the test network the transfer is signed with the vault's wallet, without a question, and the password is overwritten")
	void onTheTestNetwork_theTransferIsSigned() throws Exception
	{
		LethenonSendPanel panel = aSendWindowOn(WalletNetwork.TESTNET);
		char[] typed = password.clone();
		panel.getModelObject().setPassword(typed);

		panel.onSend();

		assertEquals(1, waiting(), textOf(panel, "lblResult"));
		assertEquals(List.of(), confirmation.asked);
		assertArrayEquals(new char[typed.length], typed);
	}

	@Test
	@DisplayName("on the main network the confirmation shows amount, fee, total and the whole recipient; Cancel signs nothing")
	void onTheMainNetwork_theConfirmationComesFirst() throws Exception
	{
		LethenonSendPanel panel = aSendWindowOn(WalletNetwork.MAINNET);
		panel.getModelObject().setPassword(password.clone());
		confirmation.sendOnMain = false;

		panel.onSend();

		assertEquals(0, waiting(), "nothing is signed after Cancel");
		assertEquals(List.of("main: 1.00000000 LETH, fee 0.01000000 LETH, together 1.01000000 LETH, to "
			+ recipient), confirmation.asked);

		confirmation.sendOnMain = true;
		panel.getModelObject().setPassword(password.clone());
		panel.onSend();

		assertEquals(1, waiting());
	}

	@Test
	@DisplayName("on an unknown network the window asks which network first; Cancel signs nothing")
	void onAnUnknownNetwork_theNetworkIsAskedFirst() throws Exception
	{
		vault.restore(wallet.phrase().toCharArray(), password.clone(), WalletNetwork.TESTNET);
		VaultWallet listed = vault.wallets().get(0);
		vault.choose(new VaultWallet(listed.id(), listed.title(), "", listed.address(),
			listed.mlDsaAccount()));
		LethenonSendPanel panel = aSendWindow();
		panel.getModelObject().setPassword(password.clone());
		confirmation.network = Optional.empty();

		panel.onSend();

		assertEquals(List.of("which network"), confirmation.asked);
		assertEquals(0, waiting());
	}

	@Test
	@DisplayName("the confirmation's texts are T5 as approved, in both languages, with the whole recipient")
	void theConfirmationTexts_areT5()
	{
		MainNetworkTransfer transfer = new MainNetworkTransfer("1.00000000 LETH",
			"0.01000000 LETH", "1.01000000 LETH", "abc123");

		assertEquals("You are sending 1.00000000 LETH on the MAIN NETWORK to abc123.",
			transfer.sentence(Locale.ENGLISH));
		assertEquals("Du sendest im HAUPTNETZ 1.00000000 LETH an abc123.",
			transfer.sentence(Locale.GERMAN));
		assertEquals("Fee: 0.01000000 LETH. Together: 1.01000000 LETH.", transfer.fee(Locale.ENGLISH));
		assertEquals("Gebühr: 0.01000000 LETH. Zusammen: 1.01000000 LETH.",
			transfer.fee(Locale.GERMAN));
	}

	/** Records what the window asked, and answers as told */
	private static final class RecordedConfirmation implements SendConfirmation
	{

		final List<String> asked = new ArrayList<>();

		boolean sendOnMain = true;

		Optional<NetworkKind> network = Optional.of(NetworkKind.TEST);

		@Override
		public boolean confirmMainNetwork(final JComponent parent, final MainNetworkTransfer transfer)
		{
			asked.add("main: " + transfer.amount() + ", fee " + transfer.fee() + ", together "
				+ transfer.total() + ", to " + transfer.recipient());
			return sendOnMain;
		}

		@Override
		public Optional<NetworkKind> askNetwork(final JComponent parent)
		{
			asked.add("which network");
			return network;
		}
	}
}
