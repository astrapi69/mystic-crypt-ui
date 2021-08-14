package io.github.astrapi69.mystic.crypt.panels.signin;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;

import javax.crypto.Cipher;

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypto.algorithm.MdAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.delete.DeleteFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationFileWithPasswordAndKeyFactoryTest
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
	String password;

	@BeforeEach void setUp() {

		Security.addProvider(new BouncyCastleProvider());
		applicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
				"empty-db-with-key-and-pw" + ApplicationFileFactory.MCRDB_FILE_EXTENSION);
		selectedApplicationFilePath = applicationFile.getAbsolutePath();
		decryptedApplicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(), "empty-db-with-key-and-pw.json");
		password = "secret";
		pemDir = new File(PathFinder.getSrcTestResourcesDir(), "pem");
		privateKeyPemFile = new File(pemDir, "private.pem");
		pemPrivateKey = RuntimeExceptionDecorator
				.decorate(() -> PrivateKeyReader.readPemPrivateKey(privateKeyPemFile));
		decryptModel = CryptModel.<Cipher, PrivateKey, byte[]>builder().key(pemPrivateKey)
				.build();
		decryptor = RuntimeExceptionDecorator
				.decorate(() -> new PrivateKeyDecryptor(decryptModel));
		genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);

		derDir = new File(PathFinder.getSrcTestResourcesDir(), "der");
		privateKeyDerFile = new File(derDir, "private.der");
		derPrivateKey = RuntimeExceptionDecorator
				.decorate(() -> PrivateKeyReader.readPrivateKey(privateKeyDerFile));



	}

	@AfterEach void tearDown() {
		RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(applicationFile));
		RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(decryptedApplicationFile));
		selectedApplicationFilePath = null;
		password = null;
		decryptor = null;
	}

	@Test void newApplicationFileWithPasswordAndPrivateKey() throws NoSuchAlgorithmException, IOException {
		// define parameter for the unit test
		String actual;
		String expected;
		File actualEncryptedFile;
		File expectedFile;
		MasterPwFileModelBean modelObject;
		// new scenario
		modelObject = MasterPwFileModelBean.builder().applicationFile(applicationFile)
			.selectedApplicationFilePath(selectedApplicationFilePath)
				.privateKey(pemPrivateKey)
				.keyFile(privateKeyPemFile)
				.masterPw(password.toCharArray())
			.applicationFilePaths(ListFactory.newArrayList(""))
			.keyFilePaths(ListFactory.newArrayList(""))
			.build();

		// test the actual method
		actualEncryptedFile = ApplicationFileFactory.newApplicationFileWithPasswordAndPrivateKey(modelObject);

//
		ApplicationModelBean modelBeanReaded = ApplicationFileReader.readApplicationFileWithPasswordAndPrivateKey(modelObject);
		assertNotNull(modelBeanReaded);
	}

}
