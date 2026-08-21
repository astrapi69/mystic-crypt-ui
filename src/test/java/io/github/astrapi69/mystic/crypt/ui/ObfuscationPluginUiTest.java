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

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * Functional end-to-end test of the obfuscation plugin: loads the plugin from its zip, opens the
 * "Simple Obfuscation" tool from the Plugins menu, adds a substitution rule, obfuscates a text and
 * disentangles it again - all through the real UI. This is the definitive proof that the
 * disentangle fix works in situ (with a substitution a-&gt;x whose replacement is not itself an
 * original character, the exact case the old code got wrong)
 */
class ObfuscationPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "obfuscation-e2e-pw-123";

	@Test
	void simpleObfuscationObfuscatesAndDisentanglesThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "obfuscation-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Simple Obfuscation", "Simple Obfuscation");

		// add the rule a -> x (x is not itself an original character - the case the bug got wrong)
		GuiActionRunner.execute(() -> {
			frame.textBox("txtOriginalChar").target().setText("a");
			frame.textBox("txtRelpaceWith").target().setText("x");
		});
		robot.waitForIdle();
		clickButton(frame, "btnAddRule");

		// type text and obfuscate
		GuiActionRunner.execute(() -> frame.textBox("txtToEncrypt").target().setText("cab"));
		robot.waitForIdle();
		clickButton(frame, "btnEncrypt");

		String obfuscated = GuiActionRunner
			.execute(() -> frame.textBox("txtEncrypted").target().getText());
		assertEquals("cxb", obfuscated, "obfuscate must replace a with x, keep c and b");

		// disentangle the obfuscated text back
		clickButton(frame, "btnDecrypt");
		String disentangled = GuiActionRunner
			.execute(() -> frame.textBox("txtToEncrypt").target().getText());
		assertEquals("cab", disentangled, "disentangle must invert obfuscate through the UI");
	}

	private void clickButton(FrameFixture frame, String name)
	{
		GuiActionRunner.execute(() -> frame.button(name).target().doClick());
		robot.waitForIdle();
		UiTestSpeed.step();
	}
}
