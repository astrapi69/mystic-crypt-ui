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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;

import javax.swing.JButton;
import javax.swing.JTextField;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the conversion plugin's guided wizard: loads the plugin from its
 * zip, opens the "Convert Key/Certificate..." wizard from the Plugins menu, walks Source -&gt;
 * Target -&gt; Review -&gt; Finish for a DER private key converted to PEM - all through the real
 * UI. The produced PEM must contain the original key. Replaces the old two-menu-item
 * {@code FileConversionPanel} flow this test used to drive (issue #182).
 */
class ConversionPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void convertDerToPemWritesTheKeyAsPemThroughTheWizard() throws Exception
	{
		installPluginRequiringItBuilt(CONVERSION_ZIP);
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKey originalKey = keyPair.getPrivate();
		File derFile = new File(tempHome, "conversion-key.der");
		PrivateKeyWriter.write(originalKey, derFile);
		File pemFile = new File(tempHome, "conversion-key.pem");

		File databaseFile = new File(tempHome, "conversion-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openConversionWizard();

		// Source: type the DER file, the wizard detects what it holds on its own
		GuiActionRunner.execute(
			() -> ((JTextField)named(wizard, "txtSourceFile")).setText(derFile.getAbsolutePath()));
		robot.waitForIdle();
		click(wizard, "Next");

		// Target: DER to PEM is the only conversion a DER private key offers besides PKCS#8/PKCS#1;
		// pick it and set the destination explicitly
		GuiActionRunner
			.execute(() -> ((javax.swing.AbstractButton)named(wizard, "rdoDerToPem")).doClick());
		GuiActionRunner.execute(
			() -> ((JTextField)named(wizard, "txtTargetFile")).setText(pemFile.getAbsolutePath()));
		robot.waitForIdle();
		click(wizard, "Next");

		click(wizard, "Finish");

		assertFalse(wizard.target().isShowing(), "the wizard must close after a successful Finish");
		assertTrue(pemFile.exists(), "the converter must produce the PEM file");
		PrivateKey keyFromPem = PrivateKeyReader.readPemPrivateKey(pemFile);
		assertArrayEquals(originalKey.getEncoded(), keyFromPem.getEncoded(),
			"the PEM the wizard wrote must contain the original private key");
	}

	/**
	 * Finish used to have no overwrite guard of its own to begin with - {@code ConversionSupport}'s
	 * writers always refused an existing target file, this only proves the wizard surfaces that
	 * refusal instead of swallowing it. Mirrors the certificate wizard's own overwrite-refusal test
	 * (#180), added for consistency across both wizards (issue #182).
	 */
	@Test
	@DisplayName("Finish refuses to overwrite a file that already exists at the target")
	void finishRefusesToOverwriteAnExistingFile() throws Exception
	{
		installPluginRequiringItBuilt(CONVERSION_ZIP);
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		File derFile = new File(tempHome, "overwrite-key.der");
		PrivateKeyWriter.write(
			KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048).getPrivate(), derFile);
		File targetFile = new File(tempHome, "overwrite-key.pem");
		String originalContent = "whatever was already there must survive";
		Files.writeString(targetFile.toPath(), originalContent, StandardCharsets.UTF_8);

		File databaseFile = new File(tempHome, "conversion-overwrite-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openConversionWizard();

		GuiActionRunner.execute(
			() -> ((JTextField)named(wizard, "txtSourceFile")).setText(derFile.getAbsolutePath()));
		robot.waitForIdle();
		click(wizard, "Next");
		GuiActionRunner
			.execute(() -> ((javax.swing.AbstractButton)named(wizard, "rdoDerToPem")).doClick());
		GuiActionRunner.execute(() -> ((JTextField)named(wizard, "txtTargetFile"))
			.setText(targetFile.getAbsolutePath()));
		robot.waitForIdle();
		click(wizard, "Next");

		// fired without waiting for it to finish: the click itself blocks on the EDT until the
		// error
		// dialog it triggers is dismissed, so a synchronous click() here would deadlock the test
		// the
		// same way it would deadlock the real application if nothing ever answered it
		javax.swing.SwingUtilities.invokeLater(() -> ((JButton)wizard.robot().finder()
			.find(wizard.target(), JButtonMatcher.withText("Finish"))).doClick());

		DialogFixture failureDialog = application.findDialogWithTitle("Conversion failed");
		assertTrue(failureDialog.target().isShowing(),
			"an existing file at the target must be refused with a dialog naming the reason");
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
			JButton button = (JButton)wizard.robot().finder().find(wizard.target(),
				JButtonMatcher.withText(buttonText));
			button.doClick();
			return null;
		});
		robot.waitForIdle();
	}
}
