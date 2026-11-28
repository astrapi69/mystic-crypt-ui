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
import java.awt.Window;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.ui.screen.ScreenPlacement;

/**
 * Who owns the sign-in dialog and where it opens (#63).
 * <p>
 * The dialog belongs to the main window; built without an owner it belonged to Swing's shared,
 * invisible owner frame. At the start the main window is not shown yet, so the dialog opens centred
 * on the screen; when "Open Database File" shows it again the main window is visible and the dialog
 * opens centred over it. The display of the tests has one screen, so the second case moves the main
 * window far from the middle of it.
 * <p>
 * "Create your master key", which the sign-in opens for a new database, belongs to the main window
 * too and, as long as that is not shown, is centred on the screen as well.
 */
class TheSignInDialogBelongsToTheMainWindowUiTest extends AbstractUiTest
{

	/** How far a centre may be off, for the window manager's frame decorations */
	private static final int TOLERANCE = 40;

	/**
	 * How far a dialog may be off the place it is given: a few pixels, for the rounding of a half
	 * width. Its bounds are read once they have settled (#1131)
	 */
	private static final int PLACEMENT_TOLERANCE = 8;

	@Test
	@DisplayName("at the start the sign-in dialog belongs to the main window and is centred on the screen")
	void atTheStart_theDialogBelongsToTheMainWindow_andIsCentredOnTheScreen()
	{
		SignInDialogSteps signIn = launchApplication();
		Window dialog = signIn.dialog().target();
		Point dialogCentre = centreOf(UiTestSpeed.awaitSettled(dialog)[0]);

		assertOwnerIs(dialog);
		Point screenCentre = screenCentreOf(dialog);
		assertTrue(near(dialogCentre, screenCentre),
			"the dialog is centred on the screen " + screenCentre + ", at " + dialogCentre);
	}

	@Test
	@DisplayName("Open Database File shows the sign-in dialog of the main window, centred over the moved window")
	void onOpenDatabaseFile_theDialogIsCentredOverTheMovedMainWindow()
	{
		launchApplication().requireOkDisabled().cancel();
		Pause.pause(new Condition("the application frame finished constructing")
		{
			@Override
			public boolean test()
			{
				MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
				return frame != null && frame.getModelObject() != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
		awaitApplicationInitialized();
		ApplicationSteps application = new ApplicationSteps(robot);
		application.showMainFrame();
		MysticCryptApplicationFrame mainWindow = MysticCryptApplicationFrame.getInstance();
		Rectangle screen = GuiActionRunner
			.execute(() -> mainWindow.getGraphicsConfiguration().getBounds());
		GuiActionRunner.execute(() -> mainWindow.setBounds(screen.x + screen.width - 960,
			screen.y + screen.height - 560, 920, 520));
		Point movedCentre = centreOf(UiTestSpeed.awaitSettled(mainWindow)[0]);
		Point screenCentre = screenCentreOf(mainWindow);
		assertTrue(Math.abs(movedCentre.x - screenCentre.x) > 3 * TOLERANCE,
			"precondition: the main window is far from the middle of the screen, window "
				+ movedCentre + ", screen " + screenCentre);

		application.fireMenuItem(MenuId.OPEN_DATABASE_FILE.propertiesKey());
		SignInDialogSteps signIn = awaitSignInDialog();
		Window dialog = signIn.dialog().target();
		Rectangle[] settled = UiTestSpeed.awaitSettled(dialog, mainWindow);
		Point dialogCentre = centreOf(settled[0]);
		Point windowCentre = centreOf(settled[1]);
		signIn.cancel();

		assertOwnerIs(dialog);
		assertTrue(near(dialogCentre, windowCentre), "the dialog is centred over the main window "
			+ windowCentre + ", at " + dialogCentre + ", not over the screen at " + screenCentre);
	}

	/**
	 * "Create your master key", opened from the sign-in for a new database, is centred on the
	 * screen: the main window it belongs to is not shown yet. It used to centre itself in its
	 * constructor, while it was still 0 by 0 pixels, and was given its size only afterwards, so its
	 * upper left corner landed on the middle of the screen
	 */
	@Test
	@DisplayName("Create your master key, opened from the sign-in, is centred on the screen")
	void createYourMasterKey_isCentredOnTheScreen()
	{
		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn
			.startNewDatabase(new File(tempHome, "master-key-placement.mcrdb"));
		Window dialog = createMasterKey.dialog().target();
		Rectangle settled = UiTestSpeed.awaitSettled(dialog)[0];
		Point dialogCentre = centreOf(settled);
		Point usableCentre = usableCentreOf(dialog);
		createMasterKey.cancelAndAwaitClose();
		signIn.cancel();

		assertTrue(near(dialogCentre, usableCentre, PLACEMENT_TOLERANCE),
			"the dialog " + settled + " is centred on the screen's usable area: its centre "
				+ dialogCentre + ", the area's " + usableCentre);
	}

	private static Point usableCentreOf(final Window window)
	{
		Rectangle usable = GuiActionRunner
			.execute(() -> ScreenPlacement.usableBoundsOf(window.getGraphicsConfiguration()));
		return new Point(usable.x + usable.width / 2, usable.y + usable.height / 2);
	}

	private static void assertOwnerIs(final Window dialog)
	{
		String ownerClass = GuiActionRunner.execute(() -> dialog.getOwner().getClass().getName());
		assertTrue(MysticCryptApplicationFrame.class.getName().equals(ownerClass),
			"the owner of the sign-in dialog is the main window, not " + ownerClass);
	}

	private static Point screenCentreOf(final Window window)
	{
		Rectangle screen = GuiActionRunner
			.execute(() -> window.getGraphicsConfiguration().getBounds());
		return new Point((int)screen.getCenterX(), (int)screen.getCenterY());
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
}
