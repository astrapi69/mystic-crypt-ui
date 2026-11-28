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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.compound.CompoundAlgorithm;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.write.StoreFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileReader;
import io.github.astrapi69.mystic.crypt.app.file.xml.PasswordVaultFormat;
import io.github.astrapi69.mystic.crypt.app.file.xml.SignInRefusedException;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultContentUnreadableException;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultOpensWithTheKeyFileAloneException;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.file.PBEFileEncryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwWithApplicationFilePanel;

/**
 * What the running application shows and logs when the sign-in refuses a database: the dialog in
 * the words of the bundle, and the records of the reader and of the sign-in panel, each with its
 * message and the message of each exception along its chain, compared with the records of that
 * refusal. On a database whose decryption authenticates nothing, two wrong passwords are answered
 * with the same dialog and the same records (#1188). The panel writes its record only after the
 * dialog is closed, so it is read here through the application. Records are taken at the root
 * logger, with the loggers of this application and its family at every level.
 */
class TheSignInRefusesInTheWordsOfTheBundleUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** The logger of the reader, as its records name it */
	private static final String READER = ApplicationXmlFileReader.class.getName();

	/** The logger of the sign-in panel, as its records name it */
	private static final String PANEL = MasterPwWithApplicationFilePanel.class.getName();

	/** The logger whose level the test opens up: the parent of every logger of the family */
	private static final String FAMILY = "io.github.astrapi69";

	/** How many passwords a pick draws at most; about one in 256 decrypts without an exception */
	private static final int MOST_DRAWS = 1 << 16;

	private final Logger rootLogger = Logger.getLogger("");

	private final Logger familyLogger = Logger.getLogger(FAMILY);

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

	private Level familyLevelBefore;

	@BeforeEach
	void recordEveryLogger()
	{
		familyLevelBefore = familyLogger.getLevel();
		recorder.setLevel(Level.ALL);
		rootLogger.addHandler(recorder);
		familyLogger.setLevel(Level.ALL);
	}

	@AfterEach
	void stopRecording()
	{
		familyLogger.setLevel(familyLevelBefore);
		rootLogger.removeHandler(recorder);
	}

	@Test
	@DisplayName("a database without the marker: a wrong password the decryption refuses and one it decrypts are answered with the same dialog and the same records")
	void beforeTheMarker_twoWrongPasswords_areAnsweredWithTheSameDialogAndRecords() throws Exception
	{
		File databaseFile = writtenBeforeTheMarker();
		String refusedByTheDecryption = aWrongPassword(databaseFile, false);
		String decryptedByIt = aWrongPassword(databaseFile, true);
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().browseApplicationFile(databaseFile);

		List<String> first = shownAndRecorded(signIn, refusedByTheDecryption);
		List<String> second = shownAndRecorded(signIn, decryptedByIt);

		assertTrue(first.size() > 1, "the dialog and the records: " + first);
		assertEquals(first, second, "both are answered alike");
		signIn.cancel();
	}

	@Test
	@DisplayName("a database of the key file alone, opened with a password and the key file: the dialog says so, and the reader and the sign-in panel record that refusal")
	void aDatabaseOfTheKeyFileAlone_openedWithAPasswordAsWell_isRefusedAsWhatItIs() throws Exception
	{
		File keyFile = new File(tempHome, "key.pem");
		File databaseFile = writtenWithTheKeyFileAlone(keyFile);
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(MASTER_PASSWORD)
			.checkKeyFile().browseKeyFile(keyFile).browseApplicationFile(databaseFile)
			.requireOkEnabled();
		clearTheRecords();
		signIn.clickOk();

		String shown = new ApplicationSteps(robot)
			.readAndDismissMessageDialog("Database not opened");
		robot.waitForIdle();

		assertTrue(shown.endsWith("<p>This database opens with its key file alone, without a "
			+ "master password. It was not opened, and the file is unchanged."), shown);
		String recorded = "The key file opened the key of the database, and the database has no "
			+ "password layer: it opens with the key file alone.";
		assertEquals(
			List.of(READER + " SEVERE " + recorded + " " + List.of(recorded),
				PANEL + " SEVERE Database not opened "
					+ List.of(SignInRefusedException.class.getName(),
						VaultOpensWithTheKeyFileAloneException.class.getName())),
			recordedByTheReaderAndThePanel());
		signIn.cancel();
	}

	@Test
	@DisplayName("a database without its sign-in data: the dialog says what it is in the words of the bundle, and the reader and the sign-in panel record that refusal")
	void aDatabaseWithoutItsSignInData_isRefusedInTheWordsOfTheBundle() throws Exception
	{
		File databaseFile = writtenWithoutItsSignInData();
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(MASTER_PASSWORD)
			.browseApplicationFile(databaseFile).requireOkEnabled();
		clearTheRecords();
		signIn.clickOk();

		String shown = new ApplicationSteps(robot)
			.readAndDismissMessageDialog("Database not opened");
		robot.waitForIdle();

		assertEquals("<html><body width='350'><h2>Database not opened</h2><p>This database was "
			+ "decrypted, but its content could not be read: it holds no sign-in data. It was not "
			+ "opened, and the file is unchanged.", shown);
		String recorded = "The database was decrypted, but its content could not be read: it holds "
			+ "no sign-in data.";
		assertEquals(
			List.of(READER + " SEVERE " + recorded + " " + List.of(recorded),
				PANEL + " SEVERE Database not opened "
					+ List.of(SignInRefusedException.class.getName(),
						VaultContentUnreadableException.class.getName())),
			recordedByTheReaderAndThePanel());
		signIn.cancel();
	}

	/**
	 * Signs in with the password, reads the dialog of the refusal and closes it, and gives back
	 * what it showed followed by the records of the family the attempt left: each by its logger,
	 * its level, its message, and the type and the message of each exception along its chain
	 */
	private List<String> shownAndRecorded(final SignInDialogSteps signIn, final String password)
	{
		clearTheRecords();
		signIn.typeMasterPassword(password).requireOkEnabled().clickOk();
		String shown = new ApplicationSteps(robot)
			.readAndDismissMessageDialog("Authentication with Password");
		robot.waitForIdle();
		List<String> shownAndRecorded = new ArrayList<>();
		shownAndRecorded.add(shown);
		for (LogRecord logRecord : snapshot())
		{
			if (logRecord.getLoggerName() != null && logRecord.getLoggerName().startsWith(FAMILY))
			{
				shownAndRecorded.add(logRecord.getLoggerName() + " " + logRecord.getLevel() + " "
					+ logRecord.getMessage() + " " + typesOf(logRecord.getThrown()) + " "
					+ messagesOf(logRecord.getThrown()));
			}
		}
		return shownAndRecorded;
	}

	/** The records so far; the recorder takes them on the threads that log, under its own lock */
	private List<LogRecord> snapshot()
	{
		synchronized (recorder)
		{
			return List.copyOf(records);
		}
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

	/** The message of each exception along the chain, which names its type and its line */
	private static List<String> messagesOf(final Throwable thrown)
	{
		List<String> messages = new ArrayList<>();
		for (Throwable link = thrown; link != null; link = link.getCause())
		{
			messages.add(link.getMessage());
		}
		return messages;
	}

	/** Forgets the records so far, so the next ones are those of the attempt that follows */
	private void clearTheRecords()
	{
		synchronized (recorder)
		{
			records.clear();
		}
	}

	/**
	 * The records of the reader and of the sign-in panel, each by its logger, its level, its
	 * message and the message of each exception along its chain
	 */
	private List<String> recordedByTheReaderAndThePanel()
	{
		List<String> recorded = new ArrayList<>();
		for (LogRecord logRecord : snapshot())
		{
			if (READER.equals(logRecord.getLoggerName()) || PANEL.equals(logRecord.getLoggerName()))
			{
				recorded.add(logRecord.getLoggerName() + " " + logRecord.getLevel() + " "
					+ logRecord.getMessage() + " " + messagesOf(logRecord.getThrown()));
			}
		}
		return recorded;
	}

	/**
	 * A database written the way every release up to 8.2 wrote it: PBEWithMD5AndDES with the fixed
	 * salt and iteration count, and no marker
	 */
	private File writtenBeforeTheMarker() throws Exception
	{
		File current = new File(tempHome, "current.mcrdb");
		createDatabaseFileHeadless(current, MASTER_PASSWORD);
		File plain = new File(tempHome, "plain.xml");
		StoreFileExtensions.toFile(plain, PasswordVaultFormat.decrypt(current, MASTER_PASSWORD));
		CryptModel<Cipher, String, String> legacyModel = CryptModel
			.<Cipher, String, String> builder().key(MASTER_PASSWORD)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).salt(CompoundAlgorithm.SALT)
			.iterationCount(CompoundAlgorithm.ITERATIONCOUNT).build();
		File written = new PBEFileEncryptor(legacyModel, new File(tempHome, "legacy.mcrdb"))
			.encrypt(plain);
		Files.delete(plain.toPath());
		assertFalse(PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(written.toPath())),
			"the database is in the format before the marker");
		return written;
	}

	/**
	 * A wrong password drawn at runtime that the decryption of the database refuses, or decrypts to
	 * a text that cannot begin an xml document
	 */
	private static String aWrongPassword(final File databaseFile, final boolean decrypted)
		throws Exception
	{
		byte[] content = Files.readAllBytes(databaseFile.toPath());
		for (int draw = 0; draw < MOST_DRAWS; draw++)
		{
			String password = TestPasswords.throwaway();
			String text;
			try
			{
				text = PasswordVaultFormat.decryptLegacy(content, password);
			}
			catch (GeneralSecurityException refused)
			{
				text = null;
			}
			if (decrypted ? text != null && !text.stripLeading().startsWith("<") : text == null)
			{
				return password;
			}
		}
		return fail("no password of " + MOST_DRAWS + " gave the outcome");
	}

	/** A database of the key file alone, written by the application's own factory */
	private File writtenWithTheKeyFileAlone(final File keyFile) throws Exception
	{
		File databaseFile = new File(tempHome, "key-file-alone.mcrdb");
		if (java.security.Security
			.getProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		}
		io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter.writeInPemFormat(
			io.github.astrapi69.crypt.data.factory.KeyPairFactory.newKeyPair("RSA").getPrivate(),
			keyFile);
		Files.createFile(databaseFile.toPath());
		ApplicationXmlFileFactory.newApplicationFileWithPrivateKey(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(databaseFile))
				.selectedApplicationFilePath(databaseFile.getAbsolutePath())
				.keyFileInfo(FileInfo.toFileInfo(keyFile)).withMasterPw(false).withKeyFile(true)
				.minPasswordLength(6).build());
		return databaseFile;
	}

	/** A database of the master password whose xml holds no sign-in data */
	private File writtenWithoutItsSignInData() throws Exception
	{
		File written = new File(tempHome, "with-its-sign-in-data.mcrdb");
		createDatabaseFileHeadless(written, MASTER_PASSWORD);
		ApplicationModelBean model = VaultXmlCodec
			.toModel(PasswordVaultFormat.decrypt(written, MASTER_PASSWORD.toCharArray()));
		model.setMasterPwFileModelBean(null);
		File databaseFile = new File(tempHome, "without-its-sign-in-data.mcrdb");
		Files.write(databaseFile.toPath(),
			PasswordVaultFormat.encrypt(VaultXmlCodec.toXml(model), MASTER_PASSWORD.toCharArray()));
		return databaseFile;
	}
}
