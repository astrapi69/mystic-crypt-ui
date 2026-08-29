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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Reported: changing the key size moves the encrypt panel to the right. Measured in the real
 * window, in the internal frame the tool actually runs in.
 */
class KeygenKeySizeLayoutUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("changing the key size leaves the encrypt panel where it was")
	void changingTheKeySizeLeavesTheEncryptPanelWhereItWas() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "keysize-layout.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		int before = GuiActionRunner
			.execute(() -> frame.textBox("txtToEncrypt").target().getLocationOnScreen().x);

		GuiActionRunner.execute(
			() -> frame.comboBox("cmbKeySize").target().setSelectedItem(KeySize.KEYSIZE_32768));
		robot.waitForIdle();

		int after = GuiActionRunner
			.execute(() -> frame.textBox("txtToEncrypt").target().getLocationOnScreen().x);

		assertEquals(before, after,
			"the encrypt panel moved when the key size changed: " + before + " -> " + after);
	}

}
