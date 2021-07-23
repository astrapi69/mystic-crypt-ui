package io.github.astrapi69.mystic.crypt.panels.signin;

import io.github.astrapi69.crypto.algorithm.AesAlgorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.SecretKeyFactoryExtensions;
import io.github.astrapi69.crypto.file.PBEFileEncryptor;
import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.PublicKeyEncryptor;
import io.github.astrapi69.crypto.key.PublicKeyGenericEncryptor;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.key.writer.EncryptedPrivateKeyWriter;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.json.ObjectToJsonExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import io.github.astrapi69.write.WriteFileExtensions;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ApplicationFileFactory
{

	public static void newApplicationFileWithPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		PrivateKey privateKey;
		PublicKey publicKey;
		SecretKey symmetricKey;
		PublicKeyEncryptor encryptor;
		PublicKeyGenericEncryptor<String> genericEncryptor;
		CryptModel<Cipher, PublicKey, byte[]> encryptModel;
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel;
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		final File applicationFile = modelObject.getApplicationFile();

		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.build();
		applicationModelBean.setMasterPwFileModelBean(masterPwFileModelBean);

		privateKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyReader.readPemPrivateKey(modelObject.getKeyFile()));

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

		String json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(applicationModelBean));

		RuntimeExceptionDecorator
			.decorate(() -> WriteFileExtensions.string2File(applicationFile, json));

		byte[] encrypt = RuntimeExceptionDecorator.decorate(() -> genericEncryptor.encrypt(json));

		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encrypt, applicationFile));
	}

	public static void newApplicationFileWithPasswordAndPrivateKey(MasterPwFileModelBean modelObject)
	{
		PrivateKey privateKey;
		PublicKey publicKey;
		SecretKey symmetricKey;
		PublicKeyEncryptor encryptor;
		PublicKeyGenericEncryptor<String> genericEncryptor;
		CryptModel<Cipher, PublicKey, byte[]> encryptModel;
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel;
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		final File applicationFile = modelObject.getApplicationFile();

		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.build();
		applicationModelBean.setMasterPwFileModelBean(masterPwFileModelBean);

		privateKey = RuntimeExceptionDecorator
			.decorate(() -> PrivateKeyReader.readPemPrivateKey(modelObject.getKeyFile()));

		char[] masterPw = modelObject.getMasterPw();

		byte[] encryptedPrivateKeyArray = RuntimeExceptionDecorator.decorate(
			() -> EncryptedPrivateKeyWriter.encryptPrivateKeyWithPassword(privateKey, String.valueOf(masterPw)));

		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encryptedPrivateKeyArray, modelObject.getKeyFile()));

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

		String json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(applicationModelBean));

		RuntimeExceptionDecorator
			.decorate(() -> WriteFileExtensions.string2File(applicationFile, json));

		byte[] encrypt = RuntimeExceptionDecorator.decorate(() -> genericEncryptor.encrypt(json));

		RuntimeExceptionDecorator.decorate(
			() -> WriteFileExtensions.storeByteArrayToFile(encrypt, applicationFile));
	}

	public static void newApplicationFileWithPassword(MasterPwFileModelBean modelObject)
	{
		PBEFileEncryptor encryptor;
		String password;
		CryptModel<Cipher, String, String> cryptModel;
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		final File applicationFile = modelObject.getApplicationFile();

		password = String.valueOf(modelObject.getMasterPw());

		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.build();
		applicationModelBean.setMasterPwFileModelBean(masterPwFileModelBean);

		cryptModel = CryptModel.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).build();
		encryptor = RuntimeExceptionDecorator.decorate(() -> new PBEFileEncryptor(cryptModel));

		String json = RuntimeExceptionDecorator
			.decorate(() -> ObjectToJsonExtensions.toJson(applicationModelBean));

		RuntimeExceptionDecorator
			.decorate(() -> WriteFileExtensions.string2File(applicationFile, json));

		RuntimeExceptionDecorator
			.decorate(() -> encryptor.encrypt(applicationFile));
	}
}
