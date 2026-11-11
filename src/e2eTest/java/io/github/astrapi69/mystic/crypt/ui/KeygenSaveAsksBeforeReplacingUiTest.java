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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The key generation window never costs the user a file without asking (#882): the save buttons
 * wait for a key pair, and a private key is written over a file that is there only after the
 * question, which No answers by leaving the file as it was.
 */
class KeygenSaveAsksBeforeReplacingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final byte[] WHAT_WAS_THERE = "the only copy of another private key"
		.getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("the save buttons wait for Generate, and replacing a file asks first")
	void savingWaitsForAKeyPairAndAsksBeforeReplacing() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "keygen-replace.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		frame.button(JButtonMatcher.withText("Save private key")).requireDisabled();
		frame.button(JButtonMatcher.withText("Save private key with password")).requireDisabled();
		frame.button(JButtonMatcher.withText("Save public key")).requireDisabled();

		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();
		frame.button(JButtonMatcher.withText("Save private key")).requireEnabled();

		File existing = new File(tempHome, "id_existing.pem");
		Files.write(existing.toPath(), WHAT_WAS_THERE);

		saveThePrivateKeyOver(frame, existing);
		answer(application, "No");
		assertArrayEquals(WHAT_WAS_THERE, Files.readAllBytes(existing.toPath()),
			"No leaves the file as it was");

		saveThePrivateKeyOver(frame, existing);
		answer(application, "Yes");
		org.assertj.swing.timing.Pause
			.pause(new org.assertj.swing.timing.Condition("the private key replaces the file")
			{
				@Override
				public boolean test()
				{
					try
					{
						return Files.readString(existing.toPath()).contains("PRIVATE KEY");
					}
					catch (java.io.IOException notReadableYet)
					{
						return false;
					}
				}
			}, 15000);
		assertTrue(Files.readString(existing.toPath()).contains("PRIVATE KEY"));
	}

	private void saveThePrivateKeyOver(final FrameFixture frame, final File existing)
	{
		SwingUtilities.invokeLater(
			() -> frame.button(JButtonMatcher.withText("Save private key")).target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(15, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(existing);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
	}

	/**
	 * Answers the question about the file that is there, after checking it names the file and says
	 * that a private key cannot be made again
	 */
	private void answer(final ApplicationSteps application, final String button)
	{
		DialogFixture question = application.findDialogWithTitle("Replace this key file?");
		String message = GuiActionRunner.execute(() -> String
			.valueOf(robot.finder().findByType(question.target(), JOptionPane.class).getMessage()));
		assertTrue(
			message.contains("id_existing.pem") && message.contains("nothing can produce it again"),
			"the question names the file and what is lost: " + message);
		SwingUtilities
			.invokeLater(() -> question.button(JButtonMatcher.withText(button)).target().doClick());
		org.assertj.swing.timing.Pause
			.pause(new org.assertj.swing.timing.Condition("the question is answered")
			{
				@Override
				public boolean test()
				{
					return !question.target().isShowing();
				}
			}, 10000);
	}
}
