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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A database with a master password and a key file reads its key file as PEM only, so the dialogs
 * that create and open one are not to offer it a DER key file (#943). A database with its key file
 * alone keeps taking DER.
 * <p>
 * PIN of the behaviour before the fix, green on it: with the master password checked, both dialogs
 * take a DER key file and enable OK, and so does checking the master password after a DER key file
 * was chosen. The fix turns those three; the key-file-only case stays.
 */
class APasswordDatabaseTakesOnlyAPemKeyFileUiTest extends AbstractUiTest
{

	private final String password = TestPasswords.throwaway();

	private PrivateKey key;

	private File derKeyFile;

	@BeforeEach
	void aKeyInDer() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		key = KeyPairFactory.newKeyPair("RSA").getPrivate();
		derKeyFile = new File(tempHome, "database.der");
		try (OutputStream out = new FileOutputStream(derKeyFile))
		{
			PrivateKeyWriter.write(key, out, KeyFileFormat.DER, KeyFormat.PKCS_8);
		}
	}

	@Test
	@DisplayName("PIN: creating with a master password, a DER key file chosen: OK goes ahead")
	void create_takesADerKeyFile_withAMasterPassword()
	{
		launchApplication().startNewDatabase(new File(tempHome, "new.mcrdb")).checkMasterPassword()
			.typeMasterPasswordWithRepeat(password).checkKeyFile().browseKeyFile(derKeyFile)
			.requireOkEnabled();
	}

	@Test
	@DisplayName("PIN: signing in with a master password, a DER key file chosen: OK goes ahead")
	void signIn_takesADerKeyFile_withAMasterPassword() throws Exception
	{
		File database = aPasswordAndKeyFileDatabase();

		launchApplication().checkMasterPassword().typeMasterPassword(password).checkKeyFile()
			.browseKeyFile(derKeyFile).browseApplicationFile(database).requireOkEnabled();
	}

	@Test
	@DisplayName("PIN: a DER key file chosen first, then the master password checked: OK goes ahead")
	void signIn_takesADerKeyFile_whenTheMasterPasswordIsCheckedAfterIt() throws Exception
	{
		File database = aPasswordAndKeyFileDatabase();
		SignInDialogSteps signIn = launchApplication().uncheckMasterPassword().checkKeyFile()
			.browseKeyFile(derKeyFile).browseApplicationFile(database);

		signIn.checkMasterPassword().typeMasterPassword(password).requireOkEnabled();
	}

	@Test
	@DisplayName("signing in with the key file alone, a DER key file chosen: OK goes ahead, as opening takes it")
	void signIn_takesADerKeyFile_withTheKeyFileAlone() throws Exception
	{
		File database = aPasswordAndKeyFileDatabase();

		launchApplication().uncheckMasterPassword().checkKeyFile().browseKeyFile(derKeyFile)
			.browseApplicationFile(database).requireOkEnabled();
	}

	/** A database with a master password and a key file, made with a PEM copy of the key */
	private File aPasswordAndKeyFileDatabase() throws Exception
	{
		File pemKeyFile = new File(tempHome, "database.pem");
		PrivateKeyWriter.writeInPemFormat(key, pemKeyFile);
		File database = new File(tempHome, "database.mcrdb");
		Files.createFile(database.toPath());
		ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(database))
				.selectedApplicationFilePath(database.getAbsolutePath())
				.keyFileInfo(FileInfo.toFileInfo(pemKeyFile))
				.privateKeyInfo(KeyModelExtensions.toKeyModel(key)).masterPw(password.toCharArray())
				.withMasterPw(true).withKeyFile(true).minPasswordLength(6).build());
		return database;
	}
}
