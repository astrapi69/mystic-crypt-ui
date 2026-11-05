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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.PasswordVaultFormat;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;

/**
 * A vault that 7.3 wrote opens through the sign-in of the running application, shows its groups and
 * entries, and is written back in this version's format (#624). A vault whose content cannot be
 * read is not reported as a wrong password: the sign-in says the database was not opened, and why.
 */
class VaultsOfSevenXSignInUiTest extends AbstractUiTest
{

	private static final String UNREADABLE_TITLE = "Database not opened";

	private static final String OLD_GROUP_CLASS = "io.github.astrapi69.swing.tree.GenericTreeElement";

	@Test
	@DisplayName("a vault of 7.3 signs in, shows its groups and entries, and saves in this version's format")
	void aVaultOfSevenX_signsIn_andIsSavedInThisVersionsFormat() throws Exception
	{
		File vault = fixture("7.3");

		ApplicationSteps application = signInWithExistingDatabase(vault,
			TestPasswords.VAULTS_OF_7X_FIXTURE);

		assertTrue(application.treeShowsARowNamed("mykeys"),
			"the group 7.3 made is in the tree: " + application.treeRootChildNames());
		assertTrue(application.entryExistsWithTitle("the bank"),
			"the entry of the root group is there");
		assertTrue(application.entryExistsWithTitle("the mail"),
			"the entry of the group mykeys is there");

		GuiActionRunner.execute(() -> ApplicationEventBus.getSaveState()
			.fireEvent(new EventObject<>(RenderMode.EDITABLE)));
		GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().getModelObject().setDirty(true));
		robot.waitForIdle();
		application.saveDatabase();

		String written = new String(
			PasswordVaultFormat.decrypt(vault, TestPasswords.VAULTS_OF_7X_FIXTURE.toCharArray()));
		assertFalse(written.contains(OLD_GROUP_CLASS),
			"the old name of the group class is read, never written");
		assertFalse(written.contains("io.github.astrapi69.file.create.FileContentInfo"),
			"the old name of the attachment class is read, never written");
		assertTrue(written.contains("formatVersion=\"" + VaultXmlCodec.FORMAT_VERSION + "\""),
			"the file is in this version's format now");
		assertTrue(written.contains("<name>codes.txt</name>"), "and still holds the attachment");
	}

	@Test
	@DisplayName("a vault whose content cannot be read is not taken for a wrong password")
	void aVaultThatCannotBeRead_isNotTakenForAWrongPassword() throws Exception
	{
		String password = TestPasswords.VAULTS_OF_7X_FIXTURE;
		File vault = fixture("7.3");
		String written = new String(PasswordVaultFormat.decrypt(vault, password.toCharArray()));
		// the class of the groups renamed to one no version has: what 7.x was to 8.1.1 to 8.13
		String unreadable = written.replace(OLD_GROUP_CLASS,
			"io.github.astrapi69.mystic.crypt.NoSuchTypeInAnyVersion");
		assertTrue(written.contains(OLD_GROUP_CLASS) && !unreadable.contains(OLD_GROUP_CLASS),
			"the precondition: the vault named the group class, and now names no class it knows");
		Files.write(vault.toPath(),
			PasswordVaultFormat.encrypt(unreadable.toCharArray(), password.toCharArray()));

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(password)
			.browseApplicationFile(vault).requireOkEnabled().clickOk();
		new ApplicationSteps(robot).dismissMessageDialog(UNREADABLE_TITLE);

		assertFalse(
			MysticCryptApplicationFrame.getInstance() != null
				&& MysticCryptApplicationFrame.getInstance().getModelObject() != null
				&& MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"a vault that cannot be read is not opened");
		assertTrue(signIn.dialogIsShowing(), "the sign-in stays open");
		signIn.cancel();
	}

	private File fixture(final String release) throws IOException
	{
		File copy = new File(tempHome, "vault-" + release + ".mcrdb");
		try (InputStream in = getClass()
			.getResourceAsStream("/compat/vaults/vault-" + release + ".mcrdb"))
		{
			assertTrue(in != null, "the fixture of " + release + " is on the class path");
			Files.copy(in, copy.toPath());
		}
		return copy;
	}
}
