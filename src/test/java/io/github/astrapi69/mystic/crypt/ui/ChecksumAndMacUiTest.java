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

/**
 * Functional end-to-end test of the two questions the checksum tool answers: the checksum of a
 * typed text with a digest the window could not offer before, and a message authentication code,
 * which the application had nowhere at all.
 */
class ChecksumAndMacUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "checksum-mac-e2e-pw-123";

	@Test
	void computesAChecksumAndAMacThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);

		File databaseFile = new File(tempHome, "checksum-mac.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Checksum and MAC", "Checksum and MAC");

		// the checksum of a typed text, with SHA3-256 - which the window could not do at all
		GuiActionRunner.execute(() -> {
			frame.comboBox("cmbDigest").target().setSelectedItem("SHA3-256");
			frame.textBox("txtChecksumText").target().setText("");
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnChecksum").target().doClick());
		robot.waitForIdle();

		assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
			GuiActionRunner.execute(() -> frame.textBox("txtChecksum").target().getText()),
			"the published SHA3-256 value of the empty input: " + result(frame));

		// comparing against a value that was pasted in
		GuiActionRunner.execute(() -> frame.textBox("txtExpected").target()
			.setText("A7FFC6F8BF1ED76651C14756A061D662F580FF4DE43B49FA82D80A4B80F8434A"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnCompare").target().doClick());
		robot.waitForIdle();
		assertEquals("the checksums are the same", result(frame),
			"a value pasted in upper case is still the same value");

		// and the other question: a code that only someone with the key can produce
		GuiActionRunner.execute(() -> {
			frame.tabbedPane("tabChecksum").target().setSelectedIndex(1);
			frame.textBox("txtMacText").target()
				.setText("The quick brown fox jumps over the lazy dog");
			frame.robot().finder().findByName("pwdMacKey", javax.swing.JPasswordField.class, true)
				.setText("key");
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnMac").target().doClick());
		robot.waitForIdle();

		assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
			GuiActionRunner.execute(() -> frame.textBox("txtMac").target().getText()),
			"the published HMAC-SHA-256 value for that message and key: " + result(frame));

		// a different key must give a different code - that is the whole point of a keyed code
		GuiActionRunner.execute(() -> frame.robot().finder()
			.findByName("pwdMacKey", javax.swing.JPasswordField.class, true)
			.setText("another key"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnMac").target().doClick());
		robot.waitForIdle();
		assertTrue(
			GuiActionRunner.execute(() -> !frame.textBox("txtMac").target().getText()
				.equals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8")),
			"a code that does not change with the key would be a checksum, not a code");
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
