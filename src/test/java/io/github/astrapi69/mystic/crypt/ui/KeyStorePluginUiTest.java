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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the key store plugin: creates a PKCS12 store through the real UI,
 * puts a generated key pair into it, exports the certificate as PEM, imports it back under a second
 * alias and finally deletes an alias - checking after every step what the table shows.
 */
class KeyStorePluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String STORE_PASSWORD = TestPasswords.throwaway();

	@Test
	void managesAKeyStoreThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(KEYSTORE_ZIP);

		File databaseFile = new File(tempHome, "keystore-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Manage Key Store", "Manage Key Store");

		File storeFile = new File(tempHome, "e2e.p12");
		File pemFile = new File(tempHome, "exported.pem");

		// create a new store
		GuiActionRunner.execute(() -> {
			frame.textBox("txtKeyStoreFile").target().setText(storeFile.getAbsolutePath());
			frame.robot().finder().findByName("pwdStore", javax.swing.JPasswordField.class, true)
				.setText(STORE_PASSWORD);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnCreate").target().doClick());
		robot.waitForIdle();
		assertTrue(storeFile.exists(), "creating must write the key store file: " + result(frame));
		assertEquals(0, rowCount(frame), "a new store starts empty");

		// add a key pair
		GuiActionRunner.execute(() -> frame.textBox("txtAlias").target().setText("server"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnAddKeyPair").target().doClick());
		robot.waitForIdle();
		assertEquals(1, rowCount(frame), "the added key pair must show up: " + result(frame));
		assertEquals("server", cell(frame, 0, 0));
		assertEquals("private key", cell(frame, 0, 1));

		// export its certificate as PEM
		GuiActionRunner.execute(
			() -> frame.textBox("txtCertificateFile").target().setText(pemFile.getAbsolutePath()));
		robot.waitForIdle();
		GuiActionRunner
			.execute(() -> frame.table("tblEntries").target().setRowSelectionInterval(0, 0));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnExport").target().doClick());
		robot.waitForIdle();
		assertTrue(pemFile.exists(), "exporting must write the pem file: " + result(frame));

		// import it back under a second alias
		GuiActionRunner.execute(() -> frame.textBox("txtAlias").target().setText("trusted"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnImport").target().doClick());
		robot.waitForIdle();
		assertEquals(2, rowCount(frame),
			"the imported certificate must show up as a second entry: " + result(frame));

		// and delete the imported one again
		GuiActionRunner.execute(() -> frame.button("btnDelete").target().doClick());
		robot.waitForIdle();
		assertEquals(1, rowCount(frame), "deleting must remove the alias: " + result(frame));
		assertEquals("server", cell(frame, 0, 0),
			"the entry that was not selected must survive the deletion");
	}

	private int rowCount(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.table("tblEntries").target().getRowCount());
	}

	private String cell(FrameFixture frame, int row, int column)
	{
		return GuiActionRunner.execute(
			() -> String.valueOf(frame.table("tblEntries").target().getValueAt(row, column)));
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
