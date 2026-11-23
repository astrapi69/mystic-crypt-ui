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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The start switches the look and feel on the event thread, never on the thread that builds the
 * main window (#1097).
 * <p>
 * Switching it re-installs the UI of every component and takes the AWT tree lock; from the start
 * thread that blocked the event thread, which held a text document's write lock while appending
 * what the console plugin captured, and the application hung at its start (thread dump of a hung
 * local run: the start thread held the tree lock and waited for the document, the event thread held
 * the document and waited for the tree lock). The main window is the owner of the sign-in dialog,
 * so it has a native peer from then on and the layout runs under the lock.
 */
class StartAppliesTheLookAndFeelOnTheEventThreadUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("every look and feel change of the start happens on the event thread")
	void everyLookAndFeelChangeOfTheStart_happensOnTheEventThread() throws IOException
	{
		File databaseFile = new File(tempHome, "look-and-feel-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		List<String> changedOn = new CopyOnWriteArrayList<>();
		List<String> offTheEventThread = new CopyOnWriteArrayList<>();
		PropertyChangeListener listener = change -> {
			if ("lookAndFeel".equals(change.getPropertyName()))
			{
				changedOn.add(Thread.currentThread().getName());
				if (!SwingUtilities.isEventDispatchThread())
				{
					offTheEventThread.add(Thread.currentThread().getName());
				}
			}
		};
		UIManager.addPropertyChangeListener(listener);
		try
		{
			signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		}
		finally
		{
			UIManager.removePropertyChangeListener(listener);
		}

		assertFalse(changedOn.isEmpty(), "the start switched the look and feel at least once");
		assertTrue(offTheEventThread.isEmpty(),
			"the look and feel was switched on " + offTheEventThread + ", not the event thread");
	}
}
