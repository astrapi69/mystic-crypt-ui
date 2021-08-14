package io.github.astrapi69.mystic.crypt.panels.signin;

import java.io.File;
import java.security.PrivateKey;

import javax.crypto.Cipher;

import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.CryptModelFactory;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.crypto.pw.PasswordStringDecryptor;
import io.github.astrapi69.delete.DeleteFileExtensions;
import io.github.astrapi69.gson.JsonFileToObjectExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
//import io.github.astrapi69.json.JsonFileToObjectExtensions;
//import io.github.astrapi69.json.JsonStringToObjectExtensions;
//import io.github.astrapi69.json.factory.ObjectMapperFactory;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.read.ReadFileExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import lombok.NonNull;

public class ApplicationFileReader
{
	public static ApplicationModelBean read(@NonNull MasterPwFileModelBean modelObject)
	{
		if (modelObject.isWithMasterPw() && modelObject.isWithKeyFile() )
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
		ApplicationModelBean applicationModelBean;

		File applicationFile = modelObject.getApplicationFile();
		char[] password = modelObject.getMasterPw();
		File keyFile = modelObject.getKeyFile();
		try
		{
			CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
			PrivateKeyDecryptor decryptor;
			PrivateKeyGenericDecryptor<String> genericDecryptor;
			PrivateKey privateKey;
			PasswordStringDecryptor passwordStringDecryptor;
			passwordStringDecryptor = new PasswordStringDecryptor(String.valueOf(password));
			privateKey = PrivateKeyReader.readPemPrivateKey(keyFile);
			
			decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey)
				.build();
			decryptor = new PrivateKeyDecryptor(decryptModel);
			genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
			byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
			String encryptedJson = genericDecryptor.decrypt(encryptedBytes);
			String json = passwordStringDecryptor.decrypt(encryptedJson);
			applicationModelBean = JsonStringToObjectExtensions.toObject(json,
				ApplicationModelBean.class);

		}
		catch (Exception exception)
		{
			String title = "Authentication with Password or key file";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Password or key file or both are not valid" + "<p>" + exception.getMessage();
			throw new RuntimeException(title + "::" + htmlMessage, exception);
		}
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean;
		File applicationFile = modelObject.getApplicationFile();
		File keyFile = modelObject.getKeyFile();
		try
		{
			PrivateKey privateKey;
			if(modelObject.getPrivateKey()!=null){
				privateKey = modelObject.getPrivateKey();
			} else {
				// TODO check which format the private key is
				privateKey = PrivateKeyReader.readPemPrivateKey(keyFile);
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

	private static ApplicationModelBean getApplicationModelBean(File applicationFile,
		PrivateKey privateKey) throws Exception
	{
		CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
		PrivateKeyDecryptor decryptor;
		PrivateKeyGenericDecryptor<String> genericDecryptor;
		ApplicationModelBean applicationModelBean;
		decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey)
			.build();
		decryptor = new PrivateKeyDecryptor(decryptModel);
		genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String json = genericDecryptor.decrypt(encryptedBytes);
		applicationModelBean = JsonStringToObjectExtensions.toObject(json,
			ApplicationModelBean.class);
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPassword(
		MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean;
		File applicationFile = modelObject.getApplicationFile();
		char[] password = modelObject.getMasterPw();
		CryptModel<Cipher, String, String> pbeCryptModel = CryptModelFactory
			.newCryptModel(SunJCEAlgorithm.PBEWithMD5AndDES, new String(password));
		try
		{
			PBEFileDecryptor fileDecryptor = new PBEFileDecryptor(pbeCryptModel);
			File decrypt = fileDecryptor.decrypt(applicationFile);
			applicationModelBean = JsonFileToObjectExtensions.toObject(decrypt,
				ApplicationModelBean.class);
			RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(decrypt));
		}
		catch (Exception exception)
		{
			String title = "Authentication with Password";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Password is not valid" + "<p>" + exception.getMessage();
			throw new RuntimeException(title + "::" + htmlMessage, exception);
		}
		return applicationModelBean;
	}
}
