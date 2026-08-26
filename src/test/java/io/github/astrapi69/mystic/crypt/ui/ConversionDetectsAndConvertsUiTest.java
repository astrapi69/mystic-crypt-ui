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
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Functional end-to-end test of converting a key file: the tool has to say what the chosen file
 * holds rather than ask, and turn a key openssl wrote into one Java reads.
 */
class ConversionDetectsAndConvertsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "conversion-detect-e2e-pw-123";

	@Test
	void saysWhatTheFileHoldsAndConvertsItToPkcs8() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		installPluginRequiringItBuilt(CONVERSION_ZIP);

		File databaseFile = new File(tempHome, "conversion-detect.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		// a key in the shape openssl writes: PKCS#1, under an algorithm specific header
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		File source = new File(tempHome, "openssl-key.pem");
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), source);
		assertTrue(Files.readString(source.toPath()).contains("BEGIN RSA PRIVATE KEY"),
			"the fixture must really be PKCS#1, otherwise this test proves nothing");
		File target = new File(tempHome, "java-key.pem");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Convert key file...", "Convert key file");

		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target().setText(source.getAbsolutePath());
			frame.textBox("txtTargetFile").target().setText(target.getAbsolutePath());
		});
		robot.waitForIdle();

		assertEquals("an RSA private key, PKCS#1",
			GuiActionRunner.execute(() -> frame.label("lblWhatItHolds").target().getText()),
			"the tool has to say what the file holds rather than ask");

		GuiActionRunner.execute(() -> frame.button("btnToPkcs8").target().doClick());
		robot.waitForIdle();

		assertTrue(target.exists(), "the converted key must be written: " + result(frame));
		assertTrue(Files.readString(target.toPath()).contains("BEGIN PRIVATE KEY"),
			"PKCS#8 is what Java reads, and it says so in its header");
		assertEquals(keyPair.getPrivate(), KeyFiles.readPrivateKey(target),
			"the converted file has to hold the same key");
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
