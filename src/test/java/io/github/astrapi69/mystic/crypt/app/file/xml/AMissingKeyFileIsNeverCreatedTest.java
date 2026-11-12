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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Each of the four reads of a key file path used to create the directory and an empty file there,
 * so the real key file could not be put back without replacing it. Now none of them creates
 * anything, and each says which key file is missing.
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

	@ParameterizedTest(name = "{0} creates nothing at a missing key file's path and names it")
	@MethodSource("everyReadOfAKeyFilePath")
	void aMissingKeyFile_isNeverCreated(final String caseName, final boolean withPassword,
		final KeyFileRead read)
	{
		File drive = new File(directory, "drive-not-connected");
		File keyFile = new File(drive, "database.pem");
		MasterPwFileModelBean credentials = credentials(withPassword, keyFile);

		RuntimeException refused = assertThrows(RuntimeException.class, () -> read.run(credentials),
			"there is no key to open or save with");

		assertFalse(keyFile.exists(),
			"no file at the key file's path, which would block " + "putting the real one back");
		assertFalse(drive.exists(), "and not its directory either");
		assertTrue(
			messagesOf(refused)
				.contains("The key file " + keyFile.getAbsolutePath() + " is not there."),
			() -> "the refusal names the missing key file: " + messagesOf(refused));
	}

	@Test
	@DisplayName("a database whose key file path is not set is refused with a reason")
	void aDatabaseWithoutAKeyFilePath_isRefusedWithAReason()
	{
		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> KeyFileOnDisk.existing(null));

		assertEquals("No key file is set for this database.", refused.getMessage());
	}

	@Test
	@DisplayName("a key file that is there is found, and left as it is")
	void aKeyFileThatIsThere_isFound() throws Exception
	{
		File keyFile = new File(directory, "database.pem");
		java.nio.file.Files.writeString(keyFile.toPath(), "the key file's content");

		assertEquals(keyFile.getAbsoluteFile(),
			KeyFileOnDisk.existing(FileInfo.toFileInfo(keyFile)).getAbsoluteFile());
		assertEquals("the key file's content", java.nio.file.Files.readString(keyFile.toPath()));
	}

	@Test
	@DisplayName("the missing key file reads the same way in English and in German, with the path in both")
	void theMissingKeyFile_readsInBothLanguages()
	{
		ResourceBundle english = ResourceBundle.getBundle("ui.messages", Locale.ROOT);
		ResourceBundle german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN);

		assertEquals(
			"The key file %1$s is not there. It was moved or deleted, or it is on a drive "
				+ "that is not connected. Put it back at this path and try again.",
			english.getString("dialog.key.file.missing.message"));
		String missing = german.getString("dialog.key.file.missing.message");
		assertTrue(missing.contains("%1$s") && missing.contains("Schlüsseldatei")
			&& missing.contains("gelöscht"), "the path and real umlauts: " + missing);
		String notSet = german.getString("dialog.key.file.not.set.message");
		assertTrue(notSet.contains("Schlüsseldatei"), notSet);
		for (String transliteration : new String[] { "Schluessel", "geloescht" })
		{
			assertFalse(missing.contains(transliteration) || notSet.contains(transliteration),
				missing + " / " + notSet);
		}
	}

	/** Every message along the cause chain, as the dialogs that show a failure read it */
	private static String messagesOf(final Throwable failure)
	{
		StringBuilder messages = new StringBuilder();
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			messages.append(cause.getMessage()).append('\n');
		}
		return messages.toString();
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
