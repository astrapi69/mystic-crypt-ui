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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.KeyPair;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.assertj.swing.timing.Condition;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Obfuscation rule export and import, on a database that has no key file to protect them with
 * (#357).
 * <p>
 * Reported from use: both operations encrypt or decrypt the rule table with the signed-in
 * database's key pair, and a database signed in with a master password alone - the ordinary case,
 * and the format this application writes today - has none. Reading it threw a
 * {@link NullPointerException} out of both panels' export and import, after the file chooser had
 * already been shown and dismissed.
 * <p>
 * Both panels wire the identical guard through {@code ObfuscationKeyAvailability}, so one pass over
 * the simple panel proves the shape; the shared class's own unit test proves the decision itself
 * for both.
 */
class ObfuscationRefusesWithoutAKeyUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("without a key file, export and import are off and say why")
	void withoutAKeyTheButtonsAreDisabledAndTheTooltipSaysWhy() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "obfuscation-no-key.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Simple Obfuscation", "Simple Obfuscation");

		assertFalse(GuiActionRunner.execute(() -> frame.button("btnExport").target().isEnabled()),
			"this database was signed in with a master password alone - the same call that used "
				+ "to reach a null private key and throw a NullPointerException (#357)");
		assertFalse(GuiActionRunner.execute(() -> frame.button("btnImport").target().isEnabled()));

		String exportTooltip = GuiActionRunner
			.execute(() -> frame.button("btnExport").target().getToolTipText());
		assertTrue(exportTooltip.contains("key"),
			"a disabled control with no reason is what i18n.md and the lock work have been moving "
				+ "away from. It said: " + exportTooltip);

		application.closeInternalFrame("Simple Obfuscation");
	}

	@Test
	@DisplayName("with a key file, export writes a real file through the UI")
	void withAKeyTheButtonsWorkAndExportWritesAFile() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "obfuscation-with-key.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		// a real usable RSA key pair - the panel encrypts through it for real, so a fake byte
		// array would fail one layer further in for a different reason than the one under test
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		KeyModel privateKeyInfo = KeyModelExtensions.toKeyModel(keyPair.getPrivate());
		GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject()
			.getMasterPwFileModelBean().setPrivateKeyInfo(privateKeyInfo));

		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Simple Obfuscation", "Simple Obfuscation");

		assertTrue(GuiActionRunner.execute(() -> frame.button("btnExport").target().isEnabled()),
			"a key is present now, so the button that reflects it must follow");
		assertTrue(GuiActionRunner.execute(() -> frame.button("btnImport").target().isEnabled()));

		// no extension is appended by the panel - the file written is exactly the one chosen here
		File exportedFile = new File(tempHome, "exported-rules.obf");
		SwingUtilities.invokeLater(() -> frame.button("btnExport").target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(exportedFile);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();

		Pause.pause(new Condition("exported rule file written")
		{
			@Override
			public boolean test()
			{
				return exportedFile.exists() && exportedFile.length() > 0;
			}
		}, 10000);
		assertTrue(exportedFile.exists(),
			"a key was present, so the guard must not have refused - if it wrongly did, no file "
				+ "chooser interaction above would ever produce this file");

		application.closeInternalFrame("Simple Obfuscation");
	}
}
