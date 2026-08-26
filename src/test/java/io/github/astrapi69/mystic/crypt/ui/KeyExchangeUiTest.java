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

/**
 * Functional end-to-end test of the key exchange: opens the "Key Exchange" tool through the real
 * application, makes a key pair on the receiving tab, carries the public key over to the sending
 * tab by hand the way a person would, sends a message back, and asserts that it comes out of the
 * receiving tab unchanged and that both sides show the same fingerprint.
 */
class KeyExchangeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "key-exchange-e2e-pw-123";

	@Test
	void carriesAMessageBetweenTheTwoSidesThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(KEM_DEMO_ZIP);

		File databaseFile = new File(tempHome, "key-exchange-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Exchange", "Key Exchange");

		String message = "the meeting is at eight";

		// the receiving side makes a key pair and hands out its public key
		GuiActionRunner.execute(() -> frame.button("btnNewKeyPair").target().doClick());
		robot.waitForIdle();
		String published = GuiActionRunner
			.execute(() -> frame.textBox("txtMyPublicKey").target().getText());
		assertFalse(published.isBlank(), "no public key was handed out: "
			+ GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()));

		// the sending side is a different tab, exactly as it would be a different machine
		GuiActionRunner
			.execute(() -> frame.tabbedPane("tabKeyExchange").target().setSelectedIndex(1));
		robot.waitForIdle();
		setText(frame, "txtTheirPublicKey", published);
		GuiActionRunner.execute(() -> frame.button("btnEncapsulate").target().doClick());
		robot.waitForIdle();
		setText(frame, "txtMessageToSend", message);
		GuiActionRunner.execute(() -> frame.button("btnEncryptMessage").target().doClick());
		robot.waitForIdle();

		String handshake = GuiActionRunner
			.execute(() -> frame.textBox("txtHandshakeOut").target().getText());
		String encrypted = GuiActionRunner
			.execute(() -> frame.textBox("txtEncryptedOut").target().getText());
		String senderFingerprint = GuiActionRunner
			.execute(() -> frame.label("lblTheirFingerprint").target().getText());
		assertFalse(handshake.isBlank() || encrypted.isBlank(),
			"the sending side produced nothing to send back");
		assertFalse(encrypted.contains("meeting"), "the message left the tool in the clear");

		// back on the receiving side, with only what the sender sent
		GuiActionRunner
			.execute(() -> frame.tabbedPane("tabKeyExchange").target().setSelectedIndex(0));
		robot.waitForIdle();
		setText(frame, "txtHandshakeIn", handshake);
		GuiActionRunner.execute(() -> frame.button("btnDecapsulate").target().doClick());
		robot.waitForIdle();
		setText(frame, "txtEncryptedIn", encrypted);
		GuiActionRunner.execute(() -> frame.button("btnDecryptMessage").target().doClick());
		robot.waitForIdle();

		assertEquals(message,
			GuiActionRunner.execute(() -> frame.textBox("txtMessageReceived").target().getText()),
			"the message did not survive the exchange: "
				+ GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()));
		String recipientFingerprint = GuiActionRunner
			.execute(() -> frame.label("lblMyFingerprint").target().getText());
		assertEquals(senderFingerprint, recipientFingerprint,
			"the two sides show different fingerprints for the same secret");
		assertTrue(recipientFingerprint.length() == 8,
			"the fingerprint must be readable out loud: " + recipientFingerprint);
	}

	private void setText(FrameFixture frame, String name, String text)
	{
		GuiActionRunner.execute(() -> frame.textBox(name).target().setText(text));
		robot.waitForIdle();
	}
}
