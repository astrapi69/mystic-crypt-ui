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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

import io.github.astrapi69.crypt.api.algorithm.AesAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.factory.SecretKeyFactoryExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.algorithm.MysticSymmetricAlgorithm;
import io.github.astrapi69.mystic.crypt.key.PublicKeyEncryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyGenericEncryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What the sign-in shows and what the reader logs when a database opens with the right password or
 * key file and its content cannot be read, on each of the ways a database is protected. Each
 * document is the xml this version writes, changed in one place: it breaks inside the first entry,
 * its last identifier is no number, or it gives a type in an element name or in the class attribute
 * of a field; or it is a root the reader fails on before it converts anything: a name whose escape
 * is no number or is cut short, or a type other than the vault. Asserted are the text of the
 * refusal the sign-in shows, the messages of the records the reader writes, the exceptions those
 * records carry below the reader's own failure, and, for every document, that the records and the
 * refusal, printed with every cause and suppressed exception, name the failure by its kind and
 * type.
 */
class UnreadableContentLogTest
{

	/** What turns the document into no xml: a markup that starts with a character no name has */
	private static final String THE_BREAK = "<<";

	/** A type of this family that no version has, as a newer version's database could give one */
	private static final String A_TYPE_OF_A_NEWER_VERSION = "io.github.astrapi69.mystic.crypt.SomeTypeOfANewerVersion";

	/** A type name of another form than a class name of this family */
	private static final String A_TYPE_OF_ANOTHER_FORM = "a-type-of-another-form";

	/** What the last identifier holds instead of a number */
	private static final String NO_NUMBER = "no-number";

	/** A root name whose escape is no hex number: the reader fails on it before it converts */
	private static final String A_ROOT_NAME_WITH_AN_ESCAPE_THAT_IS_NO_NUMBER = "<a_.zzzz/>";

	/** A root name whose escape is cut short: the reader fails on it before it converts */
	private static final String A_ROOT_NAME_WITH_AN_ESCAPE_CUT_SHORT = "<a_.zz/>";

	/** What a root of another type than the vault holds */
	private static final String A_TEXT_AS_THE_ROOT = "a-text-as-the-root";

	/** A root of another type than the vault, which the reader builds and the codec cannot take */
	private static final String A_ROOT_OF_ANOTHER_TYPE = "<string>" + A_TEXT_AS_THE_ROOT
		+ "</string>";

	/** The reader's record of a database that decrypted and could not be read, before its reason */
	private static final String RECORD_OF_UNREADABLE_CONTENT = "The database was decrypted, but its content could not be read: ";

	/** The reader's record of a decryption without authentication that gave back no xml (#1188) */
	private static final String RECORD_OF_NO_XML = "A decryption that authenticates nothing gave back no xml, which is what a wrong password or key leaves: ";

	/**
	 * The sign-in's words for a database that decrypted and could not be read, before its reason
	 */
	private static final String SHOWN_FOR_UNREADABLE_CONTENT = "This database was decrypted, but its content could not be read: ";

	private static final String NOT_OPENED = ". It was not opened, and the file is unchanged.";

	private static final String CONVERSION_EXCEPTION = ConversionException.class.getName();

	private static final String STREAM_EXCEPTION = StreamException.class.getName();

	private static final String XML_PULL_PARSER_EXCEPTION = "org.xmlpull.v1.XmlPullParserException";

	private static final String CANNOT_RESOLVE_CLASS_EXCEPTION = CannotResolveClassException.class
		.getName();

	@TempDir
	File directory;

	private final String passwordOfTheVault = TestPasswords.throwaway();

	private final Logger readerLog = Logger.getLogger(ApplicationXmlFileReader.class.getName());

	private final List<LogRecord> records = new ArrayList<>();

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
	void recordTheReadersLog()
	{
		parentHandlersBefore = readerLog.getUseParentHandlers();
		readerLog.setUseParentHandlers(false);
		readerLog.addHandler(recorder);
	}

	@AfterEach
	void stopRecording()
	{
		readerLog.removeHandler(recorder);
		readerLog.setUseParentHandlers(parentHandlersBefore);
	}

	/** One way a database is protected, written from the given xml and opened with what it needs */
	@FunctionalInterface
	interface Opening
	{
		RuntimeException refusal(UnreadableContentLogTest test, String xml) throws Exception;
	}

