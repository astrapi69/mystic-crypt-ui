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
package io.github.astrapi69.mystic.crypt.panel.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Tests that the components of {@link ExportKeePassPanel} are bound to {@link KeePassPanelModel}:
 * what the file chooser picked and what the user typed is what the export writes with.
 * <p>
 * The proof is a real {@code .kdbx} file: it appears where the panel says and opens with nothing
 * but the credentials the panel hands out, which it could only do if the destination, the password
 * and the key file travelled through the model.
 */
class ExportKeePassPanelBindingTest
{

	/** The title of the single entry every exported database in this test carries */
	private static final String ENTRY_TITLE = "BoundExportSecret";

	/** The key file material a key-file protected export in this test is built with */
	private static final byte[] KEY_FILE_MATERIAL = "mystic-crypt-binding-keyfile-material-56789"
		.getBytes(StandardCharsets.UTF_8);

	private static <T extends Component> T named(Container container, String name, Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
		}
		throw new AssertionError(
			"the panel has no " + type.getSimpleName() + " named '" + name + "'");
	}

	/**
	 * Writes a KeePass database holding one entry to the file the panel points at, protected by the
	 * credentials the panel hands out - exactly what {@code ExportKeePassDatabaseAction} does when
	 * the user confirms the dialog
	 *
	 * @param panel
	 *            the panel that was driven
	 * @return the file that was written
	 * @throws Exception
	 *             is thrown if the database cannot be written
	 */
	private static File exportWith(ExportKeePassPanel panel) throws Exception
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup rootGroup = database.getRootGroup();
		SimpleEntry entry = database.newEntry();
		entry.setTitle(ENTRY_TITLE);
		entry.setUsername("bound-user");
		rootGroup.addEntry(entry);
		File destinationFile = panel.getSelectedFile();
		try (OutputStream outputStream = new FileOutputStream(destinationFile))
		{
			database.save(credentials(panel), outputStream);
		}
		return destinationFile;
	}

	private static KdbxCreds credentials(ExportKeePassPanel panel) throws Exception
	{
		char[] password = panel.getPassword();
		File keyFile = panel.getSelectedKeyFile();
		byte[] passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8);
		if (keyFile == null)
		{
			return new KdbxCreds(passwordBytes);
		}
		try (InputStream keyFileStream = new FileInputStream(keyFile))
		{
			return new KdbxCreds(passwordBytes, keyFileStream);
		}
	}

	private static String firstEntryTitleOf(File keePassFile, KdbxCreds credentials)
		throws Exception
	{
		try (InputStream inputStream = new FileInputStream(keePassFile))
		{
			return SimpleDatabase.load(credentials, inputStream).getRootGroup().getEntries().get(0)
				.getTitle();
		}
	}

	/**
	 * The picked destination and the typed password reach the export through the model - the file
	 * that appears at that path, opening with that password, proves both arrived
	 */
	@Test
	void theExportWritesTheFileAndPasswordTheBoundComponentsCarry(@TempDir File directory)
		throws Exception
	{
		String password = TestPasswords.throwaway();
		File destinationFile = new File(directory, "bound-export.kdbx");

		ExportKeePassPanel panel = new ExportKeePassPanel();
		named(panel, "txtFile", JTextField.class).setText(destinationFile.getAbsolutePath());
		named(panel, "txtPassword", JPasswordField.class).setText(password);

		File writtenFile = exportWith(panel);

		assertEquals(destinationFile, writtenFile);
		assertTrue(destinationFile.exists(), "the export was not written where the field points");
		assertEquals(ENTRY_TITLE, firstEntryTitleOf(destinationFile,
			new KdbxCreds(password.getBytes(StandardCharsets.UTF_8))));
	}

	/**
	 * With the key file box on, the picked key file protects the exported file too - the export
	 * only opens again when password and key file are both offered
	 */
	@Test
	void theExportAddsTheKeyFileTheBoundComponentsCarry(@TempDir File directory) throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = new File(directory, "bound-export.keyx");
		Files.write(keyFile.toPath(), KEY_FILE_MATERIAL);
		File destinationFile = new File(directory, "bound-export-with-keyfile.kdbx");

		ExportKeePassPanel panel = new ExportKeePassPanel();
		named(panel, "txtFile", JTextField.class).setText(destinationFile.getAbsolutePath());
		named(panel, "txtPassword", JPasswordField.class).setText(password);
		// doClick and not setSelected, so the listener that enables the key file field runs too
		named(panel, "cbxKeyFile", JCheckBox.class).doClick();
		named(panel, "txtKeyFile", JTextField.class).setText(keyFile.getAbsolutePath());

		exportWith(panel);

		assertTrue(panel.getModelObject().isUseKeyFile());
		assertEquals(keyFile, panel.getSelectedKeyFile());
		assertEquals(ENTRY_TITLE,
			firstEntryTitleOf(destinationFile,
				new KdbxCreds(password.getBytes(StandardCharsets.UTF_8),
					new ByteArrayInputStream(KEY_FILE_MATERIAL))));
		assertThrows(Exception.class,
			() -> firstEntryTitleOf(destinationFile,
				new KdbxCreds(password.getBytes(StandardCharsets.UTF_8))),
			"the key file must be part of what protects the exported file");
	}

	/**
	 * Switching the key file box off takes the key file out of the credentials again, so the export
	 * is protected by the password alone
	 */
	@Test
	void switchingTheKeyFileBoxOffTakesTheKeyFileOutOfTheCredentials(@TempDir File directory)
		throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = new File(directory, "bound-export-unused.keyx");
		Files.write(keyFile.toPath(), KEY_FILE_MATERIAL);
		File destinationFile = new File(directory, "bound-export-password-only.kdbx");

		ExportKeePassPanel panel = new ExportKeePassPanel();
		named(panel, "txtFile", JTextField.class).setText(destinationFile.getAbsolutePath());
		named(panel, "txtPassword", JPasswordField.class).setText(password);
		JCheckBox checkBox = named(panel, "cbxKeyFile", JCheckBox.class);
		checkBox.doClick();
		named(panel, "txtKeyFile", JTextField.class).setText(keyFile.getAbsolutePath());
		checkBox.doClick();

		exportWith(panel);

		assertNull(panel.getSelectedKeyFile());
		assertEquals(ENTRY_TITLE, firstEntryTitleOf(destinationFile,
			new KdbxCreds(password.getBytes(StandardCharsets.UTF_8))));
	}

	/**
	 * The remembered destination is what the panel starts with, even though nothing has been
	 * written to it yet - unlike the import, an export names a file that does not exist
	 */
	@Test
	void theRememberedDestinationIsWhatThePanelHandsOut(@TempDir File directory)
	{
		File destinationFile = new File(directory, "remembered-export.kdbx");

		ExportKeePassPanel panel = new ExportKeePassPanel(destinationFile.getAbsolutePath(), null);

		assertEquals(destinationFile, panel.getSelectedFile());
		assertNull(panel.getSelectedKeyFile());
	}

	/**
	 * A panel nobody touched hands out nothing - this is the state the export refuses with "No
	 * destination file selected."
	 */
	@Test
	void anUntouchedPanelHandsOutNoFileAndNoPassword()
	{
		ExportKeePassPanel panel = new ExportKeePassPanel();

		assertNull(panel.getSelectedFile());
		assertNull(panel.getSelectedKeyFile());
		assertEquals(0, panel.getPassword().length);
	}

}
