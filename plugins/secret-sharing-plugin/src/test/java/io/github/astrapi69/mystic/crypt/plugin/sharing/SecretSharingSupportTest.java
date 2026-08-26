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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of splitting a secret and putting it back together: any threshold many shares are enough,
 * one fewer is not, and a share that was mistyped is refused rather than quietly rebuilding a wrong
 * secret.
 */
class SecretSharingSupportTest
{

	private static final String SECRET = "the master password of this database";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@ParameterizedTest
	@CsvSource({ "2,3", "3,5", "2,2", "5,7", "3,10" })
	void anyThresholdManySharesRebuildTheSecret(int threshold, int totalShares)
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, threshold, totalShares);

		assertEquals(totalShares, shares.size());
		for (int first = 0; first + threshold <= totalShares; first++)
		{
			List<String> some = shares.subList(first, first + threshold);
			assertEquals(SECRET, SecretSharingSupport.combineText(some),
				"shares " + first + " to " + (first + threshold - 1) + " have to be enough");
		}
	}

	@ParameterizedTest
	@CsvSource({ "2,3", "3,5", "4,6" })
	void oneShareFewerThanNeededIsRefused(int threshold, int totalShares)
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, threshold, totalShares);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(shares.subList(0, threshold - 1)));

		assertTrue(exception.getMessage().contains(String.valueOf(threshold)),
			"the message has to say how many are needed: " + exception.getMessage());
	}

	@Test
	void theOrderOfTheSharesDoesNotMatter()
	{
		List<String> shares = new ArrayList<>(SecretSharingSupport.splitText(SECRET, 3, 5));
		Collections.shuffle(shares, new SecureRandom());

		assertEquals(SECRET, SecretSharingSupport.combineText(shares.subList(0, 3)));
	}

	@Test
	void moreSharesThanNeededAreFine()
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, 3, 5);

		assertEquals(SECRET, SecretSharingSupport.combineText(shares));
	}

	@Test
	void arbitraryBytesSurviveTheRoundTrip()
	{
		byte[] secret = new byte[64];
		new SecureRandom().nextBytes(secret);

		List<String> shares = SecretSharingSupport.split(secret, 2, 3);

		assertArrayEquals(secret, SecretSharingSupport.combine(shares.subList(0, 2)),
			"a key file is bytes, not text, and has to come back byte for byte");
	}

	@Test
	void aShareCarriesNoPartOfTheSecretInPlainSight()
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, 3, 5);

		for (String share : shares)
		{
			assertFalse(share.contains("master"), share);
			assertFalse(share.contains("password"), share);
		}
	}

	@Test
	void aShareSaysWhatItIsAndWhatItNeeds()
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, 3, 5);

		String first = shares.get(0);
		assertTrue(first.startsWith(SecretSharingSupport.PREFIX + "$3$5$"), first);
		assertEquals(3, SecretSharingSupport.thresholdOf(first));
		assertTrue(SecretSharingSupport.isShare(first));
		assertTrue(SecretSharingSupport.isShare("  " + first + "  "),
			"a share pasted with stray spaces is still a share");
	}

	/** A share copied off paper with one character wrong must not rebuild a wrong secret silently */
	@Test
	void aMistypedShareIsRefused()
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, 2, 3);
		String damaged = flipOneCharacterOfTheValue(shares.get(0));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(List.of(damaged, shares.get(1))));

		assertTrue(exception.getMessage().contains("damaged"), exception.getMessage());
		assertFalse(SecretSharingSupport.isShare(damaged));
	}

	@Test
	void aShareWithAChangedThresholdIsRefused()
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, 3, 5);
		String[] parts = shares.get(0).split("\\$");
		String claimingLess = String.join("$", parts[0], "2", parts[2], parts[3], parts[4], parts[5]);

		assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(List.of(claimingLess, shares.get(1))),
			"the check value covers the threshold, so it cannot be talked down");
	}

	@Test
	void sharesOfTwoDifferentSecretsDoNotMix()
	{
		List<String> first = SecretSharingSupport.splitText("one secret", 2, 3);
		List<String> second = SecretSharingSupport.splitText("another secret entirely", 3, 5);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(List.of(first.get(0), second.get(0))));

		assertTrue(exception.getMessage().contains("not from the same secret"),
			exception.getMessage());
	}

	@Test
	void theSameShareTwiceIsNotTwoShares()
	{
		List<String> shares = SecretSharingSupport.splitText(SECRET, 2, 3);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(List.of(shares.get(0), shares.get(0))));

		assertTrue(exception.getMessage().contains("twice"), exception.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "not a share", "MCSS1$3$5$1$abc", "MCSS9$3$5$1$abc$deadbeef" })
	void somethingThatIsNoShareIsSaidToBeNone(String line)
	{
		assertFalse(SecretSharingSupport.isShare(line));
		assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(List.of(line)));
	}

	@Test
	void nothingToWorkWithIsSaidToBeNothing()
	{
		assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(List.of()));
		assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.combineText(null));
		assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.splitText("", 2, 3));
		assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.splitText(null, 2, 3));
	}

	@ParameterizedTest
	@CsvSource({ "1,3", "0,3", "-1,3" })
	void aThresholdOfOneWouldMakeEveryShareTheSecret(int threshold, int totalShares)
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.splitText(SECRET, threshold, totalShares));

		assertTrue(exception.getMessage().contains("two"), exception.getMessage());
	}

	@Test
	void moreNeededThanThereAreSharesIsRefused()
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.splitText(SECRET, 5, 3));

		assertTrue(exception.getMessage().contains("at least as many"), exception.getMessage());
	}

	/** The splitter cannot make more shares than the secret has bytes, which is worth saying plainly */
	@Test
	void aSecretTooShortForThatManySharesSaysSo()
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> SecretSharingSupport.splitText("short", 2, 10));

		assertTrue(exception.getMessage().contains("5 bytes"), exception.getMessage());
		assertTrue(exception.getMessage().contains("at most 5"), exception.getMessage());
		assertEquals(5, SecretSharingSupport.maxSharesFor(5));
		assertEquals(SecretSharingSupport.MAX_SHARES,
			SecretSharingSupport.maxSharesFor(100_000));
	}

	private String flipOneCharacterOfTheValue(String share)
	{
		String[] parts = share.split("\\$");
		char[] value = parts[4].toCharArray();
		value[0] = value[0] == 'A' ? 'B' : 'A';
		return String.join("$", parts[0], parts[1], parts[2], parts[3], new String(value), parts[5]);
	}
}
