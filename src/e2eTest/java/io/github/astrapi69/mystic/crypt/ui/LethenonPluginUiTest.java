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
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Milestone 5 of lethenon#2, the parts of it that are built: the plugin installs from its zip, its
 * submenu appears, the replay verifier behind its button reports what it verified in a chain file
 * written by the chain library itself, and the chain view lists that file's blocks.
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
}