	static Stream<Arguments> everyWayADatabaseIsProtected()
	{
		return Stream.of(
			Arguments.of("the password, in the format this version writes",
				(Opening)UnreadableContentLogTest::openWithThePasswordInTheCurrentFormat),
			Arguments.of("the password, in the format before the marker",
				(Opening)UnreadableContentLogTest::openWithThePasswordBeforeTheMarker),
			Arguments.of("the password and a key file",
				(Opening)UnreadableContentLogTest::openWithBoth),
			Arguments.of("a key file alone",
				(Opening)UnreadableContentLogTest::openWithTheKeyAlone));
	}

	/**
	 * Every way a database is protected, with what the reader records and the sign-in shows for
	 * content that is no xml: where the decryption authenticates nothing, that is a failed
	 * decryption (#1188)
	 */
	static Stream<Arguments> everyWayAndItsRefusalOfNoXml()
	{
		String reason = "the reader failed with XmlPullParserException";
		return Stream.of(
			Arguments.of("the password, in the format this version writes",
				(Opening)UnreadableContentLogTest::openWithThePasswordInTheCurrentFormat,
				RECORD_OF_UNREADABLE_CONTENT + reason + ".",
				SHOWN_FOR_UNREADABLE_CONTENT + reason + NOT_OPENED),
			Arguments.of("the password, in the format before the marker",
				(Opening)UnreadableContentLogTest::openWithThePasswordBeforeTheMarker,
				RECORD_OF_NO_XML + reason + ".", "Wrong master password."),
			Arguments.of("the password and a key file",
				(Opening)UnreadableContentLogTest::openWithBoth, RECORD_OF_NO_XML + reason + ".",
				"The master password or the key file is wrong."),
			Arguments.of("a key file alone", (Opening)UnreadableContentLogTest::openWithTheKeyAlone,
				RECORD_OF_UNREADABLE_CONTENT + reason + ".",
				SHOWN_FOR_UNREADABLE_CONTENT + reason + NOT_OPENED));
	}

	/** Every way a database is protected, with each place a document gives a type in */
	static Stream<Arguments> everyWayAndEveryPlaceOfAType()
	{
		return everyWayADatabaseIsProtected().flatMap(way -> Stream.of(
			Arguments.of(way.get()[0] + ", an element name", way.get()[1],
				(UnaryOperator<String>)UnreadableContentLogTest::asAnElementInTheFirstList),
			Arguments.of(way.get()[0] + ", the class attribute of a field", way.get()[1],
				(UnaryOperator<String>)UnreadableContentLogTest::asTheClassOfAFieldOfTheFirstEntry)));
	}

	@ParameterizedTest(name = "{0}: the record of the reader names the failure by types and positions")
	@MethodSource("everyWayAndItsRefusalOfNoXml")
	void aDocumentThatBreaks_isRefusedAndLoggedByTypesAndPositions(final String caseName,
		final Opening opening, final String record, final String shown) throws Exception
	{
		String xml = brokenInsideTheFirstEntry();
		int line = lineOf(xml, THE_BREAK);

		RuntimeException refused = opening.refusal(this, xml);

		assertTrue(refused.getMessage().endsWith("<p>" + shown), refused::getMessage);
		assertEquals(List.of(record), recordMessages());
		assertEquals(List.of(CONVERSION_EXCEPTION + " at line " + line, STREAM_EXCEPTION,
			XML_PULL_PARSER_EXCEPTION + " at line " + line), messagesBelowTheReadersFailure());
	}

	@ParameterizedTest(name = "{0}: the record of the reader names the failure by types and position")
	@MethodSource("everyWayADatabaseIsProtected")
	void aLastIdThatIsNoNumber_isRefusedAndLoggedByTypesAndPosition(final String caseName,
		final Opening opening) throws Exception
	{
		String reason = "the reader failed with NumberFormatException";
		String xml = withNoNumberAsTheLastId();

		RuntimeException refused = opening.refusal(this, xml);

		assertTrue(refused.getMessage().endsWith(
			"<p>" + SHOWN_FOR_UNREADABLE_CONTENT + reason + NOT_OPENED), refused::getMessage);
		assertEquals(List.of(RECORD_OF_UNREADABLE_CONTENT + reason + "."), recordMessages());
		assertEquals(List.of(CONVERSION_EXCEPTION + " at line " + lineOf(xml, "<lastId>"),
			NumberFormatException.class.getName()), messagesBelowTheReadersFailure());
	}

