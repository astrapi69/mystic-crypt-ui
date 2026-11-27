/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.UUID;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * What the running application shows when a KeePass export cannot write its file, for the export of
 * the whole tree and of chosen entries: the destination lies in a folder that does not exist and
 * whose name is made per run, and the refusal's dialog shows the bundle's title and words for a
 * refused export, exactly
 */
class ARefusedKeePassExportShowsTheWordsOfTheBundleUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String EXPORT_PASSWORD = TestPasswords.throwaway();

	/** The name of a folder that does not exist, made per run */
	private static final String MISSING_FOLDER = "folder-" + UUID.randomUUID();

	/** A file in a folder that does not exist, so the export cannot open it for writing */
	private File aFileTheExportCannotWrite()
	{
		return new File(new File(tempHome, MISSING_FOLDER), "export.kdbx");
	}

	@Test
	@DisplayName("the export of the whole tree to a file it cannot write shows the bundle's title and words")
	void theWholeTree_toAFileTheExportCannotWrite_showsTheWordsOfTheBundle() throws Exception
	{
		File vault = new File(tempHome, "refused-export.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);

		String refusal = application.exportKeePassDatabaseReadingTheRefusal(
			aFileTheExportCannotWrite(), EXPORT_PASSWORD,
			Messages.getString("keepass.export.failed.title", ""));

		assertEquals(Messages.getString("keepass.export.failed.message", ""), refusal);
		assertFalse(refusal.contains(MISSING_FOLDER), "nothing of the refusal is shown");
	}

	@Test
	@DisplayName("the export of chosen entries to a file it cannot write shows the bundle's words")
	void chosenEntries_toAFileTheExportCannotWrite_showTheWordsOfTheBundle() throws Exception
	{
		File vault = new File(tempHome, "refused-export-of-chosen-entries.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "mail", "mail-user", TestPasswords.throwaway());

		String refusal = application.selectEntryRowsByTitle(frame, "mail")
			.exportSelectedEntriesToKeePassReadingTheRefusal(frame, aFileTheExportCannotWrite(),
				EXPORT_PASSWORD,
				Messages.getString("keepass.export.selected.title", "Export selected to KeePass"));

		assertEquals(Messages.getString("keepass.export.failed.message", ""), refusal);
		assertFalse(refusal.contains(MISSING_FOLDER), "nothing of the refusal is shown");
	}
}
