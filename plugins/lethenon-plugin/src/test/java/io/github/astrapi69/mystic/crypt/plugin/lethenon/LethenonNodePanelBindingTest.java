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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

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
import io.github.astrapi69.lethenon.transport.PeerAddress;
import io.github.astrapi69.lethenon.transport.Sync;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The node window's state lives in its model, so what it holds is readable without a robot: whether
 * the node runs, the port it listens on, and the status it shows every second. Starting and
 * stopping run off the event dispatch thread, and the model says when they have ended.
 */
class LethenonNodePanelBindingTest
{

	@TempDir
	File configurationDirectory;

	@TempDir
	Path directory;

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
	@DisplayName("the node window starts with the configured chain file, the default port and no mining")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonNodePanel panel = new LethenonNodePanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
		assertEquals("18480", panel.getModelObject().getPort());
		assertFalse(panel.getModelObject().isMine());
		assertFalse(panel.getModelObject().isRunning());
	}

	@Test
	@DisplayName("a started node shows its status and serves the chain; a stopped one says so")
	void starting_andStopping_showTheNode() throws Exception
	{
		List<BlockBody> chain = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3,
			Bytes.of(new byte[] { 9 }));
		Path chainFile = directory.resolve("served.lethenon");
		new ChainFile(chainFile).write(chain);
		LethenonNodePanel panel = new LethenonNodePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setPort("0");

		panel.onStart();
		await("the node runs", () -> panel.getModelObject().isRunning());
		int port = panel.getModelObject().getListeningPort();
		await("the answer is shown", () -> textOf(panel, "txtAnswer").startsWith("Your node runs"));

		assertEquals("Your node runs on port " + port + ": its chain ends at block 2.",
			textOf(panel, "txtAnswer"));
		assertEquals(String.valueOf(port), textOf(panel, "txtListening"));
		assertEquals("2", textOf(panel, "txtLastBlock"));
		assertEquals("none", textOf(panel, "txtConnected"));
		assertEquals("none", textOf(panel, "txtWaiting"));
		assertEquals("not mining", textOf(panel, "txtMined"));
		assertEquals("none", textOf(panel, "txtRefusals"));
		Path copy = directory.resolve("copy.lethenon");
		Sync.once(new ChainFile(copy), new PeerAddress("127.0.0.1", port), Duration.ofSeconds(20));
		assertEquals(chain, new ChainFile(copy).require(), "the window's node serves the chain");

		panel.onStop();
		await("the node has stopped", () -> !panel.getModelObject().isRunning()
			&& panel.getModelObject().getResultText().startsWith("the node was stopped")
			&& textOf(panel, "txtAnswer").startsWith("Your node has stopped"));
		assertEquals("Your node has stopped: its chain ends at block 2.",
			textOf(panel, "txtAnswer"));
	}

	@Test
	@DisplayName("why connections were refused is behind \"Show details\", closed at first, and every field, button and choice has a tooltip (#604)")
	void theDetails_areClosedAtFirst_andEveryFieldExplainsItself()
	{
		LethenonNodePanel panel = new LethenonNodePanel();
		JPanel details = component(panel, "pnlDetails", JPanel.class);

		assertFalse(details.isVisible(), "the details are closed at first");
		assertEquals(List.of(), Windows.withoutATooltip(panel),
			"fields, buttons and choices without a tooltip");

		panel.onDetails();

		assertTrue(details.isVisible());
		assertEquals("Hide details", component(panel, "btnDetails", JButton.class).getText());
	}

	@Test
	@DisplayName("the answer of a mining node counts the blocks it found, in words (#604)")
	void theAnswer_ofAMiningNode_countsTheBlocksFound()
	{
		NodeStatus status = new NodeStatus(18480, 1200, 2, 1, 3, List.of());

		assertEquals("Your node runs on port 18480: its chain ends at block 1,200. It has found 3 "
			+ "blocks for you so far.", LethenonNodePanel.answerFor(status, true, true, Locale.ENGLISH));
		assertEquals("Your node has stopped: its chain ends at block 1,200. It found 3 blocks for "
			+ "you.", LethenonNodePanel.answerFor(status, true, false, Locale.ENGLISH));
	}

	@Test
	@DisplayName("a node that cannot start gives the reason, and the password is gone from the model")
	void starting_withoutAChain_reportsTheReason() throws Exception
	{
		LethenonNodePanel panel = new LethenonNodePanel();
		panel.getModelObject()
			.setChainFile(directory.resolve("not-there.lethenon").toString());
		panel.getModelObject().setPassword("not used".toCharArray());

		panel.onStart();
		await("the start has ended", () -> !panel.getModelObject().isStarting());

		assertFalse(panel.getModelObject().isRunning());
		assertTrue(panel.getModelObject().getResultText()
			.startsWith("the node was not started: there is no chain file at"),
			panel.getModelObject().getResultText());
		assertNull(panel.getModelObject().getPassword());
	}

	private static void await(final String what, final BooleanSupplier condition)
		throws InterruptedException
	{
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (!condition.getAsBoolean())
		{
			if (System.nanoTime() > deadline)
			{
				throw new AssertionError("waited twenty seconds for this, in vain: " + what);
			}
			Thread.sleep(20L);
		}
	}
}
