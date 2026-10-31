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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The error pattern of draft 07 (#710, README defects G4 to G6): the cause in one sentence, what to
 * do in one sentence, the technical reason under "Show details"; decided by the kind of the
 * failure, never by its text; the answer fields gone while an error shows, and the window as narrow
 * as without it.
 */
class FailurePatternTest
{

	@TempDir
	File configuration;

	@TempDir
	File directory;

	private Path chainFile;

	private Path walletFile;

	@BeforeEach
	void aChainAndAWalletFile() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.getAbsolutePath());
		Wallet wallet = Wallet.create();
		walletFile = new File(directory, "a-wallet-file-with-a-rather-long-name-to-show-the-wrap"
			+ ".lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, ThrowawayPasswords.next());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = LethenonFixtures.written(new File(directory, "chain.lethenon").toPath(),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	static Stream<Arguments> kinds()
	{
		Path nowhere = Path.of("/nowhere/chain.lethenon");
		return Stream.of(
			Arguments.of("a wrong wallet password from the vault", new SecurityException("refused"),
				"The wallet password is not right.",
				"Type it again. It is the password of the wallet, not of the vault."),
			Arguments.of("a wrong password for a wallet file",
				new IllegalArgumentException("this password does not open x",
					new SecurityException("refused")),
				"The wallet password is not right.",
				"Type it again. It is the password of the wallet, not of the vault."),
			Arguments.of("a chain that does not verify", new ChainRejected("block 2: signature"),
				"The chain is not in order; nothing in it can be relied on.",
				"Fetch the chain again from a node."),
			Arguments.of("no chain file at the place", new NoChainFile(nowhere),
				"There is no chain at this place yet.",
				nowhere + "\nFetch the chain from a node, or choose another place."),
			Arguments.of("anything else", new IOException("disk full"),
				"The balance was not read.", "Look at the details below, then try again."));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("kinds")
	@DisplayName("the kind of the failure decides its two sentences; its message goes under the details")
	void theKind_decidesTheSentences(final String caseName, final Throwable thrown,
		final String cause, final String action)
	{
		Failure failure = Failures.of(thrown, new Refusal("lethenon.balance.failure",
			"The balance was not read."), Locale.ENGLISH);

		assertEquals(cause, failure.cause());
		assertEquals(action, failure.action());
		assertEquals(thrown.getMessage(), failure.details());
	}

	@Test
	@DisplayName("the German sentences, as in draft 07")
	void theGermanSentences()
	{
		Failure failure = Failures.of(new SecurityException("refused"),
			new Refusal("lethenon.balance.failure", "The balance was not read."), Locale.GERMAN);

		assertEquals("Das Wallet-Passwort stimmt nicht.", failure.cause());
		assertEquals("Tipp es noch einmal. Es ist das Passwort der Wallet, nicht das des Tresors.",
			failure.action());
	}

	@Test
	@DisplayName("a wrong password in \"Show a Balance\" shows the cause and the action, hides the answer, and keeps the window narrow (G4, G5, G6)")
	void aWrongPassword_inTheBalanceWindow() throws Exception
	{
		LethenonBalancePanel panel = new LethenonBalancePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(ThrowawayPasswords.next());

		panel.onShow();

		assertEquals("The wallet password is not right.", textOf(panel, "txtFailureCause"));
		assertFalse(Windows.component(panel, "txtAnswer", Component.class).isVisible(),
			"no empty answer while an error shows");
		assertFalse(Windows.component(panel, "txtFailureDetails", Component.class).isVisible(),
			"the details are closed at first");
		assertEquals(" ", textOf(panel, "lblResult"), "the status line does not repeat the error");
		assertTrue(panel.getPreferredSize().width <= 1000,
			"the window wraps instead of widening: " + panel.getPreferredSize().width);
	}

	@Test
	@DisplayName("a wrong password in \"Mine a Pun\" shows the cause, and a block mined afterwards brings the answer back")
	void aWrongPassword_inTheMineWindow() throws Exception
	{
		LethenonMinePanel panel = new LethenonMinePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(ThrowawayPasswords.next());

		panel.onMine();

		assertEquals("The wallet password is not right.", textOf(panel, "txtFailureCause"));
		assertFalse(Windows.component(panel, "txtAnswer", Component.class).isVisible());
		assertTrue(panel.getPreferredSize().width <= 1000, "" + panel.getPreferredSize().width);
	}
}
