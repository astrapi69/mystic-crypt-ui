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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
 * A save that cannot finish must not take the previous database with it.
 * <p>
 * Writing straight through the database file empties it before the new content exists. Measured on
 * a file system with no room left: 40,000 bytes of database became 102,400 bytes of half a
 * database, and the previous content was gone. A full disk, a quota, a stick pulled out or a power
 * cut all land in that window.
 */
class VaultFileWriterTest
{

	private static final String MASTER_PASSWORD = "throwaway-" + java.util.UUID.randomUUID();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/**
	 * The ordinary case: the content arrives, and nothing is left lying around
	 */
	@Test
	@DisplayName("the content arrives and no half written file stays behind")
	void theContentArrives(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "arrives.mcrdb");
		byte[] content = "the database, encrypted".getBytes(StandardCharsets.UTF_8);

		VaultFileWriter.write(vault, content);

		assertArrayEquals(content, Files.readAllBytes(vault.toPath()));
		assertEquals(1, directory.list().length,
			"something was left next to the database: " + String.join(", ", directory.list()));
	}

	/**
	 * Writing over an existing database replaces it whole
	 */
	@Test
	@DisplayName("an existing database is replaced whole")
	void anExistingDatabaseIsReplaced(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "replaced.mcrdb");
		Files.write(vault.toPath(), "the old database".getBytes(StandardCharsets.UTF_8));
		byte[] fresh = "the new database, longer than the old one".getBytes(StandardCharsets.UTF_8);

		VaultFileWriter.write(vault, fresh);

		assertArrayEquals(fresh, Files.readAllBytes(vault.toPath()));
	}

	/**
	 * The reason this class exists: a write that cannot complete leaves the previous database as it
	 * was, rather than a torn file that is neither.
	 * <p>
	 * A full disk cannot be arranged from inside a test, so the write is made to fail in the other
	 * way it can: the path it writes to is occupied by a directory.
	 */
	@Test
	@DisplayName("a save that cannot be written leaves the previous database untouched")
	void aFailedSaveLeavesThePreviousDatabase(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "survives.mcrdb");
		byte[] previous = "the database that must survive".getBytes(StandardCharsets.UTF_8);
		Files.write(vault.toPath(), previous);
		// occupy the path the writer needs, so the write fails after the point where writing
		// straight through the database would already have emptied it
		File blocked = new File(directory,
			"survives.mcrdb" + VaultFileWriter.WORK_IN_PROGRESS_SUFFIX);
		assertTrue(blocked.mkdir());

		assertThrows(Exception.class, () -> VaultFileWriter.write(vault,
			"the database that never arrives".getBytes(StandardCharsets.UTF_8)));

		assertArrayEquals(previous, Files.readAllBytes(vault.toPath()),
			"the previous database was destroyed by a save that could not be written");
	}

	/**
	 * And the same through the real save, so the guarantee holds where it matters and not only in
	 * the helper
	 */
	@Test
	@DisplayName("a database saved through the application keeps its previous content on failure")
	void aFailedApplicationSaveKeepsTheDatabase(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "through-the-app.mcrdb");
		Files.createFile(vault.toPath());
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.toCharArray()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
		ApplicationXmlFileFactory.newApplicationFileWithPassword(credentials);
		byte[] afterTheFirstSave = Files.readAllBytes(vault.toPath());
		assertTrue(afterTheFirstSave.length > 0);

		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPassword(credentials);
		opened.setLastId(4242L);
		File blocked = new File(directory,
			vault.getName() + VaultFileWriter.WORK_IN_PROGRESS_SUFFIX);
		assertTrue(blocked.mkdir());

		assertThrows(Exception.class,
			() -> ApplicationXmlFileStoreWorker.saveToFileWithPassword(opened));

		assertArrayEquals(afterTheFirstSave, Files.readAllBytes(vault.toPath()),
			"the database was destroyed by a save that could not be written");
		assertFalse(PasswordVaultFormat.decrypt(vault, MASTER_PASSWORD).isEmpty(),
			"the database no longer opens after a failed save");
	}
}
