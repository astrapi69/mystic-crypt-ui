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

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end test of the keygen plugin's Clear button after a modern-algorithm generation:
 * selecting a non-RSA algorithm disables the key-size combo, and pressing Clear must reset the
 * algorithm back to RSA and re-enable the key size. Drives that reset through the real UI.
 */
class KeygenClearResetsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void clearResetsTheAlgorithmToRsaAndReenablesTheKeySize() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-clear-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		// pick a modern algorithm - the key size combo becomes irrelevant and is disabled
		GuiActionRunner.execute(() -> frame.comboBox("cmbAlgorithm").target()
			.setSelectedItem(KeyPairGeneratorAlgorithm.ML_DSA_65));
		robot.waitForIdle();
		assertTrue(
			!GuiActionRunner.execute(() -> frame.comboBox("cmbKeySize").target().isEnabled()),
			"selecting a modern algorithm must disable the key size combo");

		// Clear must put the panel back to its RSA default
		GuiActionRunner
			.execute(() -> frame.button(JButtonMatcher.withText("Clear keys")).target().doClick());
		robot.waitForIdle();

		assertEquals(KeyPairGeneratorAlgorithm.RSA,
			GuiActionRunner
				.execute(() -> frame.comboBox("cmbAlgorithm").target().getSelectedItem()),
			"Clear must reset the algorithm back to RSA");
		assertTrue(GuiActionRunner.execute(() -> frame.comboBox("cmbKeySize").target().isEnabled()),
			"Clear must re-enable the key size combo");
	}
}
