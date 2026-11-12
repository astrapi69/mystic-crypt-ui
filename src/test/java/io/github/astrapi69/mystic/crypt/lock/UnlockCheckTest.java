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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Unlocking asks for the factors the database was opened with (#880): the master password where
 * there is one, the key file where there is one - read again from the disk, its key recognised by
 * the verifier the lock left behind
 */
class UnlockCheckTest
{

	@TempDir
	Path directory;

	private char[] password;

	private PrivateKey key;

	private File keyFile;

	@BeforeAll
	static void registerBouncyCastle()
	{
		// the application registers it at start; the PEM reader needs it to read a key file
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@BeforeEach
	void aPasswordAndAKeyFile() throws Exception
	{
		password = TestPasswords.throwaway().toCharArray();
		key = KeyPairFactory.newKeyPair("RSA").getPrivate();
		keyFile = directory.resolve("database.pem").toFile();
		PrivateKeyWriter.writeInPemFormat(key, keyFile);
	}

	/** The credentials as locking leaves them: verifiers, no password, no key */
	private MasterPwFileModelBean locked(final boolean withPassword, final boolean withKeyFile)
		throws Exception
	{
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.withMasterPw(withPassword).withKeyFile(withKeyFile).build();
		if (withPassword)
		{
			credentials.setLockVerifier(MasterPasswordVerifier.of(password));
		}
		if (withKeyFile)
		{
			credentials.setKeyFileLockVerifier(KeyFileVerifier.of(key.getEncoded()));
		}
		return credentials;
	}

	private File anotherKeyFile() throws Exception
	{
		KeyPair other = KeyPairFactory.newKeyPair("RSA");
		File file = directory.resolve("another.pem").toFile();
		PrivateKeyWriter.writeInPemFormat(other.getPrivate(), file);
		return file;
	}

	@Test
	@DisplayName("a key-file-only database asks for the key file and not for a password, and unlocks with it")
	void aKeyFileOnlyDatabase_unlocksWithItsKeyFile() throws Exception
	{
		MasterPwFileModelBean credentials = locked(false, true);

		assertFalse(UnlockCheck.asksForThePassword(credentials));
		assertTrue(UnlockCheck.asksForTheKeyFile(credentials));
		UnlockCheck.Outcome outcome = UnlockCheck.check(credentials, null, keyFile);
		assertEquals(UnlockCheck.Answer.UNLOCKS, outcome.answer());
		assertArrayEquals(key.getEncoded(), outcome.privateKey().getEncoded(),
			"the key comes back from the disk, for the next save and the plugins");
	}

	@Test
	@DisplayName("a password plus key file database asks for both, and unlocks only with both")
	void aCombinedDatabase_needsBoth() throws Exception
	{
		MasterPwFileModelBean credentials = locked(true, true);

		assertTrue(UnlockCheck.asksForThePassword(credentials));
		assertTrue(UnlockCheck.asksForTheKeyFile(credentials));
		assertEquals(UnlockCheck.Answer.UNLOCKS,
			UnlockCheck.check(credentials, password, keyFile).answer());
		assertEquals(UnlockCheck.Answer.REFUSED, UnlockCheck
			.check(credentials, TestPasswords.throwaway().toCharArray(), keyFile).answer());
		assertEquals(UnlockCheck.Answer.REFUSED,
			UnlockCheck.check(credentials, password, anotherKeyFile()).answer());
	}

	@Test
	@DisplayName("a password plus key file database gives one and the same refusal for every wrong factor")
	void aCombinedDatabase_refusesEveryWrongFactorAlike() throws Exception
	{
		MasterPwFileModelBean credentials = locked(true, true);
		char[] wrongPassword = TestPasswords.throwaway().toCharArray();
		File anotherKeyFile = anotherKeyFile();
		Map<String, UnlockCheck.Answer> answers = new LinkedHashMap<>();

		answers.put("wrong password, its key file",
			UnlockCheck.check(credentials, wrongPassword, keyFile).answer());
		answers.put("its password, another key file",
			UnlockCheck.check(credentials, password, anotherKeyFile).answer());
		answers.put("wrong password, another key file",
			UnlockCheck.check(credentials, wrongPassword, anotherKeyFile).answer());

		assertEquals(1, new HashSet<>(answers.values()).size(),
			() -> "a refusal that differs by factor tells whoever guesses which factor was right: "
				+ answers);
		assertFalse(answers.containsValue(UnlockCheck.Answer.UNLOCKS), answers::toString);
	}

	@Test
	@DisplayName("a key file that is not there is named, the same whatever the password, and nothing is created at its path")
	void aMissingKeyFile_isNamedWhateverThePassword() throws Exception
	{
		MasterPwFileModelBean credentials = locked(true, true);
		File drive = directory.resolve("drive-not-connected").toFile();
		File missing = new File(drive, "database.pem");

		UnlockCheck.Outcome withItsPassword = UnlockCheck.check(credentials, password, missing);
		UnlockCheck.Outcome withAnotherOne = UnlockCheck.check(credentials,
			TestPasswords.throwaway().toCharArray(), missing);

		assertEquals(UnlockCheck.Answer.KEY_FILE_MISSING, withItsPassword.answer());
		assertEquals(withItsPassword, withAnotherOne,
			"the same answer and the same words whatever was typed: a missing file says nothing "
				+ "about the password");
		assertTrue(
			withItsPassword.reason()
				.startsWith("The key file " + missing.getAbsolutePath() + " is not there."),
			withItsPassword.reason());
		assertNull(withItsPassword.privateKey());
		assertFalse(drive.exists(),
			"nothing is created at the key file's path, as when opening and saving (#937)");
	}

	@Test
	@DisplayName("a file that is no key file is refused like a wrong one, and no key file chosen is named")
	void aFileThatIsNoKeyFile_isRefused() throws Exception
	{
		MasterPwFileModelBean credentials = locked(false, true);
		File notAKey = directory.resolve("notes.txt").toFile();
		Files.writeString(notAKey.toPath(), "not a key");

		assertEquals(UnlockCheck.Answer.REFUSED,
			UnlockCheck.check(credentials, null, notAKey).answer());
		UnlockCheck.Outcome noneChosen = UnlockCheck.check(credentials, null, null);
		assertEquals(UnlockCheck.Answer.KEY_FILE_MISSING, noneChosen.answer());
		assertEquals("No key file is set for this database.", noneChosen.reason());
	}

	@Test
	@DisplayName("a password-only database asks for the password alone, as before")
	void aPasswordOnlyDatabase_asksForThePasswordAlone() throws Exception
	{
		MasterPwFileModelBean credentials = locked(true, false);

		assertTrue(UnlockCheck.asksForThePassword(credentials));
		assertFalse(UnlockCheck.asksForTheKeyFile(credentials));
		UnlockCheck.Outcome outcome = UnlockCheck.check(credentials, password, null);
		assertEquals(UnlockCheck.Answer.UNLOCKS, outcome.answer());
		assertNull(outcome.privateKey());
	}

	@Test
	@DisplayName("a key file database whose lock left no verifier is refused rather than let in")
	void aKeyFileDatabaseWithoutAVerifier_failsClosed() throws Exception
	{
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder().withMasterPw(false)
			.withKeyFile(true).build();

		assertEquals(UnlockCheck.Answer.REFUSED,
			UnlockCheck.check(credentials, null, keyFile).answer());
	}
}
