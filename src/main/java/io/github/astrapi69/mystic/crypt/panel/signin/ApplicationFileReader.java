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

import java.io.File;
import java.security.PrivateKey;
import java.security.Security;
import java.util.logging.Level;

import javax.crypto.Cipher;

import lombok.NonNull;
import lombok.extern.java.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factory.CryptModelFactory;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.PemObjectReader;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.crypto.pw.PasswordStringDecryptor;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonFileToObjectExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

@Log
public class ApplicationFileReader
{

	public static ApplicationModelBean read(@NonNull MasterPwFileModelBean modelObject)
	{
		if (modelObject.isWithMasterPw() && modelObject.isWithKeyFile())
		{
			return readApplicationFileWithPasswordAndPrivateKey(modelObject);
		}
		else if (modelObject.isWithMasterPw())
		{
			return readApplicationFileWithPassword(modelObject);
		}
		else if (modelObject.isWithKeyFile())
		{
			return readApplicationFileWithPrivateKey(modelObject);
		}
		return null;
	}

	public static ApplicationModelBean readApplicationFileWithPasswordAndPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		File applicationFile = modelObject.getApplicationFile();
		char[] password = modelObject.getMasterPw();
		File keyFile = modelObject.getKeyFile();
		try
		{
			return getApplicationModelBean(applicationFile, password, keyFile);
		}
		catch (Exception exception)
		{
			log.log(Level.SEVERE, exception.getLocalizedMessage(), exception);
			String title = "Authentication with Password or key file";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Password or key file or both are not valid" + "<p>" + exception.getMessage();
			throw new RuntimeException(title + "::" + htmlMessage, exception);
		}
	}

	public static ApplicationModelBean readApplicationFileWithPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean;
		File applicationFile = modelObject.getApplicationFile();
		File keyFile = modelObject.getKeyFile();
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security
				.addProvider(MysticCryptApplicationFrame.getInstance().getBouncyCastleProvider());
		}
		try
		{
			PrivateKey privateKey;
			if (modelObject.getPrivateKey() != null)
			{
				privateKey = modelObject.getPrivateKey();
			}
			else
			{
				if (PemObjectReader.isPemObject(keyFile))
				{
					privateKey = PrivateKeyReader.readPemPrivateKey(keyFile);
				}
				else
				{
					privateKey = PrivateKeyReader.readPrivateKey(keyFile);
				}
			}
			applicationModelBean = getApplicationModelBean(applicationFile, privateKey);
		}
		catch (Exception exception)
		{
			String title = "Authentication with key file";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Key file is not valid" + "<p>" + exception.getMessage();
			throw new RuntimeException(title + "::" + htmlMessage, exception);
		}
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPassword(
		MasterPwFileModelBean modelObject)
	{
		File applicationFile = modelObject.getApplicationFile();
		char[] password = modelObject.getMasterPw();
		try
		{
			return getApplicationModelBean(applicationFile, password);
		}
		catch (Exception exception)
		{
			String title = "Authentication with Password";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Password is not valid" + "<p>" + exception.getMessage();
			throw new RuntimeException(title + "::" + htmlMessage, exception);
		}
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		char[] password) throws Exception
	{
		ApplicationModelBean applicationModelBean;
		CryptModel<Cipher, String, String> pbeCryptModel = CryptModelFactory
			.newCryptModel(SunJCEAlgorithm.PBEWithMD5AndDES, new String(password));

		PBEFileDecryptor fileDecryptor = new PBEFileDecryptor(pbeCryptModel);
		File decrypt = fileDecryptor.decrypt(applicationFile);
		applicationModelBean = JsonFileToObjectExtensions.toObject(decrypt,
			ApplicationModelBean.class);
		DeleteFileExtensions.delete(decrypt);

		return applicationModelBean;
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		char[] password, File keyFile) throws Exception
	{
		ApplicationModelBean applicationModelBean;
		CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
		PrivateKeyDecryptor decryptor;
		PrivateKeyGenericDecryptor<String> genericDecryptor;
		PrivateKey privateKey;
		PasswordStringDecryptor passwordStringDecryptor;
		passwordStringDecryptor = new PasswordStringDecryptor(String.valueOf(password));
		privateKey = PrivateKeyReader.readPemPrivateKey(keyFile);

		decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey).build();
		decryptor = new PrivateKeyDecryptor(decryptModel);
		genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String encryptedJson = genericDecryptor.decrypt(encryptedBytes);
		String json = passwordStringDecryptor.decrypt(encryptedJson);
		applicationModelBean = JsonStringToObjectExtensions.toObject(json,
			ApplicationModelBean.class);

		return applicationModelBean;
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile, File keyFile)
		throws Exception
	{
		return getApplicationModelBean(applicationFile,
			PrivateKeyReader.readPemPrivateKey(keyFile));
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		PrivateKey privateKey) throws Exception
	{
		CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
		PrivateKeyDecryptor decryptor;
		PrivateKeyGenericDecryptor<String> genericDecryptor;
		ApplicationModelBean applicationModelBean;
		decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey).build();
		decryptor = new PrivateKeyDecryptor(decryptModel);
		genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String json = genericDecryptor.decrypt(encryptedBytes);
		applicationModelBean = JsonStringToObjectExtensions.toObject(json,
			ApplicationModelBean.class);
		return applicationModelBean;
	}
}
