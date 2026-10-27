/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.key.PublicKeyStringEncryptor;
import io.github.astrapi69.xstream.ObjectToXmlExtensions;

/**
 * A rule file that names a type no rule table holds is refused by the import of the simple
 * obfuscation window: the dialog names the type, and the table stays as it was.
 */
class ObfuscationImportRefusesAForeignTypeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("importing a rule file that names a foreign type shows why and imports nothing")
	void aRuleFileWithAForeignType_isRefused_andNothingIsImported() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "obfuscation-foreign-type.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		// the import decrypts with the database's private key, so the rule file is encrypted for
		// its public key, as the export would encrypt it
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		KeyModel privateKeyInfo = KeyModelExtensions.toKeyModel(keyPair.getPrivate());
		GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject()
			.getMasterPwFileModelBean().setPrivateKeyInfo(privateKeyInfo));

		// a type of this project that no rule table holds: the host's own model, harmless and
		// visible to the plugin's class loader
		String foreignType = ApplicationModelBean.class.getName();
		List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> table = new ArrayList<>();
		table.add(KeyValuePair.<Character, ObfuscationRule<Character, Character>> builder().key('a')
			.value(new ObfuscationRule<>('a', 'q')).build());
		String xml = ObjectToXmlExtensions.toXml(table).replaceFirst("(<[a-z-]*list[^>]*>)",
			"$1<" + foreignType + "/>");
		assertTrue(xml.contains(foreignType), "the foreign type is in the rule file: " + xml);
		File ruleFile = new File(tempHome, "names-a-foreign-type.obf");
		Files.write(ruleFile.toPath(),
			new PublicKeyStringEncryptor(keyPair.getPublic()).encrypt(xml));

		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Simple Obfuscation", "Simple Obfuscation");
		int rowsBefore = GuiActionRunner
			.execute(() -> frame.table("tblKeyRules").target().getRowCount());

		SwingUtilities.invokeLater(() -> frame.button("btnImport").target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(ruleFile);
			fileChooser.approveSelection();
		});

		JOptionPaneFixture refusal = JOptionPaneFinder.findOptionPane()
			.withTimeout(15, TimeUnit.SECONDS).using(robot);
		String title = GuiActionRunner
			.execute(() -> ((javax.swing.JDialog)SwingUtilities.getWindowAncestor(refusal.target()))
				.getTitle());
		String message = GuiActionRunner
			.execute(() -> String.valueOf(refusal.target().getMessage()));
		int messageType = GuiActionRunner.execute(() -> refusal.target().getMessageType());
		refusal.okButton().click();

		assertEquals("Rule table not imported", title);
		assertTrue(message.contains(foreignType), "the dialog names the type: " + message);
		assertEquals(JOptionPane.ERROR_MESSAGE, messageType);
		assertEquals(rowsBefore,
			(int)GuiActionRunner.execute(() -> frame.table("tblKeyRules").target().getRowCount()),
			"nothing was imported");

		application.closeInternalFrame("Simple Obfuscation");
	}
}
