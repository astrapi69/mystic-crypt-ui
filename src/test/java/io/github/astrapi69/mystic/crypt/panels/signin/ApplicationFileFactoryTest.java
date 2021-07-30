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

class ApplicationFileFactoryTest
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

	@Test void newApplicationFileWithPassword() throws Exception
	{
		String actual;
		String expected;
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder()
			.applicationFile(applicationFile)
			.selectedApplicationFilePath(selectedApplicationFilePath)
			.applicationFilePaths(ListFactory.newArrayList(""))
			.keyFilePaths(ListFactory.newArrayList(""))
			.minPasswordLength(6)
			.masterPw("foobar".toCharArray())
			.repeatPw("foobar".toCharArray())
			.withMasterPw(true)
			.build();
		File encryptedFile = ApplicationFileFactory.newApplicationFileWithPassword(modelObject);
		System.out.println(encryptedFile.getAbsolutePath());
		final File decrypt = RuntimeExceptionDecorator
			.decorate(() -> decryptor.decrypt(applicationFile));

		File expectedFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
			"expected-empty-db.mcdb");
		expected = FileChecksumExtensions.getChecksum(expectedFile, MdAlgorithm.MD5.name());
		actual = FileChecksumExtensions.getChecksum(encryptedFile, MdAlgorithm.MD5.name());
		assertEquals(expected, actual);

		ApplicationModelBean applicationModelBean = ApplicationFileReader.readApplicationFileWithPassword(
			modelObject);
		assertNotNull(applicationModelBean);
		// cleanup
		DeleteFileExtensions.delete(decrypt);

	}

	@Test void newApplicationFileWithPrivateKey()
	{
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder().build();
		ApplicationFileFactory.newApplicationFileWithPrivateKey(modelObject);
	}

	@Test void newApplicationFileWithPasswordAndPrivateKey()
	{
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder().build();
		ApplicationFileFactory.newApplicationFileWithPasswordAndPrivateKey(modelObject);
	}

}
