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

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A start sets the persisted look and feel once, before the sign-in dialog. The components of the
 * window are built after the dialog, so a switch after them only re-installs the UI of the finished
 * menus, and the JDK's keyboard manager writes a stack trace for every menu item of a popup it does
 * not know (#1245).
 */
class AStartSetsTheLookAndFeelOnceUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void start_setsTheLookAndFeelOnce_whenTheVaultIsOpened() throws Exception
	{
		File databaseFile = new File(tempHome, "look-and-feel-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		try (StartupWatch watch = StartupWatch.begin())
		{
			signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
			awaitApplicationInitialized();

			assertEquals(1, watch.lookAndFeelChanges().size(),
				"look and feel changes of the start: " + watch.lookAndFeelChanges());
			assertEquals(0, watch.occurrences("SwingUtilities.updateComponentTreeUI("),
				"stack traces from the look and feel switches:\n" + watch.standardError());
		}
	}
}
