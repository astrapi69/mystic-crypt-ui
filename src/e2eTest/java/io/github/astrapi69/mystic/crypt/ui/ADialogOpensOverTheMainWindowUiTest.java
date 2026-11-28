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

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.SearchApplicationFileAction;
import io.github.astrapi69.mystic.crypt.panel.signin.NewMasterPwFilePanel;

/**
 * Where the dialogs of the application open when the main window is not in the middle of the screen
 * (#63).
 * <p>
 * With more than one monitor the application can sit on the second screen while its dialogs open on
 * the first. The display of the tests has one screen, so the test moves the main window far from
 * its middle instead: a dialog that belongs to the window opens centred over the window, and one
 * that is centred on the screen opens over the middle of the screen.
 */
class ADialogOpensOverTheMainWindowUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** How far a centre may be off, for the window manager's frame decorations */
	private static final int TOLERANCE = 40;

	/**
	 * How far the password generator may be off the place it is given: a few pixels, for the
	 * rounding of a half width. Its bounds are read once they have settled (#1131)
	 */
	private static final int GENERATOR_TOLERANCE = 8;

	@Test
	@DisplayName("the New node dialog opens centred over the moved main window, not over the screen")
	void theNewNodeDialog_opensOverTheMainWindow() throws Exception
	{
		MovedMainWindow moved = signInAndMoveTheMainWindowAway("dialog-placement.mcrdb");

		moved.application().openNewNodeDialog(moved.frame());
		DialogFixture newNode = moved.application().findDialogWithTitle("New node");
		Rectangle[] settled = UiTestSpeed.awaitSettled(newNode.target(), moved.window());
		Point dialogCentre = centreOf(settled[0]);
		Point windowCentre = centreOf(settled[1]);
		newNode.close();

		assertTrue(near(dialogCentre, windowCentre),
			"the dialog is centred over the main window " + windowCentre + ", at " + dialogCentre
				+ ", not over the screen at " + moved.screenCentre());
	}

	@Test
	@DisplayName("the Search dialog opens centred over the moved main window, not over the screen")
	void theSearchDialog_opensOverTheMainWindow() throws Exception
	{
		MovedMainWindow moved = signInAndMoveTheMainWindowAway("search-placement.mcrdb");

		moved.application().fireMenuItem(MenuId.SEARCH.propertiesKey());
		DialogFixture search = moved.application()
			.findDialogWithTitle(SearchApplicationFileAction.dialogTitle());
		Rectangle[] settled = UiTestSpeed.awaitSettled(search.target(), moved.window());
		Point dialogCentre = centreOf(settled[0]);
		Point windowCentre = centreOf(settled[1]);
		Point screenCentre = moved.screenCentre();
		search.close();

		assertTrue(near(dialogCentre, windowCentre),
			"the Search dialog is centred over the main window " + windowCentre + ", at "
				+ dialogCentre + ", not over the screen at " + screenCentre);
	}

	/**
	 * The password generator of a new entry opens centred over the main window it belongs to. It
	 * used to centre itself in its constructor, while it was still 0 by 0 pixels, and was given its
	 * size only afterwards, so its upper left corner landed on the middle of the screen
	 */
	@Test
	@DisplayName("the password generator of a new entry opens centred over the moved main window")
	void theGeneratorOfANewEntry_opensCentredOverTheMainWindow() throws Exception
	{
		MovedMainWindow moved = signInAndMoveTheMainWindowAway("generator-placement.mcrdb");

		Rectangle[] settled = openTheGeneratorOfANewEntry(moved);
		Point generatorCentre = centreOf(settled[0]);
		Point windowCentre = centreOf(settled[1]);

		assertTrue(near(generatorCentre, windowCentre, GENERATOR_TOLERANCE),
			"the generator " + settled[0] + " is centred over the main window " + settled[1]
				+ ": its centre " + generatorCentre + ", the window's " + windowCentre
				+ ", the screen's " + moved.screenCentre());
	}

	/**
	 * Opens the "New Crypt Entry" dialog on the first tree node, clicks its Generate button and
	 * reads where the password generator and the main window are once both have settled
	 * ({@link UiTestSpeed#awaitSettled}, #1131); closes both dialogs again
	 *
	 * @param moved
	 *            the moved main window
	 * @return the settled bounds of the generator and of the main window, in this order
	 */
	private Rectangle[] openTheGeneratorOfANewEntry(final MovedMainWindow moved)
	{
		moved.application().selectTreeRow(moved.frame(), 0);
		DialogFixture newEntry = moved.application().openTheNewEntryDialog(moved.frame());
		SwingUtilities.invokeLater(() -> newEntry.button("btnGeneratePassword").target().doClick());
		DialogFixture generator = moved.application().findDialogWithTitle("Generate Password");
		Rectangle[] settled = UiTestSpeed.awaitSettled(generator.target(), moved.window());
		generator.close();
		newEntry.close();
		return settled;
	}

	/**
	 * The offline help of the sign-in panels, shown when the wiki cannot be reached, was sized and
	 * never placed: its upper left corner landed on the middle of the screen, far from the main
	 * window it belongs to
	 */
	@Test
	@DisplayName("the offline help of the sign-in opens centred over the moved main window")
	void theOfflineHelp_opensCentredOverTheMainWindow() throws Exception
	{
		MovedMainWindow moved = signInAndMoveTheMainWindowAway("help-placement.mcrdb");
		JComponent panel = GuiActionRunner.execute(() -> new NewMasterPwFilePanel()
		{
			@Override
			protected boolean isHelpPageReachable(final String helpLink)
			{
				return false;
			}
		});
		JButton help = HelpFallbackDialogsUiTest.helpButton(panel);
		SwingUtilities.invokeLater(help::doClick);
		DialogFixture helpDialog = moved.application()
			.findDialogWithTitle(Messages.getString("signin.help.create.dialog.title", ""));
		Rectangle[] settled = UiTestSpeed.awaitSettled(helpDialog.target(), moved.window());
		Point dialogCentre = centreOf(settled[0]);
		Point windowCentre = centreOf(settled[1]);
		helpDialog.close();

		assertTrue(near(dialogCentre, windowCentre),
			"the offline help is centred over the main window " + windowCentre + ", at "
				+ dialogCentre + ", not over the screen at " + moved.screenCentre());
	}

	/**
	 * Signs in to a new database and moves the main window to the lower right of the screen, far
	 * from its middle
	 *
	 * @param databaseName
	 *            the name of the database file in the test's home
	 * @return the steps, the window and the centre of the screen
	 * @throws Exception
	 *             if the database cannot be created or the sign-in fails
	 */
	private MovedMainWindow signInAndMoveTheMainWindowAway(final String databaseName)
		throws Exception
	{
		File databaseFile = new File(tempHome, databaseName);
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		MysticCryptApplicationFrame mainWindow = MysticCryptApplicationFrame.getInstance();
		Rectangle screen = GuiActionRunner
			.execute(() -> mainWindow.getGraphicsConfiguration().getBounds());
		GuiActionRunner.execute(() -> mainWindow.setBounds(screen.x + screen.width - 760,
			screen.y + screen.height - 560, 720, 520));
		Point movedCentre = centreOf(UiTestSpeed.awaitSettled(mainWindow)[0]);
		Point screenCentre = new Point((int)screen.getCenterX(), (int)screen.getCenterY());
		assertTrue(Math.abs(movedCentre.x - screenCentre.x) > 3 * TOLERANCE,
			"precondition: the main window is far from the middle of the screen, window "
				+ movedCentre + ", screen " + screenCentre);
		return new MovedMainWindow(application, frame, mainWindow, screenCentre);
	}

	private static Point centreOf(final Rectangle boundsOnScreen)
	{
		return new Point(boundsOnScreen.x + boundsOnScreen.width / 2,
			boundsOnScreen.y + boundsOnScreen.height / 2);
	}

	private static boolean near(final Point one, final Point other)
	{
		return near(one, other, TOLERANCE);
	}

	private static boolean near(final Point one, final Point other, final int tolerance)
	{
		return Math.abs(one.x - other.x) <= tolerance && Math.abs(one.y - other.y) <= tolerance;
	}

	/**
	 * The main window, moved away from the middle of the screen
	 *
	 * @param application
	 *            the steps of the signed-in application
	 * @param frame
	 *            the fixture of the main window
	 * @param window
	 *            the main window
	 * @param screenCentre
	 *            the centre of the screen it is on
	 */
	private record MovedMainWindow(ApplicationSteps application, FrameFixture frame,
		MysticCryptApplicationFrame window, Point screenCentre) {
	}
}
