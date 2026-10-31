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
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The error pattern of draft 07 in the windows #710 left for later (#712): verify, show the chain,
 * send, sweep, synchronise and run a node. Each shows the cause and what to do, keeps the details
 * closed, hides its answer, leaves the status line empty and stays narrow.
 */
class FailurePatternInEveryWindowTest
{

	/** A path no file system accepts, so the window refuses it before anything else runs */
	private static final String NOT_A_PATH = "a\0b";

	@TempDir
	static File configuration;

	@TempDir
	static File directory;

	private static Path chainFile;

	private static Path walletFile;

	private static Path nowhere;

	@BeforeAll
	static void aChainAndAWalletFile() throws Exception
	{
		Wallet wallet = Wallet.create();
		walletFile = new File(directory, "wallet.lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, ThrowawayPasswords.next());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = LethenonFixtures.written(new File(directory, "chain.lethenon").toPath(),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
		nowhere = new File(directory, "no-chain-here.lethenon").toPath();
	}

	@BeforeEach
	void aConfigurationOfItsOwn()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.getAbsolutePath());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * One failure per window
	 *
	 * @param window
	 *            what the user sees
	 * @param panel
	 *            builds the window, once the configuration directory is set
	 * @param action
	 *            fills the window and presses its button
	 * @param cause
	 *            the sentence the window shows first
	 * @param answer
	 *            the name of the component that holds the window's answer
	 */
	record WindowFailure(String window, Supplier<JPanel> panel, Consumer<JPanel> action, String cause,
		String answer)
	{
		@Override
		public String toString()
		{
			return window;
		}
	}

	static Stream<Arguments> windows()
	{
		return Stream.of(Arguments.of(new WindowFailure("Verify the Chain, no chain file",
			LethenonChainPanel::new, panel -> {
				LethenonChainPanel verify = (LethenonChainPanel)panel;
				verify.getModelObject().setChainFile(nowhere.toString());
				verify.onVerify();
			}, "There is no chain at this place yet.", "txtAnswer")),
			Arguments.of(new WindowFailure("Show the Chain, no chain file",
				LethenonChainViewPanel::new, panel -> {
					LethenonChainViewPanel view = (LethenonChainViewPanel)panel;
					view.getModelObject().setChainFile(nowhere.toString());
					view.onShow();
				}, "There is no chain at this place yet.", "tblBlocks")),
			Arguments.of(new WindowFailure("Send LETH, a wrong wallet password",
				LethenonSendPanel::new, panel -> {
					LethenonSendPanel send = (LethenonSendPanel)panel;
					send.getModelObject().setChainFile(chainFile.toString());
					send.getModelObject().setWalletFile(walletFile.toString());
					send.getModelObject().setPassword(ThrowawayPasswords.next());
					send.getModelObject().setRecipient(TransactionSigner
						.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic())
						.toString());
					send.getModelObject().setAmount("1");
					send.onSend();
				}, "The wallet password is not right.", "txtAnswer")),
			Arguments.of(new WindowFailure("Sweep One-Time Payments, a wrong wallet password",
				LethenonSweepPanel::new, panel -> {
					LethenonSweepPanel sweep = (LethenonSweepPanel)panel;
					sweep.getModelObject().setChainFile(chainFile.toString());
					sweep.getModelObject().setWalletFile(walletFile.toString());
					sweep.getModelObject().setPassword(ThrowawayPasswords.next());
					sweep.getModelObject().setFee("0.1");
					sweep.onSweep();
				}, "The wallet password is not right.", "txtAnswer")),
			Arguments.of(new WindowFailure("Synchronise with a Node, not a path",
				LethenonSyncPanel::new, panel -> {
					LethenonSyncPanel sync = (LethenonSyncPanel)panel;
					sync.getModelObject().setChainFile(NOT_A_PATH);
					sync.onSync();
				}, "The chain was not synchronised.", "txtAnswer")),
			Arguments.of(new WindowFailure("Run Your Own Node, not a path",
				LethenonNodePanel::new, panel -> {
					LethenonNodePanel node = (LethenonNodePanel)panel;
					node.getModelObject().setChainFile(NOT_A_PATH);
					node.onStart();
				}, "The node did not start.", "txtAnswer")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("windows")
	@DisplayName("a failure shows the cause and what to do, hides the answer and keeps the window narrow (G4, G5, G6)")
	void aFailure_showsThePattern(final WindowFailure failure)
	{
		JPanel panel = failure.panel().get();

		failure.action().accept(panel);

		assertEquals(failure.cause(), textOf(panel, "txtFailureCause"));
		assertFalse(textOf(panel, "txtFailureAction").isBlank(), "what to do is said");
		assertFalse(Windows.component(panel, "txtFailureDetails", Component.class).isVisible(),
			"the details are closed at first");
		assertFalse(answerOf(panel, failure.answer()).isVisible(),
			"no empty answer while an error shows");
		assertEquals(" ", textOf(panel, "lblResult"), "the status line does not repeat the error");
		assertTrue(panel.getPreferredSize().width <= 1000,
			"the window wraps instead of widening: " + panel.getPreferredSize().width);
	}

	@Test
	@DisplayName("a chain shown after a failure brings the blocks back and takes the failure away")
	void aChainAfterAFailure_bringsTheAnswerBack()
	{
		LethenonChainViewPanel panel = new LethenonChainViewPanel();
		panel.getModelObject().setChainFile(nowhere.toString());
		panel.onShow();

		panel.getModelObject().setChainFile(chainFile.toString());
		panel.onShow();

		assertFalse(Windows.component(panel, "pnlFailure", Component.class).isVisible());
		assertTrue(answerOf(panel, "tblBlocks").isVisible(), "the blocks show again");
		assertTrue(textOf(panel, "lblResult").startsWith("the chain was accepted"),
			textOf(panel, "lblResult"));
	}

	/** The answer component, or the scroll pane around it, which is what the form hides */
	private static Component answerOf(final JPanel panel, final String name)
	{
		Component answer = Windows.component(panel, name, Component.class);
		Component viewport = answer.getParent();
		return viewport instanceof javax.swing.JViewport ? viewport.getParent() : answer;
	}
}
