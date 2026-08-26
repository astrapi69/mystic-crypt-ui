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

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.reader.PemObjectReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyDecryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.PasswordType;
import io.github.astrapi69.mystic.crypt.pw.PasswordStringDecryptor;
import io.github.astrapi69.xstream.XmlToObjectExtensions;
import lombok.NonNull;
import lombok.extern.java.Log;

@Log
public class ApplicationXmlFileReader
{

	public static ApplicationModelBean read(@NonNull MasterPwFileModelBean modelObject)
	{
		PasswordType passwordType = PasswordType.resolve(modelObject.isWithMasterPw(),
			modelObject.isWithKeyFile());
		if (passwordType.equals(PasswordType.PASSWORD_WITH_PRIVATE_KEY))
		{
			return readApplicationFileWithPasswordAndPrivateKey(modelObject);
		}
		else if (passwordType.equals(PasswordType.PASSWORD))
		{
			return readApplicationFileWithPassword(modelObject);
		}
		return readApplicationFileWithPrivateKey(modelObject);
	}

	/**
	 * Stamps the file that was actually opened onto the model that came out of it.
	 * <p>
	 * A database carries its own path inside its encrypted xml, which is the path it had when it
	 * was last saved. Without this, opening a file that was moved, renamed or copied leaves the
	 * application pointing at where the file used to be, and every later save goes there instead: a
	 * moved file is recreated at the old location holding the new entries, and a copy overwrites
	 * the database it was copied from. Neither says anything.
	 *
	 * @param applicationModelBean
	 *            the model read out of the file
	 * @param applicationFile
	 *            the file the user actually opened
	 * @return the same model, pointing at the file it was read from
	 */
	private static ApplicationModelBean openedFrom(final ApplicationModelBean applicationModelBean,
		final File applicationFile, final MasterPwFileModelBean credentials)
	{
		if (applicationModelBean == null || applicationModelBean.getMasterPwFileModelBean() == null
			|| applicationFile == null || credentials == null)
		{
			return applicationModelBean;
		}
		MasterPwFileModelBean signInModelBean = applicationModelBean.getMasterPwFileModelBean();
		signInModelBean.setApplicationFileInfo(FileInfo.toFileInfo(applicationFile));
		signInModelBean.setSelectedApplicationFilePath(applicationFile.getAbsolutePath());
		// the password is not in the file and must not be: it comes from whoever just signed in,
		// and the model needs it for the next save and for the unlock dialog
		signInModelBean.setMasterPw(credentials.getMasterPw());
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPasswordAndPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		char[] password = modelObject.getMasterPw();
		File keyFile = FileFactory.newFileQuietly(modelObject.getKeyFileInfo());
		try
		{
			return openedFrom(getApplicationModelBean(applicationFile, password, keyFile),
				applicationFile, modelObject);
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
			applicationModelBean = openedFrom(getApplicationModelBean(applicationFile, privateKey),
				applicationFile, modelObject);
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
			return openedFrom(getApplicationModelBean(applicationFile, password), applicationFile,
				modelObject);
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
		// PasswordVaultFormat decides by the marker in the file which of the two formats this is,
		// and reads a database written before the marker existed just as well
		return XmlToObjectExtensions
			.toObject(PasswordVaultFormat.decrypt(applicationFile, new String(password)));
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
		applicationModelBean.getMasterPwFileModelBean()
			.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(privateKey));
		return applicationModelBean;
	}
}
