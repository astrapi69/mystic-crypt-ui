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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Headless round-trip regression test for the password-only application-file persistence: what
 * {@link ApplicationXmlFileStoreWorker#saveToFileWithPassword} writes,
 * {@link ApplicationXmlFileReader#getApplicationModelBean(File, char[])} must be able to read back.
 * This is exactly the write/read pair behind "create first database" followed by sign-in
 */
class ApplicationXmlFilePasswordRoundTripTest
{

	private static final char[] MASTER_PASSWORD = "test-master-pw-123".toCharArray();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	void savedApplicationFileWithPasswordCanBeReadBack(@TempDir File tempDir) throws Exception
	{
		File applicationFile = new File(tempDir, "round-trip-test.mcrdb");
		Files.createFile(applicationFile.toPath());

		MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(applicationFile))
			.selectedApplicationFilePath(applicationFile.getAbsolutePath())
			.masterPw(MASTER_PASSWORD.clone()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();

		File savedFile = ApplicationXmlFileFactory
			.newApplicationFileWithPassword(masterPwFileModelBean);

		assertNotNull(savedFile);
		assertTrue(savedFile.exists(), "saved application file must exist");
		assertTrue(savedFile.length() > 0, "saved application file must not be empty");
		assertEquals(applicationFile.getAbsolutePath(), savedFile.getAbsolutePath(),
			"the file the reader will later be pointed at must be the file the writer wrote");

		ApplicationModelBean readBack = ApplicationXmlFileReader
			.getApplicationModelBean(applicationFile, MASTER_PASSWORD.clone());

		assertNotNull(readBack);
		assertNotNull(readBack.getMasterPwFileModelBean());
		assertEquals(applicationFile.getAbsolutePath(),
			readBack.getMasterPwFileModelBean().getSelectedApplicationFilePath());
	}
}
