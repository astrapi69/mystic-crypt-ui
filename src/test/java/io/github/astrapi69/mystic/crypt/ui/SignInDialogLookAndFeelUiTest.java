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

import java.io.File;

import javax.swing.UIManager;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.swing.enumeration.FrameMode;

/**
 * The sign-in dialog used to always render with Nimbus, hardcoded in
 * {@code MysticCryptApplicationFrame.showMasterPwDialog()}, no matter what look and feel was
 * configured - not even the application's own default (FlatLaf Light), which is what the rest of
 * the app opened in one click later. This pins that the look and feel persisted in
 * {@code settings.json} is already active by the time the sign-in dialog is shown, not only after
 * signing in (#192)
 */
class SignInDialogLookAndFeelUiTest extends AbstractUiTest
{

	@Test
	void thePersistedLookAndFeelIsAlreadyActiveOnTheSignInDialog()
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		new MysticCryptSettings("FlatLaf Dark", "en", FrameMode.APPLICATION_PANEL, true)
			.save(configurationDirectory);

		DialogFixture signInDialog = launchApplication().dialog();

		String activeLookAndFeelClass = GuiActionRunner
			.execute(() -> UIManager.getLookAndFeel().getClass().getName());
		assertEquals(FlatDarkLaf.class.getName(), activeLookAndFeelClass,
			"the sign-in dialog '" + signInDialog.target().getTitle()
				+ "' must already use the persisted look and feel, not the hardcoded default");
	}
}
