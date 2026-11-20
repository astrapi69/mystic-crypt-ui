/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.Security;
import java.time.Duration;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * A database whose header names an iteration count outside the bound of mystic-crypt 13.11.1 (1 to
 * 2 000 000) is refused as no database, before any key is derived (GHSA-m42j-97pf-6mc7): the
 * library throws an {@link IllegalArgumentException} for the header, and the sign-in shows what it
 * shows for a file this application cannot open (#1062). On mystic-crypt 13.11 the same file made
 * the opening spend the work the header asked for, up to two billion iterations.
 */
class SignInRefusesAnOutOfBoundIterationCountTest
{

	/** Marker "MCRDB2" (6 bytes) and salt (16 bytes) come before the 4 byte count */
	private static final int ITERATION_COUNT_OFFSET = 6 + 16;

	private static final String NOT_A_DATABASE = "This file is not a database this application can open. It was not opened, and the file is unchanged.";

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

	@ParameterizedTest(name = "a header that records {0} iterations: refused as not a database, fast")
	@ValueSource(ints = { 2_000_001, 1_000_000_000, Integer.MAX_VALUE, 0, -1, Integer.MIN_VALUE })
	void aHeaderOutsideTheBound_isRefusedAsNoDatabase_beforeAnyKeyIsDerived(final int recordedCount)
		throws Exception
	{
		char[] password = TestPasswords.throwaway().toCharArray();
		File sealed = SampleVaults.save(SampleVaults.aVaultWith(entry -> {
		}), directory, password);
		byte[] content = Files.readAllBytes(sealed.toPath());
		assertTrue(content.length > ITERATION_COUNT_OFFSET + 4, "a freshly sealed vault");
		ByteBuffer.wrap(content).putInt(ITERATION_COUNT_OFFSET, recordedCount);
		File changed = new File(directory, "changed-count.mcrdb");
		Files.write(changed.toPath(), content);
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(changed))
			.selectedApplicationFilePath(changed.getAbsolutePath()).masterPw(password)
			.withMasterPw(true).withKeyFile(false).minPasswordLength(6).build();

		RuntimeException refused = assertTimeoutPreemptively(Duration.ofSeconds(20),
			() -> assertThrows(RuntimeException.class,
				() -> ApplicationXmlFileReader.read(credentials)));

		assertNotNull(refused.getCause(), "the refusal keeps the cause for the log");
		assertTrue(refused.getMessage().endsWith("<p>" + NOT_A_DATABASE), refused.getMessage());
		assertEquals(IllegalArgumentException.class, rootOf(refused).getClass(),
			"the library refused the header, not the cipher");
	}

	private static Throwable rootOf(final Throwable throwable)
	{
		Throwable root = throwable;
		while (root.getCause() != null)
		{
			root = root.getCause();
		}
		return root;
	}
}
