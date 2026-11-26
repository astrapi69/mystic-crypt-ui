/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JInternalFrame;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.ui.icon.ApplicationIcon;
import io.github.astrapi69.mystic.crypt.ui.icon.XWindowIcons;

/**
 * Every window of the application shows the application's icon, the key monogram (#815), read where
 * the user sees it: on the X server for the windows the window manager draws, and as the frame icon
 * for the internal frames, which the look and feel draws.
 * <p>
 * Measured on 2026-11-17 before this change: the main window carried one scaled image, the dialogs
 * of the sign-in phase showed the default icon because the main window had none yet at that time,
 * and the internal frames carried the look and feel's default frame icon. The dialogs without a
 * parent are covered in {@code ApplicationIconTest}, since a UI test here fails on any such dialog
 * (#63).
 */
class TheApplicationIconIsOnEveryWindowUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the sign-in dialog, the first window of a start, shows the application icon")
	void theSignInDialogShowsTheIcon() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();

		assertArrayEquals(sixteenPixels(), XWindowIcons.sixteenBySixteen("Enter your credentials"),
			"the sign-in dialog takes the icon of the main window, which has it before the dialog "
				+ "opens");
		signIn.cancel();
	}

	@Test
	@DisplayName("a dialog of the sign-in phase, Create your master key, shows the application icon")
	void aDialogOfTheSignInPhaseShowsTheIcon() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn
			.startNewDatabase(new File(tempHome, "icon-e2e-new-database.mcrdb"));

		assertArrayEquals(sixteenPixels(), XWindowIcons.sixteenBySixteen("Create your master key"));
		createMasterKey.cancelAndAwaitClose();
		signIn.cancel();
	}

	@Test
	@DisplayName("the main window carries the icon in 16, 32, 48 and 256 px and shows it")
	void theMainWindowCarriesEverySize() throws Exception
	{
		File databaseFile = new File(tempHome, "icon-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		List<Integer> sizes = GuiActionRunner.execute(
			() -> frame.getIconImages().stream().map(image -> image.getWidth(null)).toList());
		String title = GuiActionRunner.execute(frame::getTitle);

		assertEquals(List.of(16, 32, 48, 256), sizes,
			"every size, so the window manager picks one instead of scaling a single image");
		assertArrayEquals(sixteenPixels(), XWindowIcons.sixteenBySixteen(title));
	}

	@Test
	@DisplayName("every internal frame, the database view's and a plugin's, carries the application icon")
	void everyInternalFrameCarriesTheIcon() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File databaseFile = new File(tempHome, "icon-e2e-frames-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.openPluginTool("Verify Checksum", "Verify Checksum");

		List<String> withAnotherIcon = GuiActionRunner.execute(() -> {
			Icon expected = ApplicationIcon.frameIcon();
			return Arrays
				.stream(MysticCryptApplicationFrame.getInstance().getDesktopPanePanel()
					.getDesktopPane().getAllFrames())
				.filter(frame -> frame.getFrameIcon() != expected).map(JInternalFrame::getTitle)
				.toList();
		});
		List<String> titles = GuiActionRunner.execute(
			() -> Arrays.stream(MysticCryptApplicationFrame.getInstance().getDesktopPanePanel()
				.getDesktopPane().getAllFrames()).map(JInternalFrame::getTitle).toList());

		assertTrue(titles.contains("Key database") && titles.contains("Verify Checksum"),
			"the database view and the plugin's window are open: " + titles);
		assertEquals(List.of(), withAnotherIcon, "internal frames without the application icon");
	}

	private static int[] sixteenPixels()
	{
		return ((BufferedImage)ApplicationIcon.images().get(0)).getRGB(0, 0, 16, 16, null, 0, 16);
	}
}
