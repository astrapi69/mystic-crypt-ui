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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The mining window keeps its state in its model, mines through {@link MiningSupport}, and uses the
 * wallet's password once: it is dropped after the block was mined or refused, and it reaches no
 * text the window shows.
 */
class LethenonMinePanelBindingTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File configurationDirectory;

	@TempDir
	File directory;

	private Path chainFile;

	private Path walletFile;

	@BeforeEach
	void writeAChainAndAWallet() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		Wallet wallet = Wallet.create();
		walletFile = new File(directory, "wallet.lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = new File(directory, "chain.lethenon").toPath();
		Files.write(chainFile,
			CanonicalEncoding.encodeChain(LethenonFixtures.aChainPayingTheWallet(payer, wallet)));
	}

	@AfterEach
	void releaseTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the mining window starts with the configured chain file and lethenon's default pun")
	void thePanel_startsWithTheConfiguredChainFile() throws Exception
	{
		PluginSettings.save(configurationDirectory, LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/configured.lethenon"));

		LethenonMinePanel panel = new LethenonMinePanel();

		assertEquals("/tmp/configured.lethenon", panel.getModelObject().getChainFile());
		assertEquals("watching is not protecting", panel.getModelObject().getPun());
	}

	@Test
	@DisplayName("mining writes the next block to the chain file and reports its pun")
	void mining_writesTheNextBlock() throws Exception
	{
		LethenonMinePanel panel = filledPanel(PASSWORD);
		panel.getModelObject().setPun("a pun against the cameras");

		panel.onMine();

		List<BlockBody> chain = new ChainFile(chainFile).require();
		assertEquals(3, chain.size());
		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("mined block 2"), report);
		assertTrue(report.contains(chain.getLast().pun()), report);
		assertTrue(report.contains("replayed 3 blocks"), report);
		assertEquals("the block was mined and written to the chain file",
			panel.getModelObject().getResultText());
	}

	@Test
	@DisplayName("the password is gone from the model once the block was mined, and was in no text")
	void mining_dropsThePassword() throws Exception
	{
		LethenonMinePanel panel = filledPanel(PASSWORD);

		panel.onMine();

		assertEquals(0, panel.getModelObject().getPassword().length);
		assertFalse(panel.getModelObject().getReport().contains(PASSWORD));
		assertFalse(panel.getModelObject().getResultText().contains(PASSWORD));
	}

	@Test
	@DisplayName("a wrong password leaves no report, writes no block and does not repeat the password")
	void mining_withAWrongPassword_reportsTheReasonOnly() throws Exception
	{
		byte[] chainBefore = Files.readAllBytes(chainFile);
		LethenonMinePanel panel = filledPanel("not the password at all");

		panel.onMine();

		assertEquals("", panel.getModelObject().getReport());
		String result = panel.getModelObject().getResultText();
		assertTrue(result.startsWith("no block was mined"), result);
		assertTrue(result.contains("wallet.lethenon-wallet"), result);
		assertFalse(result.contains("not the password at all"), result);
		assertArrayEquals(chainBefore, Files.readAllBytes(chainFile));
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	@Test
	@DisplayName("a new chain is a test chain unless the main chain is chosen")
	void thePanel_startsATestChainByDefault()
	{
		assertEquals(ChainKind.TEST_NETWORK,
			new LethenonMinePanel().getModelObject().getChainKind());
	}

	@Test
	@DisplayName("mining where there is no chain file starts a test chain, and the report names it")
	void mining_withoutAChainFile_startsATestChain() throws Exception
	{
		Files.delete(chainFile);
		LethenonMinePanel panel = filledPanel(PASSWORD);

		panel.onMine();

		List<BlockBody> chain = new ChainFile(chainFile).require();
		assertEquals("lethenon-test-1", chain.getFirst().chainIdentifier());
		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("mined block 0"), report);
		assertTrue(report.contains("chain lethenon-test-1"), report);
		assertFalse(componentNamed(panel, "cbxChainKind", JComboBox.class).isEnabled(),
			"once the genesis block is written, it decides");
	}

	@Test
	@DisplayName("choosing the main chain where there is no chain file starts a main chain")
	void mining_withoutAChainFile_startsTheChosenMainChain() throws Exception
	{
		Files.delete(chainFile);
		LethenonMinePanel panel = filledPanel(PASSWORD);
		panel.getModelObject().setChainKind(ChainKind.MAIN_CHAIN);

		panel.onMine();

		assertEquals("lethenon-1", new ChainFile(chainFile).require().getFirst().chainIdentifier());
		String report = panel.getModelObject().getReport();
		assertTrue(report.contains("chain lethenon-1"), report);
	}

	@Test
	@DisplayName("the kind of chain can be chosen only while the chain file does not exist")
	void theChoice_isOpenOnlyWithoutAChainFile() throws Exception
	{
		LethenonMinePanel panel = new LethenonMinePanel();
		JComboBox<?> kinds = componentNamed(panel, "cbxChainKind", JComboBox.class);
		JTextField chainFileField = componentNamed(panel, "txtChainFile", JTextField.class);

		chainFileField.setText(chainFile.toString());
		assertFalse(kinds.isEnabled(), "an existing chain's genesis block decides");

		chainFileField.setText(directory.toPath().resolve("new.lethenon").toString());
		assertTrue(kinds.isEnabled(), "a chain file that does not exist yet is a new chain");
	}

	@Test
	@DisplayName("replacing the password overwrites the array it replaces")
	void setPassword_wipesTheReplacedArray()
	{
		LethenonMinePanelModel model = new LethenonMinePanelModel();
		char[] first = "first".toCharArray();
		model.setPassword(first);

		model.setPassword("second".toCharArray());

		assertArrayEquals(new char[5], first);
	}

	private static <T extends Component> T componentNamed(final Container container,
		final String name, final Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container nested)
			{
				T found = componentNamed(nested, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private LethenonMinePanel filledPanel(final String password)
	{
		LethenonMinePanel panel = new LethenonMinePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(password.toCharArray());
		return panel;
	}
}
