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
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Obfuscate &gt; and Disentangle with an empty rule table say what is missing instead of doing
 * nothing (#891): before the fix, the library's refusal ("Given map 'rules' may not be empty") went
 * to the console and the window stayed silent
 */
class ObfuscationEmptyRuleTableSaysSoUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("Obfuscate > with an empty rule table says so, and nothing is written")
	void anEmptyRuleTable_getsAWarningDialog() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "obfuscation-empty-rules-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Simple Obfuscation", "Simple Obfuscation");

		// type a text, leave the rule table empty, and ask to obfuscate
		GuiActionRunner.execute(() -> frame.textBox("txtToEncrypt").target().setText("cab"));
		robot.waitForIdle();
		// resolve on the test thread, click via invokeLater: the warning dialog is modal, and
		// GuiActionRunner.execute would wait on the EDT it blocks
		javax.swing.JButton encrypt = frame.button("btnEncrypt").target();
		SwingUtilities.invokeLater(encrypt::doClick);

		JOptionPaneFixture warning = JOptionPaneFinder.findOptionPane()
			.withTimeout(15, TimeUnit.SECONDS).using(robot);
		String message = GuiActionRunner
			.execute(() -> String.valueOf(warning.target().getMessage()));
		assertTrue(message.contains("rule"), "the warning names the rule table: " + message);
		warning.okButton().click();
		robot.waitForIdle();

		String obfuscated = GuiActionRunner
			.execute(() -> frame.textBox("txtEncrypted").target().getText());
		assertEquals("", obfuscated, "nothing was obfuscated");
	}
}
