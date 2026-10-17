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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.component;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The panel's state lives in its model, so what it holds is readable without a robot - including
 * whether a sync is still running, since it runs off the event dispatch thread.
 */
class LethenonSyncPanelBindingTest
{

	@TempDir
	File configurationDirectory;

	@TempDir
	File chainDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void releaseTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the configured chain file is what the panel starts with, in the model too")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonSyncPanel panel = new LethenonSyncPanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
		assertFalse(panel.getModelObject().isSynchronising());
	}

	@Test
	@DisplayName("a sync into a new file reports what it took, and that the genesis block is the node's")
	void syncing_intoANewFile_reportsWhatItTook() throws Exception
	{
		List<BlockBody> served = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3,
			Bytes.of(new byte[] { 9 }));
		File chainFile = new File(chainDirectory, "synced.lethenon");
		try (Node node = Node.on(served))
		{
			String address = "127.0.0.1:" + node.listen(0);
			LethenonSyncPanel panel = new LethenonSyncPanel();
			panel.getModelObject().setChainFile(chainFile.getAbsolutePath());
			panel.getModelObject().setNode(address);

			panel.onSync();
			awaitTheEnd(panel);

			assertEquals("Your chain file is up to date now: it took 3 new blocks and ends at "
				+ "block 2.", textOf(panel, "txtAnswer"));
			assertEquals("3 blocks", textOf(panel, "txtTaken"));
			assertEquals("2", textOf(panel, "txtLastBlock"));
			assertEquals(address, textOf(panel, "txtFrom"));
			assertEquals(Chain.TEST_IDENTIFIER, textOf(panel, "txtChain"));
			assertTrue(textOf(panel, "txtFirstUse").startsWith("yes"),
				"a new file trusts the node's first block, and the window says so: "
					+ textOf(panel, "txtFirstUse"));
			assertEquals("the chain file is at the tip of the node now",
				panel.getModelObject().getResultText());
			assertEquals(served, new ChainFile(chainFile.toPath()).require());

			panel.onSync();
			awaitTheEnd(panel);

			assertEquals("Your chain file was already up to date: it ends at block 2.",
				textOf(panel, "txtAnswer"));
			assertEquals("none", textOf(panel, "txtTaken"));
			assertEquals("no", textOf(panel, "txtFirstUse"));
		}
	}

	@Test
	@DisplayName("what only a specialist reads - the chain and where its first block came from - is behind \"Show details\", closed at first, and every field and button has a tooltip (#604)")
	void theDetails_areClosedAtFirst_andEveryFieldExplainsItself() throws Exception
	{
		LethenonSyncPanel panel = new LethenonSyncPanel();
		JPanel details = component(panel, "pnlDetails", JPanel.class);

		assertFalse(details.isVisible(), "the details are closed at first");
		assertEquals(List.of(), Windows.withoutATooltip(panel),
			"fields and buttons without a tooltip");

		panel.onDetails();

		assertTrue(details.isVisible());
		assertEquals("Hide details", component(panel, "btnDetails", JButton.class).getText());
	}

	@Test
	@DisplayName("a refused sync leaves no report behind, only the reason, and can be started again")
	void syncing_withoutANode_onAChainOfItsOwn_reportsTheReasonAndNothingElse() throws Exception
	{
		// a chain of its own: with no node named, the seed nodes are not asked for it (#573), so
		// the refusal comes before anything is sent - an empty file would reach the real seed node
		File chainFile = new File(chainDirectory, "own.lethenon");
		new ChainFile(chainFile.toPath()).write(
			LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, Bytes.of(new byte[] { 9 })));
		byte[] before = Files.readAllBytes(chainFile.toPath());
		LethenonSyncPanel panel = new LethenonSyncPanel();
		panel.getModelObject().setChainFile(chainFile.getAbsolutePath());
		panel.getModelObject().setNode(" ");

		panel.onSync();
		awaitTheEnd(panel);

		assertEquals("", textOf(panel, "txtAnswer"));
		assertTrue(panel.getModelObject().getResultText()
			.startsWith("the chain file was not synchronised: " + chainFile.getAbsolutePath()
				+ ": no node was named"),
			panel.getModelObject().getResultText());
		assertArrayEquals(before, Files.readAllBytes(chainFile.toPath()));
	}

	/**
	 * Waits until the sync the panel started has ended, at most twenty seconds
	 */
	private static void awaitTheEnd(final LethenonSyncPanel panel) throws InterruptedException
	{
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (panel.getModelObject().isSynchronising())
		{
			if (System.nanoTime() > deadline)
			{
				throw new AssertionError("the sync did not end within twenty seconds; the window "
					+ "said: " + panel.getModelObject().getResultText());
			}
			Thread.sleep(20L);
		}
	}
}
