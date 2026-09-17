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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the post-quantum signature plugin: generates an ML-DSA-65 key pair,
 * signs a message, verifies the signature, and then changes the message so the very same signature
 * must be rejected - all through the real UI.
 */
class PqcSignaturePluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void signsAndVerifiesAndRejectsATamperedMessageThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(PQC_SIGNATURE_ZIP);

		File databaseFile = new File(tempHome, "pqc-signature-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Sign and Verify", "Sign and Verify");

		// pick the post-quantum ML-DSA-65 and generate a key pair
		GuiActionRunner
			.execute(() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem("ML-DSA-65"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();
		assertTrue(GuiActionRunner.execute(() -> frame.textBox("txtPublicKey").target().getText())
			.contains("PUBLIC KEY"), "the generated public key must be shown as PEM");

		// sign the message
		GuiActionRunner.execute(() -> frame.button("btnSign").target().doClick());
		robot.waitForIdle();
		String signature = GuiActionRunner
			.execute(() -> frame.textBox("txtSignature").target().getText());
		assertFalse(signature.isBlank(), "signing must produce a signature");

		// verify it
		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertEquals("signature is valid",
			GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()),
			"the fresh signature must verify");

		// change the message - the signature must no longer fit
		GuiActionRunner
			.execute(() -> frame.textBox("txtMessage").target().setText("a different message"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertEquals("signature is not valid",
			GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()),
			"a changed message must not verify with the old signature");
	}
}
