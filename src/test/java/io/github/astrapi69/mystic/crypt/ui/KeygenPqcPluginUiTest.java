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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the keygen plugin's modern-algorithm branch: opens the "Key
 * Generation" tool, selects the post-quantum ML-KEM-768 algorithm from the new dropdown, generates
 * a key pair, and asserts both keys are shown as PEM through the real UI. This is a separate class
 * from {@link KeygenPluginUiTest} on purpose - the suite runs one UI test class per JVM (forkEvery
 * = 1), so keeping a single {@code @Test} per class avoids the singleton frame leaking a second
 * open tool window between methods.
 */
class KeygenPqcPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void generatesAPostQuantumKeyPairThroughTheAlgorithmDropdown() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-pqc-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		// pick the post-quantum ML-KEM-768 algorithm from the new dropdown, then generate
		GuiActionRunner.execute(() -> frame.comboBox("cmbAlgorithm").target()
			.setSelectedItem(KeyPairGeneratorAlgorithm.ML_KEM_768));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();

		String privateKeyPem = GuiActionRunner
			.execute(() -> frame.textBox("txtPrivateKey").target().getText());
		assertTrue(privateKeyPem.contains("PRIVATE KEY"),
			"the private key area must show the generated ML-KEM PEM, was: " + privateKeyPem);
		String publicKeyPem = GuiActionRunner
			.execute(() -> frame.textBox("txtPublicKey").target().getText());
		assertTrue(publicKeyPem.contains("PUBLIC KEY"),
			"the public key area must show the generated ML-KEM PEM, was: " + publicKeyPem);
	}
}
