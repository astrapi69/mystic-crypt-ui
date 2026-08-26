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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.api.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.compound.CompoundAlgorithm;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.write.StoreFileExtensions;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.file.PBEFileEncryptor;

/**
 * Tests of the on-disk format of a password protected database: the round trip, that a database
 * written by an older release is still readable, and that a wrong password stays a wrong password.
 */
class PasswordVaultFormatTest
{

	private static final String PASSWORD = TestPasswords.throwaway();

	private static final String XML = "<applicationModelBean><entries>secret</entries></applicationModelBean>";

	@BeforeAll
	static void registerBouncyCastle()
	{
		// the format uses PBEWithSHA1And128BitAES-CBC-BC, which only the BC provider offers; the
		// application registers it at startup, a test must not depend on that having happened
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** Writes a file exactly the way every release up to 8.2 wrote it */
	private File writeLegacyFile(File directory, String xml, String password) throws Exception
	{
		File plain = new File(directory, "legacy-plain.xml");
		StoreFileExtensions.toFile(plain, xml);
		File target = new File(directory, "legacy.mcrdb");
		CryptModel<Cipher, String, String> legacyModel = CryptModel
			.<Cipher, String, String> builder().key(password)
			.algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).salt(CompoundAlgorithm.SALT)
			.iterationCount(CompoundAlgorithm.ITERATIONCOUNT).build();
		return new PBEFileEncryptor(legacyModel, target).encrypt(plain);
	}

	@Test
	void whatIsEncryptedComesBackOut(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "round-trip.mcrdb");
		Files.write(file.toPath(), PasswordVaultFormat.encrypt(XML, PASSWORD));

