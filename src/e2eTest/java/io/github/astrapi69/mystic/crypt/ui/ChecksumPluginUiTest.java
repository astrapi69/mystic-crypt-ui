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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
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
import org.assertj.swing.fixture.JInternalFrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the checksum plugin: loads the plugin from its zip, opens the
 * "Verify Checksum" tool from the Plugins menu, chooses SHA-256, browses to a file whose content is
 * "abc" and asserts the computed checksum equals the well-known SHA-256 digest, then copies it and
 * checks the real system clipboard (#120) - all through the real UI
 */
class ChecksumPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String SHA256_OF_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

	@Test
	@DisplayName("changing the algorithm clears the loaded checksum file and loads the matching one")
	void changingTheAlgorithmDoesNotKeepTheOldChecksumFile() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File abcFile = new File(tempHome, "abc.txt");
		Files.write(abcFile.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
		Files.write(new File(tempHome, "abc.txt.sha256").toPath(),
			SHA256_OF_ABC.getBytes(StandardCharsets.UTF_8));

		File databaseFile = new File(tempHome, "checksum-algorithm-switch.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Verify Checksum", "Verify Checksum");
		// the other test in this class opens the same tool, and forkEvery = 1 forks per CLASS, so
		// a frame-wide lookup finds two panels of the same name - scope it to this tool's window
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Verify Checksum"));

		chooseAlgorithm(tool, ChecksumAlgorithm.SHA_256);
		chooseFileToCheck(tool, abcFile);
		Pause.pause(new Condition("the sibling checksum file for SHA-256 is loaded")
		{
			@Override
			public boolean test()
			{
				return SHA256_OF_ABC.equals(textOf(tool, "txtOwnersChecksum").trim());
			}
		}, 10000);
		assertEquals("abc.txt.sha256", textOf(tool, "txtChecksumFile"),
			"precondition: the checksum file that belongs to SHA-256 is the one loaded");

		chooseAlgorithm(tool, ChecksumAlgorithm.SHA_512);

		assertTrue(textOf(tool, "txtChecksumFile").isBlank(),
			"the checksum file of the algorithm just left must not stay in the field - there is no "
				+ "sha512 file next to abc.txt, so nothing takes its place");
		assertTrue(textOf(tool, "txtOwnersChecksum").isBlank(),
			"and neither may its checksum, or the comparison runs across two algorithms and calls "
				+ "an intact file corrupt");
	}

	private static String textOf(final JInternalFrameFixture tool, final String componentName)
	{
		return GuiActionRunner.execute(() -> tool.textBox(componentName).target().getText());
	}

	private void chooseAlgorithm(final JInternalFrameFixture tool,
		final ChecksumAlgorithm algorithm)
	{
		GuiActionRunner.execute(() -> {
			@SuppressWarnings("unchecked")
			JComboBox<ChecksumAlgorithm> combo = (JComboBox<ChecksumAlgorithm>)tool
				.comboBox("cbxChecksumAlgorithm").target();
			combo.setSelectedItem(algorithm);
		});
		robot.waitForIdle();
		UiTestSpeed.step();
	}

	private void chooseFileToCheck(final JInternalFrameFixture tool, final File file)
	{
		SwingUtilities.invokeLater(() -> tool.button("btnOpenFile").target().doClick());
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

		// Copy the generated checksum (#120): the button next to it must put exactly that text on
		// the real system clipboard - not a mock, the same clipboard a paste anywhere else on the
		// machine would read from
		GuiActionRunner.execute(() -> frame.button("btnCopyGeneratedChecksum").target().doClick());
		robot.waitForIdle();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		String clipboardContent = (String)clipboard.getData(DataFlavor.stringFlavor);
		assertEquals(checksum, clipboardContent,
			"the clipboard must hold exactly what the field showed, not something else");
	}
}
