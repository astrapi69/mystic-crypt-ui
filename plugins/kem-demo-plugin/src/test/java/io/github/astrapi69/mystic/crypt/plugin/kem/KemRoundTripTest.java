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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Headless round-trip proof of the two-party exchange the {@link KemDemoPanel} drives, exercising
 * {@link NativeKemExchange} directly: for every ML-KEM parameter set and for the hybrid
 * X25519+ML-KEM mode, the sender encapsulates a shared secret from the recipient's public key and
 * the recipient decapsulates the same secret back. Everything resolves to the JDK 25 native
 * providers, exactly as in the running host.
 */
class KemRoundTripTest
{

	@ParameterizedTest
	@ValueSource(strings = { "ML-KEM-512", "ML-KEM-768", "ML-KEM-1024" })
	void mlKemEncapsulatesAndDecapsulatesTheSameSecret(String algorithm) throws Exception
	{
		NativeKemExchange.Result result = NativeKemExchange.mlKem(algorithm);

		assertTrue(result.getCiphertext().length > 0, "the ciphertext must be non-empty");
		assertTrue(result.getSenderSecret().length > 0, "the shared secret must be non-empty");
		assertTrue(result.secretsMatch(),
			"sender and recipient must derive the same " + algorithm + " shared secret");
	}

	@Test
	void hybridEncapsulatesAndDecapsulatesTheSameSecret() throws Exception
	{
		NativeKemExchange.Result result = NativeKemExchange.hybrid();

		assertTrue(result.getCiphertext().length > 0, "the ML-KEM ciphertext must be non-empty");
		assertTrue(result.secretsMatch(),
			"sender and recipient must derive the same hybrid shared secret");
		assertEquals(32, result.getSenderSecret().length,
			"the hybrid secret is the SHA-256 of the combined secrets, i.e. 32 bytes");
	}

	@Test
	void encapsulationIsNonDeterministic() throws Exception
	{
		NativeKemExchange.Result first = NativeKemExchange.mlKem("ML-KEM-768");
		NativeKemExchange.Result second = NativeKemExchange.mlKem("ML-KEM-768");

		assertFalse(java.util.Arrays.equals(first.getSenderSecret(), second.getSenderSecret()),
			"two independent exchanges must produce different shared secrets");
	}
}
