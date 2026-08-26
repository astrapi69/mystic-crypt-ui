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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of checksums and message authentication codes: the published values for the known inputs,
 * that a file and its text give the same answer, and that a code without a key is refused.
 */
class ChecksumSupportTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		// SHA-3 and BLAKE2 come from Bouncy Castle; the application registers it at startup
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> digests()
	{
		return ChecksumSupport.DIGESTS;
	}

	static java.util.List<String> macs()
	{
		return ChecksumSupport.MACS;
	}

	/** The values every implementation of these digests produces for the empty input */
	@ParameterizedTest
	@CsvSource({ "MD5,d41d8cd98f00b204e9800998ecf8427e",
			"SHA-1,da39a3ee5e6b4b0d3255bfef95601890afd80709",
			"SHA-256,e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
			"SHA3-256,a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a" })
	void producesThePublishedValueForTheEmptyInput(String digest, String expected) throws Exception
	{
		assertEquals(expected, ChecksumSupport.checksumOfText("", digest));
	}

	@Test
	void producesThePublishedValueForTheKnownInput() throws Exception
	{
		assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
			ChecksumSupport.checksumOfText("hello", "SHA-256"));
	}

	/** Every digest the tool offers has to exist on this machine and answer the same way twice */
	@ParameterizedTest
	@MethodSource("digests")
	void everyOfferedDigestWorks(String digest) throws Exception
	{
		String first = ChecksumSupport.checksumOfText("some text", digest);
		String second = ChecksumSupport.checksumOfText("some text", digest);

		assertEquals(first, second, "the same input must give the same checksum");
		assertTrue(first.matches("[0-9a-f]+"), "the checksum has to be hex, was: " + first);
		assertNotEquals(first, ChecksumSupport.checksumOfText("some other text", digest));
	}

	@ParameterizedTest
	@MethodSource("digests")
	void aFileAndItsTextGiveTheSameAnswer(String digest, @TempDir File directory) throws Exception
	{
		File file = new File(directory, "content.txt");
		Files.writeString(file.toPath(), "the same content", StandardCharsets.UTF_8);

		assertEquals(ChecksumSupport.checksumOfText("the same content", digest),
			ChecksumSupport.checksumOfFile(file, digest));
	}

	@Test
	void aLargeFileIsReadInBlocks(@TempDir File directory) throws Exception
	{
		byte[] content = new byte[300_000];
		new java.util.Random(42).nextBytes(content);
		File file = new File(directory, "large.bin");
		Files.write(file.toPath(), content);

		assertEquals(64, ChecksumSupport.checksumOfFile(file, "SHA-256").length(),
			"a file larger than the block size has to work just the same");
	}

	@ParameterizedTest
	@ValueSource(strings = { "SHA-999", "not a digest" })
	void aDigestThisMachineDoesNotKnowIsRefused(String digest)
	{
		assertThrows(Exception.class, () -> ChecksumSupport.checksumOfText("text", digest));
	}

	@Test
	void aMissingFileIsSaidToBeMissing(@TempDir File directory)
	{
		assertThrows(IllegalArgumentException.class,
			() -> ChecksumSupport.checksumOfFile(new File(directory, "not there"), "SHA-256"));
	}

	/** The published value for HMAC-SHA-256 over the pangram with the key "key" */
	@Test
	void producesThePublishedMacValue() throws Exception
	{
		assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
			ChecksumSupport.macOfText("The quick brown fox jumps over the lazy dog", "key",
				"HmacSHA256"));
	}

	@ParameterizedTest
	@MethodSource("macs")
	void everyOfferedCodeWorksAndDependsOnItsKey(String algorithm) throws Exception
	{
		String withOneKey = ChecksumSupport.macOfText("the message", "the key", algorithm);
		String withAnother = ChecksumSupport.macOfText("the message", "another key", algorithm);

		assertTrue(withOneKey.matches("[0-9a-f]+"), withOneKey);
		assertNotEquals(withOneKey, withAnother,
			"a code that does not change with the key is not a code, it is a checksum");
		assertEquals(withOneKey, ChecksumSupport.macOfText("the message", "the key", algorithm));
	}

	@Test
	void aCodeWithoutAKeyIsRefused()
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> ChecksumSupport.macOfText("the message", "", "HmacSHA256"));

		assertTrue(exception.getMessage().contains("key"), exception.getMessage());
	}

	@Test
	void aFileAndItsTextGiveTheSameCode(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "content.txt");
		Files.writeString(file.toPath(), "signed content", StandardCharsets.UTF_8);

		assertEquals(ChecksumSupport.macOfText("signed content", "the key", "HmacSHA256"),
			ChecksumSupport.macOfFile(file, "the key", "HmacSHA256"));
	}

	@ParameterizedTest
	@CsvSource({ "abcdef,abcdef,true", "ABCDEF,abcdef,true", " abcdef ,abcdef,true",
			"ab:cd:ef,abcdef,true", "abcdef,abcdee,false", "abcdef,abcde,false" })
	void comparesAValueWhateverWayItWasPastedIn(String expected, String actual, boolean same)
	{
		assertEquals(same, ChecksumSupport.matches(expected, actual));
	}

	@Test
	void nothingComparesEqualToNothing()
	{
		assertFalse(ChecksumSupport.matches(null, "abcdef"));
		assertFalse(ChecksumSupport.matches("abcdef", null));
	}

	@ParameterizedTest
	@CsvSource({ "d41d8cd98f00b204e9800998ecf8427e,MD5",
			"da39a3ee5e6b4b0d3255bfef95601890afd80709,SHA-1" })
	void recognisesADigestByTheLengthOfItsValue(String checksum, String expected)
	{
		assertEquals(expected, ChecksumSupport.digestByLength(checksum));
	}

	@Test
	void aLengthThatFitsSeveralDigestsIsNotGuessed()
	{
		assertNull(ChecksumSupport.digestByLength("a".repeat(64)),
			"64 hex digits fit SHA-256, SHA3-256 and BLAKE2B-256 alike - guessing would be wrong");
		assertNull(ChecksumSupport.digestByLength("a".repeat(128)));
		assertNull(ChecksumSupport.digestByLength("nonsense"));
	}
}
