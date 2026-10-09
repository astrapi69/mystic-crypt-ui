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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The chain view keeps its state in its model: the rows it shows are the rows the replay produced,
 * the table holds exactly those, and a refused chain leaves no row behind.
 */
class LethenonChainViewPanelBindingTest
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
	@DisplayName("the chain view starts with the configured chain file")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonChainViewPanel panel = new LethenonChainViewPanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
	}

	@Test
	@DisplayName("an accepted chain fills the model and the table with one row per block")
	void showing_anAcceptedChain_fillsTheRows() throws Exception
	{
		LethenonChainViewPanel panel = new LethenonChainViewPanel();
		panel.getModelObject().setChainFile(aGenesisOnlyChain().toString());

		panel.onShow();

		assertEquals(1, panel.getModelObject().getRows().size());
		assertTrue(
			panel.getModelObject().getRows().getFirst().pun().startsWith("only the genesis"));
		assertEquals(panel.getModelObject().getRows(), panel.getTableModel().getData(),
			"the table shows exactly what the model holds");
		assertEquals("the chain was accepted: 1 blocks", panel.getModelObject().getResultText());
	}

	@Test
	@DisplayName("a refused chain empties the table and says why")
	void showing_aMissingChain_leavesNoRows() throws Exception
	{
		LethenonChainViewPanel panel = new LethenonChainViewPanel();
		panel.getModelObject().setChainFile(aGenesisOnlyChain().toString());
		panel.onShow();
		panel.getModelObject()
			.setChainFile(new File(chainDirectory, "not-written-yet.lethenon").getAbsolutePath());

		panel.onShow();

		assertEquals(List.of(), panel.getModelObject().getRows(),
			"the rows of the chain shown before must not stay next to a refusal");
		assertEquals(0, panel.getTableModel().getRowCount());
		assertTrue(panel.getModelObject().getResultText().startsWith("the chain was refused"),
			panel.getModelObject().getResultText());
		assertTrue(panel.getModelObject().getResultText().contains("not-written-yet.lethenon"),
			panel.getModelObject().getResultText());
	}

	private Path aGenesisOnlyChain() throws Exception
	{
		BlockBody genesis = Genesis.candidate(Chain.TEST_IDENTIFIER, "only the genesis",
			1_759_000_000_000L);
		Path chainFile = new File(chainDirectory, "chain.lethenon").toPath();
		Files.write(chainFile, CanonicalEncoding.encodeChain(List.of(genesis)));
		return chainFile;
	}
}
