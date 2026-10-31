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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.JInternalFrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.vault.MarkedEntryAccess;
import io.github.astrapi69.mystic.crypt.plugin.vault.ModelVaultState;

/**
 * The wallet window of the lethenon plugin through the running application (#680): the plugin
 * installs from its zip, the host connects it to the vault (#677), "Wallet" opens on "No wallet in
 * the vault yet", and a wallet created there is an entry of the vault. Locking while the 24 words
 * are on screen takes the unconfirmed wallet back, which shows that the plugin hears the lock while
 * the vault can still be written.
 */
class LethenonWalletUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String WALLET_MARKER = "lethenon.wallet";

	@Test
	@DisplayName("a wallet created in the wallet window goes through the three backup steps, is an entry of the vault shown with its address, and every window shows its network")
	void aCreatedWallet_isAnEntryOfTheVault() throws Exception
	{
		JInternalFrameFixture tool = openTheWalletWindow("lethenon-wallet.mcrdb");
		assertEquals("No wallet in the vault yet", labelOf(tool, "lblEmptyTitle"));

		createAWalletUpToTheWords(tool);
		List<String> words = new ArrayList<>();
		for (int position = 1; position <= 24; position++)
		{
			words.add(labelOf(tool, "lblWord" + position).replaceFirst("^\\d+\\. ", ""));
		}
		click(tool, "btnWrittenDown");
		answerTheThreeWords(tool, words);
		click(tool, "btnQuestionConfirmed");
		Pause.pause(new Condition("step 3, the export, is shown")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> tool.button("btnDone").target().isShowing());
			}
		}, 10000);
		assertTrue(labelOf(tool, "lblWord1").isEmpty(), "no word is left in the window");
		click(tool, "btnDone");
		Pause.pause(shown(tool, "txtAddress"), 10000);

		List<MysticCryptEntryModelBean> wallets = walletsOfTheVault();
		assertEquals(1, wallets.size());
		assertEquals(VaultEntryProperties.valuesOf(wallets.get(0)).get("lethenon.address"),
			textOf(tool, "txtAddress"), "the overview shows the address the entry records");
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isDirty(),
			"the new entry is a change of the vault, to be saved");
		assertTrue(textOf(tool, "txtBackupNotice").startsWith("Your backup is not checked yet"),
			"the overview says the backup is not checked: " + textOf(tool, "txtBackupNotice"));
		assertEquals("TESTNET", labelOf(tool, "lblNetworkBadge"));
		assertTrue(textOf(tool, "txtBalanceExact").startsWith("No chain yet"),
			"without a chain file the overview says so instead of a balance (#700): "
				+ textOf(tool, "txtBalanceExact"));

		application.openPluginTool("Send LETH", "Send LETH");
		JInternalFrameFixture send = new JInternalFrameFixture(robot,
			application.internalFrame("Send LETH"));
		assertEquals("TESTNET", labelOf(send, "lblNetworkBadge"),
			"a tool window shows the network of the wallet the overview shows (#697)");
	}

	/** Types the words step 2 asks for, read from the positions its labels name */
	private void answerTheThreeWords(final JInternalFrameFixture tool, final List<String> words)
	{
		for (int answer = 1; answer <= 3; answer++)
		{
			int position = Integer
				.parseInt(labelOf(tool, "lblAnswer" + answer).replaceAll("\\D", ""));
			String field = "txtAnswer" + answer;
			String word = words.get(position - 1);
			GuiActionRunner.execute(() -> tool.textBox(field).target().setText(word));
		}
		UiTestSpeed.step();
	}

	@Test
	@DisplayName("locking while the 24 words are shown takes the unconfirmed wallet back out of the vault")
	void lockingWhileTheWordsAreShown_takesTheWalletBack() throws Exception
	{
		JInternalFrameFixture tool = openTheWalletWindow("lethenon-wallet-lock.mcrdb");
		createAWalletUpToTheWords(tool);
		assertEquals(1, walletsOfTheVault().size(),
			"the wallet is in the vault while its words show");

		application.lockWorkspace();
		application.unlockWorkspace(MASTER_PASSWORD);

		assertEquals(List.of(), walletsOfTheVault(),
			"the wallet whose words were never confirmed is gone after the lock");
	}

	private ApplicationSteps application;

	private JInternalFrameFixture openTheWalletWindow(final String databaseName) throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		File databaseFile = new File(tempHome, databaseName);
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.openPluginTool("Wallet", "Wallet");
		return new JInternalFrameFixture(robot, application.internalFrame("Wallet"));
	}

	private void createAWalletUpToTheWords(final JInternalFrameFixture tool)
	{
		String walletPassword = TestPasswords.throwaway();
		click(tool, "btnCreate");
		GuiActionRunner.execute(() -> {
			tool.textBox("txtCreatePassword").target().setText(walletPassword);
			tool.textBox("txtCreatePasswordRepeated").target().setText(walletPassword);
		});
		UiTestSpeed.step();
		click(tool, "btnCreateConfirmed");
		Pause.pause(new Condition("the 24 words are shown")
		{
			@Override
			public boolean test()
			{
				return !labelOf(tool, "lblWord24").isEmpty();
			}
		}, 20000);
	}

	private static List<MysticCryptEntryModelBean> walletsOfTheVault()
	{
		return GuiActionRunner.execute(() -> new MarkedEntryAccess(WALLET_MARKER,
			new ModelVaultState(MysticCryptApplicationFrame.getInstance().getModelObject()))
				.entries());
	}

	private void click(final JInternalFrameFixture tool, final String button)
	{
		SwingUtilities.invokeLater(() -> tool.button(button).target().doClick());
		robot.waitForIdle();
		UiTestSpeed.step();
	}

	private static Condition shown(final JInternalFrameFixture tool, final String field)
	{
		return new Condition(field + " shows a value")
		{
			@Override
			public boolean test()
			{
				return !textOf(tool, field).isEmpty();
			}
		};
	}

	private static String textOf(final JInternalFrameFixture tool, final String componentName)
	{
		return GuiActionRunner.execute(() -> tool.textBox(componentName).target().getText());
	}

	/**
	 * What a label shows, also while its view is not on screen: the words' labels are read after
	 * the window has left the view that shows them
	 */
	private String labelOf(final JInternalFrameFixture tool, final String componentName)
	{
		return GuiActionRunner.execute(() -> robot.finder()
			.findByName(tool.target(), componentName, JLabel.class, false).getText());
	}
}
