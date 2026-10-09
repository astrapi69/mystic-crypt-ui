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
 * The panel's state lives in its model, so what it holds is readable without a robot: the field
 * writes through to the model, the configured chain file arrives in both, and a verification puts
 * its outcome where a test can read it.
 */
class LethenonChainPanelBindingTest
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

		LethenonChainPanel panel = new LethenonChainPanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile(),
			"the field was filled from the settings, and the model knows it - reading the widget "
				+ "would be the pattern this replaces");
	}

	@Test
	@DisplayName("verifying an accepted chain puts the chain's own summary in the model")
	void verifying_anAcceptedChain_fillsTheReport() throws Exception
	{
		LethenonChainPanel panel = new LethenonChainPanel();
		panel.getModelObject().setChainFile(aGenesisOnlyChain().toString());

		panel.onVerify();

		assertTrue(panel.getModelObject().getReport().contains("replayed 1 blocks"),
			panel.getModelObject().getReport());
		assertEquals("the chain was accepted", panel.getModelObject().getResultText());
	}

	@Test
	@DisplayName("a refused chain leaves no report behind, only the reason")
	void verifying_aMissingChain_reportsTheReasonAndNothingElse()
	{
		LethenonChainPanel panel = new LethenonChainPanel();
		panel.getModelObject()
			.setChainFile(new File(chainDirectory, "not-written-yet.lethenon").getAbsolutePath());

		panel.onVerify();

		assertEquals("", panel.getModelObject().getReport(),
			"a chain that was not verified has no counts to show");
		assertTrue(panel.getModelObject().getResultText().startsWith("the chain was refused"),
			panel.getModelObject().getResultText());
		assertTrue(panel.getModelObject().getResultText().contains("not-written-yet.lethenon"),
			"the message names the file, so a report about it needs no follow-up question: "
				+ panel.getModelObject().getResultText());
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
