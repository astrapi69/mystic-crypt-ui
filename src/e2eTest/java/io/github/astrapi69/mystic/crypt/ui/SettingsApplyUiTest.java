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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JSpinner;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * End-to-end test of Apply in the settings dialog (#1002): a changed setting takes effect while the
 * dialog stays open, and Close afterwards changes nothing more.
 */
class SettingsApplyUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("Apply takes a changed setting in while the dialog stays open, and Close changes nothing more (#1002)")
	void apply_takesTheChangeIn_andTheDialogStaysOpen() throws Exception
	{
		File databaseFile = new File(tempHome, "apply-settings.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		int changed = saved().getAutoLockMinutes() + 3;

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		GuiActionRunner.execute(
			() -> ((JSpinner)settings.spinner("spnAutoLockMinutes").target()).setValue(changed));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> settings.button("btnApplySettings").target().doClick());
		robot.waitForIdle();

		assertEquals(changed, saved().getAutoLockMinutes(),
			"Apply did not take the changed lock time in");
		assertTrue(GuiActionRunner.execute(() -> settings.target().isShowing()),
			"Apply closed the dialog");

		String afterApply = settingsFileText();
		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();

		assertFalse(GuiActionRunner.execute(() -> settings.target().isShowing()),
			"Close left the dialog open");
		assertEquals(afterApply, settingsFileText(), "Close changed what Apply had written");
	}

	private static MysticCryptSettings saved()
	{
		return MysticCryptSettings.load(configurationDirectory());
	}

	private static String settingsFileText() throws Exception
	{
		return Files.readString(
			new File(configurationDirectory(), MysticCryptSettings.JSON_FILENAME).toPath(),
			StandardCharsets.UTF_8);
	}

	private static File configurationDirectory()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getConfigurationDirectory());
	}
}
