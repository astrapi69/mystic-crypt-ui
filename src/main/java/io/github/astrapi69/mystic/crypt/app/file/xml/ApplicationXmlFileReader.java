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

import java.io.File;
import java.security.PrivateKey;
import java.security.Security;
import java.util.logging.Level;

import javax.crypto.Cipher;

import lombok.NonNull;
import lombok.extern.java.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.data.factory.CryptModelFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.reader.PemObjectReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.file.PBEFileDecryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyDecryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.pw.PasswordStringDecryptor;
import io.github.astrapi69.xstream.XmlFileToObjectExtensions;
import io.github.astrapi69.xstream.XmlToObjectExtensions;

@Log
public class ApplicationXmlFileReader
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
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		char[] password = modelObject.getMasterPw();
		File keyFile = FileFactory.newFileQuietly(modelObject.getKeyFileInfo());
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
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		File keyFile = FileFactory.newFileQuietly(modelObject.getKeyFileInfo());
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security
				.addProvider(MysticCryptApplicationFrame.getInstance().getBouncyCastleProvider());
		}
		try
		{
			PrivateKey privateKey;
			if (modelObject.getPrivateKeyInfo() != null)
			{
				privateKey = KeyModelExtensions.toPrivateKey(modelObject.getPrivateKeyInfo());
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
		@NonNull final MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
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
		applicationModelBean = XmlFileToObjectExtensions.toObject(decrypt);
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
		String encryptedXml = genericDecryptor.decrypt(encryptedBytes);
		String xml = passwordStringDecryptor.decrypt(encryptedXml);
		applicationModelBean = XmlToObjectExtensions.toObject(xml);
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
		String xml = genericDecryptor.decrypt(encryptedBytes);
		applicationModelBean = XmlToObjectExtensions.toObject(xml);
		return applicationModelBean;
	}
}
