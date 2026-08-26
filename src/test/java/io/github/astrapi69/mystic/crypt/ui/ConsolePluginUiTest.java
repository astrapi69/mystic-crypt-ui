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
import java.io.PrintStream;

import javax.swing.JTextArea;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the console plugin: loads the plugin from its zip, opens the
 * "Console" tool from the Plugins menu, and verifies the console captures standard output - after
 * the console is open a marker line printed to {@code System.out} must appear in the console's text
 * area
 */
class ConsolePluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String MARKER = "console-plugin-e2e-marker-line";

	@Test
	void consoleCapturesStandardOutputThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(CONSOLE_ZIP);

		File databaseFile = new File(tempHome, "console-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		PrintStream originalOut = System.out;
		try
		{
			// opening the console redirects System.out into its text area
			application.openPluginTool("Console", "Console");
			System.out.println(MARKER);

			Pause.pause(new Condition("console text area shows the printed marker")
			{
				@Override
				public boolean test()
				{
					return consoleTextContainsMarker(frame);
				}
			}, 10000);

			assertTrue(consoleTextContainsMarker(frame),
				"the console must capture and display standard output");
		}
		finally
		{
			System.setOut(originalOut);
		}
	}

	private boolean consoleTextContainsMarker(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> {
			try
			{
				JTextArea consoleArea = frame.robot().finder()
					.find(new GenericTypeMatcher<JTextArea>(JTextArea.class)
					{
						@Override
						protected boolean isMatching(JTextArea candidate)
						{
							return candidate.getText() != null
								&& candidate.getText().contains(MARKER);
						}
					});
				return consoleArea != null;
			}
			catch (org.assertj.swing.exception.ComponentLookupException notFound)
			{
				return false;
			}
		});
	}
}
