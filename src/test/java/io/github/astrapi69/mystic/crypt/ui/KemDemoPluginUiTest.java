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

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * Functional end-to-end test of the KEM demo plugin: opens the "Key Encapsulation Demo" tool, runs
 * the default ML-KEM-768 exchange, and asserts through the real UI that the sender and recipient
 * derive the same shared secret.
 */
class KemDemoPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "kem-e2e-pw-123";

	@Test
	void runsAKeyEncapsulationExchangeThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(KEM_DEMO_ZIP);

		File databaseFile = new File(tempHome, "kem-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Encapsulation Demo", "Key Encapsulation Demo");

		// run the default ML-KEM-768 exchange; onRun runs synchronously
		GuiActionRunner.execute(() -> frame.button("btnRun").target().doClick());
		robot.waitForIdle();

		String senderSecret = GuiActionRunner
			.execute(() -> frame.textBox("txtSenderSecret").target().getText());
		String recipientSecret = GuiActionRunner
			.execute(() -> frame.textBox("txtRecipientSecret").target().getText());
		assertFalse(senderSecret.isBlank(), "the exchange must produce a shared secret");
		assertEquals(senderSecret, recipientSecret,
			"sender and recipient must show the same shared secret through the UI");
		assertEquals("shared secrets match",
			GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()),
			"the panel must report that the shared secrets match");
	}
}
