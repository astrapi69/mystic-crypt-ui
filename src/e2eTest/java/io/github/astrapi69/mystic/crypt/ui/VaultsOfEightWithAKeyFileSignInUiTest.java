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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyDecryptor;

/**
 * A vault that 8 wrote with a key file signs in through the sign-in of the running application,
 * with the key file alone and with a password and the key file, shows its groups and entries, and
 * is written back in this version's format (#650). Up to 8.13 the sign-in called the right key file
 * not valid.
 */
class VaultsOfEightWithAKeyFileSignInUiTest extends AbstractUiTest
{

	/**
	 * The key 8 wrote both fixtures with, the test key that has been in this repository for years
	 */
	private static final File KEY_FILE = new File("src/test/resources/pem/private.pem")
		.getAbsoluteFile();

	@Test
	@DisplayName("a vault 8 wrote with a key file signs in with that key file, shows its entries, and saves in this version's format")
	void aVaultOfEight_withAKeyFile_signsIn_andIsSavedInThisVersionsFormat() throws Exception
	{
		File vault = fixture("vault-8-key-file.mcrdb");

		launchApplication().requireOkDisabled().checkKeyFile().browseKeyFile(KEY_FILE)
			.browseApplicationFile(vault).requireOkEnabled().okAndAwaitSignIn();

		ApplicationSteps application = assertTheEntriesOfEight();
		GuiActionRunner.execute(() -> ApplicationEventBus.getSaveState()
			.fireEvent(new EventObject<>(RenderMode.EDITABLE)));
		GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().getModelObject().setDirty(true));
		robot.waitForIdle();
		application.saveDatabase();

		// the library's default reads AES-GCM only: that it reads the file is the current format
		byte[] content = new PrivateKeyDecryptor(PrivateKeyReader.readPemPrivateKey(KEY_FILE))
			.decrypt(Files.readAllBytes(vault.toPath()));
		assertTrue(new String(content, StandardCharsets.UTF_8).contains("the bank"),
			"the saved vault is in this version's format and still holds the entry");
	}

	@Test
	@DisplayName("a vault 8 wrote with a password and a key file signs in with both and shows its entries")
	void aVaultOfEight_withAPasswordAndAKeyFile_signsIn() throws Exception
	{
		File vault = fixture("vault-8-password-and-key-file.mcrdb");

		launchApplication().requireOkDisabled().checkMasterPassword()
			.typeMasterPassword(TestPasswords.VAULT_OF_8_FIXTURE).checkKeyFile()
			.browseKeyFile(KEY_FILE).browseApplicationFile(vault).requireOkEnabled()
			.okAndAwaitSignIn();

		assertTheEntriesOfEight();
	}

	private ApplicationSteps assertTheEntriesOfEight()
	{
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();
		assertTrue(application.treeShowsARowNamed("mykeys"),
			"the group 8 made is in the tree: " + application.treeRootChildNames());
		assertTrue(application.entryExistsWithTitle("the bank"),
			"the entry of the root group is there");
		assertTrue(application.entryExistsWithTitle("the mail"),
			"the entry of the group mykeys is there");
		return application;
	}

	private File fixture(final String name) throws IOException
	{
		File copy = new File(tempHome, name);
		try (InputStream in = getClass().getResourceAsStream("/compat/vaults/" + name))
		{
			assertTrue(in != null, name + " is on the class path");
			Files.copy(in, copy.toPath());
		}
		return copy;
	}
}
