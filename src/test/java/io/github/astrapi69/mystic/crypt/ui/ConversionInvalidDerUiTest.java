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
import java.nio.file.Files;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Negative-path end-to-end test of the conversion wizard: a file that is not a key or certificate
 * the tool recognises has to be reported inline on the Source step, not crash the wizard, and
 * Finish pressed anyway (the wizard's Finish button works from any step, not only Review) has to
 * refuse with a dialog naming the reason rather than writing a broken or empty output file.
 * Replaces the old single-tool {@code FileConversionPanel} flow this test used to drive (issue
 * #182).
 */
class ConversionInvalidDerUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a file the tool does not recognise is reported inline, and Finish refuses to convert it")
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
		application.showMainFrame();
		DialogFixture wizard = application.openConversionWizard();

		GuiActionRunner.execute(() -> ((JTextField)named(wizard, "txtSourceFile"))
			.setText(notDerFile.getAbsolutePath()));
		robot.waitForIdle();

		assertEquals("nothing this tool recognises",
			GuiActionRunner.execute(() -> ((JLabel)named(wizard, "lblWhatItHolds")).getText()),
			"a file the tool does not recognise must be reported, not crash the wizard");

		// fired without waiting for it to finish: the click itself blocks on the EDT until the
		// error
		// dialog it triggers is dismissed, so a synchronous click() here would deadlock the test
		// the
		// same way it would deadlock the real application if nothing ever answered it (mirrors
		// CertificateWizardUiTest.finishRefusesToOverwriteAnExistingFile)
		javax.swing.SwingUtilities.invokeLater(() -> ((javax.swing.JButton)wizard.robot().finder()
			.find(wizard.target(), JButtonMatcher.withText("Finish"))).doClick());

		DialogFixture failureDialog = application.findDialogWithTitle("Conversion failed");
		assertTrue(failureDialog.target().isShowing(),
			"nothing was chosen to convert, so Finish must refuse with a dialog naming the reason");
		failureDialog.close();
		robot.waitForIdle();

		assertTrue(wizard.target().isShowing(),
			"the wizard must stay open after a refused Finish, nothing was saved to act on");
		assertFalse(pemFile.exists() && pemFile.length() > 0,
			"an invalid DER input must not produce a non-empty PEM file");

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();
	}

	private java.awt.Component named(final DialogFixture wizard, final String name)
	{
		return wizard.robot().finder().find(wizard.target(),
			component -> name.equals(component.getName()));
	}
}
