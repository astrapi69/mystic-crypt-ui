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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A database is never written without the password that protects it (#294).
 * <p>
 * A locked workspace has no master password: locking replaces it with a verifier and clears the
 * field (#242). Until the master password stopped being turned into a String on the way to the
 * cipher, saving in that state failed by accident - {@code String.valueOf((char[])null)} throws.
 * With characters there is no accident left to rely on: {@link javax.crypto.spec.PBEKeySpec} reads
 * a null password as an EMPTY one and encrypts happily with it, which would replace a properly
 * encrypted database with one anybody can open.
 * <p>
 * {@code LockInvariantUiTest} asserts the same thing from the outside, through the real
 * application. This one says it about the writer itself, so the guarantee does not depend on which
 * callers exist today.
 */
class SavingNeedsTheMasterPasswordTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("saving without a master password is refused, and the file is left alone")
	void saveToFileWithPassword_refuses_whenThereIsNoMasterPassword(@TempDir File tempDir)
		throws Exception
	{
		File vaultFile = new File(tempDir, "locked.mcrdb");
		byte[] properlyEncrypted = PasswordVaultFormat.encrypt("<vault/>".toCharArray(),
			TestPasswords.throwawayChars());
		Files.write(vaultFile.toPath(), properlyEncrypted);
		ApplicationModelBean model = modelWithoutAMasterPassword(vaultFile);

		assertThrows(IllegalStateException.class,
			() -> ApplicationXmlFileStoreWorker.saveToFileWithPassword(model));

		assertArrayEquals(properlyEncrypted, Files.readAllBytes(vaultFile.toPath()),
			"the database that was there is still the database that is there. Writing one "
				+ "encrypted with an empty password over it is worse than not saving at all");
	}

	@Test
	@DisplayName("an empty master password counts as none")
	void saveToFileWithPassword_refuses_whenTheMasterPasswordIsEmpty(@TempDir File tempDir)
		throws Exception
	{
		File vaultFile = new File(tempDir, "empty-password.mcrdb");
		Files.createFile(vaultFile.toPath());
		ApplicationModelBean model = modelWithoutAMasterPassword(vaultFile);
		model.getMasterPwFileModelBean().setMasterPw(new char[0]);

		assertThrows(IllegalStateException.class,
			() -> ApplicationXmlFileStoreWorker.saveToFileWithPassword(model));
	}

	@Test
	@DisplayName("the password and key file path is refused for the same reason")
	void saveToFileWithPasswordAndPrivateKey_refuses_whenThereIsNoMasterPassword(
		@TempDir File tempDir) throws Exception
	{
		File vaultFile = new File(tempDir, "locked-with-key.mcrdb");
		Files.createFile(vaultFile.toPath());
		ApplicationModelBean model = modelWithoutAMasterPassword(vaultFile);
		model.getMasterPwFileModelBean().setWithKeyFile(true);

		assertThrows(IllegalStateException.class,
			() -> ApplicationXmlFileStoreWorker.saveToFileWithPasswordAndPrivateKey(model),
			"this path turned the missing password into the four letters of the word null and "
				+ "encrypted with those");
	}

	private static ApplicationModelBean modelWithoutAMasterPassword(final File vaultFile)
	{
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vaultFile))
			.selectedApplicationFilePath(vaultFile.getAbsolutePath()).withMasterPw(true)
			.withKeyFile(false).minPasswordLength(6).build();
		return ApplicationModelBean.builder().masterPwFileModelBean(credentials).signedIn(false)
			.build();
	}
}
