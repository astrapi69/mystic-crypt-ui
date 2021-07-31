package io.github.astrapi69.mystic.crypt.panels.signin;

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypto.algorithm.MdAlgorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.delete.DeleteFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationFileWithPasswordFactoryTest
{

	File applicationFile;
	String selectedApplicationFilePath;
	File decryptedApplicationFile;
	PBEFileDecryptor decryptor;
	String password;
	CryptModel<Cipher, String, String> cryptModel;

	@BeforeEach void setUp() {
		applicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
			"empty-db.mcdb");
		selectedApplicationFilePath = applicationFile.getAbsolutePath();
		decryptedApplicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(), "empty-db.json");
		password = "foobar";
		cryptModel = CryptModel.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).build();
		decryptor = RuntimeExceptionDecorator
			.decorate(() -> new PBEFileDecryptor(cryptModel, decryptedApplicationFile));
	}

	@AfterEach void tearDown() {
		RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(applicationFile));
		RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(decryptedApplicationFile));
		selectedApplicationFilePath = null;
		password = null;
		cryptModel = null;
		decryptor = null;
	}

	@Test void testNewApplicationFileWithPassword() throws Exception
	{
		// define parameter for the unit test
		String actual;
		String expected;
		File actualEncryptedFile;
		File expectedFile;
		MasterPwFileModelBean modelObject;
		ApplicationModelBean applicationModelBean;
		// create test data
		modelObject = MasterPwFileModelBean.builder()
			.applicationFile(applicationFile)
			.selectedApplicationFilePath(selectedApplicationFilePath)
			.applicationFilePaths(ListFactory.newArrayList(""))
			.keyFilePaths(ListFactory.newArrayList(""))
			.minPasswordLength(6)
			.masterPw(password.toCharArray())
			.repeatPw(password.toCharArray())
			.withMasterPw(true)
			.build();
		// test the actual method
		actualEncryptedFile = ApplicationFileFactory.newApplicationFileWithPassword(modelObject);
		// proof that method is working as expected
		final File decrypt = RuntimeExceptionDecorator
			.decorate(() -> decryptor.decrypt(applicationFile));

		expectedFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
			"expected-empty-db.mcdb");
		expected = FileChecksumExtensions.getChecksum(expectedFile, MdAlgorithm.MD5.name());
		actual = FileChecksumExtensions.getChecksum(actualEncryptedFile, MdAlgorithm.MD5.name());
		assertEquals(expected, actual);

		applicationModelBean = ApplicationFileReader.readApplicationFileWithPassword(
			modelObject);
		assertNotNull(applicationModelBean);
		// cleanup
		DeleteFileExtensions.delete(decrypt);
	}

}
