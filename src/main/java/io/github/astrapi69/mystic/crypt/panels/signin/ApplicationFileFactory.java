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
package io.github.astrapi69.mystic.crypt.panels.signin;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import io.github.astrapi69.crypto.algorithm.AesAlgorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.SecretKeyFactoryExtensions;
import io.github.astrapi69.crypto.file.PBEFileEncryptor;
import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.PublicKeyEncryptor;
import io.github.astrapi69.crypto.key.PublicKeyGenericEncryptor;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.crypto.pw.PasswordStringEncryptor;
import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.file.system.SystemFileExtensions;
import io.github.astrapi69.file.write.WriteFileExtensions;
import io.github.astrapi69.gson.ObjectToJsonExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.random.number.RandomIntFactory;
import io.github.astrapi69.random.object.RandomStringFactory;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

public class ApplicationFileFactory
{

	public static final String MCRDB_FILE_EXTENSION = ".mcrdb";

	public static File newApplicationFileWithPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		PublicKey publicKey;
		PrivateKey privateKey;
		SecretKey symmetricKey;
		PublicKeyEncryptor encryptor;
		PublicKeyGenericEncryptor<String> genericEncryptor;
		CryptModel<Cipher, PublicKey, byte[]> encryptModel;
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel;
		File applicationFile;
		String json;
		byte[] encrypt;
		ApplicationModelBean applicationModelBean;

		applicationModelBean = ApplicationModelBean.builder().build();
		applicationFile = modelObject.getApplicationFile();

		applicationModelBean.setMasterPwFileModelBean(modelObject);

		if(modelObject.getPrivateKey() != null){
			privateKey = modelObject.getPrivateKey();
		} else {
			privateKey = RuntimeExceptionDecorator
				.decorate(() -> PrivateKeyReader.readPemPrivateKey(modelObject.getKeyFile()));
		}

		publicKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyExtensions.generatePublicKey(privateKey));
		encryptModel = CryptModel.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();
		symmetricKey = RuntimeExceptionDecorator.decorate(
			() -> SecretKeyFactoryExtensions.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128));
		symmetricKeyModel = CryptModel.<Cipher, SecretKey, String> builder().key(symmetricKey)
			.algorithm(AesAlgorithm.AES).operationMode(Cipher.ENCRYPT_MODE).build();

		encryptor = RuntimeExceptionDecorator
			.decorate(() -> new PublicKeyEncryptor(encryptModel, symmetricKeyModel));
		genericEncryptor = new PublicKeyGenericEncryptor<>(encryptor);
		applicationModelBean.getMasterPwFileModelBean().setPrivateKey(null);
		json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(applicationModelBean));

		RuntimeExceptionDecorator
			.decorate(() -> WriteFileExtensions.string2File(applicationFile, json));

		encrypt = RuntimeExceptionDecorator.decorate(() -> genericEncryptor.encrypt(json));

		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encrypt, applicationFile));
		return applicationFile;
	}

	public static File newApplicationFileWithPasswordAndPrivateKey(MasterPwFileModelBean modelObject)
	{
		PrivateKey privateKey;
		PublicKey publicKey;
		SecretKey symmetricKey;
		PublicKeyEncryptor encryptor;
		PublicKeyGenericEncryptor<String> genericEncryptor;
		CryptModel<Cipher, PublicKey, byte[]> encryptModel;
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel;
		File applicationFile;
		String json;
		String encryptedJson;
		ApplicationModelBean applicationModelBean;
		PasswordStringEncryptor passwordStringEncryptor;
		char[] masterPw;
		
		applicationModelBean = ApplicationModelBean.builder().build();
		applicationFile = modelObject.getApplicationFile();

		applicationModelBean.setMasterPwFileModelBean(modelObject);

		privateKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyReader.readPemPrivateKey(modelObject.getKeyFile()));

		masterPw = modelObject.getMasterPw();

		publicKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyExtensions.generatePublicKey(privateKey));

		encryptModel = CryptModel.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();

		symmetricKey = RuntimeExceptionDecorator.decorate(
			() -> SecretKeyFactoryExtensions.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128));

		symmetricKeyModel = CryptModel.<Cipher, SecretKey, String> builder().key(symmetricKey)
			.algorithm(AesAlgorithm.AES).operationMode(Cipher.ENCRYPT_MODE).build();

		encryptor = RuntimeExceptionDecorator
			.decorate(() -> new PublicKeyEncryptor(encryptModel, symmetricKeyModel));
		

		genericEncryptor = new PublicKeyGenericEncryptor<>(encryptor);

		passwordStringEncryptor = new PasswordStringEncryptor(String.valueOf(masterPw));
		applicationModelBean.getMasterPwFileModelBean().setPrivateKey(null);

		json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(applicationModelBean));
		
		encryptedJson = RuntimeExceptionDecorator
				.decorate(() -> passwordStringEncryptor.encrypt(json));

		byte[] encrypt = RuntimeExceptionDecorator.decorate(() -> genericEncryptor.encrypt(encryptedJson));

		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encrypt, applicationFile));
		return applicationFile;
	}

	public static File newApplicationFileWithPassword(final MasterPwFileModelBean modelObject)
	{
		PBEFileEncryptor encryptor;
		String password;
		CryptModel<Cipher, String, String> cryptModel;
		ApplicationModelBean applicationModelBean;
		String randomFilename;
		File tempJsonFile;
		File applicationFile;
		String json;
		
		applicationModelBean = ApplicationModelBean.builder().build();
		applicationModelBean.setMasterPwFileModelBean(modelObject);

		randomFilename = RandomStringFactory
			.newRandomLongString(RandomIntFactory.randomIntBetween(4, 8)) + "."
			+ RandomStringFactory.newRandomLongString(RandomIntFactory.randomIntBetween(2, 4));
		tempJsonFile = new File(SystemFileExtensions.getTempDir(), randomFilename);
		RuntimeExceptionDecorator
			.decorate(() -> FileFactory.newFile(tempJsonFile));
		applicationFile = modelObject.getApplicationFile();

		password = String.valueOf(modelObject.getMasterPw());

		cryptModel = CryptModel.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).build();
		encryptor = RuntimeExceptionDecorator
			.decorate(() -> new PBEFileEncryptor(cryptModel,
				applicationFile, MCRDB_FILE_EXTENSION));

		json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(applicationModelBean));
		RuntimeExceptionDecorator.decorate(() -> WriteFileExtensions.string2File(tempJsonFile, json));

		File encryptedApplicationFile = RuntimeExceptionDecorator
			.decorate(() -> encryptor.encrypt(tempJsonFile));

		if (tempJsonFile.exists())
		{
			RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(tempJsonFile));
		}
		return encryptedApplicationFile;
	}
}
