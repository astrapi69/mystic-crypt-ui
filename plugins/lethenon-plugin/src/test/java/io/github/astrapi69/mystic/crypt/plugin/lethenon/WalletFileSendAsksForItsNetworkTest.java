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

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.JComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * "Send LETH" from a wallet file asks which network the wallet is on before it signs, and on the
 * main network then the confirmation T5, as for a wallet of the vault whose network is not
 * recorded (#978, the owner's option 1): a wallet file records no network, and until now a send
 * from one was signed on the main network without either question
 */
class WalletFileSendAsksForItsNetworkTest
{

	private static final String WHICH_NETWORK = "which network";

	private static final String MAIN_NETWORK_CONFIRMATION = "main network confirmation";

	@TempDir
	Path configuration;

	@TempDir
	Path directory;

	private final Wallet wallet = Wallet.create();

	private final char[] password = ThrowawayPasswords.next();

	private Path chainFile;

	private Path walletFile;

	@BeforeEach
	void aWalletFileAndAChainPayingIt() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = LethenonFixtures.written(directory.resolve("chain.lethenon"),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
		walletFile = directory.resolve("wallet.lethwf");
		WalletFile.write(walletFile, wallet, password.clone());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * One answer to each of the two questions, what the window must have asked, and how many
	 * transfers it must have signed
	 */
	record Case(String name, Optional<NetworkKind> network, boolean sendOnMain,
		List<String> asked, int signed)
	{
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Case> answers()
	{
		return Stream.of(
			new Case("Cancel at the question which network: nothing is signed", Optional.empty(),
				true, List.of(WHICH_NETWORK), 0),
			new Case("the test network: signed without the confirmation T5",
				Optional.of(NetworkKind.TEST), true, List.of(WHICH_NETWORK), 1),
			new Case("the main network, Cancel at T5: nothing is signed",
				Optional.of(NetworkKind.MAIN), false,
				List.of(WHICH_NETWORK, MAIN_NETWORK_CONFIRMATION), 0),
			new Case("the main network, Send at T5: signed after both questions",
				Optional.of(NetworkKind.MAIN), true,
				List.of(WHICH_NETWORK, MAIN_NETWORK_CONFIRMATION), 1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("answers")
	void aSendFromAWalletFileAsksItsNetworkFirst(final Case answer) throws Exception
	{
		RecordedConfirmation confirmation = new RecordedConfirmation(answer.network(),
			answer.sendOnMain());
		LethenonVault vault = new LethenonVault();
		vault.connect(new InMemoryVaultAccess());
		LethenonSendPanel panel = new LethenonSendPanel(ChainConversionConsent.ASK, vault,
			confirmation, Locale.ENGLISH);
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(password.clone());
		panel.getModelObject().setRecipient(TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic()).toString());
		panel.getModelObject().setAmount("1");
		panel.getModelObject().setFee("0.01");

		panel.onSend();

		assertEquals(answer.asked(), confirmation.asked);
		assertEquals(answer.signed(), new ChainFile(chainFile).readPending().size());
	}

	/** Answers the two questions as told, and records which of them it was asked */
	private static final class RecordedConfirmation implements SendConfirmation
	{

		final List<String> asked = new ArrayList<>();

		private final Optional<NetworkKind> network;

		private final boolean sendOnMain;

		RecordedConfirmation(final Optional<NetworkKind> network, final boolean sendOnMain)
		{
			this.network = network;
			this.sendOnMain = sendOnMain;
		}

		@Override
		public boolean confirmMainNetwork(final JComponent parent,
			final MainNetworkTransfer transfer)
		{
			asked.add(MAIN_NETWORK_CONFIRMATION);
			return sendOnMain;
		}

		@Override
		public Optional<NetworkKind> askNetwork(final JComponent parent)
		{
			asked.add(WHICH_NETWORK);
			return network;
		}
	}
}
