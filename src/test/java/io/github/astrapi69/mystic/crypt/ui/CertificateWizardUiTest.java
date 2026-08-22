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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * Functional end-to-end test of the certificate plugin: with the plugin installed, the Plugins menu
 * gains a "Create Certificate..." item that opens the wizard dialog, and closing it leaves the
 * application running (the old in-host wizard killed the whole app with System.exit on finish)
 */
class CertificateWizardUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "certwizard-pw-123";

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

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();

		assertFalse(wizard.target().isShowing(), "the wizard dialog must close");
		assertNotNull(MysticCryptApplicationFrame.getInstance(),
			"the application must still be running after the wizard closes");
	}
}
