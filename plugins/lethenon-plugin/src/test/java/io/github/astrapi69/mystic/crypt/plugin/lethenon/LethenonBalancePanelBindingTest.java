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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.ChainState;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * "Show a Balance" answers in fields a child can read (#604): the answer first, in one sentence;
 * then one labelled, read-only field per value, a Copy button for what a person passes on, and the
 * replay behind "Show details". Every field is bound to the panel's model, which is what these
 * tests read; the end-to-end test reads the same fields on the screen.
 */
class LethenonBalancePanelBindingTest
{

	private static final String PASSWORD = "correct horse battery staple";

	private static final String ED25519 = SignatureSuite.ED25519.identifier();

	private static final String ML_DSA_65 = SignatureSuite.ML_DSA_65.identifier();

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
	@DisplayName("the answer comes first, in one sentence, the one-time payment named apart")
	void showing_answersFirst_inOneSentence() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);

		panel.onShow();

		assertEquals("You hold 3.00000000 LETH, and 5.00000000 LETH came to your address and "
			+ "waits for a sweep.", panel.getModelObject().getAnswer());
		assertEquals("You hold 3.00000000 LETH, and 5.00000000 LETH came to your address and "
			+ "waits for a sweep.", textOf(panel, "txtAnswer"));
		assertEquals("the balance was read from the replayed chain",
			panel.getModelObject().getResultText());
	}

	@Test
	@DisplayName("every value is in a field of its own, and the one-time payment is never added to what can be spent")
	void showing_putsEveryValueInAFieldOfItsOwn() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);

		panel.onShow();

		LethenonBalancePanelModel model = panel.getModelObject();
		assertEquals(wallet.address().toText(), model.getAddress());
		assertEquals(wallet.spendKey(SignatureSuite.ED25519).toString(), model.getAccount(ED25519));
		assertEquals(wallet.spendKey(SignatureSuite.ML_DSA_65).toString(),
			model.getAccount(ML_DSA_65));
		assertEquals("3.00000000 LETH", model.getHolds(ED25519));
		assertEquals("0.00000000 LETH", model.getHolds(ML_DSA_65));
		assertEquals("1 payment, 5.00000000 LETH", model.getWaitingForSweep());
		assertEquals(model.getHolds(ED25519), textOf(panel, "txtHolds_" + ED25519));
		assertEquals(model.getWaitingForSweep(), textOf(panel, "txtWaitingForSweep"));
		assertFalse(textsOf(panel).stream().anyMatch(text -> text.contains("8.00000000")),
			"the one-time payment must not be added to what can be spent");
	}

	@Test
	@DisplayName("a value too long to read is shortened on the screen, and Copy copies all of it")
	void aLongValue_isShortened_andCopyCopiesAllOfIt() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);
		panel.onShow();
		String account = wallet.spendKey(SignatureSuite.ML_DSA_65).toString();

		assertEquals(LongValues.shortened(account, Locale.ENGLISH),
			textOf(panel, "txtAccount_" + ML_DSA_65));
		assertTrue(textOf(panel, "txtAccount_" + ML_DSA_65).length() < 60,
			textOf(panel, "txtAccount_" + ML_DSA_65));
		component(panel, "btnCopyAccount_" + ML_DSA_65, AbstractButton.class).doClick();

		assertEquals(account, Toolkit.getDefaultToolkit().getSystemClipboard()
			.getData(DataFlavor.stringFlavor));

		component(panel, "btnCopyAddress", AbstractButton.class).doClick();

		assertEquals(wallet.address().toText(), Toolkit.getDefaultToolkit().getSystemClipboard()
			.getData(DataFlavor.stringFlavor));
	}

	@Test
	@DisplayName("what only a specialist reads is behind \"Show details\", closed at first")
	void theReplay_isBehindDetails_closedAtFirst() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);
		panel.onShow();
		ChainState state = Replay.verify(new ChainFile(chainFile).read()).finalState();

		assertFalse(panel.getModelObject().isDetailsShown());
		assertFalse(component(panel, "pnlDetails", JComponent.class).isVisible());
		assertEquals("3 blocks, 2 transfers, 2 signatures",
			panel.getModelObject().getChainChecked());
		assertEquals(LethAmounts.format(state.supply(), Locale.ENGLISH),
			panel.getModelObject().getSupply());

		component(panel, "btnDetails", AbstractButton.class).doClick();

		assertTrue(panel.getModelObject().isDetailsShown());
		assertTrue(component(panel, "pnlDetails", JComponent.class).isVisible());
		assertEquals("3 blocks, 2 transfers, 2 signatures", textOf(panel, "txtChainChecked"));
	}

	@Test
	@DisplayName("without one-time payments the answer is the amount alone, summed over every account")
	void theAnswer_withoutOneTimePayments_isTheAmountAlone()
	{
		WalletBalance balance = new WalletBalance(
			List.of(new AccountBalance(ED25519, "aa", Amount.parseLeth("3")),
				new AccountBalance(ML_DSA_65, "bb", Amount.parseLeth("2"))),
			0, Amount.ZERO, "address", new ChainReplayReport(3, 0, 0, Amount.ZERO, Amount.ZERO,
				"summary"));

		assertEquals("You hold 5.00000000 LETH.",
			LethenonBalancePanel.answerFor(balance, Locale.ENGLISH));
	}

	@Test
	@DisplayName("every field, button and choice of the window has a tooltip in plain words")
	void everyFieldAndButton_hasATooltip() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);
		panel.onShow();
		List<String> silent = new ArrayList<>();
		for (JComponent component : named(panel))
		{
			boolean interactive = component instanceof JTextComponent
				|| component instanceof AbstractButton;
			String tooltip = component.getToolTipText();
			if (interactive && (tooltip == null || tooltip.isBlank()))
			{
				silent.add(component.getName());
			}
		}

		assertEquals(List.of(), silent, "fields and buttons without a tooltip");
	}

	@Test
	@DisplayName("the password is gone from the model once it was used, and was in no text")
	void showing_theBalance_dropsThePassword() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);

		panel.onShow();

		assertEquals(0, panel.getModelObject().getPassword().length,
			"a password that opened the wallet once has no reason to stay in memory");
		assertFalse(textsOf(panel).stream().anyMatch(text -> text.contains(PASSWORD)));
		assertFalse(panel.getModelObject().getResultText().contains(PASSWORD));
	}

	@Test
	@DisplayName("a wrong password says what to do, leaves every field empty and does not repeat the password")
	void showing_withAWrongPassword_saysWhatToDo() throws Exception
	{
		LethenonBalancePanel panel = filledPanel(PASSWORD);
		panel.onShow();
		panel.getModelObject().setPassword("not the password at all".toCharArray());

		panel.onShow();

		LethenonBalancePanelModel model = panel.getModelObject();
		assertEquals("", model.getAnswer());
		assertEquals("", model.getAddress());
		assertEquals("", model.getHolds(ED25519));
		assertEquals("", model.getWaitingForSweep());
		assertEquals("", textOf(panel, "txtAddress"), "nothing of the earlier answer stays");
		assertEquals("the balance could not be read: this password does not open "
			+ "wallet.lethenon-wallet; type it again, and check that it is the wallet you meant ("
			+ walletFile.toAbsolutePath() + ")", model.getResultText());
		assertEquals(0, model.getPassword().length);
	}

	@Test
	@DisplayName("an empty password is asked for, not tried")
	void showing_withoutAPassword_asksForIt() throws Exception
	{
		LethenonBalancePanel panel = filledPanel("");

		panel.onShow();

		assertEquals("the balance could not be read: type the password of wallet.lethenon-wallet "
			+ "first", panel.getModelObject().getResultText());
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

	private static String textOf(final Container panel, final String name)
	{
		Component found = component(panel, name, Component.class);
		return found instanceof JTextComponent text ? text.getText()
			: ((javax.swing.JLabel)found).getText();
	}

	private static List<String> textsOf(final Container panel)
	{
		return named(panel).stream()
			.flatMap(component -> component instanceof JTextComponent text
				? Stream.of(text.getText())
				: component instanceof javax.swing.JLabel label ? Stream.of(label.getText())
					: Stream.empty())
			.toList();
	}

	private static <T> T component(final Container panel, final String name, final Class<T> type)
	{
		T found = named(panel).stream().filter(component -> name.equals(component.getName()))
			.map(type::cast).findFirst().orElse(null);
		assertNotNull(found, "the window has no component named " + name);
		return found;
	}

	private static List<JComponent> named(final Container container)
	{
		List<JComponent> found = new ArrayList<>();
		for (Component child : container.getComponents())
		{
			if (child instanceof JComponent component && component.getName() != null)
			{
				found.add(component);
			}
			if (child instanceof Container nested)
			{
				found.addAll(named(nested));
			}
		}
		return found;
	}
}
