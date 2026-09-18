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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.io.File;
import java.io.PrintStream;
import java.util.logging.Logger;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
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
 * Functional end-to-end test of the console plugin: loads the plugin from its zip, opens the
 * "Console" tool from the Plugins menu, and verifies it opens docked at its configured share of the
 * desktop height rather than getting packed down to nothing, that it captures standard output (a
 * marker line printed to {@code System.out} must appear in its text area), and that a
 * {@link Logger} call reaches it too - {@code java.util.logging}'s default handler otherwise keeps
 * writing to the stream it captured at JVM bootstrap, never the redirected one (#133)
 */
class ConsolePluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String MARKER = "console-plugin-e2e-marker-line";
	private static final String LOGGER_MARKER = "console-plugin-e2e-logger-marker";

	/**
	 * The plugin's own documented default height fraction: "4 docks the console into the bottom
	 * quarter of the screen" (ConsoleSettingsContribution), and its documented minimum height
	 */
	private static final int DEFAULT_HEIGHT_DIVISOR = 4;
	private static final int MINIMUM_HEIGHT = 120;

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

			JInternalFrame consoleFrame = application.internalFrame("Console");
			int desktopHeight = GuiActionRunner.execute(() -> MysticCryptApplicationFrame
				.getInstance().getDesktopPanePanel().getDesktopPane().getHeight());
			int expectedHeight = Math.max(MINIMUM_HEIGHT, desktopHeight / DEFAULT_HEIGHT_DIVISOR);
			assertEquals(expectedHeight, consoleFrame.getHeight(),
				"the console must open at its configured docked height, not get packed down to "
					+ "its content's tiny preferred size (#133)");

			System.out.println(MARKER);

			Pause.pause(new Condition("console text area shows the printed marker")
			{
				@Override
				public boolean test()
				{
					return consoleTextContains(frame, MARKER);
				}
			}, 10000);

			assertTrue(consoleTextContains(frame, MARKER),
				"the console must capture and display standard output");

			Logger.getLogger("console-plugin-e2e").severe(LOGGER_MARKER);
			Pause.pause(new Condition("console text area shows the logged marker")
			{
				@Override
				public boolean test()
				{
					return consoleTextContains(frame, LOGGER_MARKER);
				}
			}, 10000);

			assertTrue(consoleTextContains(frame, LOGGER_MARKER),
				"a java.util.logging.Logger call must reach the console too, not only raw "
					+ "System.out/err writes (#133)");
		}
		finally
		{
			System.setOut(originalOut);
		}
	}

	/**
	 * The docked bounds must be right from the console's very first frame on screen, not just
	 * eventually.
	 * <p>
	 * {@code JInternalFrameExtensions.addComponentToFrame()} calls {@code JInternalFrame.pack()},
	 * which resizes the frame to its content's tiny preferred size - if that runs after
	 * {@link io.github.astrapi69.mystic.crypt.plugin.console.ConsoleDock#dock} has already set the
	 * docked bounds, it silently overwrites them. A desktop-resize listener the menu contribution
	 * also registers happens to re-dock correctly a moment later, which is exactly why a test that
	 * waits before measuring (the other test in this class included, deliberately left as-is - it
	 * still proves the EVENTUAL state is right) cannot tell the clobber from a correct first frame:
	 * both converge to the same numbers a few hundred milliseconds later. Reading the bounds inside
	 * the SAME EDT dispatch as the menu click, before any later event can run, is what actually
	 * catches it (#133)
	 */
	@Test
	@DisplayName("the console docks at its configured height on its very first frame, not only eventually")
	void theConsoleDocksCorrectlyOnItsFirstFrame() throws Exception
	{
		installPluginRequiringItBuilt(CONSOLE_ZIP);
		File databaseFile = new File(tempHome, "console-first-frame-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		JMenuItem consoleMenuItem = robot.finder()
			.find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class, false)
			{
				@Override
				protected boolean isMatching(JMenuItem candidate)
				{
					return !(candidate instanceof javax.swing.JMenu)
						&& "Console".equals(candidate.getText());
				}
			});

		int[] bounds = GuiActionRunner.execute(() -> {
			// synchronous: doClick() runs the whole menu action, including dock(), to completion
			// before this lambda continues - nothing else can run on the EDT in between
			consoleMenuItem.doClick();
			MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
			JDesktopPane desktopPane = instance.getDesktopPanePanel().getDesktopPane();
			JInternalFrame console = null;
			for (Component component : desktopPane.getComponents())
			{
				if (component instanceof JInternalFrame internalFrame
					&& "Console".equals(internalFrame.getTitle()))
				{
					console = internalFrame;
					break;
				}
			}
			int desktopHeight = desktopPane.getHeight();
			return new int[] { desktopHeight, console == null ? -1 : console.getWidth(),
					console == null ? -1 : console.getHeight() };
		});
		int desktopHeight = bounds[0];
		int consoleWidth = bounds[1];
		int consoleHeight = bounds[2];

		assertTrue(consoleWidth >= 0,
			"the console must already be on the desktop pane on its first frame");
		int expectedHeight = Math.max(120, desktopHeight / 4);
		assertEquals(desktopPaneWidth(), consoleWidth,
			"the console must already span the full desktop width on its first frame");
		assertEquals(expectedHeight, consoleHeight,
			"the console's first frame must already be docked at its configured height ("
				+ expectedHeight + " px), not packed down to its content's tiny preferred size ("
				+ consoleHeight + " px) - see the class javadoc for why a delayed measurement "
				+ "cannot tell these apart");
	}

	private int desktopPaneWidth()
	{
		return GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.getDesktopPanePanel().getDesktopPane().getWidth());
	}

	private boolean consoleTextContains(FrameFixture frame, String marker)
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
								&& candidate.getText().contains(marker);
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
