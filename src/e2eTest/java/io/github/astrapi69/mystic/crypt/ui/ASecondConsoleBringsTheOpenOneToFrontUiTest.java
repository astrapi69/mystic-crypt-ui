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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.io.PrintStream;

import javax.swing.JInternalFrame;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * One console at a time (#892): asking for the console while one is open brings the open one to the
 * front instead of opening a second one, which used to take over the saved standard streams, so
 * closing handed {@code System.out} back to a console that was gone and output then landed nowhere
 * visible
 */
class ASecondConsoleBringsTheOpenOneToFrontUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a second Console click leaves one console open, and closing it restores System.out")
	void aSecondConsoleClick_leavesOneConsoleOpen() throws Exception
	{
		installPluginRequiringItBuilt(CONSOLE_ZIP);

		File databaseFile = new File(tempHome, "console-second-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		PrintStream outBeforeTheConsole = System.out;
		application.openPluginTool("Console", "Console");
		JInternalFrame firstConsole = application.internalFrame("Console");

		// ask for the console again while it is open
		application.openPluginTool("Console", "Console");
		robot.waitForIdle();

		long consoleFrames = GuiActionRunner.execute(() -> java.util.Arrays
			.stream(MysticCryptApplicationFrame.getInstance().getDesktopPanePanel().getDesktopPane()
				.getAllFrames())
			.filter(internalFrame -> "Console".equals(internalFrame.getTitle())).count());
		assertEquals(1, consoleFrames, "one console, the open one brought to the front");
		assertSame(firstConsole, application.internalFrame("Console"),
			"the open console is still the same window");

		// closing the one console hands the streams back to the state before it opened
		GuiActionRunner.execute(() -> firstConsole.doDefaultCloseAction());
		robot.waitForIdle();
		assertSame(outBeforeTheConsole, System.out,
			"System.out is the stream from before the console opened");
	}
}