	@ParameterizedTest(name = "{0}: the sign-in and the record of the reader name a type this version does not know")
	@MethodSource("everyWayAndEveryPlaceOfAType")
	void aTypeOfAnotherForm_isNamedAsATypeThisVersionDoesNotKnow(final String caseName,
		final Opening opening, final UnaryOperator<String> place) throws Exception
	{
		String reason = "it names a type this version does not know";
		String xml = place.apply(A_TYPE_OF_ANOTHER_FORM);

		RuntimeException refused = opening.refusal(this, xml);

		assertTrue(refused.getMessage().endsWith(
			"<p>" + SHOWN_FOR_UNREADABLE_CONTENT + reason + NOT_OPENED), refused::getMessage);
		assertEquals(List.of(RECORD_OF_UNREADABLE_CONTENT + reason + "."), recordMessages());
		assertEquals(
			List.of(CONVERSION_EXCEPTION + " at line " + lineOf(xml, A_TYPE_OF_ANOTHER_FORM),
				CANNOT_RESOLVE_CLASS_EXCEPTION),
			messagesBelowTheReadersFailure());
	}

	@ParameterizedTest(name = "{0}: the sign-in and the record of the reader name the type")
	@MethodSource("everyWayAndEveryPlaceOfAType")
	void aTypeOfThisFamilyThisVersionLacks_isNamed(final String caseName, final Opening opening,
		final UnaryOperator<String> place) throws Exception
	{
		String reason = "it names the type " + A_TYPE_OF_A_NEWER_VERSION
			+ ", which this version does not know";

		RuntimeException refused = opening.refusal(this, place.apply(A_TYPE_OF_A_NEWER_VERSION));

		assertTrue(refused.getMessage().endsWith(
			"<p>" + SHOWN_FOR_UNREADABLE_CONTENT + reason + NOT_OPENED), refused::getMessage);
		assertEquals(List.of(RECORD_OF_UNREADABLE_CONTENT + reason + "."), recordMessages());
	}

	/** Every way a database is protected, with each document the reader fails before converting */
	static Stream<Arguments> everyWayAndEveryDocumentTheReaderFailsBeforeConverting()
	{
		return everyWayADatabaseIsProtected().flatMap(way -> Stream.of(
			Arguments.of(way.get()[0] + ", a root name whose escape is no number", way.get()[1],
				A_ROOT_NAME_WITH_AN_ESCAPE_THAT_IS_NO_NUMBER, NumberFormatException.class),
			Arguments.of(way.get()[0] + ", a root name whose escape is cut short", way.get()[1],
				A_ROOT_NAME_WITH_AN_ESCAPE_CUT_SHORT, StringIndexOutOfBoundsException.class),
			Arguments.of(way.get()[0] + ", a root of another type", way.get()[1],
				A_ROOT_OF_ANOTHER_TYPE, ClassCastException.class)));
	}

	/**
	 * Every way a database is protected, with every document of this test that the reader fails on,
	 * each with a text the message of that failure carries
	 */
	static Stream<Arguments> everyWayAndEveryDocumentWithATextOfItsFailure()
	{
		return everyWayADatabaseIsProtected().flatMap(way -> Stream.of(
			Arguments.of(way.get()[0] + ", a document that breaks", way.get()[1],
				brokenInsideTheFirstEntry(), "with umlauts"),
			Arguments.of(way.get()[0] + ", a last identifier that is no number", way.get()[1],
				withNoNumberAsTheLastId(), NO_NUMBER),
			Arguments.of(way.get()[0] + ", a type of another form in an element name", way.get()[1],
				asAnElementInTheFirstList(A_TYPE_OF_ANOTHER_FORM), A_TYPE_OF_ANOTHER_FORM),
			Arguments.of(way.get()[0] + ", a type of another form in a class attribute",
				way.get()[1], asTheClassOfAFieldOfTheFirstEntry(A_TYPE_OF_ANOTHER_FORM),
				A_TYPE_OF_ANOTHER_FORM),
			Arguments.of(way.get()[0] + ", a root name whose escape is no number", way.get()[1],
				A_ROOT_NAME_WITH_AN_ESCAPE_THAT_IS_NO_NUMBER, "zzzz"),
			Arguments.of(way.get()[0] + ", a root name whose escape is cut short", way.get()[1],
				A_ROOT_NAME_WITH_AN_ESCAPE_CUT_SHORT, "for length"),
			Arguments.of(way.get()[0] + ", a root of another type", way.get()[1],
				A_ROOT_OF_ANOTHER_TYPE, "cannot be cast")));
	}

