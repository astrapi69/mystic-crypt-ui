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
package io.github.astrapi69.mystic.crypt.plugin.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of hashing a password and checking one against a stored hash, with every algorithm the tool
 * offers: what was hashed matches, everything else does not, and a stored hash says by itself what
 * made it.
 */
class PasswordHashSupportTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> algorithms()
	{
		return PasswordHashSupport.algorithms();
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void whatWasHashedMatchesAndNothingElseDoes(String algorithm) throws Exception
	{
		String stored = PasswordHashSupport.hash(algorithm, PASSWORD.toCharArray());

		assertTrue(PasswordHashSupport.verify(PASSWORD.toCharArray(), stored),
			algorithm + " must recognise the password it was given");
		assertFalse(PasswordHashSupport.verify("wrong password".toCharArray(), stored));
		assertFalse(PasswordHashSupport.verify((PASSWORD + " ").toCharArray(), stored),
			"a trailing space is a different password");
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void theSamePasswordHashedTwiceGivesTwoDifferentHashes(String algorithm) throws Exception
	{
		String first = PasswordHashSupport.hash(algorithm, PASSWORD.toCharArray());
		String second = PasswordHashSupport.hash(algorithm, PASSWORD.toCharArray());

		assertNotEquals(first, second,
			"a fresh salt every time is what keeps two accounts with the same password from looking "
				+ "the same");
		assertTrue(PasswordHashSupport.verify(PASSWORD.toCharArray(), first));
		assertTrue(PasswordHashSupport.verify(PASSWORD.toCharArray(), second));
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void aStoredHashSaysWhatMadeIt(String algorithm) throws Exception
	{
		String stored = PasswordHashSupport.hash(algorithm, PASSWORD.toCharArray());

		assertEquals(algorithm, PasswordHashSupport.algorithmOf(stored),
			"a hash has to be checkable without anything being remembered alongside it, was: "
				+ stored);
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void theHashCarriesNoPassword(String algorithm) throws Exception
	{
		String stored = PasswordHashSupport.hash(algorithm, PASSWORD.toCharArray());

		assertFalse(stored.contains(PASSWORD));
		assertFalse(stored.contains("horse"));
	}

	@Test
	void bcryptAndScryptSayWhatTheyCost() throws Exception
	{
		assertTrue(PasswordHashSupport
			.costOf(PasswordHashSupport.hash(PasswordHashSupport.BCRYPT, PASSWORD.toCharArray()))
			.contains("rounds"));
		assertTrue(PasswordHashSupport
			.costOf(PasswordHashSupport.hash(PasswordHashSupport.SCRYPT, PASSWORD.toCharArray()))
			.contains("N="));
	}

	/**
	 * bcrypt looks at the first 72 bytes of a password and no further. Accepting a longer one would
	 * mean only part of it is ever checked, which nothing in the window would say
	 */
	@Test
	void aPasswordTooLongForBcryptIsRefusedRatherThanTruncated()
	{
		String tooLong = "a".repeat(73);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> PasswordHashSupport.hash(PasswordHashSupport.BCRYPT, tooLong.toCharArray()));

		assertTrue(exception.getMessage().contains("72"), exception.getMessage());
		assertTrue(exception.getMessage().contains(PasswordHashSupport.ARGON2ID),
			"the message has to say what to use instead: " + exception.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Argon2id", "PBKDF2", "scrypt" })
	void theOtherAlgorithmsTakeALongPassword(String algorithm) throws Exception
	{
		String longPassword = "a".repeat(200);

		String stored = PasswordHashSupport.hash(algorithm, longPassword.toCharArray());

		assertTrue(PasswordHashSupport.verify(longPassword.toCharArray(), stored));
		assertFalse(PasswordHashSupport.verify("a".repeat(199).toCharArray(), stored),
			"every character has to count, otherwise a shorter password would open the same door");
	}

	@Test
	void anAlgorithmThisToolDoesNotHaveIsRefused()
	{
		assertThrows(IllegalArgumentException.class,
			() -> PasswordHashSupport.hash("MD5", PASSWORD.toCharArray()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " " })
	void nothingToHashIsRefused(String password)
	{
		assertThrows(IllegalArgumentException.class,
			() -> PasswordHashSupport.hash(PasswordHashSupport.ARGON2ID, "".toCharArray()));
	}

	@Test
	void nothingMatchesNothing()
	{
		assertFalse(PasswordHashSupport.verify(null, "whatever"));
		assertFalse(PasswordHashSupport.verify(PASSWORD.toCharArray(), null));
		assertFalse(PasswordHashSupport.verify(PASSWORD.toCharArray(), ""));
		assertFalse(PasswordHashSupport.verify("".toCharArray(), "whatever"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "not a hash", "$unknown$whatever", "scrypt$broken" })
	void aValueThatIsNoHashNeverMatches(String stored)
	{
		assertFalse(PasswordHashSupport.verify(PASSWORD.toCharArray(), stored));
	}

	@Test
	void aValueThatIsNoHashHasNoAlgorithmAndNoCost()
	{
		assertNull(PasswordHashSupport.algorithmOf("not a hash"));
		assertNull(PasswordHashSupport.algorithmOf(null));
		assertEquals("", PasswordHashSupport.costOf("not a hash"));
	}

	@Test
	void aScryptHashThatWasTamperedWithDoesNotMatch() throws Exception
	{
		String stored = PasswordHashSupport.hash(PasswordHashSupport.SCRYPT, PASSWORD.toCharArray());
		String[] parts = stored.split("\\$");
		// the same hash claimed to have been made with a cheaper cost
		String claimingAnotherCost = parts[0] + "$1024$" + parts[2] + "$" + parts[3] + "$" + parts[4];

		assertFalse(PasswordHashSupport.verify(PASSWORD.toCharArray(), claimingAnotherCost),
			"a hash checked with parameters other than the ones that made it cannot match");
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void everyAlgorithmIsDescribed(String algorithm)
	{
		assertFalse(PasswordHashSupport.describe(algorithm).isBlank());
	}

	@Test
	void somethingThatIsNoAlgorithmIsNotDescribed()
	{
		assertEquals("", PasswordHashSupport.describe("MD5"));
		assertEquals("", PasswordHashSupport.describe(null));
	}
}
