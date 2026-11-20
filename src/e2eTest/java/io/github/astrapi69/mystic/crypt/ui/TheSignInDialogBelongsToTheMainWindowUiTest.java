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
import java.util.concurrent.TimeUnit;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * Who owns the sign-in dialog and where it opens (#63).
 * <p>
 * The dialog belongs to the main window; built without an owner it belonged to Swing's shared,
 * invisible owner frame. At the start the main window is not shown yet, so the dialog opens centred
 * on the screen; when "Open Database File" shows it again the main window is visible and the dialog
 * opens centred over it. The display of the tests has one screen, so the second case moves the main
 * window far from the middle of it.
 */
class TheSignInDialogBelongsToTheMainWindowUiTest extends AbstractUiTest
{

	/** How far a centre may be off, for the window manager's frame decorations */
	private static final int TOLERANCE = 40;

	@Test
	@DisplayName("at the start the sign-in dialog belongs to the main window and is centred on the screen")
	void atTheStart_theDialogBelongsToTheMainWindow_andIsCentredOnTheScreen()
	{
		SignInDialogSteps signIn = launchApplication();
		robot.waitForIdle();
		Window dialog = signIn.dialog().target();

		assertOwnerIs(dialog);
		Point screenCentre = screenCentreOf(dialog);
		Point dialogCentre = centreOf(dialog);
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
		UiTestSpeed.awaitWindowPlaced(mainWindow);
		Point windowCentre = centreOf(mainWindow);
		Point screenCentre = screenCentreOf(mainWindow);
		assertTrue(Math.abs(windowCentre.x - screenCentre.x) > 3 * TOLERANCE,
			"precondition: the main window is far from the middle of the screen, window "
				+ windowCentre + ", screen " + screenCentre);

		application.fireMenuItem(MenuId.OPEN_DATABASE_FILE.propertiesKey());
		SignInDialogSteps signIn = awaitSignInDialog();
		Window dialog = signIn.dialog().target();
		UiTestSpeed.awaitWindowPlaced(dialog);
		Point dialogCentre = centreOf(dialog);
		signIn.cancel();

		assertOwnerIs(dialog);
		assertTrue(near(dialogCentre, windowCentre), "the dialog is centred over the main window "
			+ windowCentre + ", at " + dialogCentre + ", not over the screen at " + screenCentre);
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

	private static Point centreOf(final Window window)
	{
		return GuiActionRunner.execute(() -> {
			Rectangle bounds = window.getBounds();
			Point onScreen = window.getLocationOnScreen();
			return new Point(onScreen.x + bounds.width / 2, onScreen.y + bounds.height / 2);
		});
	}

	private static boolean near(final Point one, final Point other)
	{
		return Math.abs(one.x - other.x) <= TOLERANCE && Math.abs(one.y - other.y) <= TOLERANCE;
	}
}
