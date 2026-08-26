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
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end round-trip use case "export then re-import": exporting the open database to a
 * {@code .kdbx} file and importing that same file back in must produce its own "Imported from ..."
 * group in the tree - proof that the exporter writes a file the importer can read
 */
class KeePassRoundTripUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String KDBX_PASSWORD = TestPasswords.throwaway();

	@Test
	void exportedDatabaseCanBeReimported() throws IOException
	{
		File databaseFile = new File(tempHome, "roundtrip-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File exportFile = new File(tempHome, "roundtrip-export.kdbx");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application.exportKeePassDatabase(exportFile, KDBX_PASSWORD);
		assertTrue(exportFile.exists() && exportFile.length() > 0,
			"the export must produce a non-empty .kdbx file");

		application.importKeePassDatabase(exportFile, KDBX_PASSWORD);

		assertTrue(application.treeContainsNodeStartingWith("Imported from roundtrip-export.kdbx"),
			"re-importing the exported file must add its own group to the tree");
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isDirty(),
			"a successful re-import must mark the model dirty so it gets saved");
	}
}
