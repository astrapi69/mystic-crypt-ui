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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

/**
 * End-to-end use case "export to a KeePass database": sign in, run the File menu's export action
 * with a destination file and password - the written {@code .kdbx} file must be readable by
 * KeePassJava2 with exactly those credentials (round-trip proof against real KeePass tooling)
 */
class KeePassExportUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "export-db-pw-123";
	private static final String EXPORT_PASSWORD = "exported-kdbx-pw-1";

	@Test
	void exportedKdbxFileIsReadableWithTheChosenPassword() throws Exception
	{
		File databaseFile = new File(tempHome, "export-source-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File exportFile = new File(tempHome, "exported-database.kdbx");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application.exportKeePassDatabase(exportFile, EXPORT_PASSWORD);

		assertTrue(exportFile.exists(), "the exported .kdbx file must exist on disk");
		assertTrue(exportFile.length() > 0, "the exported .kdbx file must not be empty");
		assertTrue(loadableWithPassword(exportFile, EXPORT_PASSWORD),
			"the exported file must be readable by KeePassJava2 with the chosen password");
	}

	private boolean loadableWithPassword(File kdbxFile, String password) throws IOException
	{
		try (InputStream inputStream = new FileInputStream(kdbxFile))
		{
			SimpleDatabase database = SimpleDatabase
				.load(new KdbxCreds(password.getBytes(StandardCharsets.UTF_8)), inputStream);
			return database.getRootGroup() != null;
		}
		catch (Exception exception)
		{
			return false;
		}
	}
}
