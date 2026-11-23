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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;

import javax.swing.JLabel;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Locking empties the clipboard (#238), and the unlock prompt says so when it emptied text (#1001):
 * a password copied before the lock otherwise pastes nothing into the prompt, which reads as "paste
 * does not work here". Both cases drive the real lock through the File menu
 */
class TheUnlockPromptSaysTheLockEmptiedTheClipboardUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "an entry to copy from";

	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	private static final String NOTE_NAME = "lblClipboardEmptied";

	@Test
	@DisplayName("after a lock that emptied a copied password, the unlock prompt says so (#1001)")
	void afterALockThatEmptiedACopiedPassword_theUnlockPromptSaysSo() throws Exception
	{
		File databaseFile = new File(tempHome, "clipboard-note.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.selectEntryRowByTitle(frame, ENTRY_TITLE);
		application.copyPasswordOfSelectedEntry(frame);
		assertEquals(ENTRY_PASSWORD, application.clipboardText(),
			"precondition: the password is on the clipboard when the lock happens");

		application.lockWorkspace();

		assertEquals("", application.clipboardText(),
			"the lock still empties the clipboard (#238)");
		assertEquals(
			Messages.getString("unlock.clipboard.emptied",
				"Locking emptied the clipboard. Copy the password again to paste it."),
			noteOfTheUnlockPrompt(application));
		application.cancelUnlock();
	}

	@Test
	@DisplayName("after a lock over an empty clipboard, the unlock prompt says nothing about it (#1001)")
	void afterALockOverAnEmptyClipboard_theUnlockPromptSaysNothing() throws Exception
	{
		File databaseFile = new File(tempHome, "no-clipboard-note.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		GuiActionRunner.execute(() -> Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(""), null));

		application.lockWorkspace();

		assertNull(noteOfTheUnlockPrompt(application),
			"the prompt says the lock emptied something that was not there");
		application.cancelUnlock();
	}

	/**
	 * The text of the note about the clipboard in the unlock prompt the lock opened, or null when
	 * the prompt has none
	 */
	private static String noteOfTheUnlockPrompt(final ApplicationSteps application)
	{
		DialogFixture prompt = application.findDialogWithTitle("Unlock workspace");
		return GuiActionRunner.execute(() -> textOfTheNoteIn(prompt.target()));
	}

	private static String textOfTheNoteIn(final Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JLabel label && NOTE_NAME.equals(label.getName()))
			{
				return label.getText();
			}
			if (component instanceof Container nested)
			{
				String text = textOfTheNoteIn(nested);
				if (text != null)
				{
					return text;
				}
			}
		}
		return null;
	}
}
