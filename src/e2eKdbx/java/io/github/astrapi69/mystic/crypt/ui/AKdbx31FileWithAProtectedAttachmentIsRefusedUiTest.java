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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.assertj.swing.exception.WaitTimedOutError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A KDBX 3.1 file with a protected attachment is refused by the import, with a message, and nothing
 * of it reaches the vault (#568).
 * <p>
 * KeePassJava2 3.1.0 cannot read such a file: its model of {@code Meta/Binaries} has no
 * {@code Protected} attribute, and it fails on attributes it does not know. That is the measured
 * state, and the safe one - the other possibility would have been a file that opens with the
 * attachment missing, or every protected value after it decrypted wrongly, since the inner stream
 * runs in document order. The library's author has a fix for 3.1.2 (jorabin/KeePassJava2#114). When
 * it arrives this test fails, which is its purpose: the import must then carry the attachment byte
 * for byte, and this test is turned around to say so.
 * <p>
 * The fixture and how it was built: {@code src/e2eKdbx/resources/kdbx/README.md}.
 */
class AKdbx31FileWithAProtectedAttachmentIsRefusedUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String FIXTURE = "keepass2-kdbx31-protected-attachment.kdbx";

	@Test
	@DisplayName("the import refuses it with the reason, and adds nothing to the vault")
	void theImport_refusesItWithTheReason_andAddsNothing() throws Exception
	{
		File vault = new File(tempHome, "refuses-a-protected-attachment.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		int before = application.treeNodeCount();

		String refusal;
		try
		{
			refusal = application.importKeePassDatabaseReadingTheRefusal(copyTheFixture(),
				TestPasswords.KEEPASS_FIXTURE);
		}
		catch (WaitTimedOutError noRefusal)
		{
			throw new AssertionError("the import did not refuse the file: KeePassJava2 reads a "
				+ "protected attachment now (jorabin/KeePassJava2#114). Check that the attachment "
				+ "arrives byte for byte and turn this test around (#568)", noRefusal);
		}

		assertTrue(refusal.contains("Could not import the KeePass file"), refusal);
		assertTrue(refusal.contains("\"Protected\"") && refusal.contains("Binary"),
			"the refusal names what the library could not read, the protection of an attachment, "
				+ "not some other reason such as the password: " + refusal);
		assertEquals(before, application.treeNodeCount(),
			"a refused import must not add any group to the tree");
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isDirty(),
			"a refused import must not mark the vault dirty");
	}

	private File copyTheFixture() throws Exception
	{
		File copy = new File(tempHome, FIXTURE);
		try (InputStream fixture = getClass().getResourceAsStream("/kdbx/" + FIXTURE))
		{
			assertNotNull(fixture, "the fixture is on the e2eKdbx class path");
			Files.copy(fixture, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		assertTrue(copy.length() > 0, "the fixture was copied");
		return copy;
	}
}
