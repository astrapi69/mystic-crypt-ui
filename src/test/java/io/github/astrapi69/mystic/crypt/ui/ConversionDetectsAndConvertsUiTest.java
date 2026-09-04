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
import java.security.KeyPair;
import java.security.Security;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Functional end-to-end test of the conversion wizard's auto-detection and the PKCS#8 conversion:
 * the Source step has to say what a chosen file holds rather than ask, and the Target step has to
 * turn a key openssl wrote into one Java reads. Replaces the old single-tool
 * {@code ConversionPanel} flow this test used to drive (issue #182).
 */
class ConversionDetectsAndConvertsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

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
		application.showMainFrame();
		DialogFixture wizard = application.openConversionWizard();

		GuiActionRunner.execute(
			() -> ((JTextField)named(wizard, "txtSourceFile")).setText(source.getAbsolutePath()));
		robot.waitForIdle();

		assertEquals("an RSA private key, PKCS#1",
			GuiActionRunner.execute(() -> ((JLabel)named(wizard, "lblWhatItHolds")).getText()),
			"the Source step has to say what the file holds rather than ask");

		click(wizard, "Next");
		GuiActionRunner.execute(() -> ((AbstractButton)named(wizard, "rdoToPkcs8")).doClick());
		GuiActionRunner.execute(
			() -> ((JTextField)named(wizard, "txtTargetFile")).setText(target.getAbsolutePath()));
		robot.waitForIdle();
		click(wizard, "Next");
		click(wizard, "Finish");

		assertFalse(wizard.target().isShowing(), "the wizard must close after a successful Finish");
		assertTrue(target.exists(), "the converted key must be written");
		assertTrue(Files.readString(target.toPath()).contains("BEGIN PRIVATE KEY"),
			"PKCS#8 is what Java reads, and it says so in its header");
		assertEquals(keyPair.getPrivate(), KeyFiles.readPrivateKey(target),
			"the converted file has to hold the same key");
	}

	private java.awt.Component named(final DialogFixture wizard, final String name)
	{
		return wizard.robot().finder().find(wizard.target(),
			component -> name.equals(component.getName()));
	}

	private void click(final DialogFixture wizard, final String buttonText)
	{
		GuiActionRunner.execute(() -> {
			JButton button = (JButton)wizard.robot().finder().find(wizard.target(),
				JButtonMatcher.withText(buttonText));
			button.doClick();
			return null;
		});
		robot.waitForIdle();
	}
}
