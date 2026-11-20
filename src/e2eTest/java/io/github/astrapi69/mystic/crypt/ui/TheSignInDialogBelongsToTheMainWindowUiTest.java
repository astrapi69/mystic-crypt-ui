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
 * Measured on develop a80d9c54: the dialog is placed before it gets its size, so what sits at the
 * centre of the screen is its upper left corner, not its middle.
 * <p>
 * A dialog built without an owner belongs to Swing's shared, invisible owner frame. At the start
 * the main window is not shown yet, so the dialog opens centred on the screen; when "Open Database
 * File" shows it again the main window is visible and the dialog opens centred over it. The display
 * of the tests has one screen, so the second case moves the main window far from the middle of it.
 */
class TheSignInDialogBelongsToTheMainWindowUiTest extends AbstractUiTest
{

	/** How far a centre may be off, for the window manager's frame decorations */
	private static final int TOLERANCE = 40;

	@Test
	@DisplayName("pin: at the start the sign-in dialog belongs to the shared owner frame, its corner sits at the screen's centre")
	void atTheStart_theDialogBelongsToTheSharedOwnerFrame_andItsCornerSitsAtTheScreenCentre()
	{
		SignInDialogSteps signIn = launchApplication();
		robot.waitForIdle();
		Window dialog = signIn.dialog().target();

		assertOwnerIs(dialog);
		Point screenCentre = screenCentreOf(dialog);
		Point dialogCorner = cornerOf(dialog);
		assertTrue(near(dialogCorner, screenCentre),
			"today the upper left corner of the dialog sits at the centre of the screen "
				+ screenCentre + ", at " + dialogCorner + ", centre of the dialog "
				+ centreOf(dialog));
	}

	@Test
	@DisplayName("pin: Open Database File places the ownerless sign-in dialog by the screen's centre, not by the moved window")
	void onOpenDatabaseFile_theOwnerlessDialogIsPlacedByTheScreenCentre()
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
		Point dialogCorner = cornerOf(dialog);
		Point dialogCentre = centreOf(dialog);
		signIn.cancel();

		assertOwnerIs(dialog);
		assertTrue(near(dialogCorner, screenCentre),
			"today the upper left corner of the dialog sits at the centre of the screen "
				+ screenCentre + ", at " + dialogCorner + ", centre of the dialog " + dialogCentre
				+ ", the main window is at " + windowCentre);
	}

	private static void assertOwnerIs(final Window dialog)
	{
		String ownerClass = GuiActionRunner.execute(() -> dialog.getOwner().getClass().getName());
		assertTrue("javax.swing.SwingUtilities$SharedOwnerFrame".equals(ownerClass),
			"the owner of the sign-in dialog is Swing's shared owner frame today, not "
				+ ownerClass);
	}

	private static Point screenCentreOf(final Window window)
	{
		Rectangle screen = GuiActionRunner
			.execute(() -> window.getGraphicsConfiguration().getBounds());
		return new Point((int)screen.getCenterX(), (int)screen.getCenterY());
	}

	private static Point cornerOf(final Window window)
	{
		return GuiActionRunner.execute(window::getLocationOnScreen);
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
