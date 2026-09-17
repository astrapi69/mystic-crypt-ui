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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextField;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the certificate plugin: with the plugin installed, the Plugins menu
 * gains a "Create Certificate..." item that opens the wizard dialog, and closing it leaves the
 * application running (the old in-host wizard killed the whole app with System.exit on finish)
 */
class CertificateWizardUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void certificateWizardOpensAndClosesWithoutKillingTheApp() throws Exception
	{
		installPluginRequiringItBuilt(CERTIFICATE_ZIP);

		File databaseFile = new File(tempHome, "certwizard-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openCertificateWizard();

		assertTrue(wizard.target().isShowing(), "the certificate wizard dialog must open");
		assertTrue(wizard.target().getComponentCount() > 0, "the wizard must render its content");

		// the first step is a short form, and it used to come up with a scroll bar next to it that
		// revealed nothing but the empty space a later step would have needed
		String tooTall = GuiActionRunner.execute(() -> {
			javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane)wizard.robot().finder()
				.find(wizard.target(), component -> component instanceof javax.swing.JScrollPane
					&& "scpCertificateWizard".equals(component.getName()));
			java.awt.Dimension shown = scrollPane.getViewport().getExtentSize();
			java.awt.Dimension wanted = scrollPane.getViewport().getView().getPreferredSize();
			return wanted.height <= shown.height
				? null
				: "the wizard asks for " + wanted.height + " pixels of height in a window that "
					+ "shows " + shown.height;
		});
		assertNull(tooTall, "the first step of the wizard makes the user scroll: " + tooTall);

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();

		assertFalse(wizard.target().isShowing(), "the wizard dialog must close");
		assertNotNull(MysticCryptApplicationFrame.getInstance(),
			"the application must still be running after the wizard closes");
	}

	/**
	 * The earlier check only ever looked at the step the wizard opens on - Issuer. Every other step
	 * was never driven through Next, which is exactly how a stuck Previous button on the Dates step
	 * (#93 follow-up) and scrolling on later steps went unnoticed.
	 */
	@Test
	@DisplayName("walking every step forward and back never needs a scroll bar, and Previous always works")
	void walkingEveryStepNeverNeedsAScrollBarAndPreviousAlwaysWorks() throws Exception
	{
		installPluginRequiringItBuilt(CERTIFICATE_ZIP);
		File databaseFile = new File(tempHome, "certwizard-walk-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openCertificateWizard();

		List<String> steps = List.of("Issuer", "Subject", "Dates", "Extensions", "Review");
		for (int index = 0; index < steps.size(); index++)
		{
			assertStepDoesNotNeedToScroll(wizard, steps.get(index));
			assertPreviousIsEnabled(wizard, index > 0, steps.get(index));
			if (index < steps.size() - 1)
			{
				click(wizard, "Next");
			}
		}
		for (int index = steps.size() - 1; index > 0; index--)
		{
			click(wizard, "Previous");
			assertStepDoesNotNeedToScroll(wizard, steps.get(index - 1));
		}

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();
	}

	/**
	 * The review step (#146) replaced the old post-save "Certificate created" confirmation dialog:
	 * walking to it and pressing Finish must save a real, readable certificate to the directory and
	 * file name the step shows, and close the wizard without any dialog popping up in between.
	 */
	@Test
	@DisplayName("Finish from the Review step saves the certificate without a confirmation dialog")
	void finishFromTheReviewStepSavesTheCertificateWithoutAConfirmationDialog() throws Exception
	{
		installPluginRequiringItBuilt(CERTIFICATE_ZIP);
		File databaseFile = new File(tempHome, "certwizard-finish-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openCertificateWizard();

		for (int index = 0; index < 4; index++)
		{
			click(wizard, "Next");
		}

		File savedFile = GuiActionRunner
			.execute(() -> new File(((JTextField)named(wizard, "txtSaveDirectory")).getText(),
				((JTextField)named(wizard, "txtFileName")).getText()));

		click(wizard, "Finish");

		assertFalse(wizard.target().isShowing(),
			"the wizard dialog must close without a confirmation dialog appearing first");
		assertTrue(savedFile.exists(), "the certificate must actually be written to " + savedFile);
		assertNotNull(CertificateReader.readPemCertificate(savedFile),
			"what was written must be a certificate the library can read back");
	}

	/**
	 * Finish used to write straight over whatever file was already at the save target, destroying
	 * it without asking - the same bug class as #28 ("Create empties an existing key store without
	 * asking"), never fixed here until now (#180)
	 */
	@Test
	@DisplayName("Finish refuses to overwrite a file that already exists at the save target")
	void finishRefusesToOverwriteAnExistingFile() throws Exception
	{
		installPluginRequiringItBuilt(CERTIFICATE_ZIP);
		File databaseFile = new File(tempHome, "certwizard-overwrite-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openCertificateWizard();

		for (int index = 0; index < 4; index++)
		{
			click(wizard, "Next");
		}

		File targetFile = GuiActionRunner
			.execute(() -> new File(((JTextField)named(wizard, "txtSaveDirectory")).getText(),
				((JTextField)named(wizard, "txtFileName")).getText()));
		String originalContent = "whatever was already there must survive";
		Files.writeString(targetFile.toPath(), originalContent, StandardCharsets.UTF_8);

		// fired without waiting for it to finish: the click itself blocks on the EDT until the
		// error dialog it triggers is dismissed, so a synchronous click() here would deadlock the
		// test the same way it would deadlock the real application if nothing ever answered it
		javax.swing.SwingUtilities.invokeLater(() -> onlyOneMatching(wizard, "Finish").doClick());

		DialogFixture failureDialog = application.findDialogWithTitle("Certificate failed");
		assertTrue(failureDialog.target().isShowing(),
			"an existing file at the save target must be refused with a dialog naming the reason");
		failureDialog.close();
		robot.waitForIdle();

		assertTrue(wizard.target().isShowing(),
			"the wizard must stay open after a refused Finish, nothing was saved to act on");
		assertEquals(originalContent, Files.readString(targetFile.toPath(), StandardCharsets.UTF_8),
			"the file that was already there must not have been touched");

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();
	}

	private java.awt.Component named(final DialogFixture wizard, final String name)
	{
		return wizard.robot().finder().find(wizard.target(),
			component -> name.equals(component.getName()));
	}

	private void click(final DialogFixture wizard, final String buttonText)
	{
		GuiActionRunner.execute(() -> {
			javax.swing.JButton button = onlyOneMatching(wizard, buttonText);
			button.doClick();
			return null;
		});
		robot.waitForIdle();
	}

	/**
	 * The wizard must hold exactly one button with this text - {@code find} (as opposed to
	 * {@code findAll}) fails on its own, with every match listed, if there is more than one
	 * (regression pin for the duplicate {@code NavigationPanel} construction fixed alongside this
	 * test - #93 follow-up)
	 */
	private JButton onlyOneMatching(final DialogFixture wizard, final String buttonText)
	{
		return (JButton)wizard.robot().finder().find(wizard.target(),
			JButtonMatcher.withText(buttonText));
	}

	private void assertPreviousIsEnabled(final DialogFixture wizard, final boolean expected,
		final String step)
	{
		boolean enabled = GuiActionRunner
			.execute(() -> onlyOneMatching(wizard, "Previous").isEnabled());
		assertTrue(enabled == expected, "Previous should be " + (expected ? "enabled" : "disabled")
			+ " on the " + step + " step, was " + (enabled ? "enabled" : "disabled"));
	}

	private void assertStepDoesNotNeedToScroll(final DialogFixture wizard, final String step)
	{
		String tooTall = GuiActionRunner.execute(() -> {
			javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane)wizard.robot().finder()
				.find(wizard.target(), component -> component instanceof javax.swing.JScrollPane
					&& "scpCertificateWizard".equals(component.getName()));
			java.awt.Dimension shown = scrollPane.getViewport().getExtentSize();
			// the content panel reports the size of whichever card is visible right now (see
			// CertificateWizardContentPanel.getPreferredSize) - which is exactly the step under
			// test at this point in the walk
			java.awt.Dimension wanted = scrollPane.getViewport().getView().getPreferredSize();
			return wanted.height <= shown.height
				? null
				: "asks for " + wanted.height + " pixels of height in a window that shows "
					+ shown.height;
		});
		assertNull(tooTall, "the " + step + " step makes the user scroll: " + tooTall);
	}
}
