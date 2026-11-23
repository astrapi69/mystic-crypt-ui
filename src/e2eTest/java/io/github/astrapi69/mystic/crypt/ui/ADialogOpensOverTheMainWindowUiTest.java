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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

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
	@DisplayName("the Search dialog opens centred over the screen, not over the moved main window")
	void theSearchDialog_opensOverTheScreen() throws Exception
	{
		MovedMainWindow moved = signInAndMoveTheMainWindowAway("search-placement.mcrdb");

		moved.application().fireMenuItem(MenuId.SEARCH.propertiesKey());
		DialogFixture search = moved.application().findDialogWithTitle("Search");
		Rectangle[] settled = UiTestSpeed.awaitSettled(search.target(), moved.window());
		Point dialogCentre = centreOf(settled[0]);
		Point windowCentre = centreOf(settled[1]);
		Point screenCentre = moved.screenCentre();
		search.close();

		assertTrue(near(dialogCentre, screenCentre),
			"the Search dialog is centred over the screen " + screenCentre + ", at " + dialogCentre
				+ ", not over the main window at " + windowCentre);
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
		return Math.abs(one.x - other.x) <= TOLERANCE && Math.abs(one.y - other.y) <= TOLERANCE;
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
