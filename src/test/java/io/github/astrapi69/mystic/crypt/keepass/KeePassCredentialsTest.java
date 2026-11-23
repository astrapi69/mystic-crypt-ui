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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The credentials of a KeePass file, built in one place for the import, the export and their tests
 * (#1039): a password alone, a key file alone, or both. The other side of every case builds its
 * credentials with the library's own constructors, the way KeePass reads and writes the file: a
 * test that builds both sides with the helper passes on a helper that is wrong the same way on both
 * sides, which is what the import binding test's copy of this logic did before #1039
 */
class KeePassCredentialsTest
{

	@TempDir
	Path directory;

	/**
	 * Registers Bouncy Castle for this class instead of relying on another test having done it
	 */
	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	record Factors(String name, boolean password, boolean keyFile) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Factors> factors()
	{
		return Stream.of(new Factors("a password alone", true, false),
			new Factors("a key file alone", false, true),
			new Factors("a password and a key file", true, true));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("factors")
	@DisplayName("the import: a file KeePass wrote opens with the credentials the helper builds")
	void aFileKeePassWrote_opensWithTheHelpersCredentials(final Factors factors) throws Exception
	{
		char[] password = passwordOf(factors);
		File keyFile = factors.keyFile() ? aKeyFile() : null;

		byte[] kdbx = written(asKeePassBuildsThem(password, keyFile));

		assertEquals(1, read(kdbx, KeePassCredentials.of(password, keyFile)).getRootGroup()
			.getEntries().size());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("factors")
	@DisplayName("the export: a file written with the helper's credentials opens in KeePass")
	void aFileWrittenWithTheHelpersCredentials_opensInKeePass(final Factors factors)
		throws Exception
	{
		char[] password = passwordOf(factors);
		File keyFile = factors.keyFile() ? aKeyFile() : null;

		byte[] kdbx = written(KeePassCredentials.of(password, keyFile));

		assertEquals(1,
			read(kdbx, asKeePassBuildsThem(password, keyFile)).getRootGroup().getEntries().size());
	}

	private static char[] passwordOf(final Factors factors)
	{
		return factors.password() ? TestPasswords.throwaway().toCharArray() : new char[0];
	}

	/**
	 * The credentials as the library's constructors build them for the factors a file has: the
	 * other side of each case, which does not go through the helper
	 */
	private static KdbxCredentials asKeePassBuildsThem(final char[] password, final File keyFile)
		throws Exception
	{
		byte[] passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8);
		if (keyFile == null)
		{
			return new KdbxCredentials(passwordBytes);
		}
		try (InputStream key = new FileInputStream(keyFile))
		{
			return password.length == 0
				? new KdbxCredentials(key)
				: new KdbxCredentials(passwordBytes, key);
		}
	}

	@Test
	@DisplayName("a file written with a key file alone does not open with an empty password alone")
	void aKeyFileOnlyFile_doesNotOpenWithAnEmptyPassword() throws Exception
	{
		byte[] kdbx = written(asKeePassBuildsThem(new char[0], aKeyFile()));

		assertThrows(Exception.class, () -> read(kdbx, KeePassCredentials.of(new char[0], null)));
	}

	@Test
	@DisplayName("building the credentials leaves the caller's password as it was")
	void buildingTheCredentials_leavesTheCallersPassword() throws Exception
	{
		char[] password = TestPasswords.throwaway().toCharArray();
		char[] before = password.clone();

		KeePassCredentials.of(password, aKeyFile());

		assertArrayEquals(before, password);
	}

	private File aKeyFile() throws Exception
	{
		Path keyFile = Files.createTempFile(directory, "keepass", ".keyx");
		byte[] key = new byte[32];
		for (int index = 0; index < key.length; index++)
		{
			key[index] = (byte)(index * 7);
		}
		Files.write(keyFile, key);
		return keyFile.toFile();
	}

	private static byte[] written(final KdbxCredentials credentials) throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		database.getRootGroup().addEntry(database.newEntry("an entry"));
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		database.write(credentials, bytes);
		return bytes.toByteArray();
	}

	private static KdbxDatabase read(final byte[] kdbx, final KdbxCredentials credentials)
		throws Exception
	{
		return KdbxDatabase.read(credentials, new ByteArrayInputStream(kdbx));
	}
}
