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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.list.ListFactory;
import io.github.astrapi69.gson.ObjectToJsonFileExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MemoizedSigninModelBean;

/**
 * A remembered sign-in whose key file path is empty still starts the application and signs in
 * (#499).
 * <p>
 * It ended the application before the sign-in dialog appeared, on JDK 24 and later, with a
 * NullPointerException in {@code MasterPwWithApplicationFilePanel.onInitializeComponents}: the
 * empty path names the current directory there, which exists, and was taken for a key file. The way
 * out was deleting {@code memoizedSignin.json} by hand. The application does not write that value
 * itself (measured in #499); a hand edit or another tool does.
 */
class ARememberedBlankKeyFilePathStillSignsInUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a remembered empty key file path still shows the sign-in dialog, and signing in works")
	void aBlankRememberedKeyFilePathStillSignsIn() throws IOException
	{
		File databaseFile = new File(tempHome, "remembered.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		rememberASignInWithAnEmptyKeyFilePath(databaseFile);

		SignInDialogSteps signIn = launchApplication();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();

		MasterPwFileModelBean signedIn = GuiActionRunner.execute(() -> MysticCryptApplicationFrame
			.getInstance().getModelObject().getMasterPwFileModelBean());
		assertEquals(databaseFile.getAbsoluteFile(),
			signedIn.getApplicationFileInfo().toFile().getAbsoluteFile(),
			"signed in to the remembered vault");
		assertNull(signedIn.getKeyFileInfo(),
			"and the empty remembered path was not taken for a key file");
	}

	/**
	 * The file a hand edit leaves: the vault remembered, the key file path empty. Written by the
	 * same writer the application uses, into the configuration directory of the test's own home
	 */
	private void rememberASignInWithAnEmptyKeyFilePath(final File databaseFile) throws IOException
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		assertTrue(configurationDirectory.mkdirs());
		MemoizedSigninModelBean remembered = MemoizedSigninModelBean.builder()
			.selectedApplicationFilePath(databaseFile.getAbsolutePath())
			.applicationFilePaths(ListFactory.newArrayList(databaseFile.getAbsolutePath()))
			.selectedKeyFilePath("").keyFilePaths(ListFactory.newArrayList("")).withKeyFile(false)
			.withMasterPw(true).build();
		ObjectToJsonFileExtensions.toJsonFile(remembered, new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME));
	}
}
