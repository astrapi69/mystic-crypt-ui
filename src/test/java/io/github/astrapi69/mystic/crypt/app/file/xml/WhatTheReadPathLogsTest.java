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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.thoughtworks.xstream.security.ForbiddenClassException;

import io.github.astrapi69.crypt.api.algorithm.AesAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.Algorithm;
import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.factory.SecretKeyFactoryExtensions;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.algorithm.MysticSymmetricAlgorithm;
import io.github.astrapi69.mystic.crypt.app.file.xml.UnauthenticatedVaults.Decryption;
import io.github.astrapi69.mystic.crypt.key.PublicKeyEncryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyGenericEncryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What the sign-in path refuses and records when it reads a database: the refusal the dialog shows,
 * and the records of this application and its family, each with its message and the message of each
 * exception along its chain, compared with the records the reader writes for that refusal. On a way
 * whose decryption authenticates nothing, two wrong passwords are answered with the same records
 * and the same refusal (#1188). The records are taken at the root logger, whose level is set to ALL
 * for each test, so a logger of a library on the path is heard as well as the reader's own.
 */
class WhatTheReadPathLogsTest
{

	private static final String NOT_A_DATABASE = "This file is not a database this application can open. It was not opened, and the file is unchanged.";

	private static final String OPENS_WITH_ITS_KEY_FILE_ALONE = "This database opens with its key file alone, without a master password. It was not opened, and the file is unchanged.";

	private static final String HOLDS_NO_SIGN_IN_DATA = "This database was decrypted, but its content could not be read: it holds no sign-in data. It was not opened, and the file is unchanged.";

	private static final String NOT_IN_A_FORMAT_THIS_VERSION_READS = "The key file opened the key of this database, but its content is in no format this version reads (the reader failed with StreamCorruptedException). It was not opened, and the file is unchanged.";

	/** A type that exists and that no database holds */
	private static final String A_TYPE_NO_DATABASE_HOLDS = "java.io.File";

	private static final String NAMES_A_TYPE_NO_DATABASE_HOLDS = "This file names the type "
		+ A_TYPE_NO_DATABASE_HOLDS
		+ ", which no database of this application holds. It was not opened.";

	/** The namespace of the loggers of this application and of its family's libraries */
	private static final String FAMILY = "io.github.astrapi69.";

	/** The logger of the reader, as its records name it */
	private static final String READER = ApplicationXmlFileReader.class.getName();

	@TempDir
	File directory;

	private final String passwordOfTheVault = TestPasswords.throwaway();

	private final Logger rootLogger = Logger.getLogger("");

	private final List<LogRecord> records = new ArrayList<>();

	private final Handler recorder = new Handler()
	{
		@Override
		public synchronized void publish(final LogRecord logRecord)
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

	private Level rootLevelBefore;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@BeforeEach
	void recordEveryLogger()
	{
		rootLevelBefore = rootLogger.getLevel();
		recorder.setLevel(Level.ALL);
		rootLogger.addHandler(recorder);
		rootLogger.setLevel(Level.ALL);
	}

	@AfterEach
	void stopRecording()
	{
		rootLogger.setLevel(rootLevelBefore);
		rootLogger.removeHandler(recorder);
	}

	/** What a read left: the refusal as the sign-in dialog reads it, and the records */
	private record Outcome(String shown, List<String> records) {
	}

	@Test
	@DisplayName("password alone, the format before the marker: a wrong password the decryption refuses and one it decrypts are answered with the same records and the same refusal")
	void beforeTheMarker_twoWrongPasswords_areAnsweredWithTheSameRecordsAndRefusal()
		throws Exception
	{
		File vault = UnauthenticatedVaults.writtenBeforeTheMarker(directory, xmlOfAVault(),
			passwordOfTheVault);
		Decryption theReaders = UnauthenticatedVaults.withThePasswordAlone(vault);
		String refusedByTheDecryption = UnauthenticatedVaults.aPasswordRefusedBy(theReaders);
		String decryptedByIt = UnauthenticatedVaults.aPasswordPassing(theReaders);

		Outcome first = outcomeOf(vault, null, refusedByTheDecryption);
		Outcome second = outcomeOf(vault, null, decryptedByIt);

		assertTheSame(first, second);
	}

	@Test
	@DisplayName("password and key file, the password layer: a wrong password both formats refuse and one 8's format decrypts are answered with the same records and the same refusal")
	void passwordAndKeyFile_twoWrongPasswords_areAnsweredWithTheSameRecordsAndRefusal()
		throws Exception
	{
		KeyPair key = newKey();
		File keyFile = keyFileOf(key);
		File vault = UnauthenticatedVaults.savedWithBoth(new File(directory, "both.mcrdb"), keyFile,
			passwordOfTheVault.toCharArray());
		String layer = UnauthenticatedVaults.passwordLayerOf(vault, key.getPrivate());
		Decryption ofEight = UnauthenticatedVaults.passwordLayerOfEight(layer);
		Decryption ofThisVersion = UnauthenticatedVaults.passwordLayerOfThisVersion(layer);
		String refusedByBoth = UnauthenticatedVaults.aPasswordRefusedBy(ofEight, ofThisVersion);
		String decryptedByEight = UnauthenticatedVaults.aPasswordPassing(ofEight, ofThisVersion);

		Outcome first = outcomeOf(vault, keyFile, refusedByBoth);
		Outcome second = outcomeOf(vault, keyFile, decryptedByEight);

		assertTheSame(first, second);
	}

	@Test
	@DisplayName("a database of the key file alone, opened with a password and the key file: refused as a database of the key file alone, and the reader records that refusal")
	void aDatabaseOfTheKeyFileAlone_openedWithAPasswordAsWell_isRefusedAsWhatItIs() throws Exception
	{
		KeyPair key = newKey();
		File keyFile = keyFileOf(key);
		File vault = new File(directory, "key-only.mcrdb");
		Files.write(vault.toPath(),
			keyLayerFor(key.getPublic(), MysticSymmetricAlgorithm.AES_GCM_NO_PADDING)
				.encrypt(xmlOfAVault()));

		RuntimeException refused = open(vault, keyFile, passwordOfTheVault);

		assertTrue(refused.getMessage().endsWith("<p>" + OPENS_WITH_ITS_KEY_FILE_ALONE),
			refused::getMessage);
		String recorded = "The key file opened the key of the database, and the database has no "
			+ "password layer: it opens with the key file alone.";
		assertEquals(List.of(READER + " SEVERE " + recorded + " " + List.of(recorded)),
			recordedByTheFamily());
	}

	static Stream<Arguments> keyFilePathsOnADatabaseOfThePasswordAlone()
	{
		return Stream.of(Arguments.of("the password and a key file", true),
			Arguments.of("a key file alone", false));
	}

	@ParameterizedTest(name = "{0}, on a database of the password alone: no database, and the reader records that refusal")
	@MethodSource("keyFilePathsOnADatabaseOfThePasswordAlone")
	void aDatabaseOfThePasswordAlone_openedWithAKeyFile_isNoDatabase_recordedByTypes(
		final String caseName, final boolean withThePassword) throws Exception
	{
		File vault = new File(directory, "password-only.mcrdb");
		Files.write(vault.toPath(), PasswordVaultFormat.encrypt(xmlOfAVault().toCharArray(),
			passwordOfTheVault.toCharArray()));

		RuntimeException refused = open(vault, keyFileOf(newKey()),
			withThePassword ? passwordOfTheVault : null);

		assertTrue(refused.getMessage().endsWith("<p>" + NOT_A_DATABASE), refused::getMessage);
		String streamCorrupted = StreamCorruptedException.class.getName();
		assertEquals(
			List.of(READER + " SEVERE " + streamCorrupted + " " + List.of(streamCorrupted)),
			recordedByTheFamily());
	}

	@Test
	@DisplayName("a key file layer as 8 wrote it, whose content is no serialized object: no format this version reads, and the reader records that refusal")
	void aKeyFileLayerOfEight_whoseContentIsNoObject_isInNoFormat_recordedByTypes() throws Exception
	{
		KeyPair key = newKey();
		File keyFile = keyFileOf(key);
		byte[] content = new byte[32];
		new SecureRandom().nextBytes(content);
		content[0] = 0x5A;
		File vault = new File(directory, "key-only-of-eight.mcrdb");
		Files.write(vault.toPath(), keyLayerBytesFor(key.getPublic(), AesAlgorithm.AES, content));

		RuntimeException refused = open(vault, keyFile, null);

		assertTrue(refused.getMessage().endsWith("<p>" + NOT_IN_A_FORMAT_THIS_VERSION_READS),
			refused::getMessage);
		String recorded = "The key file opened the key of the database, but its content is in no "
			+ "known format: the reader failed with StreamCorruptedException.";
		assertEquals(
			List.of(READER + " SEVERE " + recorded + " "
				+ List.of(recorded, StreamCorruptedException.class.getName())),
			recordedByTheFamily());
	}

	/**
	 * One way a database is protected, written around the given xml and opened with what it needs
	 */
	@FunctionalInterface
	interface Opening
	{
		RuntimeException refusal(WhatTheReadPathLogsTest test, String xml) throws Exception;
	}

	static Stream<Arguments> everyWayADatabaseIsProtected()
	{
		return Stream.of(
			Arguments.of("the password, in the format this version writes",
				(Opening)WhatTheReadPathLogsTest::openWithThePasswordInTheCurrentFormat),
			Arguments.of("the password, in the format before the marker",
				(Opening)WhatTheReadPathLogsTest::openWithThePasswordBeforeTheMarker),
			Arguments.of("the password and a key file",
				(Opening)WhatTheReadPathLogsTest::openWithBoth),
			Arguments.of("a key file alone",
				(Opening)WhatTheReadPathLogsTest::openWithTheKeyAlone));
	}

	@ParameterizedTest(name = "{0}: content without its sign-in data is refused in the words of the bundle, and the reader records that refusal")
	@MethodSource("everyWayADatabaseIsProtected")
	void contentWithoutItsSignInData_isRefusedInTheWordsOfTheBundle(final String caseName,
		final Opening opening) throws Exception
	{
		RuntimeException refused = opening.refusal(this, xmlOfAVault());

		assertTrue(refused.getMessage().endsWith("<p>" + HOLDS_NO_SIGN_IN_DATA),
			refused::getMessage);
		String recorded = "The database was decrypted, but its content could not be read: it holds "
			+ "no sign-in data.";
		assertEquals(List.of(READER + " SEVERE " + recorded + " " + List.of(recorded)),
			recordedByTheFamily());
	}

	@ParameterizedTest(name = "{0}: a type no database holds is named in the refusal, and the reader records that refusal")
	@MethodSource("everyWayADatabaseIsProtected")
	void aTypeNoDatabaseHolds_isNamed_andTheRefusalOfTheReaderIsRecordedByItsType(
		final String caseName, final Opening opening) throws Exception
	{
		String xml = xmlOfAVault().replaceFirst("<list>",
			"<list><" + A_TYPE_NO_DATABASE_HOLDS + "/>");

		RuntimeException refused = opening.refusal(this, xml);

		assertTrue(refused.getMessage().endsWith("<p>" + NAMES_A_TYPE_NO_DATABASE_HOLDS),
			refused::getMessage);
		assertEquals(
			List.of(READER + " SEVERE " + NAMES_A_TYPE_NO_DATABASE_HOLDS + " "
				+ List.of(NAMES_A_TYPE_NO_DATABASE_HOLDS, ForbiddenClassException.class.getName())),
			recordedByTheFamily());
	}

	/**
	 * The xml of the sample vault as this version writes it, without the sign-in data a database
	 * saved by the application holds
	 */
	private static String xmlOfAVault()
	{
		return new String(VaultXmlCodec.toXml(SampleVaults.aVaultWith(entry -> {
		})));
	}

	private Outcome outcomeOf(final File vault, final File keyFile, final String password)
	{
		records.clear();
		RuntimeException refused = open(vault, keyFile, password);
		List<String> described = new ArrayList<>();
		for (LogRecord logRecord : List.copyOf(records))
		{
			if (logRecord.getLoggerName() != null && logRecord.getLoggerName().startsWith(FAMILY))
			{
				described.add(described(logRecord));
			}
		}
		return new Outcome(refused.getMessage(), described);
	}

	/**
	 * Both reads were answered alike: the same refusal, and records of the same loggers, at the
	 * same levels, with the same messages and the same types along their exceptions. Compared are
	 * the records of this application and its family; a record of the JDK can depend on what was
	 * loaded first
	 */
	private static void assertTheSame(final Outcome first, final Outcome second)
	{
		assertFalse(first.records().isEmpty(), "the reader records the refusal");
		assertEquals(first.shown(), second.shown(), "both are refused alike");
		assertEquals(first.records(), second.records(), "both are recorded alike");
	}

	/** A record by its logger, its level, its message and the types along its exception */
	private static String described(final LogRecord logRecord)
	{
		return logRecord.getLoggerName() + " " + logRecord.getLevel() + " " + logRecord.getMessage()
			+ " " + typesOf(logRecord.getThrown());
	}

	private static List<String> typesOf(final Throwable thrown)
	{
		List<String> types = new ArrayList<>();
		for (Throwable link = thrown; link != null; link = link.getCause())
		{
			types.add(link.getClass().getName());
		}
		return types;
	}

	/**
	 * The records of this application and its family, each by its logger, its level, its message
	 * and the message of each exception along its chain. None of them carries a parameter or a
	 * suppressed exception, which the description would not show
	 */
	private List<String> recordedByTheFamily()
	{
		List<String> recorded = new ArrayList<>();
		for (LogRecord logRecord : List.copyOf(records))
		{
			if (logRecord.getLoggerName() != null && logRecord.getLoggerName().startsWith(FAMILY))
			{
				assertNull(logRecord.getParameters(), "the record carries no parameter");
				List<String> messages = new ArrayList<>();
				for (Throwable link = logRecord.getThrown(); link != null; link = link.getCause())
				{
					assertEquals(0, link.getSuppressed().length,
						"the record carries no suppressed exception");
					messages.add(link.getMessage());
				}
				recorded.add(logRecord.getLoggerName() + " " + logRecord.getLevel() + " "
					+ logRecord.getMessage() + " " + messages);
			}
		}
		return recorded;
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
		KeyPair key = newKey();
		File keyFile = keyFileOf(key);
		File vault = UnauthenticatedVaults.writtenWithBoth(
			new File(directory, "both-" + UUID.randomUUID() + ".mcrdb"), xml, passwordOfTheVault,
			key.getPublic());
		return open(vault, keyFile, passwordOfTheVault);
	}

	private RuntimeException openWithTheKeyAlone(final String xml) throws Exception
	{
		KeyPair key = newKey();
		File keyFile = keyFileOf(key);
		File vault = new File(directory, "key-only-" + UUID.randomUUID() + ".mcrdb");
		Files.write(vault.toPath(),
			keyLayerFor(key.getPublic(), MysticSymmetricAlgorithm.AES_GCM_NO_PADDING).encrypt(xml));
		return open(vault, keyFile, null);
	}

	private static KeyPair newKey() throws Exception
	{
		return KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
	}

	private File keyFileOf(final KeyPair key) throws Exception
	{
		File keyFile = new File(directory, "key-" + UUID.randomUUID() + ".pem");
		PrivateKeyWriter.writeInPemFormat(key.getPrivate(), keyFile);
		return keyFile;
	}

	/** The key layer as ApplicationXmlFileStoreWorker writes it, around a text */
	private static PublicKeyGenericEncryptor<String> keyLayerFor(final PublicKey publicKey,
		final Algorithm symmetricAlgorithm) throws Exception
	{
		return new PublicKeyGenericEncryptor<>(keyLayerEncryptorFor(publicKey, symmetricAlgorithm));
	}

	/** The key layer around the given bytes as they are, not serialized first */
	private static byte[] keyLayerBytesFor(final PublicKey publicKey,
		final Algorithm symmetricAlgorithm, final byte[] content) throws Exception
	{
		return keyLayerEncryptorFor(publicKey, symmetricAlgorithm).encrypt(content);
	}

	private static PublicKeyEncryptor keyLayerEncryptorFor(final PublicKey publicKey,
		final Algorithm symmetricAlgorithm) throws Exception
	{
		SecretKey symmetricKey = SecretKeyFactoryExtensions
			.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128);
		CryptModel<Cipher, SecretKey, String> symmetricKeyModel = CryptModel
			.<Cipher, SecretKey, String> builder().key(symmetricKey).algorithm(symmetricAlgorithm)
			.operationMode(Cipher.ENCRYPT_MODE).build();
		CryptModel<Cipher, PublicKey, byte[]> encryptionModel = CryptModel
			.<Cipher, PublicKey, byte[]> builder().key(publicKey).build();
		return new PublicKeyEncryptor(encryptionModel, symmetricKeyModel);
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
		// the records of the read alone: writing the database before it is no part of the read
		records.clear();
		return assertThrows(RuntimeException.class, () -> ApplicationXmlFileReader.read(opening));
	}
}
