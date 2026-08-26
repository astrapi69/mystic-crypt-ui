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
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * The password that opens the database has no business being inside it.
 * <p>
 * It was serialized like any other property, so every decrypted copy of a database - an export, a
 * support request, a debugging session - carried the credential as well as the data. It is not
 * needed there either: the application knows the password because someone just typed it.
 */
class MasterPasswordIsNotStoredTest
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

	private static MasterPwFileModelBean credentialsFor(File vault)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.toCharArray()).repeatPw(MASTER_PASSWORD.toCharArray())
			.withMasterPw(true).withKeyFile(false).minPasswordLength(6).build();
	}

	private static File newDatabase(File directory) throws Exception
	{
		File vault = new File(directory, "without-its-own-password.mcrdb");
		Files.createFile(vault.toPath());
		return ApplicationXmlFileFactory.newApplicationFileWithPassword(credentialsFor(vault));
	}

	/**
	 * What is inside the encrypted file must not include the password that opens it
	 */
	@Test
	@DisplayName("the database does not carry the password that opens it")
	void theDatabaseDoesNotCarryItsOwnPassword(@TempDir File directory) throws Exception
	{
		File vault = newDatabase(directory);

		String xml = PasswordVaultFormat.decrypt(vault, MASTER_PASSWORD);

		assertFalse(xml.contains(MASTER_PASSWORD), "the master password is stored in the database");
		assertFalse(xml.contains("<masterPw>") || xml.contains("<repeatPw>"),
			"the password fields are written into the database: " + xml);
	}

	/**
	 * Taking the password out of the file must not take it out of the running application: the
	 * model that comes back from a read carries the credentials that were used to open it, so the
	 * next save and the unlock dialog still have them
	 */
	@Test
	@DisplayName("opening carries the credentials that were used, so saving still works")
	void openingCarriesTheCredentialsThatWereUsed(@TempDir File directory) throws Exception
	{
		File vault = newDatabase(directory);

		ApplicationModelBean opened = ApplicationXmlFileReader
			.readApplicationFileWithPassword(credentialsFor(vault));

		assertNotNull(opened.getMasterPwFileModelBean().getMasterPw(),
			"the opened database has no password to save itself with");
		assertArrayEquals(MASTER_PASSWORD.toCharArray(),
			opened.getMasterPwFileModelBean().getMasterPw());

		// and it really can save itself again, which is what the credentials are for
		opened.setDirty(true);
		ApplicationXmlFileStoreWorker.storeApplicationFile(opened);
		ApplicationModelBean reopened = ApplicationXmlFileReader
			.readApplicationFileWithPassword(credentialsFor(vault));
		assertEquals(vault.getAbsolutePath(),
			FileInfo.toFile(reopened.getMasterPwFileModelBean().getApplicationFileInfo())
				.getAbsolutePath());
		assertFalse(PasswordVaultFormat.decrypt(vault, MASTER_PASSWORD).contains(MASTER_PASSWORD),
			"saving put the password back into the database");
	}
}
