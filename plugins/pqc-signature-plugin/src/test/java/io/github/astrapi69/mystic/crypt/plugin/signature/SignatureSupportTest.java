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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Headless round-trip proof for every signature algorithm the panel offers: generating a key pair,
 * signing a message, verifying it, and confirming that a changed message no longer verifies.
 */
class SignatureSupportTest
{

	private static final byte[] MESSAGE = "The quick brown fox jumps over the lazy dog"
		.getBytes(StandardCharsets.UTF_8);
	private static final byte[] TAMPERED = "The quick brown fox jumps over the lazy dogs"
		.getBytes(StandardCharsets.UTF_8);

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static List<String> algorithms()
	{
		return SignatureSupport.algorithms();
	}

	@Test
	void offersEd25519AndEveryPostQuantumParameterSet()
	{
		List<String> algorithms = SignatureSupport.algorithms();

		assertEquals(SignatureSupport.ED25519, algorithms.get(0), "Ed25519 comes first");
		assertEquals(3, algorithms.stream().filter(a -> a.startsWith("ML-DSA")).count(),
			"all three ML-DSA parameter sets are offered");
		assertEquals(12, algorithms.stream().filter(a -> a.startsWith("SLH-DSA")).count(),
			"all twelve SLH-DSA parameter sets are offered");
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void signsAndVerifiesWithEveryAlgorithm(String algorithm) throws Exception
	{
		KeyPair keyPair = SignatureSupport.newKeyPair(algorithm);
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());

		byte[] signature = SignatureSupport.sign(algorithm, keyPair.getPrivate(), MESSAGE);
		assertTrue(signature.length > 0, "a signature must be produced for " + algorithm);

		assertTrue(SignatureSupport.verify(algorithm, keyPair.getPublic(), MESSAGE, signature),
			"the signature must verify for " + algorithm);
	}

	@ParameterizedTest
	@MethodSource("algorithms")
	void aChangedMessageDoesNotVerify(String algorithm) throws Exception
	{
		KeyPair keyPair = SignatureSupport.newKeyPair(algorithm);
		byte[] signature = SignatureSupport.sign(algorithm, keyPair.getPrivate(), MESSAGE);

		assertFalse(SignatureSupport.verify(algorithm, keyPair.getPublic(), TAMPERED, signature),
			"a changed message must not verify for " + algorithm);
	}

	@Test
	void aSignatureFromAnotherKeyDoesNotVerify() throws Exception
	{
		String algorithm = "ML-DSA-65";
		KeyPair signing = SignatureSupport.newKeyPair(algorithm);
		KeyPair other = SignatureSupport.newKeyPair(algorithm);

		byte[] signature = SignatureSupport.sign(algorithm, signing.getPrivate(), MESSAGE);

		assertFalse(SignatureSupport.verify(algorithm, other.getPublic(), MESSAGE, signature),
			"a signature must not verify against an unrelated public key");
	}

	@Test
	void anUnknownAlgorithmIsRejected()
	{
		assertThrows(IllegalArgumentException.class,
			() -> SignatureSupport.newKeyPair("NO-SUCH-ALGORITHM"));
	}
}
