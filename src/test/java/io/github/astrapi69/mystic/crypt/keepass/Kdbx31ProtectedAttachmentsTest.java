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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.format.KdbxHeader;
import org.linguafranca.pwdb.format.KdbxSerializer;
import org.linguafranca.pwdb.format.KdbxStreamFormat;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Whether a KeePass file is KDBX 3.1 with protected attachments - the one file KeePassJava2 3.1.0
 * refuses for its attachments (#568). Read from the files themselves, the fixtures of the KDBX
 * end-to-end suite and the unit suite's KDBX 3.1 file, and from KDBX 3.1 files the test writes
 * whose {@code Meta} the reader refuses or whose outer header the library cannot read, with every
 * record of the class's logger at every level.
 */
class Kdbx31ProtectedAttachmentsTest
{

	private static final String KDBX_FIXTURES = "src/e2eKdbx/resources/kdbx/";

	/** The name of an entity no document declares, made per run */
	private static final String ENTITY = "text" + UUID.randomUUID().toString().replace("-", "");

	/** What the library writes as the database name, before it is replaced */
	private static final String NAME_TO_REPLACE = "name-to-replace";

	/** Where the fields of a KDBX outer header start: after two signatures and the version */
	private static final int HEADER_FIELDS = 12;

	/** The id of the outer header field that names the cipher, sixteen bytes */
	private static final int CIPHER_ID = 2;

	/** The id of the outer header field of the compression flags, four bytes */
	private static final int COMPRESSION_FLAGS = 3;

	@TempDir
	File directory;

	/** Every record of the class's logger, at every level */
	private final List<LogRecord> records = new ArrayList<>();

	private final Logger classLog = Logger.getLogger(Kdbx31ProtectedAttachments.class.getName());

	private final Handler recorder = new Handler()
	{
		@Override
		public void publish(final LogRecord logRecord)
		{
			records.add(logRecord);
		}

		@Override
		public void flush()
		{
		}

		@Override
		public void close()
		{
		}
	};

	private Level levelBefore;

	private boolean parentHandlersBefore;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@BeforeEach
	void recordEveryLevel()
	{
		levelBefore = classLog.getLevel();
		parentHandlersBefore = classLog.getUseParentHandlers();
		recorder.setLevel(Level.ALL);
		classLog.setLevel(Level.ALL);
		classLog.setUseParentHandlers(false);
		classLog.addHandler(recorder);
	}

	@AfterEach
	void stopRecording()
	{
		classLog.removeHandler(recorder);
		classLog.setUseParentHandlers(parentHandlersBefore);
		classLog.setLevel(levelBefore);
	}

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
			new KdbxCredentials(TestPasswords.throwaway().getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	@DisplayName("a file that does not exist is not known to be the case")
	void aMissingFile_isNotTheCase()
	{
		assertFalse(Kdbx31ProtectedAttachments.in(new File(KDBX_FIXTURES + "no-such-file.kdbx"),
			credentials("keepass")));
	}

	@Test
	@DisplayName("a KDBX 3.1 file whose Meta the reader refuses is recorded by its path and the type of the failure")
	void aFileTheReaderRefuses_isLoggedByItsPathAndTheTypeOfTheFailure() throws Exception
	{
		String password = TestPasswords.throwaway();
		File file = kdbx31WithTheDatabaseName("&" + ENTITY + ";", password);

		boolean theCase = Kdbx31ProtectedAttachments.in(file, credentials(password));

		assertFalse(theCase, "a file that cannot be read is not known to be the case");
		assertEquals(1, records.size(), "one record");
		LogRecord logRecord = records.get(0);
		assertEquals(Level.FINE, logRecord.getLevel());
		assertNull(logRecord.getThrown(), "the record carries no exception");
		assertEquals(2, logRecord.getParameters().length, "the path and the type");
		assertEquals(file, logRecord.getParameters()[0]);
		assertTrue(
			XMLStreamException.class
				.isAssignableFrom(Class.forName((String)logRecord.getParameters()[1])),
			"the type of the reader's failure");
		assertTrue(
			formatted(logRecord)
				.contains("not known to hold protected KDBX 3.1 attachments: " + file + " ("),
			"the formatter prints the path and the type");
		assertFalse(formatted(logRecord).contains(ENTITY), "and nothing else of the failure");
	}

	/**
	 * KDBX 3.1 files the library writes, with one field of the outer header changed so that the
	 * library cannot read the header and ends with a RuntimeException
	 */
	static Stream<Arguments> outerHeadersTheLibraryCannotRead()
	{
		return Stream.of(
			Arguments.of("compression flags 2",
				(UnaryOperator<byte[]>)file -> withAFieldChanged(file, COMPRESSION_FLAGS,
					data -> new byte[] { 2, 0, 0, 0 })),
			Arguments.of("a cipher id of 8 bytes",
				(UnaryOperator<byte[]>)file -> withAFieldChanged(file, CIPHER_ID,
					data -> Arrays.copyOf(data, 8))),
			Arguments.of("a length of 0x8000 for the first field",
				(UnaryOperator<byte[]>)Kdbx31ProtectedAttachmentsTest::withTheFirstFieldLength0x8000));
	}

	@ParameterizedTest(name = "{0}: not known to be the case, recorded by its path and the type of the failure")
	@MethodSource("outerHeadersTheLibraryCannotRead")
	void anOuterHeaderTheLibraryCannotRead_isNotTheCase(final String caseName,
		final UnaryOperator<byte[]> change) throws Exception
	{
		String password = TestPasswords.throwaway();
		File written = kdbx31WithTheDatabaseName("a name", password);
		File changed = new File(directory, "header-changed.kdbx");
		Files.write(changed.toPath(), change.apply(Files.readAllBytes(written.toPath())));

		boolean theCase = Kdbx31ProtectedAttachments.in(changed, credentials(password));

		assertFalse(theCase, "a file that cannot be read is not known to be the case");
		assertEquals(1, records.size(), "one record");
		LogRecord logRecord = records.get(0);
		assertEquals(Level.FINE, logRecord.getLevel());
		assertNull(logRecord.getThrown(), "the record carries no exception");
		assertEquals("not known to hold protected KDBX 3.1 attachments: {0} ({1})",
			logRecord.getMessage(), "the message names the path and the type, and nothing else");
		assertEquals(2, logRecord.getParameters().length, "the path and the type");
		assertEquals(changed, logRecord.getParameters()[0]);
		assertTrue(
			RuntimeException.class
				.isAssignableFrom(Class.forName((String)logRecord.getParameters()[1])),
			"the type of the failure, a RuntimeException of any type");
		assertTrue(
			formatted(logRecord).contains("not known to hold protected KDBX 3.1 attachments: "
				+ changed + " (" + logRecord.getParameters()[1] + ")"),
			"the formatter prints the path and the type");
	}

	/**
	 * The file with one field of its KDBX 3 outer header replaced: the header starts after the two
	 * signatures and the version, twelve bytes, and each field is one byte of id, two of length in
	 * little endian, and its data; the field of id 0 ends it
	 */
	private static byte[] withAFieldChanged(final byte[] file, final int id,
		final UnaryOperator<byte[]> change)
	{
		ByteArrayOutputStream changed = new ByteArrayOutputStream();
		changed.write(file, 0, HEADER_FIELDS);
		int at = HEADER_FIELDS;
		int fieldId;
		do
		{
			fieldId = file[at] & 0xFF;
			int length = (file[at + 1] & 0xFF) | (file[at + 2] & 0xFF) << 8;
			byte[] data = Arrays.copyOfRange(file, at + 3, at + 3 + length);
			byte[] written = fieldId == id ? change.apply(data) : data;
			changed.write(fieldId);
			changed.write(written.length & 0xFF);
			changed.write(written.length >> 8 & 0xFF);
			changed.write(written, 0, written.length);
			at += 3 + length;
		}
		while (fieldId != 0);
		changed.write(file, at, file.length - at);
		return changed.toByteArray();
	}

	/** The file with the length of its first header field set to 0x8000 */
	private static byte[] withTheFirstFieldLength0x8000(final byte[] file)
	{
		byte[] changed = file.clone();
		changed[HEADER_FIELDS + 1] = 0x00;
		changed[HEADER_FIELDS + 2] = (byte)0x80;
		return changed;
	}

	/** The record as a formatter prints it: the message with its parameters, and the throwable */
	private static String formatted(final LogRecord logRecord)
	{
		return new SimpleFormatter().format(logRecord);
	}

	/**
	 * A KDBX 3.1 file whose database name in {@code Meta} is the given xml text: the library writes
	 * the file with a placeholder name, its xml is read under the file's header, the text takes the
	 * place of the placeholder, and the xml is written under the same header
	 */
	private File kdbx31WithTheDatabaseName(final String xmlText, final String password)
		throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		database.setName(NAME_TO_REPLACE);
		database.getRootGroup().addEntry(database.newEntry("an entry"));
		File written = new File(directory, "written.kdbx");
		try (OutputStream out = new FileOutputStream(written))
		{
			database.write(new KdbxStreamFormat(KdbxStreamFormat.Version.KDBX31),
				credentials(password), out);
		}
		KdbxHeader header = new KdbxHeader();
		String xml;
		try (InputStream in = new FileInputStream(written);
			InputStream plain = KdbxSerializer.createUnencryptedInputStream(credentials(password),
				header, in))
		{
			xml = new String(plain.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertTrue(xml.contains(NAME_TO_REPLACE), "the library wrote the database name");
		File changed = new File(directory, "changed.kdbx");
		try (OutputStream out = new FileOutputStream(changed);
			OutputStream encrypted = KdbxSerializer
				.createEncryptedOutputStream(credentials(password), header, out))
		{
			encrypted.write(xml.replace(NAME_TO_REPLACE, xmlText).getBytes(StandardCharsets.UTF_8));
		}
		return changed;
	}

	private static KdbxCredentials credentials(String password)
	{
		String value = "keepass".equals(password) ? TestPasswords.KEEPASS_FIXTURE : password;
		return new KdbxCredentials(value.getBytes(StandardCharsets.UTF_8));
	}
}
