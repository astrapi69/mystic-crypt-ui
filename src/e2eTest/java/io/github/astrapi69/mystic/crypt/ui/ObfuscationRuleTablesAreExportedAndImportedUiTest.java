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

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyStringDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyStringEncryptor;
import io.github.astrapi69.xstream.ObjectToXmlExtensions;

/**
 * The rule tables of both obfuscation windows go out through Export and come back through Import,
 * and the file Export writes holds the table.
 * <p>
 * The plugin writes the encrypted table with file-worker's {@code StoreFileExtensions.toFile} and
 * reads it with {@code ReadFileExtensions.readFileToBytearray}. The tests that reach those calls
 * check that the exported file exists, or that an import is refused; none reads what the export
 * wrote, nor sees a table arrive through an import. Here a table is imported from a file that was
 * made as the export makes one, exported again, and the new file is decrypted with the database's
 * key: its content is the XML of the table that went in, so a file that is short, empty or of
 * another table fails, and so does an import that took nothing in.
 */
class ObfuscationRuleTablesAreExportedAndImportedUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the simple obfuscation window: a table imported from a file is exported again with the same content")
	void theSimpleWindow_importedTable_isExportedWithTheSameContent() throws Exception
	{
		List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> table = new ArrayList<>();
		table.add(KeyValuePair.<Character, ObfuscationRule<Character, Character>> builder().key('a')
			.value(new ObfuscationRule<>('a', 'q')).build());
		table.add(KeyValuePair.<Character, ObfuscationRule<Character, Character>> builder().key('ä')
			.value(new ObfuscationRule<>('ä', '7')).build());

		importThenExport("Simple Obfuscation", ObjectToXmlExtensions.toXml(table));
	}

	@Test
	@DisplayName("the operated obfuscation window: a table imported from a file is exported again with the same content")
	void theOperatedWindow_importedTable_isExportedWithTheSameContent() throws Exception
	{
		Set<Integer> indexes = new LinkedHashSet<>(List.of(0, 3, 7));
		List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> table = new ArrayList<>();
		table.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
			.key('b').value(new ObfuscationOperationRule<>('b', indexes, true, Optional.of('B'),
				Operation.UPPERCASE, 'x'))
			.build());
		table.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
			.key('c').value(new ObfuscationOperationRule<>('c', new LinkedHashSet<>(), false,
				Optional.empty(), Operation.NONE, 'y'))
			.build());

		importThenExport("Operated Obfuscation", ObjectToXmlExtensions.toXml(table));
	}

	private void importThenExport(final String tool, final String tableXml) throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "obfuscation-tables.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		KeyModel privateKeyInfo = KeyModelExtensions.toKeyModel(keyPair.getPrivate());
		GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject()
			.getMasterPwFileModelBean().setPrivateKeyInfo(privateKeyInfo));
		File fileToImport = new File(tempHome, "table-to-import.obf");
		Files.write(fileToImport.toPath(),
			new PublicKeyStringEncryptor(keyPair.getPublic()).encrypt(tableXml));
		File exportedFile = new File(tempHome, "table-exported.obf");

		FrameFixture frame = application.showMainFrame();
		application.openPluginTool(tool, tool);
		chooseFileFor(frame, "btnImport", fileToImport);
		robot.waitForIdle();
		chooseFileFor(frame, "btnExport", exportedFile);
		Pause.pause(new Condition("the exported rule file is written")
		{
			@Override
			public boolean test()
			{
				return exportedFile.isFile() && 0 < exportedFile.length();
			}
		}, 10000);

		String exportedXml = new PrivateKeyStringDecryptor(keyPair.getPrivate())
			.decrypt(Files.readAllBytes(exportedFile.toPath()));
		assertEquals(tableXml, exportedXml,
			"the file Export wrote holds the table that Import took in");

		application.closeInternalFrame(tool);
	}

	private void chooseFileFor(final FrameFixture frame, final String button, final File file)
	{
		SwingUtilities.invokeLater(() -> frame.button(button).target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(file);
			fileChooser.approveSelection();
		});
	}
}
