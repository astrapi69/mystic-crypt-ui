package io.github.astrapi69.mystic.crypt.panels.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import javax.crypto.Cipher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.crypto.algorithm.MdAlgorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.delete.DeleteFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

class ApplicationFileWithPasswordAndKeyFactoryTest
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

	@Test void newApplicationFileWithPasswordAndPrivateKey()
	{
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder().applicationFile(applicationFile)
			.selectedApplicationFilePath(selectedApplicationFilePath)
			.applicationFilePaths(ListFactory.newArrayList(""))
			.keyFilePaths(ListFactory.newArrayList(""))
			.build();
		ApplicationFileFactory.newApplicationFileWithPasswordAndPrivateKey(modelObject);
	}

}
