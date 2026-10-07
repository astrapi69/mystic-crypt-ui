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
package io.github.astrapi69.mystic.crypt.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The report of a failed save names the file it was aimed at, and naming it touches nothing on disk
 * (#512).
 * <p>
 * It named the file through {@code FileFactory.newFileQuietly}, which creates the file when it is
 * missing. So when the save failed because the file could not be created, the report failed the
 * same way, inside the catch that was there to report it, and no message appeared at all.
 */
class AFailedSaveIsNamedWithoutTouchingTheDiskTest
{

	@Test
	@DisplayName("a vault file that cannot be created is still named, without an exception")
	void aFileThatCannotBeCreatedIsNamed(@TempDir File directory) throws Exception
	{
		File notADirectory = new File(directory, "not-a-directory");
		Files.writeString(notADirectory.toPath(), "a file, so nothing can be created beneath it");
		File vault = new File(notADirectory, "unreachable.mcrdb");

		String named = GuardedSave.whereItWouldHaveGone(aVaultSavedTo(vault));

		assertEquals(vault.getAbsolutePath(), named);
	}

	@Test
	@DisplayName("naming a vault file that does not exist creates neither the file nor its directory")
	void namingAMissingFileCreatesNothing(@TempDir File directory)
	{
		File missingDirectory = new File(directory, "gone");
		File vault = new File(missingDirectory, "vault.mcrdb");

		String named = GuardedSave.whereItWouldHaveGone(aVaultSavedTo(vault));

		assertEquals(vault.getAbsolutePath(), named);
		assertFalse(vault.exists(), "the report created the file it names");
		assertFalse(missingDirectory.exists(), "the report created the directory of the file");
	}

	@Test
	@DisplayName("a vault without a file is named as such")
	void aVaultWithoutAFileSaysSo()
	{
		ApplicationModelBean noFile = ApplicationModelBean.builder().build();
		noFile.setMasterPwFileModelBean(MasterPwFileModelBean.builder().build());

		assertNull(noFile.getMasterPwFileModelBean().getApplicationFileInfo());
		assertEquals("no file is set for this database", GuardedSave.whereItWouldHaveGone(noFile));
		assertTrue(GuardedSave.whereItWouldHaveGone(null).startsWith("no file"));
	}

	private static ApplicationModelBean aVaultSavedTo(final File vault)
	{
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		applicationModelBean.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.masterPw(TestPasswords.throwaway().toCharArray()).withMasterPw(true)
				.withKeyFile(false).build());
		return applicationModelBean;
	}
}
