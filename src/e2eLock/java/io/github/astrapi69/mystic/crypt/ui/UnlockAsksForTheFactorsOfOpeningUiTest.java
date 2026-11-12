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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Unlocking asks for the factors the database was opened with (#880): its master password where it
 * has one, its key file where it has one, read from the disk again. A database with a password and
 * a key file needs both, so a key file gone from the disk keeps the workspace locked; a database
 * with its key file alone unlocks with that, and no password is asked for.
 */
class UnlockAsksForTheFactorsOfOpeningUiTest extends AbstractUiTest
{

	private static final String KEY_FILE_NAME = "unlock-factors.pem";

	/** The one refusal of a database with a password and a key file, whichever factor is wrong */
	private static final String REFUSAL_OF_BOTH = "The master password or the key file is wrong.";

	/** The refusal of a database with its key file alone */
	private static final String REFUSAL_OF_THE_KEY_FILE = "This key file does not unlock this database.";

	@Test
	@DisplayName("a password plus key file database stays locked while its key file is gone, names it, creates nothing, and unlocks once it is back")
	void aCombinedDatabase_needsItsKeyFileOnTheDisk() throws Exception
	{
		String password = TestPasswords.throwaway();
		File keyFile = signInWithANewDatabase(password);
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();
		File pulledOut = new File(tempHome, "pulled-out.pem");
		Files.move(keyFile.toPath(), pulledOut.toPath());

		application.lockWorkspace();
		assertTrue(application.unlockPromptHas("txtUnlockPassword"));
		assertEquals(keyFile.getAbsolutePath(), application.unlockPromptKeyFile(),
			"preset with the key file the database was opened with");
		String refusal = application.unlockExpectingRefusal(password, null);

		assertEquals("The key file " + keyFile.getAbsolutePath() + " is not there. It was moved or "
			+ "deleted, or it is on a drive that is not connected. Put it back at this path and "
			+ "try again.", refusal, "the same words opening and saving use for it (#937)");
		assertFalse(keyFile.exists(), "and nothing was created where the key file was");
		assertFalse(signedIn(), "the password alone is not what this database was opened with");

		Files.move(pulledOut.toPath(), keyFile.toPath());
		application.unlockWorkspaceWith(password, null);

		assertTrue(signedIn());
		assertNotNull(credential(MasterPwFileModelBean::getPrivateKeyInfo),
			"the key is back where opening put it, for the next save");
		assertNull(credential(MasterPwFileModelBean::getKeyFileLockVerifier),
			"and the verifier that stood in for it is gone");
	}

	@Test
	@DisplayName("a password plus key file database refuses every wrong factor with one and the same message")
	void aCombinedDatabase_refusesEveryWrongFactorAlike() throws Exception
	{
		String password = TestPasswords.throwaway();
		String wrongPassword = TestPasswords.throwaway();
		File keyFile = signInWithANewDatabase(password);
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();
		File anotherKeyFile = anotherKeyFile();

		application.lockWorkspace();

		assertEquals(REFUSAL_OF_BOTH, application.unlockExpectingRefusal(password, anotherKeyFile),
			"its password, another key file");
		assertEquals(REFUSAL_OF_BOTH, application.unlockExpectingRefusal(wrongPassword, keyFile),
			"a wrong password, its key file: the same words, so the refusal does not say which "
				+ "factor was right");
		assertEquals(REFUSAL_OF_BOTH,
			application.unlockExpectingRefusal(wrongPassword, anotherKeyFile),
			"a wrong password, another key file");
		assertFalse(signedIn());
		application.unlockWorkspaceWith(password, keyFile);
		assertTrue(signedIn());
	}

	@Test
	@DisplayName("a key-file-only database asks for its key file and no password, and unlocks with it")
	void aKeyFileOnlyDatabase_unlocksWithItsKeyFile() throws Exception
	{
		File keyFile = signInWithANewDatabase(null);
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();

		application.lockWorkspace();

		assertNull(credential(MasterPwFileModelBean::getPrivateKeyInfo),
			"while locked the key is not in memory");
		assertNotNull(credential(MasterPwFileModelBean::getKeyFileLockVerifier),
			"only what recognises it");
		assertFalse(application.unlockPromptHas("txtUnlockPassword"),
			"this database has no master password to ask for");
		assertEquals(keyFile.getAbsolutePath(), application.unlockPromptKeyFile());
		assertEquals(REFUSAL_OF_THE_KEY_FILE,
			application.unlockExpectingRefusal(null, anotherKeyFile()));
		assertFalse(signedIn());

		application.unlockWorkspaceWith(null, keyFile);

		assertTrue(signedIn());
		assertNotNull(credential(MasterPwFileModelBean::getPrivateKeyInfo));
	}

	private File anotherKeyFile() throws Exception
	{
		File anotherKeyFile = new File(tempHome, "another.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(),
			anotherKeyFile);
		return anotherKeyFile;
	}

	/**
	 * Creates a database with a key file, and a master password when one is given, and signs in
	 * with what it was created with
	 *
	 * @param password
	 *            the master password, or null for a database that opens with its key file alone
	 * @return the key file
	 */
	private File signInWithANewDatabase(final String password) throws Exception
	{
		File database = new File(tempHome, "unlock-factors.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(database);
		if (password != null)
		{
			createMasterKey.checkMasterPassword().typeMasterPasswordWithRepeat(password);
		}
		createMasterKey.checkKeyFile().createKeyFile(KEY_FILE_NAME).okAndAwaitClose();
		File keyFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(), KEY_FILE_NAME);
		if (password != null)
		{
			signIn.checkMasterPassword().typeMasterPassword(password);
		}
		else
		{
			signIn.uncheckMasterPassword();
		}
		signIn.checkKeyFile().browseKeyFile(keyFile).okAndAwaitSignIn();
		return keyFile;
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	private static Object credential(
		final java.util.function.Function<MasterPwFileModelBean, Object> field)
	{
		return GuiActionRunner.execute(() -> field.apply(
			MysticCryptApplicationFrame.getInstance().getModelObject().getMasterPwFileModelBean()));
	}
}
