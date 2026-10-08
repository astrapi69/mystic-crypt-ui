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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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
			LethenonSyncPanel panel = new LethenonSyncPanel();
			panel.getModelObject().setChainFile(chainFile.getAbsolutePath());
			panel.getModelObject().setNode("127.0.0.1:" + node.listen(0));

			panel.onSync();
			awaitTheEnd(panel);

			String report = panel.getModelObject().getReport();
			assertTrue(report.contains("chain " + Chain.TEST_IDENTIFIER), report);
			assertTrue(report.contains("took 3 block(s), now at height 2"), report);
			assertTrue(report.contains("taken on first use"),
				"a new file trusts the node's genesis block, and the window says so: " + report);
			assertEquals("the chain file is at the tip of the node now",
				panel.getModelObject().getResultText());
			assertEquals(served, new ChainFile(chainFile.toPath()).require());
		}
	}

	@Test
	@DisplayName("a refused sync leaves no report behind, only the reason, and can be started again")
	void syncing_withoutANode_reportsTheReasonAndNothingElse() throws Exception
	{
		LethenonSyncPanel panel = new LethenonSyncPanel();
		panel.getModelObject()
			.setChainFile(new File(chainDirectory, "never.lethenon").getAbsolutePath());
		panel.getModelObject().setNode(" ");

		panel.onSync();
		awaitTheEnd(panel);

		assertEquals("", panel.getModelObject().getReport());
		assertTrue(panel.getModelObject().getResultText()
			.startsWith("the chain file was not synchronised: no node was named"),
			panel.getModelObject().getResultText());
		assertFalse(new File(chainDirectory, "never.lethenon").exists());
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
