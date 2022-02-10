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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.security.PrivateKey;
import java.security.Security;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.file.search.PathFinder;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.io.file.FileExtension;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

public class ApplicationFileWithKeyFactoryTest
{

	PrivateKey derPrivateKey;
	PrivateKey pemPrivateKey;

	File derDir;
	File pemDir;

	File privateKeyDerFile;
	File privateKeyPemFile;

	File applicationFile;
	String selectedApplicationFilePath;
	File decryptedApplicationFile;
	CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
	PrivateKeyDecryptor decryptor;
	PrivateKeyGenericDecryptor<String> genericDecryptor;

	@BeforeEach
	void setUp()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		applicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
			"empty-db-with-key" + FileExtension.MYSTIC_CRYPT_ENCRYPTED.getExtension());
		selectedApplicationFilePath = applicationFile.getAbsolutePath();
		decryptedApplicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
			"empty-db-with-key.json");

		pemDir = new File(PathFinder.getSrcTestResourcesDir(), "pem");
		privateKeyPemFile = new File(pemDir, "private.pem");

		pemPrivateKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyReader.readPemPrivateKey(privateKeyPemFile));
		decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(pemPrivateKey).build();
		decryptor = RuntimeExceptionDecorator.decorate(() -> new PrivateKeyDecryptor(decryptModel));
		genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);

		derDir = new File(PathFinder.getSrcTestResourcesDir(), "der");
		privateKeyDerFile = new File(derDir, "private.der");
		derPrivateKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyReader.readPrivateKey(privateKeyDerFile));

	}

	@AfterEach
	void tearDown()
	{
		RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(applicationFile));
		RuntimeExceptionDecorator
			.decorate(() -> DeleteFileExtensions.delete(decryptedApplicationFile));
		selectedApplicationFilePath = null;
		decryptModel = null;
	}

	@Test
	// @Disabled
	void newApplicationFileWithPrivateKey() throws Exception
	{
		// define parameter for the unit test
		File actualEncryptedFile;
		MasterPwFileModelBean modelObject;
		ApplicationModelBean applicationModelBean;
		// create test data
		modelObject = MasterPwFileModelBean.builder().applicationFile(applicationFile)
			.privateKey(pemPrivateKey).keyFile(privateKeyPemFile)
			.applicationFilePaths(ListFactory.newArrayList(""))
			.keyFilePaths(ListFactory.newArrayList("")).build();
		// test the actual method
		actualEncryptedFile = ApplicationFileFactory.newApplicationFileWithPrivateKey(modelObject);

		// proof that method is working as expected
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(actualEncryptedFile);
		String json = genericDecryptor.decrypt(encryptedBytes);
		applicationModelBean = JsonStringToObjectExtensions.toObject(json,
			ApplicationModelBean.class);
		assertNotNull(applicationModelBean);
		MasterPwFileModelBean masterPwFileModelBean = applicationModelBean
			.getMasterPwFileModelBean();
		assertEquals(modelObject, masterPwFileModelBean);

		ApplicationModelBean modelBeanReaded = ApplicationFileReader
			.readApplicationFileWithPrivateKey(modelObject);
		assertNotNull(modelBeanReaded);
		assertEquals(applicationModelBean, modelBeanReaded);
	}

}
