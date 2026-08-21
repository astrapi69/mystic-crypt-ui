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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;

/**
 * Functional end-to-end test of the checksum plugin: loads the plugin from its zip, opens the
 * "Verify Checksum" tool from the Plugins menu, chooses SHA-256, browses to a file whose content is
 * "abc" and asserts the computed checksum equals the well-known SHA-256 digest - all through the
 * real UI
 */
class ChecksumPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "checksum-e2e-pw-123";
	private static final String SHA256_OF_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

	@Test
	void verifyChecksumComputesTheFileChecksumThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File abcFile = new File(tempHome, "abc.txt");
		Files.write(abcFile.toPath(), "abc".getBytes(StandardCharsets.UTF_8));

		File databaseFile = new File(tempHome, "checksum-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Verify Checksum", "Verify Checksum");

		// pick SHA-256 (a real change from the MD5 the combo starts on, so the change listener
		// stores it on the model before the file is chosen)
		GuiActionRunner.execute(() -> {
			@SuppressWarnings("unchecked")
			JComboBox<ChecksumAlgorithm> combo = (JComboBox<ChecksumAlgorithm>)frame
				.comboBox("cbxChecksumAlgorithm").target();
			combo.setSelectedItem(ChecksumAlgorithm.SHA_256);
		});
		robot.waitForIdle();
		UiTestSpeed.step();

		// "Open File to check" opens a modal chooser - must not block the test thread waiting on it
		SwingUtilities.invokeLater(() -> frame.button("btnOpenFile").target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(abcFile);
			fileChooser.approveSelection();
		});

		Pause.pause(new Condition("checksum computed")
		{
			@Override
			public boolean test()
			{
				return !GuiActionRunner
					.execute(() -> frame.textBox("txtGeneratedChecksum").target().getText())
					.isBlank();
			}
		}, 10000);

		String checksum = GuiActionRunner
			.execute(() -> frame.textBox("txtGeneratedChecksum").target().getText());
		assertEquals(SHA256_OF_ABC, checksum.trim(),
			"the tool must compute the SHA-256 of the file content \"abc\"");
	}
}
