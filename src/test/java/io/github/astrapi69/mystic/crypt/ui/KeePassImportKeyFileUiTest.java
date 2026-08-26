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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end test of importing a KeePass database that is protected by BOTH a password and a key
 * file: builds such a {@code .kdbx} in-place, then drives the import dialog with the "Key File"
 * option enabled and asserts the entries appear in the tree. Complements
 * {@link KeePassImportUiTest}, which covers the password-only path.
 */
class KeePassImportKeyFileUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String KEEPASS_PASSWORD = TestPasswords.throwaway();

	@Test
	void importKeePassDatabaseProtectedByAKeyFileShowsEntriesInTree() throws Exception
	{
		// build a KeePass database protected by password + key file
		File keyFile = new File(tempHome, "keepass.keyx");
		byte[] keyFileBytes = "mystic-crypt-keepass-keyfile-material-0123456789"
			.getBytes(StandardCharsets.UTF_8);
		Files.write(keyFile.toPath(), keyFileBytes);

		File keePassFile = new File(tempHome, "keyfile-db.kdbx");
		SimpleDatabase keePassDatabase = new SimpleDatabase();
		SimpleGroup rootGroup = keePassDatabase.getRootGroup();
		SimpleEntry entry = keePassDatabase.newEntry();
		entry.setTitle("KeyFileProtectedSecret");
		entry.setUsername("kf-user");
		entry.setPassword("kf-pass");
		rootGroup.addEntry(entry);
		try (OutputStream out = new FileOutputStream(keePassFile))
		{
			keePassDatabase.save(new KdbxCreds(KEEPASS_PASSWORD.getBytes(StandardCharsets.UTF_8),
				new ByteArrayInputStream(keyFileBytes)), out);
		}

		File databaseFile = new File(tempHome, "import-keyfile-target.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.importKeePassDatabaseWithKeyFile(keePassFile, KEEPASS_PASSWORD, keyFile);

		assertTrue(application.treeContainsNodeStartingWith("Imported from keyfile-db.kdbx"),
			"the tree must contain the 'Imported from ...' group after a key-file-protected import");
	}
}
