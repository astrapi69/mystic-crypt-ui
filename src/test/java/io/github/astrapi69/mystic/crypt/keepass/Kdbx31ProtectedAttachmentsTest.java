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
package io.github.astrapi69.mystic.crypt.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.linguafranca.pwdb.format.KdbxCredentials;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Whether a KeePass file is KDBX 3.1 with protected attachments - the one file KeePassJava2 3.1.0
 * refuses for its attachments (#568). Read from the files themselves, the fixtures of the KDBX
 * end-to-end suite and the unit suite's KDBX 3.1 file.
 */
class Kdbx31ProtectedAttachmentsTest
{

	private static final String KDBX_FIXTURES = "src/e2eKdbx/resources/kdbx/";

	@ParameterizedTest(name = "{0}: {3}")
	@CsvSource(delimiter = '|', textBlock = """
		KDBX 3.1 with a protected attachment            | src/e2eKdbx/resources/kdbx/keepass2-kdbx31-protected-attachment.kdbx                     | keepass | true
		KDBX 3.1 without attachments                    | src/test/resources/test-db.kdbx                                                          | keepass | false
		the same file saved as KDBX 4 by KeePassXC      | src/e2eKdbx/resources/kdbx/keepassxc-2.7.10-kdbx4-saved-from-kdbx31-protected-attachment.kdbx | keepass | false
		KDBX 4 with an attachment, written by KeePassXC | src/e2eKdbx/resources/kdbx/keepassxc-2.7.10-kdbx4.kdbx                                    | fixture | false
		""")
	void isTheCase_onlyForKdbx31WithAProtectedAttachment(String caseName, String path,
		String password, boolean expected) throws Exception
	{
		assertEquals(expected, Kdbx31ProtectedAttachments.in(new File(path), credentials(password)),
			caseName);
	}

	@Test
	@DisplayName("a file that cannot be opened with the credentials is not known to be the case")
	void aWrongPassword_isNotTheCase()
	{
		assertFalse(Kdbx31ProtectedAttachments.in(
			new File(KDBX_FIXTURES + "keepass2-kdbx31-protected-attachment.kdbx"),
			new KdbxCredentials("not the password".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	@DisplayName("a file that does not exist is not known to be the case")
	void aMissingFile_isNotTheCase()
	{
		assertFalse(Kdbx31ProtectedAttachments.in(new File(KDBX_FIXTURES + "no-such-file.kdbx"),
			credentials("keepass")));
	}

	private static KdbxCredentials credentials(String password)
	{
		String value = "keepass".equals(password) ? TestPasswords.KEEPASS_FIXTURE : password;
		return new KdbxCredentials(value.getBytes(StandardCharsets.UTF_8));
	}
}