	@ParameterizedTest(name = "{0}: the record of the reader names the failure by its kind and type")
	@MethodSource("everyWayAndEveryDocumentTheReaderFailsBeforeConverting")
	void aDocumentTheReaderFailsBeforeConverting_isRefusedAndLoggedByKindAndType(
		final String caseName, final Opening opening, final String xml,
		final Class<? extends RuntimeException> kind) throws Exception
	{
		String reason = "the reader failed with " + kind.getSimpleName();

		RuntimeException refused = opening.refusal(this, xml);

		assertTrue(refused.getMessage().endsWith(
			"<p>" + SHOWN_FOR_UNREADABLE_CONTENT + reason + NOT_OPENED), refused::getMessage);
		assertEquals(List.of(RECORD_OF_UNREADABLE_CONTENT + reason + "."), recordMessages());
		assertEquals(List.of(kind.getName()), messagesBelowTheReadersFailure());
	}

	@ParameterizedTest(name = "{0}: the records and the refusal, printed with their stack traces, name the failure by its kind and type")
	@MethodSource("everyWayAndEveryDocumentWithATextOfItsFailure")
	void theRecordsAndTheRefusal_nameTheFailureByItsKindAndType(final String caseName,
		final Opening opening, final String xml, final String textOfTheFailure) throws Exception
	{
		RuntimeException refused = opening.refusal(this, xml);

		assertFalse(records.isEmpty(), "the reader logs why the database was not opened");
		for (LogRecord logRecord : records)
		{
			assertFalse(printed(logRecord).contains(textOfTheFailure),
				() -> "the record of the reader at " + logRecord.getLevel());
		}
		assertFalse(printedAsTheSignInLogsIt(refused).contains(textOfTheFailure),
			"the refusal as the sign-in logs it");
	}

	/**
	 * A record as a formatter can print it: its message, its parameters, and the stack trace of its
	 * throwable with every cause and every suppressed exception
	 */
	private static String printed(final LogRecord logRecord)
	{
		StringBuilder printed = new StringBuilder(String.valueOf(logRecord.getMessage()));
		if (logRecord.getParameters() != null)
		{
			for (Object parameter : logRecord.getParameters())
			{
				printed.append(' ').append(parameter);
			}
		}
		if (logRecord.getThrown() != null)
		{
			printed.append(stackTraceOf(logRecord.getThrown()));
		}
		return printed.toString();
	}

	/** The refusal as the sign-in panel logs it: its message, and its stack trace */
	private static String printedAsTheSignInLogsIt(final RuntimeException refused)
	{
		return refused.getMessage() + stackTraceOf(refused);
	}

	/** The stack trace as printStackTrace writes it, causes and suppressed exceptions included */
	private static String stackTraceOf(final Throwable thrown)
	{
		StringWriter printed = new StringWriter();
		thrown.printStackTrace(new PrintWriter(printed));
		return printed.toString();
	}

	/** The messages of the records the reader wrote, in order */
	private List<String> recordMessages()
	{
		return records.stream().map(LogRecord::getMessage).toList();
	}

	/** The messages of the exceptions below the reader's failure, in the reader's one record */
	private List<String> messagesBelowTheReadersFailure()
	{
		List<String> messages = new ArrayList<>();
		for (Throwable link = theReadersFailure().getCause(); link != null; link = link.getCause())
		{
			messages.add(link.getMessage());
		}
		return messages;
	}

	/** The line the given part of the xml starts on, counted from 1 as the parser counts */
	private static int lineOf(final String xml, final String part)
	{
		return 1 + (int)xml.substring(0, xml.indexOf(part)).chars()
			.filter(character -> character == '\n').count();
	}

	private VaultContentUnreadableException theReadersFailure()
	{
		assertEquals(1, records.size(), "the reader writes one record");
		for (Throwable link = records.get(0).getThrown(); link != null; link = link.getCause())
		{
			if (link instanceof VaultContentUnreadableException unreadable)
			{
				return unreadable;
			}
		}
		return fail("the record carries the reader's failure");
	}

