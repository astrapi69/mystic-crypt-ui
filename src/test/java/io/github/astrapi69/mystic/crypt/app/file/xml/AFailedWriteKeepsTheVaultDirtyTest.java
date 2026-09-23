/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A write that fails leaves the vault dirty (#424).
 * <p>
 * {@code storeApplicationFile} cleared the dirty flag before it wrote anything, so a write that
 * then threw - a full disk, a directory the user cannot write, a share that went away - left the
 * model saying it had been saved. {@code SaveBeforeCloseConfirmation} reads exactly that flag, so
 * ending the application afterwards discarded everything since the last successful save without
 * asking. {@code VaultFormatVersionTest} pins the same property for the read-only refusal, which
 * happens BEFORE the flag is cleared; this class pins it for the write itself.
 */
class AFailedWriteKeepsTheVaultDirtyTest
{

	private static final String ENTRY_TITLE = "survives-a-failed-write";

	@Test
	@DisplayName("a write into a directory that cannot be written leaves the changes unsaved")
	void aFailedWrite_leavesTheModelDirty(@TempDir File directory) throws Exception
	{
		File readOnlyDirectory = new File(directory, "read-only");
		assertTrue(readOnlyDirectory.mkdir());
		File vault = new File(readOnlyDirectory, "unwritable.mcrdb");
		ApplicationModelBean applicationModelBean = aDirtyModelSavedTo(vault);
		Files.setPosixFilePermissions(readOnlyDirectory.toPath(),
			PosixFilePermissions.fromString("r-xr-xr-x"));
		try
		{
			assertThrows(RuntimeException.class,
				() -> ApplicationXmlFileStoreWorker.storeApplicationFile(applicationModelBean),
				"the precondition: the write really fails");

			assertTrue(applicationModelBean.isDirty(),
				"the changes are still unsaved after a write that did not happen. The close "
					+ "question reads this flag, so clearing it here ends the application without "
					+ "asking and drops them (#424)");
			assertFalse(vault.exists(), "and nothing was written");
		}
		finally
		{
			Files.setPosixFilePermissions(readOnlyDirectory.toPath(),
				PosixFilePermissions.fromString("rwxr-xr-x"));
		}
	}

	@Test
	@DisplayName("a write that succeeds marks the changes as saved")
	void aSuccessfulWrite_clearsTheDirtyFlag(@TempDir File directory)
	{
		File vault = new File(directory, "writable.mcrdb");
		ApplicationModelBean applicationModelBean = aDirtyModelSavedTo(vault);

		ApplicationXmlFileStoreWorker.storeApplicationFile(applicationModelBean);

		assertFalse(applicationModelBean.isDirty(),
			"the other half: a write that happened does clear the flag, or every ending would ask "
				+ "about changes that are on disk");
		assertTrue(vault.exists() && vault.length() > 0, "and the file is there");
	}

	private static ApplicationModelBean aDirtyModelSavedTo(final File vault)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title(ENTRY_TITLE.toCharArray()).password(TestPasswords.throwaway().toCharArray())
			.build();
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entries.add(entry);
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new HashMap<>();
		dataOfNodes.put(1L, entries);
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.dataOfNodes(dataOfNodes).build();
		applicationModelBean.setDirty(true);
		applicationModelBean.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.masterPw(TestPasswords.throwaway().toCharArray()).withMasterPw(true)
				.withKeyFile(false).build());
		return applicationModelBean;
	}
}
