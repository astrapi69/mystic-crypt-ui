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

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

/**
 * End-to-end file-IO test of the keygen plugin: generates an RSA key pair, presses "Save public
 * key", and through the real save dialog writes the public key to a file on disk, asserting the
 * file is actually produced.
 */
class KeygenSavePublicKeyUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "keygen-save-e2e-pw-123";

	@Test
	void savesTheGeneratedPublicKeyToAFileThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-save-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File publicKeyFile = new File(tempHome, "generated-public-key.pem");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		// generate the default RSA key pair (key size 1024 is fast)
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();

		// press "Save public key" and approve the save dialog with our target file
		SwingUtilities.invokeLater(
			() -> frame.button(JButtonMatcher.withText("Save public key")).target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(publicKeyFile);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();

		Pause.pause(new Condition("public key file written")
		{
			@Override
			public boolean test()
			{
				return publicKeyFile.exists() && publicKeyFile.length() > 0;
			}
		}, 10000);
		assertTrue(publicKeyFile.exists() && publicKeyFile.length() > 0,
			"saving the public key must produce a non-empty file on disk");
	}
}
