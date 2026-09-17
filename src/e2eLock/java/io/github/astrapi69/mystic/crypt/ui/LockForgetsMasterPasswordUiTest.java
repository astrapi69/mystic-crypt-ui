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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileReader;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A locked workspace used to keep the master password itself in memory, because unlocking compared
 * the typed characters against it - so anything able to read the process could read the password
 * out of a workspace its owner had locked (#242).
 * <p>
 * What replaces it has to satisfy two things at once, and both are asserted here: the password is
 * really gone while locked, and unlocking still produces a workspace that can save its database,
 * which re-encrypts with exactly that password (ApplicationXmlFileStoreWorker:145). The second is
 * proven by a round trip through the real file - saved, then read back with the reader the sign-in
 * uses - rather than by looking at the model that is still in memory.
 */
class LockForgetsMasterPasswordUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static MasterPwFileModelBean credentials()
	{
		return GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.getModelObject().getMasterPwFileModelBean());
	}

	@Test
	@DisplayName("locking takes the master password out of memory and leaves a verifier")
	void lockingForgetsTheMasterPassword() throws IOException
	{
		File databaseFile = new File(tempHome, "forget-master-pw.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		assertNotNull(credentials().getMasterPw(),
			"precondition: a signed-in workspace holds the master password, because saving needs it");

		application.lockWorkspace();

		assertNull(credentials().getMasterPw(),
			"a locked workspace must not carry the password that opens its database");
		assertNotNull(credentials().getLockVerifier(),
			"and it must carry what recognises that password again, or it could never be unlocked");
	}

	@Test
	@DisplayName("unlocking restores what saving needs: lock, unlock, save, read the file back")
	void unlockingRestoresAWorkspaceThatCanStillSave() throws IOException
	{
		File databaseFile = new File(tempHome, "forget-master-pw-roundtrip.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "BeforeTheLock");

		application.lockWorkspace();
		application.unlockWorkspace(MASTER_PASSWORD);

		assertNotNull(credentials().getMasterPw(),
			"unlocking has to put the password back, or the next save cannot encrypt");
		assertNull(credentials().getLockVerifier(),
			"and the verifier has done its job, so it is gone again");

		application.addNodeToTreeRoot(application.showMainFrame(), "AfterTheUnlock");
		application.saveDatabase();

		ApplicationModelBean reopened = readTheFileBack(databaseFile);
		assertTrue(containsNodeNamed(reopened, "BeforeTheLock"),
			"the node from before the lock has to be in the file that was just written");
		assertTrue(containsNodeNamed(reopened, "AfterTheUnlock"),
			"and so has the one added after unlocking - which proves the restored password "
				+ "encrypted a file the reader can open again");
	}

	/**
	 * Reads the database the way signing in reads it, so the round trip goes through the real
	 * format rather than through the model that is still in memory
	 *
	 * @param databaseFile
	 *            the file that was just saved
	 * @return what the reader makes of it
	 */
	private static ApplicationModelBean readTheFileBack(final File databaseFile)
	{
		return ApplicationXmlFileReader.read(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(databaseFile))
				.selectedApplicationFilePath(databaseFile.getAbsolutePath())
				.masterPw(MASTER_PASSWORD.toCharArray()).withMasterPw(true).withKeyFile(false)
				.minPasswordLength(6).build());
	}

	private static boolean containsNodeNamed(final ApplicationModelBean model, final String name)
	{
		return model != null && model.getRootTreeAsMap() != null
			&& model.getRootTreeAsMap().values().stream().anyMatch(node -> node.getValue() != null
				&& node.getValue().getName() != null && node.getValue().getName().equals(name));
	}
}
