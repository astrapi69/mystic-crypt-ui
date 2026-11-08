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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.JInternalFrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.transport.Node;
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
		assertEquals("No chain yet", labelOf(tool, "lblNoChainTitle"),
			"without a chain file the overview says so instead of a balance (#700, #827)");
		assertTrue(tool.button("btnFetchChain").target().isShowing(),
			"the overview offers to fetch the chain (#827)");

		application.openPluginTool("Send LETH", "Send LETH");
		JInternalFrameFixture send = new JInternalFrameFixture(robot,
			application.internalFrame("Send LETH"));
		assertEquals("TESTNET", labelOf(send, "lblNetworkBadge"),
			"a tool window shows the network of the wallet the overview shows (#697)");
		assertEquals("lethenon wallet (testnet), from the vault", textOf(send, "txtFromWallet"),
			"the send window sends from the wallet in the vault, not from a file (#704)");
		assertFalse(
			GuiActionRunner
				.execute(
					() -> robot.finder()
						.findByName(send.target(), "txtWalletFileNote", java.awt.Component.class,
							false)
						.isVisible()),
			"no note about a file while the vault's wallet is used (#718)");
	}

	/**
	 * The words pasted as people paste them from the backup sheet, numbered "1. word 2. word ...",
	 * restore the wallet (#802): lethenon 0.8.1 reads the numbering away (lethenon#286). 8.14
	 * refused this paste as "you typed 48"
	 */
	@Test
	@DisplayName("a wallet is restored from the 24 words pasted with the numbers of the backup sheet (#802)")
	void aPhrasePastedWithNumbers_restoresTheWallet() throws Exception
	{
		JInternalFrameFixture tool = openTheWalletWindow("lethenon-wallet-restore.mcrdb");
		assertEquals("No wallet in the vault yet", labelOf(tool, "lblEmptyTitle"));

		Wallet wallet = Wallet.create();
		List<String> words = List.of(wallet.phrase().split(" "));
		StringBuilder numbered = new StringBuilder();
		for (int position = 1; position <= words.size(); position++)
		{
			numbered.append(position).append(". ").append(words.get(position - 1)).append(' ');
		}
		String walletPassword = TestPasswords.throwaway();
		click(tool, "btnRestore");
		GuiActionRunner.execute(() -> {
			tool.textBox("txtWords").target().setText(numbered.toString().strip());
			tool.textBox("txtRestorePassword").target().setText(walletPassword);
			tool.textBox("txtRestorePasswordRepeated").target().setText(walletPassword);
		});
		UiTestSpeed.step();
		click(tool, "btnRestoreConfirmed");
		Pause.pause(shown(tool, "txtAddress"), 10000);

		assertEquals(wallet.address().toText(), textOf(tool, "txtAddress"),
			"the overview shows the restored wallet's address");
		assertEquals(1, walletsOfTheVault().size(), "the restored wallet is an entry of the vault");
	}

	/**
	 * A5 of #804 through the installed plugin (#845): a sync through the plugin is what the
	 * overview's line names, with the age of the chain's last block beside it (#840), and the
	 * payment that came to the published address is taken over from the overview with the wallet of
	 * the vault (#834). No node of this program serves the chain file, so the sweep waits next to
	 * it
	 */
	@Test
	@DisplayName("after a sync through the plugin the overview says when it was synced and how old the last block is, and \"Take over\" sweeps what came to the published address (#831)")
	void afterASync_theOverviewSaysHowCurrentItIs_andTakesTheIncomingPaymentOver() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		Wallet wallet = Wallet.create();
		List<BlockBody> served = new ChainFile(
			LethenonTestChains.payingTheWallet(wallet, tempHome).toPath()).require();
		File chainFile = new File(tempHome, "synced.lethenon");
		File databaseFile = new File(tempHome, "lethenon-wallet-take-over.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		try (Node node = Node.on(served))
		{
			int port = node.listen(0);
			application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
			application.showMainFrame();
			syncThroughThePlugin(chainFile, port);
			application.openPluginTool("Wallet", "Wallet");
			JInternalFrameFixture tool = new JInternalFrameFixture(robot,
				application.internalFrame("Wallet"));
			String walletPassword = restore(tool, wallet);

			Pause.pause(shown(tool, "txtChainState"), 20000);
			assertTrue(
				textOf(tool, "txtChainState")
					.matches("Synced: today \\d\\d:\\d\\d, last block: \\d+ days ago"),
				"the line names the sync this program did and the age of the last block: "
					+ textOf(tool, "txtChainState"));

			click(tool, "btnCheckIncoming");
			GuiActionRunner.execute(
				() -> tool.textBox("txtIncomingPassword").target().setText(walletPassword));
			click(tool, "btnCheckIncomingConfirmed");
			Pause.pause(containing(tool, "txtBalanceExact", "is not yet taken over"), 20000);
			assertTrue(
				GuiActionRunner
					.execute(() -> tool.button("btnTakeOverIncoming").target().isShowing()),
				"the payment to the published address can be taken over from the overview");

			click(tool, "btnTakeOverIncoming");
			GuiActionRunner.execute(
				() -> tool.textBox("txtSweepIncomingPassword").target().setText(walletPassword));
			click(tool, "btnTakeOverIncomingConfirmed");
			Pause.pause(containing(tool, "txtNotice", "Swept:"), 20000);

			assertEquals(
				"Swept: 5.00000000 LETH from 1 payment onto your account. It waits next "
					+ "to the chain file until a miner that reads this file mines a block.",
				textOf(tool, "txtNotice"));
			assertEquals(1, new ChainFile(chainFile.toPath()).readPending().size(),
				"the sweep waits in the synced chain file");
			Pause.pause(containing(tool, "txtBalanceExact", "without unchecked incoming payments"),
				20000);
			assertEquals("exactly 3.00000000 LETH, without unchecked incoming payments",
				textOf(tool, "txtBalanceExact"));
		}
	}

	/** Brings a new chain file up to the node's tip through the plugin's sync window */
	private void syncThroughThePlugin(final File chainFile, final int port)
	{
		application.openPluginTool("Synchronise with a Node", "Synchronise with a Node");
		JInternalFrameFixture sync = new JInternalFrameFixture(robot,
			application.internalFrame("Synchronise with a Node"));
		GuiActionRunner.execute(() -> {
			sync.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			sync.textBox("txtNode").target().setText("127.0.0.1:" + port);
		});
		UiTestSpeed.step();
		click(sync, "btnSync");
		Pause.pause(containing(sync, "txtAnswer", "up to date"), 20000);
	}

	/**
	 * Restores the wallet from its 24 words into the vault
	 *
	 * @return the password the wallet is sealed with
	 */
	private String restore(final JInternalFrameFixture tool, final Wallet wallet)
	{
		String walletPassword = TestPasswords.throwaway();
		click(tool, "btnRestore");
		GuiActionRunner.execute(() -> {
			tool.textBox("txtWords").target().setText(wallet.phrase());
			tool.textBox("txtRestorePassword").target().setText(walletPassword);
			tool.textBox("txtRestorePasswordRepeated").target().setText(walletPassword);
		});
		UiTestSpeed.step();
		click(tool, "btnRestoreConfirmed");
		Pause.pause(shown(tool, "txtAddress"), 10000);
		return walletPassword;
	}

	@Test
	@DisplayName("an entry whose address is not its wallet's warns with both addresses when the wallet is opened; Close changes nothing, a confirmed replacement writes the wallet's address (#716)")
	void anEntryWithAForeignAddress_warnsAndReplacesOnlyWhenConfirmed() throws Exception
	{
		JInternalFrameFixture tool = openTheWalletWindow("lethenon-wallet-mismatch.mcrdb");
		String walletPassword = createAWalletUpToTheWords(tool);
		List<String> words = new ArrayList<>();
		for (int position = 1; position <= 24; position++)
		{
			words.add(labelOf(tool, "lblWord" + position).replaceFirst("^\\d+\\. ", ""));
		}
		String actual = Wallet.fromPhrase(String.join(" ", words)).address().toText();
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
		click(tool, "btnDone");
		Pause.pause(shown(tool, "txtAddress"), 10000);
		String foreign = Wallet.create().address().toText();
		recordTheAddress(foreign);

		JOptionPaneFixture warning = openTheWords(tool, walletPassword);
		assertEquals("The address in this entry does not belong to its wallet",
			GuiActionRunner
				.execute(() -> ((java.awt.Dialog)SwingUtilities.getWindowAncestor(warning.target()))
					.getTitle()));
		String message = GuiActionRunner
			.execute(() -> String.valueOf(warning.target().getMessage()));
		assertTrue(message.contains(foreign) && message.contains(actual), message);
		assertTrue(message.contains("Nothing was changed."), message);
		assertEquals("Close",
			GuiActionRunner.execute(() -> String.valueOf(warning.target().getInitialValue())),
			"Close is the default");
		warning.buttonWithText("Close").click();
		robot.waitForIdle();
		assertEquals(foreign, addressOnTheEntry(), "Close changes nothing");

		// after Close the words show, as asked for; leaving them keeps the wallet
		Pause.pause(new Condition("the words are shown after Close")
		{
			@Override
			public boolean test()
			{
				return !labelOf(tool, "lblWord24").isEmpty();
			}
		}, 10000);
		click(tool, "btnWordsCancelled");
		Pause.pause(shown(tool, "txtAddress"), 10000);
		JOptionPaneFixture again = openTheWords(tool, walletPassword);
		// the confirmation is modal too: a robot click would wait for it inside the first dialog
		javax.swing.JButton use = again.buttonWithText("Use the wallet's address").target();
		SwingUtilities.invokeLater(use::doClick);
		Pause.pause(new Condition("the confirmation T6a shows")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner
					.execute(
						() -> java.util.Arrays.stream(java.awt.Window.getWindows())
							.anyMatch(window -> window.isShowing()
								&& window instanceof java.awt.Dialog dialog
								&& "Replace the address in the entry?".equals(dialog.getTitle())));
			}
		}, 20000);
		JOptionPaneFixture confirmation = JOptionPaneFinder.findOptionPane()
			.withTimeout(10, TimeUnit.SECONDS).using(robot);
		String confirmText = GuiActionRunner
			.execute(() -> String.valueOf(confirmation.target().getMessage()));
		assertTrue(confirmText.contains("Old: " + foreign) && confirmText.contains(actual),
			confirmText);
		assertEquals("Cancel",
			GuiActionRunner.execute(() -> String.valueOf(confirmation.target().getInitialValue())),
			"Cancel is the default");
		confirmation.buttonWithText("Replace").click();
		robot.waitForIdle();

		assertEquals(actual, addressOnTheEntry(),
			"the confirmed replacement wrote the wallet's address");
	}

	/** Asks for the words on request, which unseals the wallet, and waits for the warning */
	private JOptionPaneFixture openTheWords(final JInternalFrameFixture tool,
		final String walletPassword)
	{
		click(tool, "btnBackup");
		GuiActionRunner
			.execute(() -> tool.textBox("txtUnsealPassword").target().setText(walletPassword));
		SwingUtilities.invokeLater(() -> tool.button("btnUnsealConfirmed").target().doClick());
		return JOptionPaneFinder.findOptionPane().withTimeout(20, TimeUnit.SECONDS).using(robot);
	}

	/** Writes another address on the wallet's entry, through the plugin's own vault access */
	private static void recordTheAddress(final String address)
	{
		GuiActionRunner.execute(() -> {
			MarkedEntryAccess access = new MarkedEntryAccess(WALLET_MARKER,
				new ModelVaultState(MysticCryptApplicationFrame.getInstance().getModelObject()));
			MysticCryptEntryModelBean entry = access.entries().get(0);
			Map<String, String> values = new LinkedHashMap<>(VaultEntryProperties.valuesOf(entry));
			values.put("lethenon.address", address);
			VaultEntryProperties.replace(entry, values,
				VaultEntryProperties.protectedNamesOf(entry));
			access.change(entry);
		});
	}

	private static String addressOnTheEntry()
	{
		return VaultEntryProperties.valuesOf(walletsOfTheVault().get(0)).get("lethenon.address");
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

	private String createAWalletUpToTheWords(final JInternalFrameFixture tool)
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
		return walletPassword;
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

	private static Condition containing(final JInternalFrameFixture tool, final String field,
		final String expected)
	{
		return new Condition(field + " contains \"" + expected + "\"")
		{
			@Override
			public boolean test()
			{
				return textOf(tool, field).contains(expected);
			}
		};
	}

	/**
	 * What a text field holds, also while it is not on screen, so a condition can wait on a field
	 * that shows only once its answer is there
	 */
	private static String textOf(final JInternalFrameFixture tool, final String componentName)
	{
		return GuiActionRunner.execute(() -> tool.robot().finder()
			.findByName(tool.target(), componentName, JTextComponent.class, false).getText());
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
