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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What opening and saving leave at the path of a key file that is not there (#937): the key file is
 * in a directory that is gone as well, as on a removable drive that is not connected.
 * <p>
 * PIN of the behaviour before the fix, green on it: each of the four reads of a key file path
 * creates the directory and an empty file there. The fix turns it.
 */
class AMissingKeyFileIsNeverCreatedTest
{

	@TempDir
	File directory;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** One way the application reads the key file of a database */
	@FunctionalInterface
	interface KeyFileRead
	{
		void run(MasterPwFileModelBean credentials);
	}

	static Stream<Arguments> everyReadOfAKeyFilePath()
	{
		return Stream.of(Arguments.of("opening a password and key file database", true,
			(KeyFileRead)ApplicationXmlFileReader::readApplicationFileWithPasswordAndPrivateKey),
			Arguments.of("opening a key file database", false,
				(KeyFileRead)ApplicationXmlFileReader::readApplicationFileWithPrivateKey),
			Arguments.of("saving a password and key file database", true,
				(KeyFileRead)AMissingKeyFileIsNeverCreatedTest::save),
			Arguments.of("saving a key file database whose key is not in memory", false,
				(KeyFileRead)AMissingKeyFileIsNeverCreatedTest::save));
	}

	@ParameterizedTest(name = "PIN: {0} leaves an empty file and its directory at the key file's path")
	@MethodSource("everyReadOfAKeyFilePath")
	void aMissingKeyFile_isCreated(final String caseName, final boolean withPassword,
		final KeyFileRead read)
	{
		File drive = new File(directory, "drive-not-connected");
		File keyFile = new File(drive, "database.pem");
		MasterPwFileModelBean credentials = credentials(withPassword, keyFile);

		assertThrows(RuntimeException.class, () -> read.run(credentials),
			"there is no key to open or save with");

		assertTrue(drive.isDirectory(), "today the directory of the key file is created");
		assertTrue(keyFile.isFile(), "and a file at its path");
		assertEquals(0L, keyFile.length(), "an empty one");
	}

	private MasterPwFileModelBean credentials(final boolean withPassword, final File keyFile)
	{
		File vault = new File(directory, "database.mcrdb");
		MasterPwFileModelBean.MasterPwFileModelBeanBuilder credentials = MasterPwFileModelBean
			.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.keyFileInfo(FileInfo.toFileInfo(keyFile)).withMasterPw(withPassword).withKeyFile(true)
			.minPasswordLength(6);
		if (withPassword)
		{
			credentials.masterPw(TestPasswords.throwaway().toCharArray());
		}
		return credentials.build();
	}

	private static void save(final MasterPwFileModelBean credentials)
	{
		ApplicationXmlFileStoreWorker.storeApplicationFile(
			ApplicationModelBean.builder().masterPwFileModelBean(credentials).build());
	}
}
