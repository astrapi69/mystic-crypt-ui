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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of encrypting and decrypting with a passphrase: what goes in comes back out, a wrong
 * passphrase is refused rather than answered with rubbish, and a file that was changed afterwards
 * does not open at all.
 */
class FileCryptSupportTest
{

	private static final String PASSPHRASE = "file-crypt-test-passphrase-1969";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private File newFileOfSize(File directory, String name, int size) throws Exception
	{
		byte[] content = new byte[size];
		new SecureRandom().nextBytes(content);
		File file = new File(directory, name);
		Files.write(file.toPath(), content);
		return file;
	}

	/** Empty, one byte, exactly a block, over a block, and something large enough to matter */
	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 16, 17, 4096, 100_000 })
	void aFileComesBackByteForByte(int size, @TempDir File directory) throws Exception
	{
		File source = newFileOfSize(directory, "source.bin", size);
		byte[] original = Files.readAllBytes(source.toPath());

		File encrypted = FileCryptSupport.encryptFile(source, null, PASSPHRASE);
		Files.delete(source.toPath());
		File decrypted = FileCryptSupport.decryptFile(encrypted, null, PASSPHRASE);

		assertArrayEquals(original, Files.readAllBytes(decrypted.toPath()),
			"a file of " + size + " bytes must come back exactly as it was");
	}

	@Test
	void theEncryptedFileIsNamedAfterTheOriginalAndBackAgain(@TempDir File directory)
		throws Exception
	{
		File source = newFileOfSize(directory, "report.pdf", 32);

		File encrypted = FileCryptSupport.encryptFile(source, null, PASSPHRASE);
		assertEquals("report.pdf.mcenc", encrypted.getName());

		Files.delete(source.toPath());
		File decrypted = FileCryptSupport.decryptFile(encrypted, null, PASSPHRASE);
		assertEquals("report.pdf", decrypted.getName(),
			"decrypting must give the original name back");
	}

	@Test
	void aFileThatDoesNotEndInTheExtensionGetsAReadableName(@TempDir File directory)
		throws Exception
	{
		File source = newFileOfSize(directory, "plain.bin", 8);
		File encrypted = FileCryptSupport.encryptFile(source, new File(directory, "elsewhere.dat"),
			PASSPHRASE);

		File decrypted = FileCryptSupport.decryptFile(encrypted, null, PASSPHRASE);

		assertEquals("elsewhere.dat.decrypted", decrypted.getName());
	}

	@Test
	void theEncryptedFileCarriesTheMarkerAndNothingReadable(@TempDir File directory)
		throws Exception
	{
		File source = new File(directory, "secret.txt");
		Files.writeString(source.toPath(), "the launch code is 1234");

		File encrypted = FileCryptSupport.encryptFile(source, null, PASSPHRASE);

		assertTrue(FileCryptSupport.isEncrypted(encrypted));
		assertFalse(FileCryptSupport.isEncrypted(source), "the original is not encrypted");
		assertFalse(
			new String(Files.readAllBytes(encrypted.toPath()), StandardCharsets.ISO_8859_1)
				.contains("launch code"),
			"the content must not be findable in the encrypted file");
	}

	@Test
	void theSameFileEncryptedTwiceLooksDifferent(@TempDir File directory) throws Exception
	{
		File source = newFileOfSize(directory, "twice.bin", 64);

		byte[] first = Files
			.readAllBytes(FileCryptSupport.encryptFile(source, new File(directory, "a"), PASSPHRASE)
				.toPath());
		byte[] second = Files
			.readAllBytes(FileCryptSupport.encryptFile(source, new File(directory, "b"), PASSPHRASE)
				.toPath());

		assertFalse(Arrays.equals(first, second),
			"a fresh salt every time is what keeps two encryptions from being comparable");
	}

	@ParameterizedTest
	@ValueSource(strings = { "not the passphrase", "file-crypt-test-passphrase-196", " " })
	void aWrongPassphraseIsRefused(String wrong, @TempDir File directory) throws Exception
	{
		File source = newFileOfSize(directory, "wrong.bin", 40);
		File encrypted = FileCryptSupport.encryptFile(source, null, PASSPHRASE);

		assertThrows(Exception.class,
			() -> FileCryptSupport.decryptFile(encrypted, new File(directory, "out.bin"), wrong),
			"the cipher checks its tag, so a wrong passphrase must fail rather than produce rubbish");
	}

	@Test
	void aFileThatWasChangedAfterwardsDoesNotOpen(@TempDir File directory) throws Exception
	{
		File source = newFileOfSize(directory, "tamper.bin", 128);
		File encrypted = FileCryptSupport.encryptFile(source, null, PASSPHRASE);
		byte[] content = Files.readAllBytes(encrypted.toPath());
		content[content.length / 2] ^= 0x01;
		Files.write(encrypted.toPath(), content);

		assertThrows(Exception.class,
			() -> FileCryptSupport.decryptFile(encrypted, new File(directory, "out.bin"),
				PASSPHRASE));
	}

	@Test
	void somethingThisToolDidNotWriteIsSaidToBeSo(@TempDir File directory) throws Exception
	{
		File foreign = newFileOfSize(directory, "foreign.bin", 200);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> FileCryptSupport.decryptFile(foreign, new File(directory, "out.bin"), PASSPHRASE));

		assertTrue(exception.getMessage().contains("marker"), exception.getMessage());
	}

	@Test
	void anExistingFileIsNeverOverwritten(@TempDir File directory) throws Exception
	{
		File source = newFileOfSize(directory, "source.bin", 16);
		File occupied = new File(directory, "occupied.bin");
		Files.writeString(occupied.toPath(), "something valuable");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> FileCryptSupport.encryptFile(source, occupied, PASSPHRASE));

		assertTrue(exception.getMessage().contains("already exists"), exception.getMessage());
		assertEquals("something valuable", Files.readString(occupied.toPath()),
			"overwriting the wrong file with ciphertext is not something anybody recovers from");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "a", "a text with umlauts: aeoeue and an emoji", "   spaces   ",
			"line\nbreaks\r\nand\ttabs" })
	void aTextComesBackCharacterForCharacter(String text) throws Exception
	{
		String encrypted = FileCryptSupport.encryptText(text, PASSPHRASE);

		assertEquals(text, FileCryptSupport.decryptText(encrypted, PASSPHRASE));
	}

	@Test
	void anEncryptedTextIsBase64AndCanBePastedAnywhere() throws Exception
	{
		String encrypted = FileCryptSupport.encryptText("a note to myself", PASSPHRASE);

		assertTrue(encrypted.matches("[A-Za-z0-9+/=]+"), "was: " + encrypted);
		assertEquals("a note to myself",
			FileCryptSupport.decryptText("  " + encrypted + "  ", PASSPHRASE),
			"a value that was copied with stray spaces must still work");
	}

	@ParameterizedTest
	@CsvSource({ "not base64 at all", "'###'" })
	void aTextThisToolDidNotWriteIsRefused(String encrypted)
	{
		assertThrows(IllegalArgumentException.class,
			() -> FileCryptSupport.decryptText(encrypted, PASSPHRASE));
	}

	@Test
	void aWrongPassphraseDoesNotOpenAText() throws Exception
	{
		String encrypted = FileCryptSupport.encryptText("a note to myself", PASSPHRASE);

		assertThrows(Exception.class,
			() -> FileCryptSupport.decryptText(encrypted, "not the passphrase"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " " })
	void anEmptyPassphraseIsRefusedEverywhere(String passphrase, @TempDir File directory)
		throws Exception
	{
		File source = newFileOfSize(directory, "source.bin", 8);
		if (passphrase.isEmpty())
		{
			assertThrows(IllegalArgumentException.class,
				() -> FileCryptSupport.encryptFile(source, null, passphrase));
			assertThrows(IllegalArgumentException.class,
				() -> FileCryptSupport.encryptText("text", passphrase));
		}
		else
		{
			// a passphrase of spaces is poor but it is a passphrase; it must at least round trip
			assertEquals("text",
				FileCryptSupport.decryptText(FileCryptSupport.encryptText("text", passphrase),
					passphrase));
		}
	}

	@Test
	void aMissingFileIsSaidToBeMissing(@TempDir File directory)
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> FileCryptSupport.encryptFile(new File(directory, "not there"), null, PASSPHRASE));

		assertTrue(exception.getMessage().contains("not a file"), exception.getMessage());
	}

	@Test
	void aDirectoryIsNotAFileThatCanBeEncrypted(@TempDir File directory) throws Exception
	{
		assertThrows(IllegalArgumentException.class,
			() -> FileCryptSupport.encryptFile(directory, null, PASSPHRASE));
		assertFalse(FileCryptSupport.isEncrypted(directory),
			"a directory is not an encrypted file");
	}
}
