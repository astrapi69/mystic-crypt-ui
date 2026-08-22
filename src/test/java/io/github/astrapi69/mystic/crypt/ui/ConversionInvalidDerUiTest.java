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

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * Negative-path end-to-end test of the DER-to-PEM conversion plugin: choosing a file that is not a
 * valid DER key must not produce a PEM output file - the converter logs the error and writes
 * nothing, rather than crashing. The happy path lives in {@link ConversionPluginUiTest}.
 */
class ConversionInvalidDerUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "conversion-neg-e2e-pw-123";

	@Test
	void convertingAnInvalidDerFileProducesNoPemFile() throws Exception
	{
		installPluginRequiringItBuilt(CONVERSION_ZIP);

		// a file that is definitely not a DER-encoded key
		File notDerFile = new File(tempHome, "not-a-key.der");
		Files.write(notDerFile.toPath(), "this is plainly not a DER key".getBytes());
		File pemFile = new File(tempHome, "conversion-neg-out.pem");

		File databaseFile = new File(tempHome, "conversion-neg-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Convert DER to PEM", "Convert *.der-file to *.pem-file");

		clickAndApproveInChooser(frame, "btnChoose", notDerFile);
		clickAndApproveInChooser(frame, "btnSaveTo", pemFile);
		GuiActionRunner.execute(() -> frame.button("btnConvert").target().doClick());
		robot.waitForIdle();

		assertFalse(pemFile.exists() && pemFile.length() > 0,
			"an invalid DER input must not produce a non-empty PEM file");
	}

	private void clickAndApproveInChooser(FrameFixture frame, String buttonName, File file)
	{
		SwingUtilities.invokeLater(() -> frame.button(buttonName).target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(file);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
		UiTestSpeed.step();
	}
}
