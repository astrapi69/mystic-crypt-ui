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

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The half of the checksum tool that was missing: it could say whether a file matched a published
 * checksum and it could show a hash, but it could not write the artifact people exchange (#296).
 * <p>
 * Its own class rather than a third test in {@link ChecksumAndMacUiTest}: {@code forkEvery = 1}
 * forks per CLASS, so tests in one class share a JVM - and each of these opens the same tool
 * window, which makes a lookup by component name ambiguous once a second one is on screen.
 */
class ChecksumFileWritingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@org.junit.jupiter.api.DisplayName("saving writes the checksum file sha256sum -c reads")
	void savesTheChecksumNextToTheFileItDescribes() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);

		File databaseFile = new File(tempHome, "checksum-save.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File described = new File(tempHome, "download.bin");
		java.nio.file.Files.writeString(described.toPath(), "the bytes someone published");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Checksum and MAC", "Checksum and MAC");

		GuiActionRunner.execute(() -> {
			frame.comboBox("cmbDigest").target().setSelectedItem("SHA-256");
			frame.textBox("txtChecksumFile").target().setText(described.getAbsolutePath());
			frame.checkBox("chkChecksumUseFile").target().setSelected(true);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnChecksum").target().doClick());
		robot.waitForIdle();
		String computed = GuiActionRunner
			.execute(() -> frame.textBox("txtChecksum").target().getText());

		GuiActionRunner.execute(() -> frame.button("btnSaveChecksum").target().doClick());
		robot.waitForIdle();

		File checksumFile = new File(tempHome, "download.bin.sha256");
		assertTrue(checksumFile.isFile(),
			"the checksum file has to be next to the file it describes: " + result(frame));
		assertEquals(computed + "  download.bin\n",
			java.nio.file.Files.readString(checksumFile.toPath()),
			"and it has to be the coreutils form, or sha256sum -c cannot read it");

		// the tool window is closed again: forkEvery = 1 forks per class, so the next test in this
		// class opens the same window into the same JVM, and two of them make every lookup by
		// component name ambiguous
		application.closeInternalFrame("Checksum and MAC");
	}

	@Test
	@org.junit.jupiter.api.DisplayName("a checksum over typed text is refused, with the reason")
	void refusesToSaveAChecksumThatNamesNoFile() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);

		File databaseFile = new File(tempHome, "checksum-refuse.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Checksum and MAC", "Checksum and MAC");

		GuiActionRunner.execute(() -> frame.textBox("txtChecksumText").target().setText("a text"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnChecksum").target().doClick());
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnSaveChecksum").target().doClick());
		robot.waitForIdle();

		assertTrue(result(frame).contains("use the file instead of the text"),
			"the refusal has to say what to change - a checksum file names a file, and typed "
				+ "text is not one. Read: " + result(frame));

		application.closeInternalFrame("Checksum and MAC");
	}

	@Test
	@org.junit.jupiter.api.DisplayName("an existing checksum file is kept when the question is answered with no")
	void answeringNoKeepsTheChecksumFileThatIsAlreadyThere() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);

		File databaseFile = new File(tempHome, "checksum-keep.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File described = new File(tempHome, "download.bin");
		java.nio.file.Files.writeString(described.toPath(), "the bytes someone published");
		File checksumFile = new File(tempHome, "download.bin.sha256");
		java.nio.file.Files.writeString(checksumFile.toPath(), "an older value  download.bin\n");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Checksum and MAC", "Checksum and MAC");
		computeChecksumOfTheFile(frame, described);

		answerTheReplaceQuestion(frame, javax.swing.JOptionPane.NO_OPTION);

		assertEquals("an older value  download.bin\n",
			java.nio.file.Files.readString(checksumFile.toPath()),
			"answering no has to leave the published value alone - it is not recoverable "
				+ "afterwards. Read: " + result(frame));
		application.closeInternalFrame("Checksum and MAC");
	}

	@Test
	@org.junit.jupiter.api.DisplayName("answering yes replaces it with the value just computed")
	void answeringYesReplacesTheChecksumFile() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);

		File databaseFile = new File(tempHome, "checksum-replace.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File described = new File(tempHome, "download.bin");
		java.nio.file.Files.writeString(described.toPath(), "the bytes someone published");
		File checksumFile = new File(tempHome, "download.bin.sha256");
		java.nio.file.Files.writeString(checksumFile.toPath(), "an older value  download.bin\n");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Checksum and MAC", "Checksum and MAC");
		String computed = computeChecksumOfTheFile(frame, described);

		answerTheReplaceQuestion(frame, javax.swing.JOptionPane.YES_OPTION);

		assertEquals(computed + "  download.bin\n",
			java.nio.file.Files.readString(checksumFile.toPath()),
			"and answering yes writes the value the window showed: " + result(frame));
		application.closeInternalFrame("Checksum and MAC");
	}

	/** Picks the file, ticks the checkbox and computes, returning what the window shows */
	private String computeChecksumOfTheFile(FrameFixture frame, File described)
	{
		GuiActionRunner.execute(() -> {
			frame.comboBox("cmbDigest").target().setSelectedItem("SHA-256");
			frame.textBox("txtChecksumFile").target().setText(described.getAbsolutePath());
			frame.checkBox("chkChecksumUseFile").target().setSelected(true);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnChecksum").target().doClick());
		robot.waitForIdle();
		return GuiActionRunner.execute(() -> frame.textBox("txtChecksum").target().getText());
	}

	/**
	 * Presses "Save checksum" and answers the modal question with the given option pane value.
	 * <p>
	 * The click is posted rather than executed: the handler opens a MODAL dialog on the event
	 * thread, so waiting for the click to return would wait for the dialog that is not answered
	 * yet. The answer is given through the option pane's own value rather than by pressing a
	 * button, because the button labels follow the locale
	 */
	private void answerTheReplaceQuestion(FrameFixture frame, int optionPaneValue)
	{
		javax.swing.SwingUtilities
			.invokeLater(() -> frame.button("btnSaveChecksum").target().doClick());
		org.assertj.swing.fixture.DialogFixture question = org.assertj.swing.finder.WindowFinder
			.findDialog(new org.assertj.swing.core.GenericTypeMatcher<java.awt.Dialog>(
				java.awt.Dialog.class)
			{
				@Override
				protected boolean isMatching(java.awt.Dialog dialog)
				{
					return "Replace the checksum file?".equals(dialog.getTitle())
						&& dialog.isShowing();
				}
			}).withTimeout(15, java.util.concurrent.TimeUnit.SECONDS).using(robot);
		GuiActionRunner.execute(() -> {
			javax.swing.JOptionPane optionPane = (javax.swing.JOptionPane)robot.finder()
				.findByType(question.target(), javax.swing.JOptionPane.class);
			optionPane.setValue(optionPaneValue);
		});
		robot.waitForIdle();
	}

	private static String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