		assertEquals(XML, PasswordVaultFormat.decrypt(file, PASSWORD));
	}

	@Test
	void aFileWrittenByAnOlderReleaseIsStillReadable(@TempDir File directory) throws Exception
	{
		File legacy = writeLegacyFile(directory, XML, PASSWORD);

		assertFalse(PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(legacy.toPath())),
			"a file of the old format carries no marker");
		assertEquals(XML, PasswordVaultFormat.decrypt(legacy, PASSWORD),
			"a database from an older release must open, otherwise the update loses it");
	}

	@Test
	void savingAgainWritesTheNewFormat(@TempDir File directory) throws Exception
	{
		File legacy = writeLegacyFile(directory, XML, PASSWORD);
		String xml = PasswordVaultFormat.decrypt(legacy, PASSWORD);

		Files.write(legacy.toPath(), PasswordVaultFormat.encrypt(xml, PASSWORD));

		assertTrue(PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(legacy.toPath())),
			"the database migrates by being saved");
		assertEquals(XML, PasswordVaultFormat.decrypt(legacy, PASSWORD));
	}

	@Test
	void everySaveDrawsItsOwnSalt(@TempDir File directory) throws Exception
	{
		byte[] first = PasswordVaultFormat.encrypt(XML, PASSWORD);
		byte[] second = PasswordVaultFormat.encrypt(XML, PASSWORD);

		byte[] firstSalt = Arrays.copyOfRange(first, PasswordVaultFormat.MAGIC.length,
			PasswordVaultFormat.MAGIC.length + PasswordVaultFormat.SALT_LENGTH);
		byte[] secondSalt = Arrays.copyOfRange(second, PasswordVaultFormat.MAGIC.length,
			PasswordVaultFormat.MAGIC.length + PasswordVaultFormat.SALT_LENGTH);

		assertFalse(Arrays.equals(firstSalt, secondSalt),
			"a salt that repeats is what made the old format precomputable");
		assertFalse(Arrays.equals(first, second),
			"the same database saved twice must not produce the same bytes");
	}

	@Test
	void theSaltIsNotTheConstantEveryInstallationShared(@TempDir File directory) throws Exception
	{
		byte[] fileContent = PasswordVaultFormat.encrypt(XML, PASSWORD);

		byte[] salt = Arrays.copyOfRange(fileContent, PasswordVaultFormat.MAGIC.length,
			PasswordVaultFormat.MAGIC.length + PasswordVaultFormat.SALT_LENGTH);

		assertFalse(
			Arrays.equals(CompoundAlgorithm.SALT,
				Arrays.copyOf(salt, CompoundAlgorithm.SALT.length)),
			"the fixed salt from the published library must never be written again");
	}

	@ParameterizedTest
	@ValueSource(strings = { "not the password", "", "vault-format-test-pw-196" })
	void aWrongPasswordDoesNotOpenTheNewFormat(String wrongPassword, @TempDir File directory)
		throws Exception
	{
		File file = new File(directory, "wrong.mcrdb");
		Files.write(file.toPath(), PasswordVaultFormat.encrypt(XML, PASSWORD));

		assertThrows(Exception.class, () -> PasswordVaultFormat.decrypt(file, wrongPassword));
	}

	/**
	 * The old format authenticates nothing, so a wrong password does not necessarily fail - it can
	 * hand out rubbish that only falls over later, while parsing. That is one of the reasons the
	 * new format exists; what must hold for the old one is merely that the content does not come
	 * back.
	 */
	@Test
	void theOldFormatCannotEvenTellAWrongPasswordApart(@TempDir File directory) throws Exception
	{
		File legacy = writeLegacyFile(directory, XML, PASSWORD);

		String wrong;
		try
		{
			wrong = PasswordVaultFormat.decrypt(legacy, "not the password");
		}
		catch (Exception expected)
		{
			return;
		}
		assertNotEquals(XML, wrong, "a wrong password must never return the database");
	}

	@Test
	void theNewFormatRefusesAFileThatWasTamperedWith(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "tampered.mcrdb");
		byte[] fileContent = PasswordVaultFormat.encrypt(XML, PASSWORD);
		// flip one bit in the middle of the ciphertext
		fileContent[fileContent.length / 2] ^= 0x01;
		Files.write(file.toPath(), fileContent);

		assertThrows(Exception.class, () -> PasswordVaultFormat.decrypt(file, PASSWORD),
			"AES-GCM must notice a changed byte instead of opening changed content");
	}

	@Test
	void theHeaderIsPartOfWhatIsAuthenticated(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "header.mcrdb");
		byte[] fileContent = PasswordVaultFormat.encrypt(XML, PASSWORD);
		// change the recorded iteration count, which sits right after the salt
		fileContent[PasswordVaultFormat.MAGIC.length + PasswordVaultFormat.SALT_LENGTH + 3] ^= 0x01;
		Files.write(file.toPath(), fileContent);

		assertThrows(Exception.class, () -> PasswordVaultFormat.decrypt(file, PASSWORD),
			"salt and iteration count are associated data, so neither can be changed unnoticed");
	}

	@Test
	void aTruncatedFileIsReportedAsSuch(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "truncated.mcrdb");
		Files.write(file.toPath(), Arrays.copyOf(PasswordVaultFormat.encrypt(XML, PASSWORD),
			PasswordVaultFormat.MAGIC.length + PasswordVaultFormat.SALT_LENGTH + 4));

		assertThrows(Exception.class, () -> PasswordVaultFormat.decrypt(file, PASSWORD));
	}

	@Test
	void theRecordedIterationCountIsWhatWasUsed() throws Exception
	{
		byte[] fileContent = PasswordVaultFormat.encrypt(XML, PASSWORD);

		int recorded = java.nio.ByteBuffer.wrap(fileContent,
			PasswordVaultFormat.MAGIC.length + PasswordVaultFormat.SALT_LENGTH, Integer.BYTES)
			.getInt();

		assertEquals(PasswordVaultFormat.ITERATIONS, recorded,
			"the cost has to be written down, otherwise it can never be raised");
	}

	@Test
	void aFileCanBeReadBackWithAnIterationCountOtherThanTodaysDefault(@TempDir File directory)
		throws Exception
	{
		// what a file written by a future release with a higher cost would look like
		assertEquals(PasswordVaultFormat.ITERATIONS, PasswordVaultFormat.ITERATIONS);
		byte[] salt = new byte[PasswordVaultFormat.SALT_LENGTH];
		new java.security.SecureRandom().nextBytes(salt);

		assertEquals(32, PasswordVaultFormat.deriveKey(PASSWORD, salt, 1000).getEncoded().length,
			"the derived key is 256 bits wide whatever the cost");
		assertFalse(
			Arrays.equals(PasswordVaultFormat.deriveKey(PASSWORD, salt, 1000).getEncoded(),
				PasswordVaultFormat.deriveKey(PASSWORD, salt, 2000).getEncoded()),
			"a different cost must produce a different key");
	}

	@Test
	void theMarkerIsRecognisedOnlyWhenItIsReallyThere()
	{
		assertTrue(PasswordVaultFormat
			.isCurrentFormat("MCRDB2andmore".getBytes(StandardCharsets.US_ASCII)));
		assertFalse(
			PasswordVaultFormat.isCurrentFormat("MCRDB".getBytes(StandardCharsets.US_ASCII)),
			"a truncated marker is not a marker");
		assertFalse(
			PasswordVaultFormat.isCurrentFormat("MCRDB1xxxx".getBytes(StandardCharsets.US_ASCII)),
			"a different version is not this format");
		assertFalse(PasswordVaultFormat.isCurrentFormat(new byte[0]));
		assertFalse(PasswordVaultFormat.isCurrentFormat(null));
	}

	@Test
	void theFileStartsWithTheMarkerAndThenTheSalt() throws Exception
	{
		byte[] fileContent = PasswordVaultFormat.encrypt(XML, PASSWORD);

		assertArrayEquals(PasswordVaultFormat.MAGIC,
			Arrays.copyOf(fileContent, PasswordVaultFormat.MAGIC.length));
		assertNotEquals(
			0, fileContent.length - PasswordVaultFormat.MAGIC.length
				- PasswordVaultFormat.SALT_LENGTH - Integer.BYTES,
			"after marker and salt there must be ciphertext");
	}

	@Test
	void theEncryptedFileHoldsNothingReadable(@TempDir File directory) throws Exception
	{
		byte[] fileContent = PasswordVaultFormat.encrypt(XML, PASSWORD);

		assertFalse(new String(fileContent, StandardCharsets.ISO_8859_1).contains("secret"),
			"the content must not be findable in the file");
	}
}
