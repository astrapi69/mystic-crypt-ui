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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.KEM;
import javax.crypto.KeyAgreement;

/**
 * Self-contained two-party key-encapsulation exchange built only on the JDK 25 native crypto
 * providers - {@link KEM} for ML-KEM and {@link KeyPairGenerator}/{@link KeyAgreement} for X25519.
 * It requests no explicit security provider, so it uses whatever the platform registers for these
 * standard algorithms, and pulls in no third-party crypto library.
 * <p>
 * In each exchange the recipient generates a key pair; the sender derives a fresh shared secret from
 * only the recipient's public key(s) and produces a ciphertext; the recipient recovers the same
 * shared secret from the ciphertext with its private key(s).
 */
public final class NativeKemExchange
{

	private static final String X25519 = "X25519";

	private NativeKemExchange()
	{
	}

	/**
	 * The outcome of a two-party exchange: the ciphertext the sender transmits, plus the shared
	 * secret each side derived independently (which must be equal).
	 */
	public static final class Result
	{
		private final byte[] ciphertext;
		private final byte[] senderSecret;
		private final byte[] recipientSecret;

		Result(byte[] ciphertext, byte[] senderSecret, byte[] recipientSecret)
		{
			this.ciphertext = ciphertext;
			this.senderSecret = senderSecret;
			this.recipientSecret = recipientSecret;
		}

		public byte[] getCiphertext()
		{
			return ciphertext.clone();
		}

		public byte[] getSenderSecret()
		{
			return senderSecret.clone();
		}

		public byte[] getRecipientSecret()
		{
			return recipientSecret.clone();
		}

		public boolean secretsMatch()
		{
			return MessageDigest.isEqual(senderSecret, recipientSecret);
		}
	}

	/**
	 * Runs a pure ML-KEM exchange for the given parameter set (e.g. {@code "ML-KEM-768"}).
	 *
	 * @param algorithm
	 *            the ML-KEM algorithm name
	 * @return the exchange result
	 * @throws GeneralSecurityException
	 *             if the platform cannot perform the ML-KEM exchange
	 */
	public static Result mlKem(String algorithm) throws GeneralSecurityException
	{
		// recipient side
		KeyPair recipient = KeyPairGenerator.getInstance(algorithm).generateKeyPair();

		// sender side: encapsulate against the recipient's public key
		KEM kem = KEM.getInstance(algorithm);
		KEM.Encapsulated encapsulated = kem.newEncapsulator(recipient.getPublic()).encapsulate();
		byte[] senderSecret = encapsulated.key().getEncoded();
		byte[] ciphertext = encapsulated.encapsulation();

		// recipient side: decapsulate the same secret
		byte[] recipientSecret = kem.newDecapsulator(recipient.getPrivate())
			.decapsulate(ciphertext).getEncoded();

		return new Result(ciphertext, senderSecret, recipientSecret);
	}

	/**
	 * Runs a hybrid exchange that combines classical X25519 key agreement with post-quantum
	 * ML-KEM-768: the derived secret is {@code SHA-256(x25519_secret || ml_kem_secret)}, so it stays
	 * secure as long as either building block does. The ciphertext returned is the ML-KEM
	 * ciphertext; the sender's ephemeral X25519 public key is exchanged alongside it (kept internal
	 * to this demo).
	 *
	 * @return the exchange result
	 * @throws GeneralSecurityException
	 *             if the platform cannot perform the hybrid exchange
	 */
	public static Result hybrid() throws GeneralSecurityException
	{
		final String mlKemAlgorithm = "ML-KEM-768";

		// recipient side: one X25519 and one ML-KEM key pair
		KeyPair recipientX25519 = KeyPairGenerator.getInstance(X25519).generateKeyPair();
		KeyPair recipientMlKem = KeyPairGenerator.getInstance(mlKemAlgorithm).generateKeyPair();

		// sender side: an ephemeral X25519 key pair for agreement + an ML-KEM encapsulation
		KeyPair senderX25519 = KeyPairGenerator.getInstance(X25519).generateKeyPair();
		byte[] senderClassical = agree(senderX25519.getPrivate(), recipientX25519.getPublic());

		KEM kem = KEM.getInstance(mlKemAlgorithm);
		KEM.Encapsulated encapsulated = kem.newEncapsulator(recipientMlKem.getPublic())
			.encapsulate();
		byte[] ciphertext = encapsulated.encapsulation();
		byte[] senderSecret = combine(senderClassical, encapsulated.key().getEncoded());

		// recipient side: agree with the sender's ephemeral public key + decapsulate ML-KEM
		byte[] recipientClassical = agree(recipientX25519.getPrivate(), senderX25519.getPublic());
		byte[] recipientMlKemSecret = kem.newDecapsulator(recipientMlKem.getPrivate())
			.decapsulate(ciphertext).getEncoded();
		byte[] recipientSecret = combine(recipientClassical, recipientMlKemSecret);

		return new Result(ciphertext, senderSecret, recipientSecret);
	}

	private static byte[] agree(PrivateKey privateKey, PublicKey publicKey)
		throws GeneralSecurityException
	{
		KeyAgreement keyAgreement = KeyAgreement.getInstance(X25519);
		keyAgreement.init(privateKey);
		keyAgreement.doPhase(publicKey, true);
		return keyAgreement.generateSecret();
	}

	private static byte[] combine(byte[] classicalSecret, byte[] postQuantumSecret)
		throws GeneralSecurityException
	{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(classicalSecret);
		digest.update(postQuantumSecret);
		return digest.digest();
	}
}