	private static String theXmlThisVersionWrites()
	{
		return new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		})));
	}

	private static String brokenInsideTheFirstEntry()
	{
		return withTheFirst(theXmlThisVersionWrites(), "</notes>", THE_BREAK + "</notes>");
	}

	private static String withNoNumberAsTheLastId()
	{
		return withTheFirst(theXmlThisVersionWrites(), "<lastId>2</lastId>",
			"<lastId>" + NO_NUMBER + "</lastId>");
	}

	private static String asAnElementInTheFirstList(final String typeName)
	{
		return withTheFirst(theXmlThisVersionWrites(), "<list>", "<list><" + typeName + "/>");
	}

	private static String asTheClassOfAFieldOfTheFirstEntry(final String typeName)
	{
		return withTheFirst(theXmlThisVersionWrites(), "<notes>",
			"<notes class=\"" + typeName + "\">");
	}

	private static String withTheFirst(final String xml, final String part,
		final String replacement)
	{
		int at = xml.indexOf(part);
		assertTrue(0 <= at, () -> "the xml this version writes holds " + part);
		return xml.substring(0, at) + replacement + xml.substring(at + part.length());
	}

	private RuntimeException openWithThePasswordInTheCurrentFormat(final String xml)
		throws Exception
	{
		File vault = new File(directory, "current-" + UUID.randomUUID() + ".mcrdb");
		Files.write(vault.toPath(),
			PasswordVaultFormat.encrypt(xml.toCharArray(), passwordOfTheVault.toCharArray()));
		return open(vault, null, passwordOfTheVault);
	}

	private RuntimeException openWithThePasswordBeforeTheMarker(final String xml) throws Exception
	{
		File vault = UnauthenticatedVaults.writtenBeforeTheMarker(directory, xml,
			passwordOfTheVault);
		return open(vault, null, passwordOfTheVault);
	}

	private RuntimeException openWithBoth(final String xml) throws Exception
	{
		KeyPair key = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		File keyFile = keyFileOf(key);
		File vault = UnauthenticatedVaults.writtenWithBoth(
			new File(directory, "both-" + UUID.randomUUID() + ".mcrdb"), xml, passwordOfTheVault,
			key.getPublic());
		return open(vault, keyFile, passwordOfTheVault);
	}

	private RuntimeException openWithTheKeyAlone(final String xml) throws Exception
	{
		KeyPair key = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		File keyFile = keyFileOf(key);
		File vault = new File(directory, "key-only-" + UUID.randomUUID() + ".mcrdb");
		Files.write(vault.toPath(), keyLayerFor(key.getPublic()).encrypt(xml));
		return open(vault, keyFile, null);
	}

	private File keyFileOf(final KeyPair key) throws Exception
	{
		File keyFile = new File(directory, "key-" + UUID.randomUUID() + ".pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		return keyFile;
	}

	/** The key layer as ApplicationXmlFileStoreWorker writes it */
	private static PublicKeyGenericEncryptor<String> keyLayerFor(final PublicKey publicKey)
		throws Exception
	{
		SecretKey symmetricKey = SecretKeyFactoryExtensions
			.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128);
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel = CryptModel
			.<Cipher, SecretKey, String> builder().key(symmetricKey)
			.algorithm(MysticSymmetricAlgorithm.AES_GCM_NO_PADDING)
			.operationMode(Cipher.ENCRYPT_MODE).build();
		CryptModel<Cipher, PublicKey, byte[]> encryptionModel = CryptModel
			.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();
		return new PublicKeyGenericEncryptor<>(
			new PublicKeyEncryptor(encryptionModel, symmetricKeyModel));
	}

	private RuntimeException open(final File vault, final File keyFile, final String password)
	{
		char[] masterPassword = password == null ? null : password.toCharArray();
		MasterPwFileModelBean.MasterPwFileModelBeanBuilder credentials = MasterPwFileModelBean
			.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(masterPassword)
			.withMasterPw(masterPassword != null).withKeyFile(keyFile != null).minPasswordLength(6);
		if (keyFile != null)
		{
			credentials.keyFileInfo(FileInfo.toFileInfo(keyFile));
		}
		MasterPwFileModelBean opening = credentials.build();
		return assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader.read(opening));
	}
}
