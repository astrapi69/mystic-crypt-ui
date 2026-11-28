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

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Rearranging the menu bar at the start writes no stack trace. The JDK's keyboard manager keeps the
 * keystrokes of a menu in a hash table keyed by the main window, and removing a menu looks the
 * window up again: a window whose hash code changed while it was built is not found, and the
 * manager writes a stack trace for every keystroke of the menu (#1245).
 */
class AMenuBarIsRearrangedWithoutStackTracesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void start_writesStackTracesFromReorganizeMenus_whenTheWindowHashCodeChanges() throws Exception
	{
		File databaseFile = new File(tempHome, "menu-bar-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		try (StartupWatch watch = StartupWatch.begin())
		{
			signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
			awaitApplicationInitialized();

			assertTrue(watch.occurrences("DesktopMenu.reorganizeMenus") > 0,
				"standard error of the start:\n" + watch.standardError());
		}
	}
}
