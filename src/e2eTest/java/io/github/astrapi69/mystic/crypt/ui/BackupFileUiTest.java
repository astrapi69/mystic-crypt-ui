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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The backup file (#847), TC-0020 and TC-0021 of the test plan, driven through the running
 * application: a save of an opened database keeps the previous state as the sibling {@code .bak},
 * and the next successful sign-in removes it.
 * <p>
 * The unit tests call the writer and the reader directly. What only the application can show is the
 * wiring: that the Save menu reaches the writer on every one of the store worker's three save paths
 * (password, key file, password and key file), and that the sign-in dialog opens through the reader
 * that removes the backup. Three databases, since the paths are separate lines of the store worker.
 * <p>
 * Each vault sits in a directory of its own, because the application's home holds its settings and
 * plugins too, and a listing of the home would never be exactly the database and its backup.
 */
class BackupFileUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String KEY_FILE_NAME = "backup-file.pem";

	private static final String NODE_NAME = "Node added after the sign-in";

	/** One way to get a signed-in application on a given database file */
	private interface SignIn
	{
		ApplicationSteps on(File database) throws Exception;
	}

	@Test
	@DisplayName("a save of a password database keeps the previous state as the .bak, the next sign-in removes it")
	void aSaveOfAPasswordDatabaseKeepsThePreviousStateAsTheSiblingBackup() throws Exception
	{
		saveKeepsTheBackupAndTheNextSignInRemovesIt("vault-password", database -> {
			createDatabaseFileHeadless(database, MASTER_PASSWORD);
			return signInWithExistingDatabase(database, MASTER_PASSWORD);
		}, database -> signInWithExistingDatabase(database, MASTER_PASSWORD));
	}

	@Test
	@DisplayName("a save of a key file database keeps the previous state as the .bak, the next sign-in removes it")
	void aSaveOfAKeyFileDatabaseKeepsThePreviousStateAsTheSiblingBackup() throws Exception
	{
		saveKeepsTheBackupAndTheNextSignInRemovesIt("vault-key-file", database -> {
			SignInDialogSteps signIn = launchApplication();
			signIn.startNewDatabase(database).checkKeyFile().createKeyFile(KEY_FILE_NAME)
				.okAndAwaitClose();
			signIn.uncheckMasterPassword().checkKeyFile().browseKeyFile(keyFile())
				.okAndAwaitSignIn();
			return new ApplicationSteps(robot).awaitSignedIn();
		}, database -> {
			SignInDialogSteps signIn = launchApplication();
			signIn.uncheckMasterPassword().checkKeyFile().browseKeyFile(keyFile())
				.browseApplicationFile(database).okAndAwaitSignIn();
			return new ApplicationSteps(robot).awaitSignedIn();
		});
	}

	@Test
	@DisplayName("a save of a password and key file database keeps the previous state as the .bak, the next sign-in removes it")
	void aSaveOfAPasswordAndKeyFileDatabaseKeepsThePreviousStateAsTheSiblingBackup()
		throws Exception
	{
		saveKeepsTheBackupAndTheNextSignInRemovesIt("vault-password-and-key-file", database -> {
			SignInDialogSteps signIn = launchApplication();
			signIn.startNewDatabase(database).checkMasterPassword()
				.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile()
				.createKeyFile(KEY_FILE_NAME).okAndAwaitClose();
			signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).checkKeyFile()
				.browseKeyFile(keyFile()).okAndAwaitSignIn();
			return new ApplicationSteps(robot).awaitSignedIn();
		}, database -> {
			SignInDialogSteps signIn = launchApplication();
			signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).checkKeyFile()
				.browseKeyFile(keyFile()).browseApplicationFile(database).okAndAwaitSignIn();
			return new ApplicationSteps(robot).awaitSignedIn();
		});
	}

	private static File keyFile()
	{
		return new File(MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			KEY_FILE_NAME);
	}

	private void saveKeepsTheBackupAndTheNextSignInRemovesIt(final String directoryName,
		final SignIn firstSignIn, final SignIn secondSignIn) throws Exception
	{
		File directory = Files.createDirectories(tempHome.toPath().resolve(directoryName)).toFile();
		File database = new File(directory, "backup-case.mcrdb");
		File backup = new File(directory, database.getName() + ".bak");

		FrameFixture frame = firstSignIn.on(database).showMainFrame();
		assertFalse(backup.exists(), "a sign-in leaves no backup of the file it has just read");
		byte[] stateBeforeTheSave = Files.readAllBytes(database.toPath());

		new ApplicationSteps(robot).addNodeToTreeRoot(frame, NODE_NAME);
		new ApplicationSteps(robot).saveDatabase();

		assertTrue(backup.exists(), "the save kept no sibling .bak of the previous state (#847)");
		assertArrayEquals(stateBeforeTheSave, Files.readAllBytes(backup.toPath()),
			"the .bak does not hold the file as it was before the save");
		assertFalse(Arrays.equals(stateBeforeTheSave, Files.readAllBytes(database.toPath())),
			"the save did not write the database");
		assertEquals(Set.of(database.getName(), backup.getName()), Set.of(directory.list()),
			"next to the database there must be its backup and nothing else");

		shutdownApplication();
		ApplicationSteps reopened = secondSignIn.on(database);
		reopened.showMainFrame();

		assertFalse(backup.exists(), "a successful sign-in must remove the .bak (#847)");
		assertTrue(reopened.treeContainsNodeStartingWith(NODE_NAME),
			"what was saved is missing after the reopen");
	}
}
