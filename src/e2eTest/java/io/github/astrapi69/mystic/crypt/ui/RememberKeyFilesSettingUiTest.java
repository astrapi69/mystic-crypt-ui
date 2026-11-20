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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * The setting "Remember key files" (#997): switched off in the settings dialog, no key file path
 * stays on disk, and the sign-in dialog after a restart offers none, while it still offers the
 * database signed in to last.
 */
class RememberKeyFilesSettingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("switching the setting off removes the key file paths at once, and a restart offers none")
	void switchingRememberKeyFilesOff_removesTheKeyFilePaths_andARestartOffersNone()
		throws Exception
	{
		File database = new File(tempHome, "remember.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
			.createKeyFile("remember-key.pem").requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		ApplicationSteps application = new ApplicationSteps(robot);
		application.awaitSignedIn();
		File configurationDirectory = MysticCryptApplicationFrame.getInstance()
			.getConfigurationDirectory();
		File remembered = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(Files.readString(remembered.toPath()).contains("remember-key.pem"),
			"the premise: by default the key file is remembered");

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		assertTrue(
			GuiActionRunner
				.execute(() -> settings.checkBox("chkRememberKeyFiles").target().isSelected()),
			"on by default, as before");
		settings.checkBox("chkRememberKeyFiles").uncheck();
		settings.button("btnCloseSettings").click();
		robot.waitForIdle();

		assertFalse(MysticCryptSettings.load(configurationDirectory).isRememberKeyFiles());
		assertFalse(Files.readString(remembered.toPath()).contains("remember-key.pem"),
			"no key file path stays in the remembered sign-in");
		shutdownApplication();

		SignInDialogSteps afterRestart = launchApplication();
		String offered = afterRestart.selectedKeyFile();
		assertTrue(offered == null || offered.isEmpty(),
			"no key file is offered after a restart: " + offered);
		afterRestart.requireSelectedApplicationFile(database.getAbsolutePath());
	}
}
