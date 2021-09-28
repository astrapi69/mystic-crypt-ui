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

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypto.algorithm.MdAlgorithm;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.file.delete.DeleteFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.file.search.PathFinder;
import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApplicationFileWithPasswordFactoryTest
{

	File applicationFile;
	String selectedApplicationFilePath;
	File decryptedApplicationFile;
	PBEFileDecryptor decryptor;
	String password;
	CryptModel<Cipher, String, String> cryptModel;

	@BeforeEach void setUp() {
		applicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
			"empty-db"+ApplicationFileFactory.MCRDB_FILE_EXTENSION);
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
				"expected-empty-db"+ApplicationFileFactory.MCRDB_FILE_EXTENSION);
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
