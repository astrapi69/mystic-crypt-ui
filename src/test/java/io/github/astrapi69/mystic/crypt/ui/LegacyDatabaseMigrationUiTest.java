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
import java.nio.file.Files;

import javax.crypto.Cipher;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.compound.CompoundAlgorithm;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.write.StoreFileExtensions;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.PasswordVaultFormat;
import io.github.astrapi69.mystic.crypt.file.PBEFileEncryptor;

/**
 * A database written by an older release must still open, and saving it must move it to the format
 * that is actually safe - through the real user interface, not through the format class alone.
 */
class LegacyDatabaseMigrationUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String NODE_NAME = "Node in the migrated database";

	/**
	 * Writes a database exactly the way every release up to 8.2 wrote it: PBEWithMD5AndDES, with
	 * the fixed salt from the published library and 19 iterations
	 */
	private File writeDatabaseInTheOldFormat(File directory, String password) throws Exception
	{
		// the old format has to end up at the path the database was created for, because the
		// fixture is built by decrypting and re-encrypting that very file in place
		File databaseFile = new File(directory, "legacy.mcrdb");
		createDatabaseFileHeadless(databaseFile, password);
		String xml = PasswordVaultFormat.decrypt(databaseFile, password);

		File plain = new File(directory, "plain.xml");
		StoreFileExtensions.toFile(plain, xml);
		CryptModel<Cipher, String, String> legacyModel = CryptModel
			.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).salt(CompoundAlgorithm.SALT)
			.iterationCount(CompoundAlgorithm.ITERATIONCOUNT).build();
		Files.delete(databaseFile.toPath());
		File written = new PBEFileEncryptor(legacyModel, databaseFile).encrypt(plain);
		Files.delete(plain.toPath());
		return written;
	}

	@Test
	void anOldDatabaseOpensAndMigratesWhenItIsSaved() throws Exception
	{
		File databaseFile = writeDatabaseInTheOldFormat(tempHome, MASTER_PASSWORD);
		assertFalse(PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(databaseFile.toPath())),
			"the fixture must really be in the old format, otherwise this test proves nothing");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, NODE_NAME);
		application.saveDatabase();

		// the save runs on the event thread, so the file is not necessarily written the instant the
		// menu item has been fired
		org.assertj.swing.timing.Pause.pause(
			new org.assertj.swing.timing.Condition("the database is written in the new format")
			{
				@Override
				public boolean test()
				{
					try
					{
						return PasswordVaultFormat
							.isCurrentFormat(Files.readAllBytes(databaseFile.toPath()));
					}
					catch (Exception exception)
					{
						return false;
					}
				}
			}, 20000);

		assertTrue(PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(databaseFile.toPath())),
			"saving must write the database in the format that authenticates and salts properly");

		shutdownApplication();
		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		reopened.showMainFrame();

		assertTrue(reopened.treeContainsNodeStartingWith(NODE_NAME),
			"nothing may be lost on the way from the old format to the new one");
	}
}
