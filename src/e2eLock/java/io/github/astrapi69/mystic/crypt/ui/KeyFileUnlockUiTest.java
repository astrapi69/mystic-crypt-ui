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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Unlocking demands the same factors as opening (#880): the key file for a key-file database, read
 * from the disk again - nothing of the key stays in memory while the workspace is locked - and the
 * password where the database has one. Before this, the unlock prompt asked a key-file-only
 * database for a master password it does not have, so it could never be unlocked.
 */
class KeyFileUnlockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a key-file-only workspace locks and unlocks with its key file, read from disk (#880)")
	void aKeyFileOnlyWorkspace_unlocksWithItsKeyFile() throws Exception
	{
		File databaseFile = new File(tempHome, "keyfile-only.mcrdb");
		File keyFile = new File(tempHome, "keyfile-only.pem");
		createKeyFileDatabaseHeadless(databaseFile, keyFile, null);
		ApplicationSteps application = signInWithKeyFileDatabase(databaseFile, keyFile, null);

		application.lockWorkspace();
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
		assertNotNull(MysticCryptApplicationFrame.getInstance().getModelObject()
			.getMasterPwFileModelBean().getLockKeyVerifier(),
			"locking must leave the key verifier behind (#880)");

		application.unlockWorkspaceWithKeyFile(null, keyFile.getAbsolutePath());

		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
		assertNotNull(
			MysticCryptApplicationFrame.getInstance().getModelObject().getMasterPwFileModelBean()
				.getPrivateKeyInfo(),
			"unlocking must read the key from the file again, saving needs it");
	}

	@Test
	@DisplayName("a password-and-key-file workspace is not unlocked by the password alone (#880)")
	void aPasswordAndKeyFileWorkspace_demandsBothToUnlock() throws Exception
	{
		File databaseFile = new File(tempHome, "pw-and-keyfile.mcrdb");
		File keyFile = new File(tempHome, "pw-and-keyfile.pem");
		createKeyFileDatabaseHeadless(databaseFile, keyFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithKeyFileDatabase(databaseFile, keyFile,
			MASTER_PASSWORD);

		application.lockWorkspace();
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());

		application.enterUnlockWithoutKeyFileExpectingFailure(MASTER_PASSWORD);
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"the password alone must not unlock a workspace opened with a key file (#880)");

		application.unlockWorkspaceWithKeyFile(MASTER_PASSWORD, keyFile.getAbsolutePath());
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}
}
