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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JInternalFrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.DifficultyRule;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.lethenon.Mining;
import io.github.astrapi69.lethenon.OneTimeAddresses;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Transfers;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.lethenon.transport.PeerAddress;
import io.github.astrapi69.lethenon.transport.Sync;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Milestone 5 of lethenon#2, the parts of it that are built: the plugin installs from its zip, its
 * submenu appears, the replay verifier behind its button reports what it verified in a chain file
 * written by the chain library itself, the chain view lists that file's blocks, the balance window
 * reads a wallet, the send window signs a transfer that the chain library reads back, the mining
 * window writes the next block, which the chain library reads back and replays, the sync window
 * brings a chain file up to the tip of a node running in this test, the send window hands a
 * transfer to such a node, and the node window runs a mining node of its own (#530).
 * <p>
 * The chain here is built with lethenon's own encoder rather than with a committed fixture, because
 * what a user's chain file looks like is whatever that encoder writes - a fixture would freeze one
 * version of the format and keep passing after the format moved.
 */
class LethenonPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the plugin replays a chain file and reports what it verified")
	void thePlugin_verifiesAChainFile() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		File chainFile = aChainWithOneTransfer();
		File databaseFile = new File(tempHome, "lethenon-plugin.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Verify a Chain", "Verify a Chain");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Verify a Chain"));
		GuiActionRunner.execute(
			() -> tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath()));
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnVerify").target().doClick());

		Pause.pause(new Condition("the replay has reported what it checked")
		{
			@Override
			public boolean test()
			{
				return textOf(tool, "txtReport").contains("replayed 3 blocks");
			}
		}, 20000);

		String report = textOf(tool, "txtReport");
		assertTrue(report.contains("applied 1 transfers"), report);
		assertTrue(report.contains("which is the supply"),
			"the chain's own summary carries the supply invariant: " + report);
		String result = GuiActionRunner.execute(() -> tool.label("lblResult").target().getText());
		assertTrue(result.contains("accepted"), result);
		assertTrue(frame.isEnabled(), "the application is still usable after a replay");
	}

	@Test
	@DisplayName("the plugin lists the blocks of a chain file with their puns and whom they paid")
	void thePlugin_showsTheBlocksOfAChain() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		File chainFile = aChainWithOneTransfer();
		File databaseFile = new File(tempHome, "lethenon-chain-view.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Show a Chain", "Show a Chain");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Show a Chain"));
		GuiActionRunner.execute(
			() -> tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath()));
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnShowBlocks").target().doClick());

		Pause.pause(new Condition("the chain view lists the blocks")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner
					.execute(() -> tool.table("tblBlocks").target().getRowCount()) == 3;
			}
		}, 20000);

		String genesisPun = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(0, 1)));
		String transferPun = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(2, 1)));
		String transfers = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(2, 3)));
		String genesisPaidTo = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(0, 2)));
		assertTrue(genesisPun.startsWith("in the beginning was the pun"), genesisPun);
		assertEquals(Genesis.NOBODY.toString(), genesisPaidTo,
			"the genesis block paid the burn account (lethenon#148)");
		assertTrue(transferPun.startsWith("the block with the transfer"), transferPun);
		assertTrue("1".equals(transfers), "the third block carries the one transfer: " + transfers);
		String result = GuiActionRunner.execute(() -> tool.label("lblResult").target().getText());
		assertTrue(result.contains("accepted: 3 blocks"), result);
		assertTrue(frame.isEnabled(), "the application is still usable after listing a chain");
	}

	@Test
	@DisplayName("the plugin shows a wallet's balance, the one-time payment apart and not spendable")
	void thePlugin_showsTheBalanceOfAWallet() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "wallet.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		File chainFile = aChainPayingTheWallet(wallet);
		File databaseFile = new File(tempHome, "lethenon-balance.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Show a Balance", "Show a Balance");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Show a Balance"));
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			tool.textBox("txtPassword").target().setText(walletPassword);
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnShowBalance").target().doClick());

		Pause.pause(new Condition("the balance is shown")
		{
			@Override
			public boolean test()
			{
				return textOf(tool, "txtAnswer").startsWith("You hold");
			}
		}, 30000);

		assertEquals(
			"You hold 3.00000000 LETH, and 5.00000000 LETH came to your address and "
				+ "waits for a sweep.",
			textOf(tool, "txtAnswer"),
			"the answer comes first, the one-time payment named apart and not added (#604)");
		assertEquals("3.00000000 LETH", textOf(tool, "txtHolds_ed25519"));
		assertEquals("0.00000000 LETH", textOf(tool, "txtHolds_ml-dsa-65"));
		assertEquals("1 payment, 5.00000000 LETH", textOf(tool, "txtWaitingForSweep"));
		String address = wallet.address().toText();
		String shownAddress = textOf(tool, "txtAddress");
		assertTrue(shownAddress.startsWith(address.substring(0, 12)), shownAddress);
		assertTrue(shownAddress.endsWith("(" + address.length() + " characters)"), shownAddress);
		GuiActionRunner.execute(() -> tool.button("btnCopyAddress").target().doClick());
		assertEquals(address,
			GuiActionRunner.execute(() -> (String)Toolkit.getDefaultToolkit().getSystemClipboard()
				.getData(DataFlavor.stringFlavor)),
			"Copy copies the whole address, not what the field shows");
		assertFalse(
			GuiActionRunner.execute(() -> robot.finder()
				.findByName(tool.target(), "pnlDetails", JPanel.class, false).isShowing()),
			"the details are closed at first");
		GuiActionRunner.execute(() -> tool.button("btnDetails").target().doClick());
		assertEquals("3 blocks, 2 transfers, 2 signatures", textOf(tool, "txtChainChecked"));
		assertEquals("1,984,000,000.00000000 LETH", textOf(tool, "txtSupply"));
		for (String field : new String[] { "txtAddress", "txtAccount_ed25519",
				"txtAccount_ml-dsa-65", "txtHolds_ed25519", "txtWaitingForSweep", "txtChainChecked",
				"txtSupply" })
		{
			assertFalse(textOf(tool, field).contains(walletPassword),
				"the password is in no text on the screen");
		}
		String passwordLeft = GuiActionRunner
			.execute(() -> tool.textBox("txtPassword").target().getText());
		assertTrue(passwordLeft.isEmpty(), "the password field is cleared after one use");
		assertTrue(frame.isEnabled(), "the application is still usable after a balance");
	}

	@Test
	@DisplayName("a balance once read is remembered: the next \"Show a Balance\" starts with its chain and wallet, never the password (#605)")
	void thePlugin_remembersTheChainAndTheWalletThatWorked() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "remembered.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		File chainFile = aChainPayingTheWallet(wallet);
		File databaseFile = new File(tempHome, "lethenon-remembered.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.openPluginTool("Show a Balance", "Show a Balance");
		JInternalFrameFixture first = new JInternalFrameFixture(robot,
			application.internalFrame("Show a Balance"));
		GuiActionRunner.execute(() -> {
			first.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			first.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			first.textBox("txtPassword").target().setText(walletPassword);
		});
		SwingUtilities.invokeLater(() -> first.button("btnShowBalance").target().doClick());
		Pause.pause(new Condition("the balance is read")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> first.label("lblResult").target().getText())
					.startsWith("the balance was read");
			}
		}, 30000);
		first.close();

		application.openPluginTool("Show a Balance", "Show a Balance");
		JInternalFrameFixture second = new JInternalFrameFixture(robot,
			application.internalFrame("Show a Balance"));

		assertEquals(chainFile.getAbsolutePath(), textOf(second, "txtChainFile"),
			"the chain that worked is what the next window starts with");
		assertEquals(walletFile.getAbsolutePath(), textOf(second, "txtWalletFile"),
			"so is the wallet that opened");
		assertTrue(textOf(second, "txtPassword").isEmpty(), "the password is never remembered");
		try (Stream<Path> files = Files.walk(tempHome.toPath().resolve(".config")))
		{
			for (Path file : files.filter(Files::isRegularFile).toList())
			{
				assertFalse(
					Files.readString(file, StandardCharsets.ISO_8859_1).contains(walletPassword),
					file + " holds the wallet's password");
			}
		}
	}

	@Test
	@DisplayName("the plugin signs a transfer with a memo and leaves it waiting next to the chain file")
	void thePlugin_sendsATransferWithAMemo() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "wallet.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		File chainFile = aChainPayingTheWallet(wallet);
		Bytes recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
		File databaseFile = new File(tempHome, "lethenon-send.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Send LETH", "Send LETH");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Send LETH"));
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			tool.textBox("txtPassword").target().setText(walletPassword);
			tool.textBox("txtRecipient").target().setText(recipient.toString());
			tool.textBox("txtAmount").target().setText("1.25");
			tool.textBox("txtMemo").target().setText("watching is not protecting");
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnSend").target().doClick());

		Pause.pause(new Condition("the transfer is signed")
		{
			@Override
			public boolean test()
			{
				return textOf(tool, "txtAnswer").startsWith("Sent:");
			}
		}, 30000);

		assertEquals("Sent: 1.25000000 LETH to the account you named. It counts once the next "
			+ "block is mined.", textOf(tool, "txtAnswer"));
		assertEquals("1.25000000 LETH", textOf(tool, "txtSentAmount"));
		assertEquals(recipient.toString(), textOf(tool, "txtSentTo"));
		assertEquals("next to the chain file, until the next block is mined",
			textOf(tool, "txtWhereItWaits"));
		tool.button("btnDetails").click();
		assertEquals("0", textOf(tool, "txtNonce"));
		for (String field : List.of("txtAnswer", "txtSentAmount", "txtSentFee", "txtSentTo",
			"txtWhereItWaits", "txtNonce", "txtWaiting"))
		{
			assertFalse(textOf(tool, field).contains(walletPassword),
				"the password is in no text on the screen");
		}
		List<SignedTransaction> pending = new ChainFile(chainFile.toPath()).readPending();
		assertEquals(1, pending.size(), "the transfer waits in the chain library's pending file");
		assertEquals("watching is not protecting", pending.getFirst().body().memo());
		assertEquals(Destination.direct(recipient), pending.getFirst().body().recipient());
		assertEquals(Amount.parseLeth("1.25"), pending.getFirst().body().amount());
		String passwordLeft = GuiActionRunner
			.execute(() -> tool.textBox("txtPassword").target().getText());
		assertTrue(passwordLeft.isEmpty(), "the password field is cleared after one use");
		assertTrue(frame.isEnabled(), "the application is still usable after a transfer");
	}

	@Test
	@DisplayName("the plugin mines a pun into the next block, carrying the transfer that waited")
	void thePlugin_minesAPunIntoTheNextBlock() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "wallet.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		File chainFile = aChainPayingTheWallet(wallet);
		ChainFile chain = new ChainFile(chainFile.toPath());
		Bytes recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
		chain.writePending(List.of(Transfers.prepare(wallet, SignatureSuite.ED25519,
			chain.require(), List.of(), Destination.direct(recipient), Amount.parseLeth("2"),
			Amount.ZERO, "waiting for a pun")));
		File databaseFile = new File(tempHome, "lethenon-mine.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Mine a Pun", "Mine a Pun");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Mine a Pun"));
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			tool.textBox("txtPassword").target().setText(walletPassword);
			tool.textBox("txtPun").target().setText("a pun against the cameras");
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnMine").target().doClick());

		Pause.pause(new Condition("the block is mined")
		{
			@Override
			public boolean test()
			{
				return textOf(tool, "txtAnswer").contains("is yours");
			}
		}, 30000);

		String answer = textOf(tool, "txtAnswer");
		assertTrue(answer.startsWith("Block 3 is yours: it paid you "), answer);
		assertTrue(answer.endsWith(" LETH."), answer);
		assertEquals("3", textOf(tool, "txtBlock"));
		assertEquals("1 transfer", textOf(tool, "txtCarried"));
		assertTrue(textOf(tool, "txtMinedPun").startsWith("a pun against the cameras"),
			textOf(tool, "txtMinedPun"));
		assertFalse(
			robot.finder().findByName(tool.target(), "pnlDetails", JPanel.class, false).isVisible(),
			"what only a specialist reads is closed at first");
		tool.button("btnDetails").click();
		assertEquals(Chain.TEST_IDENTIFIER, textOf(tool, "txtChain"));
		assertEquals(wallet.spendKey(SignatureSuite.ED25519).toString(), textOf(tool, "txtPaidTo"));
		assertTrue(textOf(tool, "txtChainChecked").startsWith("4 blocks"),
			textOf(tool, "txtChainChecked"));
		for (String field : List.of("txtAnswer", "txtBlock", "txtPaid", "txtCarried", "txtMinedPun",
			"txtChain", "txtPaidTo", "txtChainChecked", "txtSupply"))
		{
			assertFalse(textOf(tool, field).contains(walletPassword),
				"the password is in no text on the screen");
		}
		List<BlockBody> blocks = chain.require();
		assertEquals(4, blocks.size(), "the block was written to the chain file");
		assertTrue(blocks.getLast().pun().startsWith("a pun against the cameras"),
			blocks.getLast().pun());
		assertEquals(wallet.spendKey(SignatureSuite.ED25519), blocks.getLast().beneficiary());
		assertEquals(Amount.parseLeth("2"),
			Replay.verify(blocks).finalState().balanceOf(recipient));
		assertTrue(chain.readPending().isEmpty(), "the waiting transfer went into the block");
		String passwordLeft = GuiActionRunner
			.execute(() -> tool.textBox("txtPassword").target().getText());
		assertTrue(passwordLeft.isEmpty(), "the password field is cleared after one use");
		assertTrue(frame.isEnabled(), "the application is still usable after mining");
	}

	@Test
	@DisplayName("the plugin starts a test chain where there is no chain file, and then closes the choice")
	void thePlugin_startsATestChain() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "wallet.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		File chainFile = new File(tempHome, "new-test-chain.lethenon");
		File databaseFile = new File(tempHome, "lethenon-test-chain.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		application.openPluginTool("Mine a Pun", "Mine a Pun");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Mine a Pun"));
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			tool.textBox("txtPassword").target().setText(walletPassword);
		});
		JComboBoxFixture kinds = tool.comboBox("cbxChainKind");
		assertTrue(GuiActionRunner.execute(() -> kinds.target().isEnabled()),
			"where there is no chain file, the window asks which chain to start");
		assertEquals("the test network", kinds.selectedItem(),
			"a new chain is a test chain unless the main chain is chosen");
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnMine").target().doClick());
		awaitText(tool, "txtAnswer", "You started a new chain", "the genesis block is mined");

		assertEquals("0", textOf(tool, "txtBlock"));
		assertEquals("nothing - the first block of a chain pays nobody", textOf(tool, "txtPaid"));
		tool.button("btnDetails").click();
		assertEquals(Chain.TEST_IDENTIFIER, textOf(tool, "txtChain"));
		assertEquals(Genesis.NOBODY.toString(), textOf(tool, "txtPaidTo"));
		List<BlockBody> blocks = new ChainFile(chainFile.toPath()).require();
		assertEquals(1, blocks.size());
		assertEquals(Chain.TEST_IDENTIFIER, blocks.getFirst().chainIdentifier());
		assertEquals(Genesis.NOBODY, blocks.getFirst().beneficiary(),
			"a genesis block pays the burn account (lethenon#148)");
		assertFalse(GuiActionRunner.execute(() -> kinds.target().isEnabled()),
			"once the genesis block is written, it decides which chain this is");
	}

	@Test
	@DisplayName("the mine window offers the main chain as starting with lethenon 1.0.0, and refuses to start it (#535, #544)")
	void thePlugin_refusesToStartTheMainChain() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		File walletFile = new File(tempHome, "wallet.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), Wallet.create(), walletPassword.toCharArray());
		File chainFile = new File(tempHome, "main.lethenon");
		File databaseFile = new File(tempHome, "lethenon-main-chain.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		application.openPluginTool("Mine a Pun", "Mine a Pun");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Mine a Pun"));
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			tool.textBox("txtPassword").target().setText(walletPassword);
		});
		JComboBoxFixture kinds = tool.comboBox("cbxChainKind");
		String[] offered = kinds.contents();
		assertTrue(java.util.Arrays.stream(offered).anyMatch(each -> each.contains("1.0.0")),
			"the main chain is offered as starting with lethenon 1.0.0: "
				+ String.join(" / ", offered));
		UiTestSpeed.step();
		kinds.selectItem(1);
		SwingUtilities.invokeLater(() -> tool.button("btnMine").target().doClick());
		Pause.pause(new Condition("the window reports why nothing was mined")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> tool.label("lblResult").target().getText())
					.startsWith("no block was mined");
			}
		}, 20000);

		String result = GuiActionRunner.execute(() -> tool.label("lblResult").target().getText());
		assertTrue(result.contains("lethenon 1.0.0"), result);
		assertFalse(result.contains(walletPassword), "the password is in no text on the screen");
		assertFalse(chainFile.exists(), "no main chain file is written");
	}

	@Test
	@DisplayName("the plugin shows \"Before you start\" with its notice, from the menu and before the choice of chain (#540)")
	void thePlugin_showsBeforeYouStart() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		File databaseFile = new File(tempHome, "lethenon-before-you-start.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		application.openPluginTool("Before You Start", "Before You Start");
		JInternalFrameFixture view = new JInternalFrameFixture(robot,
			application.internalFrame("Before You Start"));
		JLabel notice = view.label("lblNotLegalAdvice").target();
		assertTrue(GuiActionRunner.execute(notice::isShowing), "the notice is on the screen");
		String noticeText = GuiActionRunner.execute(notice::getText);
		assertTrue(noticeText.startsWith("Not legal advice."), noticeText);
		assertTrue(
			GuiActionRunner.execute(() -> view.table("tblLaunchChecks").target().getRowCount()) > 0,
			"the checklist has rows");
		for (String document : List.of("btnOpenRegulatoryOverview", "btnOpenLaunchChecklist",
			"btnOpenMessagingGuide", "btnOpenSpecification", "btnOpenInfrastructure"))
		{
			view.button(document).requireVisible();
		}
		UiTestSpeed.step();

		application.openPluginTool("Mine a Pun", "Mine a Pun");
		JInternalFrameFixture mine = new JInternalFrameFixture(robot,
			application.internalFrame("Mine a Pun"));
		GuiActionRunner.execute(() -> mine.textBox("txtChainFile").target()
			.setText(new File(tempHome, "not-mined-yet.lethenon").getAbsolutePath()));
		JLabel noticeBeforeTheChoice = mine.label("lblNotLegalAdvice").target();
		assertTrue(GuiActionRunner.execute(noticeBeforeTheChoice::isShowing),
			"a genesis block is about to be mined: the notice stands before the choice of chain");
		GuiActionRunner.execute(
			() -> mine.textBox("txtChainFile").target().setText(databaseFile.getAbsolutePath()));
		robot.waitForIdle();
		assertFalse(GuiActionRunner.execute(noticeBeforeTheChoice::isShowing),
			"an existing file decides which chain it is: no choice, and the view goes with it");
	}

	@Test
	@DisplayName("a published address is paid, the payment is mined, swept and mined again, and the account holds it")
	void thePlugin_paysAPublishedAddressAndTheRecipientSweepsIt() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String payerPassword = TestPasswords.throwaway();
		Wallet payer = Wallet.create();
		File payerFile = new File(tempHome, "payer.lethenon-wallet");
		WalletFile.write(payerFile.toPath(), payer, payerPassword.toCharArray());
		String payeePassword = TestPasswords.throwaway();
		Wallet payee = Wallet.create();
		File payeeFile = new File(tempHome, "payee.lethenon-wallet");
		WalletFile.write(payeeFile.toPath(), payee, payeePassword.toCharArray());
		File chainFile = aChainPayingTheWallet(payer);
		ChainFile chain = new ChainFile(chainFile.toPath());
		String address = payee.address().toText();
		File databaseFile = new File(tempHome, "lethenon-stealth.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		// the payer pays the payee's published address
		application.openPluginTool("Send LETH", "Send LETH");
		JInternalFrameFixture send = new JInternalFrameFixture(robot,
			application.internalFrame("Send LETH"));
		GuiActionRunner.execute(() -> {
			send.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			send.textBox("txtWalletFile").target().setText(payerFile.getAbsolutePath());
			send.textBox("txtPassword").target().setText(payerPassword);
			// chosen by value, not by index: EnumComboBoxModel does not keep the declaration order
			// (measured: PUBLISHED_ADDRESS first in one run, second in another), and the
			// plugin's enum is not on this classpath, so its name is what is compared
			javax.swing.JComboBox<?> kinds = send.comboBox("cbxRecipientKind").target();
			for (int index = 0; index < kinds.getItemCount(); index++)
			{
				if ("PUBLISHED_ADDRESS".equals(String.valueOf(kinds.getItemAt(index))))
				{
					kinds.setSelectedIndex(index);
				}
			}
			send.textBox("txtRecipient").target().setText(address);
			send.textBox("txtAmount").target().setText("2");
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> send.button("btnSend").target().doClick());
		awaitText(send, "txtAnswer", "Sent:", "the payment is signed");
		String sendReport = textOf(send, "txtAnswer") + " " + textOf(send, "txtSentTo");
		Destination oneTime = chain.readPending().getFirst().body().recipient();
		assertTrue(sendReport.contains("to the address you named"), sendReport);
		assertTrue(textOf(send, "txtSentTo").startsWith(address.substring(0, 12)),
			"the window names the address that was typed: " + textOf(send, "txtSentTo"));
		assertFalse(sendReport.contains(oneTime.key().toString()),
			"the sender's screen never shows the destination it derived");

		mineThroughTheWindow(application, chainFile, payerFile, payerPassword, "a pun to carry it");
		assertEquals(Amount.parseLeth("2"),
			Replay.verify(chain.require()).finalState().balanceOf(oneTime.key()),
			"the payment arrived at its one-time destination");

		// the payee sweeps it, and is told what that costs before signing
		application.openPluginTool("Sweep One-Time Payments", "Sweep One-Time Payments");
		JInternalFrameFixture sweep = new JInternalFrameFixture(robot,
			application.internalFrame("Sweep One-Time Payments"));
		String cost = textOf(sweep, "txtCost");
		assertTrue(cost.contains("Receiving is unlinkable; spending is the moment that ends"),
			cost);
		GuiActionRunner.execute(() -> {
			sweep.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			sweep.textBox("txtWalletFile").target().setText(payeeFile.getAbsolutePath());
			sweep.textBox("txtPassword").target().setText(payeePassword);
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> sweep.button("btnSweep").target().doClick());
		awaitReport(sweep, "waiting", "the sweep is signed");
		String sweepReport = textOf(sweep, "txtReport");
		assertTrue(sweepReport.contains("signed 1 transfer sweeping 2.00000000 LETH"), sweepReport);
		assertTrue(sweepReport.contains(cost), "the cost is repeated after signing");
		assertFalse(sweepReport.contains(payeePassword), "the password is in no text");
		List<SignedTransaction> swept = chain.readPending();
		assertEquals(1, swept.size(), "the sweep waits in the chain library's pending file");
		assertEquals(oneTime.key(), swept.getFirst().body().sender());

		mineThroughTheWindow(application, chainFile, payerFile, payerPassword, "a pun to sweep it");
		List<BlockBody> blocks = chain.require();
		assertEquals(5, blocks.size(), "two blocks were mined through the window");
		Bytes account = payee.spendKey(SignatureSuite.ED25519);
		assertEquals(Amount.parseLeth("2"), Replay.verify(blocks).finalState().balanceOf(account),
			"the payee's own account holds what was paid to its address");
		assertEquals(Amount.ZERO, Replay.verify(blocks).finalState().balanceOf(oneTime.key()));
		assertTrue(frame.isEnabled(), "the application is still usable after a sweep");
	}

	@Test
	@DisplayName("the plugin brings a new chain file up to a running node's tip, every block verified")
	void thePlugin_synchronisesAChainFileFromANode() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		List<BlockBody> served = aTestChainOf(3, Bytes.of(new byte[] { 9 }));
		File chainFile = new File(tempHome, "synced.lethenon");
		File databaseFile = new File(tempHome, "lethenon-sync.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		try (Node node = Node.on(served))
		{
			int port = node.listen(0);
			ApplicationSteps application = signInWithExistingDatabase(databaseFile,
				MASTER_PASSWORD);
			FrameFixture frame = application.showMainFrame();

			application.openPluginTool("Synchronise with a Node", "Synchronise with a Node");
			JInternalFrameFixture tool = new JInternalFrameFixture(robot,
				application.internalFrame("Synchronise with a Node"));
			GuiActionRunner.execute(() -> {
				tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
				tool.textBox("txtNode").target().setText("127.0.0.1:" + port);
			});
			UiTestSpeed.step();
			SwingUtilities.invokeLater(() -> tool.button("btnSync").target().doClick());
			awaitReport(tool, "took 3 block(s)", "the chain file is synchronised");

			String report = textOf(tool, "txtReport");
			assertTrue(report.contains("now at height 2"), report);
			assertTrue(report.contains("taken on first use"), report);
			assertEquals(served, new ChainFile(chainFile.toPath()).require(),
				"the file holds the node's chain, read back with the chain library");
			assertTrue(frame.isEnabled(), "the application is still usable after a sync");
		}
	}

	@Test
	@DisplayName("with the node field left empty, a chain of its own is refused with the reason before any seed node is dialled, and the field says what empty means (#573)")
	void thePlugin_withoutANode_refusesAChainOfItsOwn_beforeDialling() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		File chainFile = new File(tempHome, "own.lethenon");
		new ChainFile(chainFile.toPath()).write(aTestChainOf(2, Bytes.of(new byte[] { 9 })));
		byte[] before = Files.readAllBytes(chainFile.toPath());
		File databaseFile = new File(tempHome, "lethenon-sync-seeds.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Synchronise with a Node", "Synchronise with a Node");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Synchronise with a Node"));
		String tooltip = GuiActionRunner
			.execute(() -> tool.textBox("txtNode").target().getToolTipText());
		assertTrue(tooltip.contains("seed nodes"), tooltip);
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtNode").target().setText("");
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnSync").target().doClick());
		Pause.pause(new Condition("the window reports why nothing was synchronised")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> tool.label("lblResult").target().getText())
					.contains("seed nodes serve another chain");
			}
		}, 20000);

		String result = GuiActionRunner.execute(() -> tool.label("lblResult").target().getText());
		assertTrue(result.contains("no node was named"), result);
		assertTrue(result.contains(chainFile.getAbsolutePath()), result);
		assertArrayEquals(before, Files.readAllBytes(chainFile.toPath()),
			"the chain file is as it was");
		assertTrue(frame.isEnabled(), "the application is still usable after a refusal");
	}

	@Test
	@DisplayName("the plugin hands a signed transfer to a running node that serves the chain file")
	void thePlugin_handsATransferToANode() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "sender.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		File chainFile = new File(tempHome, "served.lethenon");
		ChainFile chain = new ChainFile(chainFile.toPath());
		chain.write(aTestChainOf(2, wallet.spendKey(SignatureSuite.ED25519)));
		Bytes recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
		File databaseFile = new File(tempHome, "lethenon-handover.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		try (Node node = Node.serving(chain))
		{
			int port = node.listen(0);
			ApplicationSteps application = signInWithExistingDatabase(databaseFile,
				MASTER_PASSWORD);
			FrameFixture frame = application.showMainFrame();

			application.openPluginTool("Send LETH", "Send LETH");
			JInternalFrameFixture send = new JInternalFrameFixture(robot,
				application.internalFrame("Send LETH"));
			GuiActionRunner.execute(() -> {
				send.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
				send.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
				send.textBox("txtPassword").target().setText(walletPassword);
				send.textBox("txtRecipient").target().setText(recipient.toString());
				send.textBox("txtAmount").target().setText("4");
				send.textBox("txtNode").target().setText("127.0.0.1:" + port);
			});
			UiTestSpeed.step();
			SwingUtilities.invokeLater(() -> send.button("btnSend").target().doClick());
			awaitText(send, "txtAnswer", "The node at 127.0.0.1:" + port,
				"the transfer is handed to the node");
			assertEquals("in the pool of the node at 127.0.0.1:" + port,
				textOf(send, "txtWhereItWaits"));

			List<SignedTransaction> pool = node.pending();
			assertEquals(1, pool.size(), "the node admitted the transfer");
			assertEquals(Destination.direct(recipient), pool.getFirst().body().recipient());
			assertEquals(Amount.parseLeth("4"), pool.getFirst().body().amount());
			assertEquals(pool, chain.readPending(),
				"the node keeps its pool next to the chain file it serves");
			assertTrue(frame.isEnabled(), "the application is still usable after a handover");
		}
	}

	@Test
	@DisplayName("the plugin runs a mining node that serves its chain file, and stops it")
	void thePlugin_runsAMiningNode() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		String walletPassword = TestPasswords.throwaway();
		Wallet wallet = Wallet.create();
		File walletFile = new File(tempHome, "miner.lethenon-wallet");
		WalletFile.write(walletFile.toPath(), wallet, walletPassword.toCharArray());
		List<BlockBody> started = aTestChainOf(3, Bytes.of(new byte[] { 9 }));
		File chainFile = new File(tempHome, "node.lethenon");
		ChainFile chain = new ChainFile(chainFile.toPath());
		chain.write(started);
		File databaseFile = new File(tempHome, "lethenon-node.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.openPluginTool("Run a Node", "Run a Node");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Run a Node"));
		GuiActionRunner.execute(() -> {
			tool.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			tool.textBox("txtPort").target().setText("0");
			tool.checkBox("chkMine").target().doClick();
			tool.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			tool.textBox("txtPassword").target().setText(walletPassword);
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> tool.button("btnStartNode").target().doClick());
		awaitStatus(tool, "mined [1-9]", "the node runs and has mined a block");

		Matcher listening = Pattern.compile("listening on port (\\d+)")
			.matcher(textOf(tool, "txtStatus"));
		assertTrue(listening.find(), textOf(tool, "txtStatus"));
		ChainFile copy = new ChainFile(new File(tempHome, "copy.lethenon").toPath());
		Sync.once(copy, new PeerAddress("127.0.0.1", Integer.parseInt(listening.group(1))),
			Duration.ofSeconds(30));
		assertEquals(started, copy.require().subList(0, started.size()),
			"another node takes the chain from the window's node, the mined blocks after it");
		assertTrue(textOf(tool, "txtPassword").isEmpty(), "the password field is cleared");

		SwingUtilities.invokeLater(() -> tool.button("btnStopNode").target().doClick());
		awaitStatus(tool, "stopped at height", "the node has stopped");

		List<BlockBody> written = chain.require();
		assertTrue(written.size() > started.size(), "the node wrote what it mined");
		assertEquals(wallet.spendKey(SignatureSuite.ED25519), written.getLast().beneficiary(),
			"the mined blocks pay the wallet");
		Replay.verify(written);
		assertTrue(frame.isEnabled(), "the application is still usable after a node ran");
	}

	/**
	 * Waits until the node window's status matches, and when it never does, fails with the line the
	 * window wrote instead
	 */
	private static void awaitStatus(final JInternalFrameFixture tool, final String expected,
		final String description)
	{
		Pattern pattern = Pattern.compile(expected);
		try
		{
			Pause.pause(new Condition(description)
			{
				@Override
				public boolean test()
				{
					return pattern.matcher(textOf(tool, "txtStatus")).find();
				}
			}, 30000);
		}
		catch (org.assertj.swing.exception.WaitTimedOutError timedOut)
		{
			throw new AssertionError("timed out waiting until " + description
				+ "; the window said: '"
				+ GuiActionRunner.execute(() -> tool.label("lblResult").target().getText()) + "'",
				timedOut);
		}
	}

	/**
	 * Mines the next block through the "Mine a Pun" window, which is opened when it is not open yet
	 */
	private void mineThroughTheWindow(final ApplicationSteps application, final File chainFile,
		final File walletFile, final String walletPassword, final String pun)
	{
		if (application.internalFrame("Mine a Pun") == null)
		{
			application.openPluginTool("Mine a Pun", "Mine a Pun");
		}
		JInternalFrameFixture mine = new JInternalFrameFixture(robot,
			application.internalFrame("Mine a Pun"));
		GuiActionRunner.execute(() -> {
			mine.textBox("txtAnswer").target().setText("");
			mine.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			mine.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			mine.textBox("txtPassword").target().setText(walletPassword);
			mine.textBox("txtPun").target().setText(pun);
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> mine.button("btnMine").target().doClick());
		awaitText(mine, "txtAnswer", "is yours", "the block is mined");
	}

	/**
	 * Waits for the report to say what was expected, and when it never does, fails with the line
	 * the window wrote instead - a timeout alone says that something went wrong, not what
	 */
	private static void awaitReport(final JInternalFrameFixture tool, final String expected,
		final String description)
	{
		awaitText(tool, "txtReport", expected, description);
	}

	/**
	 * Waits for a field to say what was expected, and when it never does, fails with the line the
	 * window wrote instead
	 */
	private static void awaitText(final JInternalFrameFixture tool, final String field,
		final String expected, final String description)
	{
		try
		{
			Pause.pause(new Condition(description)
			{
				@Override
				public boolean test()
				{
					return textOf(tool, field).contains(expected);
				}
			}, 30000);
		}
		catch (org.assertj.swing.exception.WaitTimedOutError timedOut)
		{
			throw new AssertionError("timed out waiting until " + description
				+ "; the window said: '"
				+ GuiActionRunner.execute(() -> tool.label("lblResult").target().getText()) + "'",
				timedOut);
		}
	}

	private static String textOf(final JInternalFrameFixture tool, final String componentName)
	{
		return GuiActionRunner.execute(() -> tool.textBox(componentName).target().getText());
	}

	/**
	 * A test chain in the shape lethenon 0.4.0 accepts: a genesis block for the burn account
	 * (lethenon#148), a block paying the holder its reward, and a third with one transfer out of
	 * it, written to the test's own home directory as lethenon 0.6.0 keeps a chain, in a database;
	 * a chain file of the old format is LethenonChainFileOf88UiTest's (#581)
	 */
	private File aChainWithOneTransfer() throws Exception
	{
		KeyPair holder = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		Bytes holderKey = TransactionSigner.asBytes(holder.getPublic());
		List<BlockBody> chain = new ArrayList<>(List.of(Genesis.candidate(Chain.TEST_IDENTIFIER,
			"in the beginning was the pun", 1_759_000_000_000L)));
		chain.add(Blocks.mine(
			Mining.nextBlock(chain, holderKey, List.of(), "the holder's block", 1_759_000_120_000L),
			1_000_000L).orElseThrow());
		SignedTransaction transfer = TransactionSigner
			.sign(new TransactionBody(Chain.TEST_IDENTIFIER, 0L, holderKey,
				Destination.direct(Bytes.of(new byte[] { 7 })), Amount.ofLeth(3L), Amount.ZERO,
				"a protest in three lethe"), SignatureSuite.ED25519, holder.getPrivate());
		chain
			.add(
				Blocks
					.mine(Mining.nextBlock(chain, holderKey, List.of(transfer),
						"the block with the transfer", 1_759_000_240_000L), 1_000_000L)
					.orElseThrow());
		File chainFile = new File(tempHome, "chain.lethenon");
		new ChainFile(chainFile.toPath()).write(chain);
		return chainFile;
	}

	/**
	 * A test chain mined the way a node mines it, every block from the chain library's own
	 * {@link Mining#nextBlock}, one target block time after the one before, so that it keeps the
	 * minimum difficulty; every block pays the given miner
	 */
	private static List<BlockBody> aTestChainOf(final int blocks, final Bytes miner)
	{
		long start = 1_759_000_000_000L;
		List<BlockBody> chain = new ArrayList<>();
		chain.add(Blocks.mine(Mining.nextBlock(Chain.TEST_IDENTIFIER, List.of(), miner, List.of(),
			"in the beginning was the pun", start), 1_000_000L).orElseThrow());
		while (chain.size() < blocks)
		{
			chain.add(Blocks
				.mine(Mining.nextBlock(chain, miner, List.of(), "pun " + chain.size(),
					start + DifficultyRule.TARGET_BLOCK_MILLIS * chain.size()), 1_000_000L)
				.orElseThrow());
		}
		return List.copyOf(chain);
	}

	/**
	 * A test chain in the shape lethenon 0.4.0 accepts, on which a payer mines block 1 and then
	 * pays the wallet 3 LETH to its Ed25519 account and 5 LETH to a one-time destination of its
	 * published address in block 2, written as lethenon 0.6.0 keeps a chain, in a database
	 */
	private File aChainPayingTheWallet(final Wallet wallet) throws Exception
	{
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		Bytes payerKey = TransactionSigner.asBytes(payer.getPublic());
		List<BlockBody> chain = new ArrayList<>(List.of(Genesis.candidate(Chain.TEST_IDENTIFIER,
			"in the beginning was the pun", 1_759_000_000_000L)));
		chain.add(Blocks.mine(
			Mining.nextBlock(chain, payerKey, List.of(), "the payer's block", 1_759_000_120_000L),
			1_000_000L).orElseThrow());
		SignedTransaction direct = TransactionSigner
			.sign(
				new TransactionBody(Chain.TEST_IDENTIFIER, 0L, payerKey,
					Destination.direct(wallet.spendKey(SignatureSuite.ED25519)), Amount.ofLeth(3L),
					Amount.ZERO, "three, to the account"),
				SignatureSuite.ED25519, payer.getPrivate());
		SignedTransaction oneTime = TransactionSigner
			.sign(new TransactionBody(Chain.TEST_IDENTIFIER, 1L, payerKey,
				OneTimeAddresses.destinationFor(wallet.address(),
					OneTimeAddresses.newEphemeralKeyPair()),
				Amount.ofLeth(5L), Amount.ZERO, "five, to a one-time destination"),
				SignatureSuite.ED25519, payer.getPrivate());
		chain.add(Blocks.mine(Mining.nextBlock(chain, payerKey, List.of(direct, oneTime),
			"the second pun", 1_759_000_240_000L), 1_000_000L).orElseThrow());
		File chainFile = new File(tempHome, "paid.lethenon");
		new ChainFile(chainFile.toPath()).write(chain);
		return chainFile;
	}
}
