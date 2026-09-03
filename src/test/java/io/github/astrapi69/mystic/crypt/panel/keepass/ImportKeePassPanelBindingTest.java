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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import javax.swing.JComponent;
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
 * Tests that the components of {@link ImportKeePassPanel} are bound to {@link KeePassPanelModel}:
 * what the file chooser picked and what the user typed is what the import works with.
 * <p>
 * The proof is a real {@code .kdbx} file, written in a temporary directory and then opened with
 * nothing but the credentials the panel hands out - the entry that comes back could only come back
 * if the path, the password and the key file travelled through the model.
 */
class ImportKeePassPanelBindingTest
{

	/** The title of the single entry every database in this test carries */
	private static final String ENTRY_TITLE = "BoundImportSecret";

	/** The key file material a key-file protected database in this test is built with */
	private static final byte[] KEY_FILE_MATERIAL = "mystic-crypt-binding-keyfile-material-01234"
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
	 * Writes a KeePass database holding one entry, protected by the given credentials
	 *
	 * @param keePassFile
	 *            the file to write
	 * @param credentials
	 *            the credentials the file is protected with
	 * @throws Exception
	 *             is thrown if the database cannot be written
	 */
	private static void writeDatabase(File keePassFile, KdbxCreds credentials) throws Exception
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup rootGroup = database.getRootGroup();
		SimpleEntry entry = database.newEntry();
		entry.setTitle(ENTRY_TITLE);
		entry.setUsername("bound-user");
		rootGroup.addEntry(entry);
		try (OutputStream outputStream = new FileOutputStream(keePassFile))
		{
			database.save(credentials, outputStream);
		}
	}

	/**
	 * Opens the KeePass file the panel points at with the credentials the panel hands out - exactly
	 * what {@code ImportKeePassDatabaseAction} does when the user confirms the dialog
	 *
	 * @param panel
	 *            the panel that was driven
	 * @return the loaded database
	 * @throws Exception
	 *             is thrown if the database cannot be opened
	 */
	private static SimpleDatabase importWith(ImportKeePassPanel panel) throws Exception
	{
		try (InputStream inputStream = new FileInputStream(panel.getSelectedFile()))
		{
			return SimpleDatabase.load(credentials(panel), inputStream);
		}
	}

	private static KdbxCreds credentials(ImportKeePassPanel panel) throws Exception
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

	/**
	 * The picked file and the typed password reach the import through the model - the entry that
	 * comes out of the database proves both arrived
	 */
	@Test
	void theImportOpensTheFileAndPasswordTheBoundComponentsCarry(@TempDir File directory)
		throws Exception
	{
		String password = TestPasswords.throwaway();
		File keePassFile = new File(directory, "bound-import.kdbx");
		writeDatabase(keePassFile, new KdbxCreds(password.getBytes(StandardCharsets.UTF_8)));

		ImportKeePassPanel panel = new ImportKeePassPanel();
		named(panel, "txtFile", JTextField.class).setText(keePassFile.getAbsolutePath());
		named(panel, "txtPassword", JPasswordField.class).setText(password);

		SimpleDatabase imported = importWith(panel);

		assertEquals(keePassFile, panel.getSelectedFile());
		assertEquals(ENTRY_TITLE, imported.getRootGroup().getEntries().get(0).getTitle());
	}

	/**
	 * With the key file box on, the key file the chooser picked takes part in the credentials - a
	 * database that is protected by password and key file only opens when both travelled through
	 * the model
	 */
	@Test
	void theImportAddsTheKeyFileTheBoundComponentsCarry(@TempDir File directory) throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = new File(directory, "bound-import.keyx");
		Files.write(keyFile.toPath(), KEY_FILE_MATERIAL);
		File keePassFile = new File(directory, "bound-import-with-keyfile.kdbx");
		writeDatabase(keePassFile, new KdbxCreds(password.getBytes(StandardCharsets.UTF_8),
			new ByteArrayInputStream(KEY_FILE_MATERIAL)));

		ImportKeePassPanel panel = new ImportKeePassPanel();
		named(panel, "txtFile", JTextField.class).setText(keePassFile.getAbsolutePath());
		named(panel, "txtPassword", JPasswordField.class).setText(password);
		// doClick and not setSelected, so the listener that enables the key file field runs too
		named(panel, "cbxKeyFile", JCheckBox.class).doClick();
		named(panel, "txtKeyFile", JTextField.class).setText(keyFile.getAbsolutePath());

		SimpleDatabase imported = importWith(panel);

		assertTrue(panel.getModelObject().isUseKeyFile());
		assertEquals(keyFile, panel.getSelectedKeyFile());
		assertEquals(ENTRY_TITLE, imported.getRootGroup().getEntries().get(0).getTitle());
	}

	/**
	 * Switching the key file box off takes the key file out of the credentials again: the same
	 * password-only database that could not be opened with a key file opens without it
	 */
	@Test
	void switchingTheKeyFileBoxOffTakesTheKeyFileOutOfTheCredentials(@TempDir File directory)
		throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = new File(directory, "bound-import-unused.keyx");
		Files.write(keyFile.toPath(), KEY_FILE_MATERIAL);
		File keePassFile = new File(directory, "bound-import-password-only.kdbx");
		writeDatabase(keePassFile, new KdbxCreds(password.getBytes(StandardCharsets.UTF_8)));

		ImportKeePassPanel panel = new ImportKeePassPanel();
		named(panel, "txtFile", JTextField.class).setText(keePassFile.getAbsolutePath());
		named(panel, "txtPassword", JPasswordField.class).setText(password);
		JCheckBox checkBox = named(panel, "cbxKeyFile", JCheckBox.class);
		checkBox.doClick();
		named(panel, "txtKeyFile", JTextField.class).setText(keyFile.getAbsolutePath());

		assertThrows(Exception.class, () -> importWith(panel),
			"with the key file in the credentials the password-only database must not open");

		checkBox.doClick();

		assertFalse(panel.getModelObject().isUseKeyFile());
		assertNull(panel.getSelectedKeyFile());
		assertEquals(ENTRY_TITLE, importWith(panel).getRootGroup().getEntries().get(0).getTitle());
	}

	/**
	 * The remembered paths are what the panel starts with: an existing file is taken over, one that
	 * has been deleted since is not, and a remembered key file switches the key file box on
	 */
	@Test
	void theRememberedPathsAreWhatThePanelHandsOut(@TempDir File directory) throws Exception
	{
		File keePassFile = new File(directory, "remembered.kdbx");
		writeDatabase(keePassFile, new KdbxCreds(TestPasswords.throwaway().getBytes()));
		File keyFile = new File(directory, "remembered.keyx");
		Files.write(keyFile.toPath(), KEY_FILE_MATERIAL);

		ImportKeePassPanel panel = new ImportKeePassPanel(keePassFile.getAbsolutePath(),
			keyFile.getAbsolutePath());

		assertEquals(keePassFile, panel.getSelectedFile());
		assertEquals(keyFile, panel.getSelectedKeyFile());
		assertTrue(named(panel, "cbxKeyFile", JCheckBox.class).isSelected());
		assertTrue(panel.getModelObject().isUseKeyFile());

		ImportKeePassPanel withDeletedFile = new ImportKeePassPanel(
			new File(directory, "gone.kdbx").getAbsolutePath(), null);

		assertNull(withDeletedFile.getSelectedFile());
	}

	/**
	 * A panel nobody touched hands out nothing - this is the state the import refuses with "No
	 * KeePass file selected."
	 */
	@Test
	void anUntouchedPanelHandsOutNoFileAndNoPassword()
	{
		ImportKeePassPanel panel = new ImportKeePassPanel();

		assertNull(panel.getSelectedFile());
		assertNull(panel.getSelectedKeyFile());
		assertEquals(0, panel.getPassword().length);
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	/**
	 * "Key File" may not mean anything to a user who has not used KeePass before (#163)
	 */
	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		ImportKeePassPanel panel = new ImportKeePassPanel();

		assertHasTooltip(named(panel, "txtFile", JComponent.class), "file");
		assertHasTooltip(named(panel, "btnFile", JComponent.class), "browse file button");
		assertHasTooltip(named(panel, "txtPassword", JComponent.class), "password");
		assertHasTooltip(named(panel, "cbxKeyFile", JComponent.class), "key file checkbox");
		assertHasTooltip(named(panel, "txtKeyFile", JComponent.class), "key file");
		assertHasTooltip(named(panel, "btnKeyFile", JComponent.class), "browse key file button");
	}

}
