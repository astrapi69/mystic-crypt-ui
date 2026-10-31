/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The random values the application draws itself, until mystic-crypt 13.10 brings them
 * (astrapi69/mystic-crypt#252, #253), and the rule that nothing of the application draws through
 * randomizer, a library for test data (#658)
 */
class SecureRandomValuesTest
{

	@Test
	@DisplayName("no class of the application refers to randomizer")
	void noMainClass_refersToRandomizer() throws IOException, URISyntaxException
	{
		Path classes = Path.of(
			SecureRandomValues.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		List<Path> refersToIt;
		try (Stream<Path> files = Files.walk(classes))
		{
			refersToIt = files.filter(file -> file.toString().endsWith(".class"))
				.filter(SecureRandomValuesTest::refersToRandomizer).toList();
		}

		assertTrue(refersToIt.isEmpty(),
			"randomizer is for test data; these classes still use it: " + refersToIt);
	}

	@Test
	@DisplayName("characters come in the asked length, only from the alphabet, and reach all of it")
	void characters_haveTheLength_andComeFromTheAlphabet()
	{
		String alphabet = "abc#";

		char[] drawn = SecureRandomValues.characters(4000, alphabet);

		assertEquals(4000, drawn.length);
		String text = new String(drawn);
		assertTrue(text.chars().allMatch(c -> alphabet.indexOf(c) >= 0), "only the alphabet");
		assertTrue(alphabet.chars().allMatch(c -> text.indexOf(c) >= 0),
			"4000 draws reach every one of the 4 characters");
	}

	@Test
	@DisplayName("each of three characters comes up about a third of the time")
	void characters_areUniform()
	{
		int draws = 300_000;
		char[] drawn = SecureRandomValues.characters(draws, "xyz");
		int[] counts = new int[3];
		for (char each : drawn)
		{
			counts[each - 'x']++;
		}

		for (int count : counts)
		{
			// a third is 100000, and 1500 is about six standard deviations (258)
			assertTrue(Math.abs(count - draws / 3) < 1500, "count " + count + " of " + draws);
		}
	}

	@ParameterizedTest(name = "length {0} over \"{1}\" is refused")
	@CsvSource(value = { "-1, abc", "5, ''" }, emptyValue = "")
	@DisplayName("a negative length or an empty alphabet is refused")
	void characters_refuses(final int length, final String alphabet)
	{
		assertThrows(IllegalArgumentException.class,
			() -> SecureRandomValues.characters(length, alphabet));
	}

	@Test
	@DisplayName("length 0 gives no characters, an alphabet of one gives only that one")
	void characters_atTheBoundaries()
	{
		assertEquals(0, SecureRandomValues.characters(0, "ab").length);
		assertEquals("qqq", new String(SecureRandomValues.characters(3, "q")));
	}

	@Test
	@DisplayName("a serial number is positive, at most 20 octets, at least 64 bits, and never repeats")
	void serialNumber_followsRfc5280()
	{
		Set<BigInteger> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++)
		{
			BigInteger serial = SecureRandomValues.serialNumber();

			assertEquals(1, serial.signum(), "positive: " + serial);
			assertTrue(serial.toByteArray().length <= 20, "at most 20 octets: " + serial);
			// 159 random bits: below 64 only when the top 95 come out zero, probability 2^-95
			assertTrue(serial.bitLength() >= 64, "at least 64 bits: " + serial);
			seen.add(serial);
		}
		assertEquals(1000, seen.size(), "1000 draws, 1000 different serial numbers");
	}

	private static boolean refersToRandomizer(final Path classFile)
	{
		try
		{
			return new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1)
				.contains("io/github/astrapi69/random/");
		}
		catch (IOException unreadable)
		{
			throw new IllegalStateException("cannot read " + classFile, unreadable);
		}
	}
}
