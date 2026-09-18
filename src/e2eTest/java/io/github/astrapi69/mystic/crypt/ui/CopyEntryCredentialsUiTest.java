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

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use cases "copy user name" and "copy password" - the daily bread of a password
 * manager: select the entry, use the context menu, and the credential must land on the system
 * clipboard.
 * <p>
 * Note: this test writes to the REAL system clipboard (that is the feature under test), so whatever
 * was on the clipboard before the run gets replaced
 */
class CopyEntryCredentialsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String ENTRY_TITLE = "Copy Source";
	private static final String ENTRY_USERNAME = "copy-user";
	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	@Test
	void copyUsernameAndPasswordPutThemOnTheClipboard() throws Exception
	{
		File databaseFile = new File(tempHome, "copy-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ENTRY_TITLE, ENTRY_USERNAME, ENTRY_PASSWORD);
		application.selectEntryRowByTitle(frame, ENTRY_TITLE);

		application.copyUsernameOfSelectedEntry(frame);
		assertEquals(ENTRY_USERNAME, application.clipboardText(),
			"Copy Username must put the entry's user name on the clipboard");

		application.copyPasswordOfSelectedEntry(frame);
		assertEquals(ENTRY_PASSWORD, application.clipboardText(),
			"Copy Password must put the entry's password on the clipboard");
	}
}
