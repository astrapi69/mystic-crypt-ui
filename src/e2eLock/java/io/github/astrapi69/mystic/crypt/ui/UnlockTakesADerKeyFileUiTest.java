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
import java.nio.file.Files;
import java.security.KeyPair;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Opening a key-file-only database accepts a binary DER key file (the non-PEM branch of the
 * reader), so the unlock prompt accepts the same file: a database opened with a DER key file is
 * locked and unlocked with exactly that file (#880, the review of #936). A COMBINED database takes
 * only PEM at opening and saving (ApplicationXmlFileReader line 333, ApplicationXmlFileStoreWorker
 * line 160), so there is no combined DER database to unlock - measured here: creating one fails in
 * the factory.
 */
class UnlockTakesADerKeyFileUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("a key-file-only database with a DER key file unlocks with that file")
	void aDerKeyFileOnlyDatabase_unlocksWithItsKeyFile() throws Exception
	{
		File keyFile = new File(tempHome, "database.der");
		File database = createDatabase(keyFile, null);
		signInWithKeyFile(database, keyFile, null);
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();

		application.lockWorkspace();
		application.unlockWorkspaceWith(null, keyFile);

		assertTrue(signedIn(), "the DER key file that opened the database must unlock it");
	}

	/** Creates the database headless, with the private key written as binary DER */
	private File createDatabase(final File keyFile, final String password) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA", 2048);
		Files.write(keyFile.toPath(), keyPair.getPrivate().getEncoded());
		File database = new File(tempHome, keyFile.getName() + ".mcrdb");
		Files.createFile(database.toPath());
		MasterPwFileModelBean.MasterPwFileModelBeanBuilder<?, ?> builder = MasterPwFileModelBean
			.builder().applicationFileInfo(FileInfo.toFileInfo(database))
			.selectedApplicationFilePath(database.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile))
			.privateKeyInfo(KeyModelExtensions.toKeyModel(keyPair.getPrivate())).withKeyFile(true)
			.minPasswordLength(6);
		if (password != null)
		{
			ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(
				builder.masterPw(password.toCharArray()).withMasterPw(true).build());
		}
		else
		{
			ApplicationXmlFileFactory
				.newApplicationFileWithPrivateKey(builder.withMasterPw(false).build());
		}
		return database;
	}

	private void signInWithKeyFile(final File database, final File keyFile, final String password)
	{
		SignInDialogSteps signIn = launchApplication();
		if (password != null)
		{
			signIn.checkMasterPassword().typeMasterPassword(password);
		}
		else
		{
			signIn.uncheckMasterPassword();
		}
		signIn.checkKeyFile().browseKeyFile(keyFile).browseApplicationFile(database)
			.okAndAwaitSignIn();
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}
}
