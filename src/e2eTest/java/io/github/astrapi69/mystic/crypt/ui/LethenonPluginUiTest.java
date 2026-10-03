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
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JInternalFrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.OneTimeAddresses;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Transfers;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Milestone 5 of lethenon#2, the parts of it that are built: the plugin installs from its zip, its
 * submenu appears, the replay verifier behind its button reports what it verified in a chain file
 * written by the chain library itself, the chain view lists that file's blocks, the balance window
 * reads a wallet, the send window signs a transfer that the chain library reads back, and the
 * mining window writes the next block, which the chain library reads back and replays.
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
				return textOf(tool, "txtReport").contains("replayed 2 blocks");
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
					.execute(() -> tool.table("tblBlocks").target().getRowCount()) == 2;
			}
		}, 20000);

		String genesisPun = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(0, 1)));
		String secondPun = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(1, 1)));
		String transfers = GuiActionRunner
			.execute(() -> String.valueOf(tool.table("tblBlocks").target().getValueAt(1, 3)));
		assertTrue(genesisPun.startsWith("in the beginning was the pun"), genesisPun);
		assertTrue(secondPun.startsWith("the second pun"), secondPun);
		assertTrue("1".equals(transfers),
			"the second block carries the one transfer: " + transfers);
		String result = GuiActionRunner.execute(() -> tool.label("lblResult").target().getText());
		assertTrue(result.contains("accepted: 2 blocks"), result);
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
				return textOf(tool, "txtReport").contains("LETH, spendable");
			}
		}, 30000);

		String report = textOf(tool, "txtReport");
		assertTrue(report.contains("3.00000000 LETH, spendable"), report);
		assertTrue(report.contains("1 one-time payments holding 5.00000000 LETH"), report);
		assertTrue(report.contains("spendable after a sweep"), report);
		assertTrue(report.contains("address (publish this): " + wallet.address().toText()), report);
		assertFalse(report.contains(walletPassword), "the password is in no text on the screen");
		String passwordLeft = GuiActionRunner
			.execute(() -> tool.textBox("txtPassword").target().getText());
		assertTrue(passwordLeft.isEmpty(), "the password field is cleared after one use");
		assertTrue(frame.isEnabled(), "the application is still usable after a balance");
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
				return textOf(tool, "txtReport").contains("waiting");
			}
		}, 30000);

		String report = textOf(tool, "txtReport");
		assertTrue(report.contains("1.25000000 LETH"), report);
		assertTrue(report.contains("with nonce 0"), report);
		assertFalse(report.contains(walletPassword), "the password is in no text on the screen");
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
				return textOf(tool, "txtReport").contains("mined block");
			}
		}, 30000);

		String report = textOf(tool, "txtReport");
		assertTrue(report.contains("mined block 2 with 1 transfer(s)"), report);
		assertFalse(report.contains(walletPassword), "the password is in no text on the screen");
		List<BlockBody> blocks = chain.require();
		assertEquals(3, blocks.size(), "the block was written to the chain file");
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
		awaitReport(send, "waiting", "the payment is signed");
		String sendReport = textOf(send, "txtReport");
		Destination oneTime = chain.readPending().getFirst().body().recipient();
		assertTrue(sendReport.contains("a one-time destination of " + address), sendReport);
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
		assertEquals(4, blocks.size(), "two blocks were mined through the window");
		Bytes account = payee.spendKey(SignatureSuite.ED25519);
		assertEquals(Amount.parseLeth("2"), Replay.verify(blocks).finalState().balanceOf(account),
			"the payee's own account holds what was paid to its address");
		assertEquals(Amount.ZERO, Replay.verify(blocks).finalState().balanceOf(oneTime.key()));
		assertTrue(frame.isEnabled(), "the application is still usable after a sweep");
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
			mine.textBox("txtReport").target().setText("");
			mine.textBox("txtChainFile").target().setText(chainFile.getAbsolutePath());
			mine.textBox("txtWalletFile").target().setText(walletFile.getAbsolutePath());
			mine.textBox("txtPassword").target().setText(walletPassword);
			mine.textBox("txtPun").target().setText(pun);
		});
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> mine.button("btnMine").target().doClick());
		awaitReport(mine, "mined block", "the block is mined");
	}

	/**
	 * Waits for the report to say what was expected, and when it never does, fails with the line
	 * the window wrote instead - a timeout alone says that something went wrong, not what
	 */
	private static void awaitReport(final JInternalFrameFixture tool, final String expected,
		final String description)
	{
		try
		{
			Pause.pause(new Condition(description)
			{
				@Override
				public boolean test()
				{
					return textOf(tool, "txtReport").contains(expected);
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
	 * A genesis block that allocates the supply, and a second block with one transfer out of it,
	 * written to the test's own home directory the way a lethenon command line would write it
	 */
	private File aChainWithOneTransfer() throws Exception
	{
		KeyPair holder = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		Bytes holderKey = TransactionSigner.asBytes(holder.getPublic());
		BlockBody genesis = Blocks.mine(
			new BlockBody(Chain.IDENTIFIER, 0L, Bytes.of(new byte[32]), holderKey,
				new ArrayList<>(), 1_759_000_000_000L, 8, "in the beginning was the pun"),
			1_000_000L).orElseThrow();
		SignedTransaction transfer = TransactionSigner.sign(new TransactionBody(Chain.IDENTIFIER,
			0L, holderKey, Destination.direct(Bytes.of(new byte[] { 7 })), Amount.ofLeth(3L),
			Amount.ZERO, "a protest in three lethe"), SignatureSuite.ED25519, holder.getPrivate());
		BlockBody second = Blocks
			.mine(new BlockBody(Chain.IDENTIFIER, 1L, Blocks.hashOf(genesis), holderKey,
				List.of(transfer), 1_759_000_120_000L, 8, "the second pun"), 1_000_000L)
			.orElseThrow();
		File chainFile = new File(tempHome, "chain.lethenon");
		Files.write(chainFile.toPath(), CanonicalEncoding.encodeChain(List.of(genesis, second)));
		return chainFile;
	}

	/**
	 * A genesis holder who pays the wallet 3 LETH to its Ed25519 account and 5 LETH to a one-time
	 * destination of its published address, written the way a lethenon command line would write it
	 */
	private File aChainPayingTheWallet(final Wallet wallet) throws Exception
	{
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		Bytes payerKey = TransactionSigner.asBytes(payer.getPublic());
		BlockBody genesis = Blocks
			.mine(
				new BlockBody(Chain.IDENTIFIER, 0L, Bytes.of(new byte[32]), payerKey,
					new ArrayList<>(), 1_759_000_000_000L, 8, "in the beginning was the pun"),
				1_000_000L)
			.orElseThrow();
		SignedTransaction direct = TransactionSigner
			.sign(
				new TransactionBody(Chain.IDENTIFIER, 0L, payerKey,
					Destination.direct(wallet.spendKey(SignatureSuite.ED25519)), Amount.ofLeth(3L),
					Amount.ZERO, "three, to the account"),
				SignatureSuite.ED25519, payer.getPrivate());
		SignedTransaction oneTime = TransactionSigner
			.sign(new TransactionBody(Chain.IDENTIFIER, 1L, payerKey,
				OneTimeAddresses.destinationFor(wallet.address(),
					OneTimeAddresses.newEphemeralKeyPair()),
				Amount.ofLeth(5L), Amount.ZERO, "five, to a one-time destination"),
				SignatureSuite.ED25519, payer.getPrivate());
		BlockBody second = Blocks
			.mine(new BlockBody(Chain.IDENTIFIER, 1L, Blocks.hashOf(genesis), payerKey,
				List.of(direct, oneTime), 1_759_000_120_000L, 8, "the second pun"), 1_000_000L)
			.orElseThrow();
		File chainFile = new File(tempHome, "paid.lethenon");
		Files.write(chainFile.toPath(), CanonicalEncoding.encodeChain(List.of(genesis, second)));
		return chainFile;
	}
}
