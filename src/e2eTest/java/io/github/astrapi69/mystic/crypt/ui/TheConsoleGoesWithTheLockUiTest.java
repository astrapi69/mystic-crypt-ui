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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintStream;

import javax.swing.JInternalFrame;
import javax.swing.JTextArea;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Locking takes the console with it, buffer and all (#375).
 * <p>
 * Measured before this: after a lock the console stood on the desktop with its whole scrollback -
 * 452 lines, 39410 characters - and it held the vault's full path twice, out of the stack trace of
 * a save that failed. Nothing ever cleared it, and closing the window would not have cleared it
 * either: the redirected streams kept writing into the text area of a window nobody could see.
 */
class TheConsoleGoesWithTheLockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String MARKER = "console-scrollback-that-must-not-survive-the-lock";

	@Test
	@DisplayName("locking closes the console, erases its buffer, and unlocking does not bring it back")
	void lockingClosesTheConsoleAndErasesIt() throws Exception
	{
		installPluginRequiringItBuilt(CONSOLE_ZIP);
		File databaseFile = new File(tempHome, "console-and-lock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		PrintStream systemOutBeforeTheConsole = System.out;
		try
		{
			application.openPluginTool("Console", "Console");
			System.out.println(MARKER);
			Pause.pause(new Condition("the console shows what was printed")
			{
				@Override
				public boolean test()
				{
					return consoleShows(frame, MARKER);
				}
			}, 15000);
			assertTrue(consoleShows(frame, MARKER), "the precondition: the console has content");

			application.lockWorkspace();

			assertFalse(anyConsoleWindowIsShowing(),
				"a locked workspace has no console standing on it (#375)");
			assertFalse(consoleShows(frame, MARKER),
				"and its scrollback is erased, not just hidden - the measurement found vault "
					+ "paths in exactly this buffer");
			assertSame(systemOutBeforeTheConsole, System.out,
				"standard output points where it did before the console opened; a stream still "
					+ "writing into a closed window keeps the whole scrollback reachable");

			application.unlockWorkspace(MASTER_PASSWORD);

			assertFalse(anyConsoleWindowIsShowing(), "unlocking does not bring the console back");
			assertFalse(consoleShows(frame, MARKER), "and nothing of what it held comes back");
		}
		finally
		{
			System.setOut(systemOutBeforeTheConsole);
		}
	}

	private static boolean anyConsoleWindowIsShowing()
	{
		return GuiActionRunner.execute(() -> {
			MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
			if (instance.getDesktopPanePanel() == null
				|| instance.getDesktopPanePanel().getDesktopPane() == null)
			{
				return false;
			}
			for (JInternalFrame internalFrame : instance.getDesktopPanePanel().getDesktopPane()
				.getAllFrames())
			{
				if ("Console".equals(internalFrame.getTitle()) && internalFrame.isShowing())
				{
					return true;
				}
			}
			return false;
		});
	}

	private boolean consoleShows(final FrameFixture frame, final String marker)
	{
		return GuiActionRunner.execute(() -> {
			try
			{
				return frame.robot().finder()
					.find(new GenericTypeMatcher<JTextArea>(JTextArea.class)
					{
						@Override
						protected boolean isMatching(JTextArea candidate)
						{
							return candidate.getText() != null
								&& candidate.getText().contains(marker);
						}
					}) != null;
			}
			catch (org.assertj.swing.exception.ComponentLookupException notFound)
			{
				return false;
			}
		});
	}
}
