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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Regression tests for a database file that is not where it was when it was last saved.
 * <p>
 * A database carries its own path inside its encrypted xml. Opening a file that was moved, renamed
 * or copied must therefore not leave the application pointing at where the file used to be:
 * everything the user adds afterwards would be written there instead, while the file in front of
 * them stays as it was, and nothing would say so.
 */
class MovedDatabaseFileTest
{

	private static final char[] MASTER_PASSWORD = "moved-database-pw-123".toCharArray();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static MasterPwFileModelBean pointingAt(File file)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(file))
			.selectedApplicationFilePath(file.getAbsolutePath()).masterPw(MASTER_PASSWORD.clone())
			.withMasterPw(true).withKeyFile(false).minPasswordLength(6).build();
	}

	private static File newDatabase(File directory, String name) throws Exception
	{
		File file = new File(directory, name);
		Files.createFile(file.toPath());
		return ApplicationXmlFileFactory.newApplicationFileWithPassword(pointingAt(file));
	}

	/**
	 * After opening a file from a new location, the application must know that location - it is the
	 * one every later save depends on
	 */
	@Test
	@DisplayName("opening a moved database points the application at where the file now is")
	void openingAMovedDatabasePointsAtTheNewLocation(@TempDir File directory) throws Exception
	{
		File original = newDatabase(directory, "before-the-move.mcrdb");
		File moved = new File(directory, "after-the-move.mcrdb");
		Files.move(original.toPath(), moved.toPath());

		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPassword(pointingAt(moved));

		assertEquals(moved.getAbsolutePath(),
			FileInfo.toFile(opened.getMasterPwFileModelBean().getApplicationFileInfo())
				.getAbsolutePath(),
			"the opened database still points at where the file used to be");
	}

	/**
	 * The write has to land in the file the user opened. The old path is gone, so a save that goes
	 * there brings it back as a file the user believes they got rid of, holding their new entries.
	 */
	@Test
	@DisplayName("saving a moved database writes to the file that was opened")
	void savingAMovedDatabaseWritesToTheOpenedFile(@TempDir File directory) throws Exception
	{
		File original = newDatabase(directory, "gone.mcrdb");
		File moved = new File(directory, "here-now.mcrdb");
		Files.move(original.toPath(), moved.toPath());
		long lengthBeforeSave = moved.length();

		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPassword(pointingAt(moved));
		opened.setDirty(true);
		ApplicationXmlFileStoreWorker.storeApplicationFile(opened);

		assertFalse(original.exists(), "the save brought back the file at the old path");
		assertNotEquals(lengthBeforeSave, moved.length(),
			"the file that was opened was not written to");
	}

	/**
	 * The destructive variant: the original is still there, so the save overwrites a second
	 * database the user never opened - restoring a backup and adding one entry would take the live
	 * database with it
	 */
	@Test
	@DisplayName("saving a copied database leaves the file it was copied from alone")
	void savingACopiedDatabaseLeavesTheOriginalAlone(@TempDir File directory) throws Exception
	{
		File original = newDatabase(directory, "the-real-one.mcrdb");
		File copy = new File(directory, "the-copy.mcrdb");
		Files.copy(original.toPath(), copy.toPath());
		byte[] originalBefore = Files.readAllBytes(original.toPath());

		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPassword(pointingAt(copy));
		opened.setDirty(true);
		ApplicationXmlFileStoreWorker.storeApplicationFile(opened);

		assertTrue(java.util.Arrays.equals(originalBefore, Files.readAllBytes(original.toPath())),
			"the database that was never opened was overwritten");
	}
}
