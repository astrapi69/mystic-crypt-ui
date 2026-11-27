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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.format.KdbxHeader;
import org.linguafranca.pwdb.format.KdbxSerializer;
import org.linguafranca.pwdb.format.KdbxStreamFormat;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KeePassCredentials;

/**
 * What the running application shows when a KeePass file opens with its password and its content
 * then cannot be read: the database name of the file is an element that is opened and never closed,
 * named per run, and the refusal's dialog shows the bundle's words for a refused import, exactly
 */
class ARefusedKeePassImportShowsTheWordsOfTheBundleUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String KEEPASS_PASSWORD = TestPasswords.throwaway();

	/** The name of the element that is never closed, made per run */
	private static final String ELEMENT = "element" + UUID.randomUUID().toString().replace("-", "");

	private static final String NAME_TO_REPLACE = "name-to-replace";

	@Test
	@DisplayName("a file whose content cannot be read after its password opened it shows the bundle's words")
	void aFileWhoseContentCannotBeRead_showsTheWordsOfTheBundle() throws Exception
	{
		File vault = new File(tempHome, "refused-import.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		File keePassFile = aKdbx31FileWithAnElementNeverClosed();
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);

		String refusal = application.importKeePassDatabaseReadingTheRefusal(keePassFile,
			KEEPASS_PASSWORD);

		assertEquals(Messages.getString("keepass.import.failed.message", ""), refusal);
		assertFalse(refusal.contains(ELEMENT), "nothing of the content is shown");
	}

	/**
	 * A KDBX 3.1 file written by the library, whose database name in {@code Meta} is replaced by
	 * the start tag of an element that is never closed: the xml is read under the file's header,
	 * the name replaced, and the xml written under the same header again
	 */
	private File aKdbx31FileWithAnElementNeverClosed() throws Exception
	{
		KdbxCredentials credentials = KeePassCredentials.of(KEEPASS_PASSWORD.toCharArray(), null);
		KdbxDatabase database = new KdbxDatabase();
		database.setName(NAME_TO_REPLACE);
		database.getRootGroup().addEntry(database.newEntry("an entry"));
		File written = new File(tempHome, "written.kdbx");
		try (OutputStream out = new FileOutputStream(written))
		{
			database.write(new KdbxStreamFormat(KdbxStreamFormat.Version.KDBX31), credentials, out);
		}
		KdbxHeader header = new KdbxHeader();
		String xml;
		try (InputStream in = new FileInputStream(written);
			InputStream plain = KdbxSerializer.createUnencryptedInputStream(credentials, header,
				in))
		{
			xml = new String(plain.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertTrue(xml.contains(NAME_TO_REPLACE), "the library wrote the database name");
		File keePassFile = new File(tempHome, "content-cannot-be-read.kdbx");
		try (OutputStream out = new FileOutputStream(keePassFile);
			OutputStream encrypted = KdbxSerializer.createEncryptedOutputStream(credentials, header,
				out))
		{
			encrypted.write(
				xml.replace(NAME_TO_REPLACE, "<" + ELEMENT + ">").getBytes(StandardCharsets.UTF_8));
		}
		return keePassFile;
	}
}
