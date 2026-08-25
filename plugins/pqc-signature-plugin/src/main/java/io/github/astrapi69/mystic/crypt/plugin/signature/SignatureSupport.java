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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.mystic.crypt.key.Ed25519Signer;
import io.github.astrapi69.mystic.crypt.key.Ed25519Verifier;
import io.github.astrapi69.mystic.crypt.key.MlDsaSigner;
import io.github.astrapi69.mystic.crypt.key.MlDsaVerifier;
import io.github.astrapi69.mystic.crypt.key.SlhDsaSigner;
import io.github.astrapi69.mystic.crypt.key.SlhDsaVerifier;

/**
 * One entry point for the three signature families the mystic-crypt library provides: the classical
 * Ed25519 and the NIST post-quantum ML-DSA (FIPS 204) and SLH-DSA (FIPS 205).
 * <p>
 * Each family has its own signer and verifier class, and Ed25519 differs from the other two in that
 * it takes no algorithm parameter - it is not a {@link KeyPairGeneratorAlgorithm} constant at all.
 * This class hides that split behind one string-keyed API, so the panel and the tests can treat all
 * of them alike.
 */
public final class SignatureSupport
{

	/** The name under which Ed25519 is offered; it has no {@link KeyPairGeneratorAlgorithm} */
	public static final String ED25519 = "Ed25519";

	private SignatureSupport()
	{
	}

	/**
	 * Lists every offered signature algorithm: Ed25519 first, then the ML-DSA parameter sets, then
	 * the SLH-DSA ones
	 *
	 * @return the algorithm names
	 */
	public static List<String> algorithms()
	{
		List<String> algorithms = new ArrayList<>();
		algorithms.add(ED25519);
		for (KeyPairGeneratorAlgorithm algorithm : KeyPairGeneratorAlgorithm.values())
		{
			if (algorithm.name().startsWith("ML_DSA"))
			{
				algorithms.add(display(algorithm));
			}
		}
		for (KeyPairGeneratorAlgorithm algorithm : KeyPairGeneratorAlgorithm.values())
		{
			if (algorithm.name().startsWith("SLH_DSA"))
			{
				algorithms.add(display(algorithm));
			}
		}
		return algorithms;
	}

	private static String display(final KeyPairGeneratorAlgorithm algorithm)
	{
		return algorithm.name().replace('_', '-');
	}

	private static KeyPairGeneratorAlgorithm parse(final String name)
	{
		return KeyPairGeneratorAlgorithm.valueOf(name.trim().toUpperCase().replace('-', '_'));
	}

	/**
	 * Generates a key pair for the given algorithm
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @return the new key pair
	 * @throws Exception
	 *             if the platform cannot generate a key pair for the algorithm
	 */
	public static KeyPair newKeyPair(final String algorithm) throws Exception
	{
		if (ED25519.equals(algorithm))
		{
			return Ed25519Signer.newKeyPair();
		}
		KeyPairGeneratorAlgorithm parsed = parse(algorithm);
		return parsed.name().startsWith("ML_DSA")
			? MlDsaSigner.newKeyPair(parsed)
			: SlhDsaSigner.newKeyPair(parsed);
	}

	/**
	 * Signs the given bytes
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @param privateKey
	 *            the signing key
	 * @param data
	 *            the bytes to sign
	 * @return the signature
	 * @throws Exception
	 *             if signing fails
	 */
	public static byte[] sign(final String algorithm, final PrivateKey privateKey,
		final byte[] data) throws Exception
	{
		if (ED25519.equals(algorithm))
		{
			return new Ed25519Signer(privateKey).sign(data);
		}
		KeyPairGeneratorAlgorithm parsed = parse(algorithm);
		return parsed.name().startsWith("ML_DSA")
			? new MlDsaSigner(privateKey, parsed).sign(data)
			: new SlhDsaSigner(privateKey, parsed).sign(data);
	}

	/**
	 * Verifies a signature over the given bytes
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @param publicKey
	 *            the verification key
	 * @param data
	 *            the signed bytes
	 * @param signature
	 *            the signature to check
	 * @return true if the signature belongs to the data and the key
	 * @throws Exception
	 *             if verifying fails
	 */
	public static boolean verify(final String algorithm, final PublicKey publicKey,
		final byte[] data, final byte[] signature) throws Exception
	{
		if (ED25519.equals(algorithm))
		{
			return new Ed25519Verifier(publicKey).verify(data, signature);
		}
		KeyPairGeneratorAlgorithm parsed = parse(algorithm);
		return parsed.name().startsWith("ML_DSA")
			? new MlDsaVerifier(publicKey, parsed).verify(data, signature)
			: new SlhDsaVerifier(publicKey, parsed).verify(data, signature);
	}
}
