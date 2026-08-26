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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the exchange the way two people would run it: each side is given only its own half, and
 * what travels between them is the text that {@link KeyExchangeSupport} produces.
 */
class KeyExchangeSupportTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		// self-contained on purpose: the application registers the provider at startup, but a test
		// must never depend on another class having run first
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> algorithms()
	{
		return KeyExchangeSupport.algorithms();
	}

	/**
	 * The whole point: two sides that never share a private key arrive at the same secret
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("both sides arrive at the same secret while each holds only its own half")
	void bothSidesArriveAtTheSameSecret(String algorithm) throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport.newParty(algorithm);

		// everything the sender ever sees of the recipient is this one line of text
		String published = KeyExchangeSupport.publicKeyOf(recipient);
		KeyExchangeSupport.Handshake sender = KeyExchangeSupport.encapsulate(published);

		SecretKey recipientSecret = KeyExchangeSupport.decapsulate(recipient, sender.handshake());

		assertArrayEquals(sender.sharedSecret().getEncoded(), recipientSecret.getEncoded(),
			"the two sides did not arrive at the same secret");
		assertEquals(KeyExchangeSupport.SECRET_LENGTH, recipientSecret.getEncoded().length);
		assertEquals(KeyExchangeSupport.fingerprintOf(sender.sharedSecret()),
			KeyExchangeSupport.fingerprintOf(recipientSecret));
	}

	/**
	 * The secret is worth having only if it can carry a message
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("a message encrypted by the sender is readable by the recipient")
	void aMessageTravelsOnTheSharedSecret(String algorithm) throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport.newParty(algorithm);
		KeyExchangeSupport.Handshake sender = KeyExchangeSupport
			.encapsulate(KeyExchangeSupport.publicKeyOf(recipient));
		String message = "the meeting is at eight, bring the umlauts: \u00e4\u00f6\u00fc\u00df";

		String encrypted = KeyExchangeSupport.encryptMessage(sender.sharedSecret(),
			message.getBytes(StandardCharsets.UTF_8));
		byte[] read = KeyExchangeSupport.decryptMessage(
			KeyExchangeSupport.decapsulate(recipient, sender.handshake()), encrypted);

		assertEquals(message, new String(read, StandardCharsets.UTF_8));
		assertFalse(encrypted.contains("meeting"), "the message travelled in the clear");
	}

	/**
	 * Someone who is not the recipient must not end up with the recipient's secret. ML-KEM does not
	 * fail here - a wrong private key yields a different secret rather than an error - so what is
	 * asserted is what matters: the message stays unreadable.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("a different pair of keys does not open the message")
	void theWrongRecipientCannotRead(String algorithm) throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport.newParty(algorithm);
		KeyExchangeSupport.Party eavesdropper = KeyExchangeSupport.newParty(algorithm);
		KeyExchangeSupport.Handshake sender = KeyExchangeSupport
			.encapsulate(KeyExchangeSupport.publicKeyOf(recipient));
		String encrypted = KeyExchangeSupport.encryptMessage(sender.sharedSecret(),
			"for the recipient only".getBytes(StandardCharsets.UTF_8));

		SecretKey wrong = KeyExchangeSupport.decapsulate(eavesdropper, sender.handshake());

		assertNotEquals(KeyExchangeSupport.fingerprintOf(sender.sharedSecret()),
			KeyExchangeSupport.fingerprintOf(wrong));
		assertThrows(Exception.class, () -> KeyExchangeSupport.decryptMessage(wrong, encrypted),
			"the wrong secret opened the message");
	}

	/**
	 * A message that was altered on the way must fail rather than decrypt into rubbish
	 */
	@Test
	@DisplayName("an altered message is refused rather than decrypted into rubbish")
	void anAlteredMessageIsRefused() throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport
			.newParty(KeyExchangeSupport.ML_KEM_768);
		KeyExchangeSupport.Handshake sender = KeyExchangeSupport
			.encapsulate(KeyExchangeSupport.publicKeyOf(recipient));
		String encrypted = KeyExchangeSupport.encryptMessage(sender.sharedSecret(),
			"unaltered".getBytes(StandardCharsets.UTF_8));

		byte[] raw = java.util.Base64.getDecoder().decode(encrypted);
		raw[raw.length - 1] ^= 0x01;
		String altered = java.util.Base64.getEncoder().encodeToString(raw);

		assertThrows(Exception.class,
			() -> KeyExchangeSupport.decryptMessage(sender.sharedSecret(), altered));
	}

	/**
	 * Every exchange produces its own secret, so two runs of the same pair of keys never repeat
	 */
	@Test
	@DisplayName("two exchanges against the same public key produce different secrets")
	void everyExchangeHasItsOwnSecret() throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport
			.newParty(KeyExchangeSupport.ML_KEM_768);
		String published = KeyExchangeSupport.publicKeyOf(recipient);

		KeyExchangeSupport.Handshake first = KeyExchangeSupport.encapsulate(published);
		KeyExchangeSupport.Handshake second = KeyExchangeSupport.encapsulate(published);

		assertNotEquals(KeyExchangeSupport.fingerprintOf(first.sharedSecret()),
			KeyExchangeSupport.fingerprintOf(second.sharedSecret()));
		assertArrayEquals(first.sharedSecret().getEncoded(),
			KeyExchangeSupport.decapsulate(recipient, first.handshake()).getEncoded());
		assertArrayEquals(second.sharedSecret().getEncoded(),
			KeyExchangeSupport.decapsulate(recipient, second.handshake()).getEncoded());
	}

	/**
	 * A public key of one algorithm and a party of another must not be quietly mixed
	 */
	@Test
	@DisplayName("a handshake from another algorithm is refused by name")
	void aHandshakeOfAnotherAlgorithmIsRefused() throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport.newParty(KeyExchangeSupport.X25519);
		KeyExchangeSupport.Party other = KeyExchangeSupport.newParty(KeyExchangeSupport.ML_KEM_512);
		KeyExchangeSupport.Handshake foreign = KeyExchangeSupport
			.encapsulate(KeyExchangeSupport.publicKeyOf(other));

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> KeyExchangeSupport.decapsulate(recipient, foreign.handshake()));

		assertTrue(thrown.getMessage().contains(KeyExchangeSupport.ML_KEM_512)
			&& thrown.getMessage().contains(KeyExchangeSupport.X25519), thrown.getMessage());
	}

	/**
	 * The two kinds of text look alike, so mixing them up has to be said out loud
	 */
	@Test
	@DisplayName("a public key handed in where a handshake belongs is named as such")
	void theTwoKindsOfTextAreToldApart() throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport
			.newParty(KeyExchangeSupport.ML_KEM_768);
		String publicKey = KeyExchangeSupport.publicKeyOf(recipient);
		KeyExchangeSupport.Handshake handshake = KeyExchangeSupport.encapsulate(publicKey);

		assertTrue(assertThrows(IllegalArgumentException.class,
			() -> KeyExchangeSupport.decapsulate(recipient, publicKey)).getMessage()
				.contains("a public key"));
		assertTrue(assertThrows(IllegalArgumentException.class,
			() -> KeyExchangeSupport.encapsulate(handshake.handshake())).getMessage()
				.contains("a handshake"));
	}

	/**
	 * What arrives is not always what was sent: empty, truncated and foreign text all have to be
	 * refused with something the user can act on
	 */
	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@MethodSource("unusableText")
	@DisplayName("text that is not from this tool is refused with a readable reason")
	void unusableTextIsRefused(String text)
	{
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> KeyExchangeSupport.encapsulate(text));
		assertFalse(thrown.getMessage().isBlank());
	}

	static java.util.List<String> unusableText()
	{
		return java.util.Arrays.asList("", "   ", "not a key at all", "MCKX1", "MCKX1$PUB",
			"MCKX1$PUB$ML-KEM-768", "MCKX2$PUB$ML-KEM-768$AAAA",
			"MCKX1$PUB$" + KeyExchangeSupport.HYBRID + "$AAAA");
	}

	/**
	 * A hybrid public key carries two halves, and dropping one of them must not go unnoticed
	 */
	@Test
	@DisplayName("a hybrid public key with one half missing is refused")
	void aHalvedHybridPublicKeyIsRefused() throws Exception
	{
		String published = KeyExchangeSupport
			.publicKeyOf(KeyExchangeSupport.newParty(KeyExchangeSupport.HYBRID));
		String halved = published.substring(0, published.lastIndexOf('$'));

		assertTrue(assertThrows(IllegalArgumentException.class,
			() -> KeyExchangeSupport.encapsulate(halved)).getMessage().contains("two halves"));
	}

	/**
	 * What travels must not contain the secret, and the public key must not contain the private one
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("neither the public key nor the handshake carries anything private")
	void nothingPrivateTravels(String algorithm) throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport.newParty(algorithm);
		String published = KeyExchangeSupport.publicKeyOf(recipient);
		KeyExchangeSupport.Handshake sender = KeyExchangeSupport.encapsulate(published);

		String secret = java.util.Base64.getEncoder()
			.encodeToString(sender.sharedSecret().getEncoded());
		String privateKey = java.util.Base64.getEncoder()
			.encodeToString(recipient.first().getPrivate().getEncoded());

		assertFalse(published.contains(secret) || sender.handshake().contains(secret),
			"the secret itself is in the text that travels");
		assertFalse(published.contains(privateKey), "the published key carries the private half");
	}

	/**
	 * The algorithm can be read off a piece of text without trying to use it
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("the algorithm is readable from the text itself")
	void theAlgorithmIsReadableFromTheText(String algorithm) throws Exception
	{
		KeyExchangeSupport.Party recipient = KeyExchangeSupport.newParty(algorithm);
		String published = KeyExchangeSupport.publicKeyOf(recipient);

		assertEquals(algorithm, KeyExchangeSupport.algorithmOf(published));
		assertEquals(algorithm,
			KeyExchangeSupport.algorithmOf(KeyExchangeSupport.encapsulate(published).handshake()));
	}
}
