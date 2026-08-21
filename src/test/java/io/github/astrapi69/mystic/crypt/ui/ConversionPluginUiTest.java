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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;

/**
 * Functional end-to-end test of the conversion plugin: loads the plugin from its zip, opens the
 * "Convert DER to PEM" tool from the Plugins menu, chooses a DER private key file, picks a PEM
 * destination and converts - all through the real UI. The produced PEM must contain the original
 * key
 */
class ConversionPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "conversion-e2e-pw-123";

	@Test
	void convertDerToPemWritesTheKeyAsPemThroughTheUi() throws Exception
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
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Convert DER to PEM", "Convert *.der-file to *.pem-file");

		// choose the DER input file (key type defaults to PRIVATE_KEY)
		clickAndApproveInChooser(frame, "btnChoose", derFile);
		// choose the PEM destination file
		clickAndApproveInChooser(frame, "btnSaveTo", pemFile);
		// convert
		GuiActionRunner.execute(() -> frame.button("btnConvert").target().doClick());
		robot.waitForIdle();

		Pause.pause(new Condition("pem file written")
		{
			@Override
			public boolean test()
			{
				return pemFile.exists() && pemFile.length() > 0;
			}
		}, 10000);

		assertTrue(pemFile.exists(), "the converter must produce the PEM file");
		PrivateKey keyFromPem = PrivateKeyReader.readPemPrivateKey(pemFile);
		assertArrayEquals(originalKey.getEncoded(), keyFromPem.getEncoded(),
			"the PEM the tool wrote must contain the original private key");
	}

	private void clickAndApproveInChooser(FrameFixture frame, String buttonName, File file)
	{
		SwingUtilities.invokeLater(() -> frame.button(buttonName).target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(file);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
		UiTestSpeed.step();
	}
}
