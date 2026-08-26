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

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.SecretKey;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.mystic.crypt.aead.KeyCommittingAeadEncryptor;
import io.github.astrapi69.mystic.crypt.key.HybridKemKeyExchange;
import io.github.astrapi69.mystic.crypt.key.MlKemKeyExchange;
import io.github.astrapi69.mystic.crypt.key.X25519KeyExchange;

/**
 * A key exchange between two parties who each hold only their own half - which is what a key
 * exchange is for.
 * <p>
 * The demo next to this one plays both sides on one machine, which shows that the two derive the
 * same secret and nothing else. Here the two halves are separate:
 *
 * <pre>
 * the recipient  generates a key pair and hands out the public half
 * the sender     takes that public half, encapsulates against it, and gets
 *                a shared secret plus a handshake to send back
 * the recipient  puts the handshake together with their private half and
 *                arrives at the same shared secret
 * </pre>
 *
 * Both halves are text, so they can be mailed, pasted or written to a file. Neither of them is the
 * secret: what travels is a public key and a handshake, and someone who reads both still cannot
 * work out the shared secret.
 * <p>
 * Three families are offered. ML-KEM is the post-quantum one, where the handshake is a proper
 * ciphertext. X25519 is the classical one, where "encapsulating" means making an ephemeral key pair
 * and sending its public half. The hybrid does both at once and mixes the two results, so it stays
 * safe as long as either of the two does.
 */
public final class KeyExchangeSupport
{

	/** What every public key and every handshake of this tool starts with */
	public static final String PREFIX = "MCKX1";

	/** The classical elliptic curve exchange */
	public static final String X25519 = "X25519";

	/** The post-quantum ones */
	public static final String ML_KEM_512 = "ML-KEM-512";
	public static final String ML_KEM_768 = "ML-KEM-768";
	public static final String ML_KEM_1024 = "ML-KEM-1024";

	/** Both at once, safe as long as either half is */
	public static final String HYBRID = "Hybrid X25519 + ML-KEM-768";

	/** How long the derived secret is, in bytes */
	public static final int SECRET_LENGTH = 32;

	private KeyExchangeSupport()
	{
	}

	/**
	 * What one side keeps to itself. The public half is handed out with {@link #publicKeyOf}.
	 *
	 * @param algorithm
	 *            the algorithm this party is set up for
	 * @param first
	 *            the key pair; for the hybrid this is the X25519 half
	 * @param second
	 *            the ML-KEM half of a hybrid party, {@code null} otherwise
	 */
	public record Party(String algorithm, KeyPair first, KeyPair second)
	{
	}

	/**
	 * What the sender ends up with: a secret to use, and a handshake for the other side.
	 *
	 * @param sharedSecret
	 *            the secret both sides will hold
	 * @param handshake
	 *            the text to send back to the recipient
	 */
	public record Handshake(SecretKey sharedSecret, String handshake)
	{
	}

	/**
	 * The algorithms this tool offers, in the order they are shown
	 *
	 * @return the algorithms
	 */
	public static List<String> algorithms()
	{
		return List.of(ML_KEM_768, ML_KEM_512, ML_KEM_1024, X25519, HYBRID);
	}

	/**
	 * Sets up one side of an exchange
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @return the party, whose public half is handed to the other side
	 * @throws Exception
	 *             if this machine cannot produce such a key pair
	 */
	public static Party newParty(final String algorithm) throws Exception
	{
		if (X25519.equals(algorithm))
		{
			return new Party(algorithm, X25519KeyExchange.newKeyPair(), null);
		}
		if (HYBRID.equals(algorithm))
		{
			HybridKemKeyExchange.HybridKeyPair hybrid = HybridKemKeyExchange
				.newHybridKeyPair(KeyPairGeneratorAlgorithm.ML_KEM_768);
			return new Party(algorithm, hybrid.getX25519KeyPair(), hybrid.getMlKemKeyPair());
		}
		return new Party(algorithm, MlKemKeyExchange.newKeyPair(mlKemAlgorithm(algorithm)), null);
	}

	/**
	 * The public half of a party, as one line of text to hand to the other side
	 *
	 * @param party
	 *            the party
	 * @return the public key as text
	 */
	public static String publicKeyOf(final Party party)
	{
		if (party.second() != null)
		{
			return envelope("PUB", party.algorithm(),
				party.first().getPublic().getEncoded(), party.second().getPublic().getEncoded());
		}
		return envelope("PUB", party.algorithm(), party.first().getPublic().getEncoded(), null);
	}

	/**
	 * The sender's side: takes the recipient's public half and produces a secret and a handshake
	 *
	 * @param recipientPublicKey
	 *            what the recipient handed out
	 * @return the shared secret and the handshake to send back
	 * @throws Exception
	 *             if the public key cannot be read or the exchange fails
	 */
	public static Handshake encapsulate(final String recipientPublicKey) throws Exception
	{
		String[] parts = read("PUB", recipientPublicKey);
		String algorithm = parts[2];
		byte[] firstKey = Base64.getDecoder().decode(parts[3]);
		if (X25519.equals(algorithm))
		{
			// there is no ciphertext in an elliptic curve exchange: the sender makes an ephemeral
			// key pair, and its public half is what travels
			KeyPair ephemeral = X25519KeyExchange.newKeyPair();
			SecretKey secret = X25519KeyExchange.deriveSharedSecret(ephemeral.getPrivate(),
				toPublicKey(firstKey, "X25519"), SECRET_LENGTH);
			return new Handshake(secret,
				envelope("HS", algorithm, ephemeral.getPublic().getEncoded(), null));
		}
		if (HYBRID.equals(algorithm))
		{
			byte[] secondKey = Base64.getDecoder().decode(parts[4]);
			HybridKemKeyExchange.HybridEncapsulation encapsulation = HybridKemKeyExchange
				.hybridEncapsulate(toPublicKey(firstKey, "X25519"),
					toPublicKey(secondKey, "ML-KEM-768"), KeyPairGeneratorAlgorithm.ML_KEM_768,
					SECRET_LENGTH);
			return new Handshake(encapsulation.getSharedSecret(),
				envelope("HS", algorithm, encapsulation.getMlKemCiphertext(),
					encapsulation.getSenderX25519PublicKey().getEncoded()));
		}
		MlKemKeyExchange.Encapsulation encapsulation = MlKemKeyExchange
			.encapsulate(toPublicKey(firstKey, algorithm), mlKemAlgorithm(algorithm));
		return new Handshake(encapsulation.getSharedSecret(),
			envelope("HS", algorithm, encapsulation.getCiphertext(), null));
	}

	/**
	 * The recipient's side: takes the handshake and arrives at the same secret
	 *
	 * @param party
	 *            the recipient, holding the private half
	 * @param handshake
	 *            what came back from the sender
	 * @return the shared secret, the same one the sender holds
	 * @throws Exception
	 *             if the handshake does not belong to this party or cannot be read
	 */
	public static SecretKey decapsulate(final Party party, final String handshake) throws Exception
	{
		String[] parts = read("HS", handshake);
		String algorithm = parts[2];
		if (!party.algorithm().equals(algorithm))
		{
			throw new IllegalArgumentException("this handshake is for " + algorithm
				+ " and these keys are for " + party.algorithm());
		}
		byte[] first = Base64.getDecoder().decode(parts[3]);
		if (X25519.equals(algorithm))
		{
			return X25519KeyExchange.deriveSharedSecret(party.first().getPrivate(),
				toPublicKey(first, "X25519"), SECRET_LENGTH);
		}
		if (HYBRID.equals(algorithm))
		{
			byte[] senderPublicKey = Base64.getDecoder().decode(parts[4]);
			return HybridKemKeyExchange.hybridDecapsulate(party.first().getPrivate(),
				party.second().getPrivate(), toPublicKey(senderPublicKey, "X25519"), first,
				KeyPairGeneratorAlgorithm.ML_KEM_768, SECRET_LENGTH);
		}
		return MlKemKeyExchange.decapsulate(party.first().getPrivate(), first,
			mlKemAlgorithm(algorithm));
	}

	/**
	 * Encrypts a message with a shared secret, so the secret is used for something rather than only
	 * looked at
	 *
	 * @param sharedSecret
	 *            the secret both sides hold
	 * @param message
	 *            what to encrypt
	 * @return the encrypted message as Base64
	 * @throws Exception
	 *             if encrypting fails
	 */
	public static String encryptMessage(final SecretKey sharedSecret, final byte[] message)
		throws Exception
	{
		return Base64.getEncoder()
			.encodeToString(new KeyCommittingAeadEncryptor(sharedSecret).encrypt(message));
	}

	/**
	 * Decrypts what {@link #encryptMessage} produced. A secret that is not the same one fails here
	 * rather than producing rubbish
	 *
	 * @param sharedSecret
	 *            the secret
	 * @param encrypted
	 *            the encrypted message as Base64
	 * @return the message
	 * @throws Exception
	 *             if the secret is wrong or the message was changed
	 */
	public static byte[] decryptMessage(final SecretKey sharedSecret, final String encrypted)
		throws Exception
	{
		return new KeyCommittingAeadEncryptor(sharedSecret)
			.decrypt(Base64.getDecoder().decode(encrypted.trim()));
	}

	/**
	 * A short value over a secret, so two sides can compare that they arrived at the same one
	 * without either of them showing it
	 *
	 * @param sharedSecret
	 *            the secret
	 * @return eight hex characters
	 * @throws Exception
	 *             if the digest is unavailable
	 */
	public static String fingerprintOf(final SecretKey sharedSecret) throws Exception
	{
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(sharedSecret.getEncoded());
		return HexFormat.of().formatHex(digest).substring(0, 8);
	}

	/**
	 * Which algorithm a public key or handshake belongs to
	 *
	 * @param envelope
	 *            the text
	 * @return the algorithm
	 */
	public static String algorithmOf(final String envelope)
	{
		String[] parts = envelope == null ? new String[0] : envelope.trim().split("\\$");
		if (parts.length < 3 || !PREFIX.equals(parts[0]))
		{
			throw new IllegalArgumentException("'" + envelope + "' is not from this tool");
		}
		return parts[2];
	}


	/**
	 * The whole of one side, private halves included, as one line of text - so a party can be put
	 * away and taken up again later, or in another run of the command line.
	 * <p>
	 * This text is the private key. Whoever holds it can read everything that was ever sent to this
	 * party, so it belongs where a private key belongs and not next to the public one.
	 *
	 * @param party
	 *            the party
	 * @return the party as text
	 */
	public static String privateKeyOf(final Party party)
	{
		StringBuilder text = new StringBuilder(PREFIX).append("$PRV$").append(party.algorithm())
			.append("$").append(encode(party.first().getPrivate().getEncoded())).append("$")
			.append(encode(party.first().getPublic().getEncoded()));
		if (party.second() != null)
		{
			text.append("$").append(encode(party.second().getPrivate().getEncoded())).append("$")
				.append(encode(party.second().getPublic().getEncoded()));
		}
		return text.toString();
	}

	/**
	 * Takes up a party that {@link #privateKeyOf} put away
	 *
	 * @param text
	 *            what {@link #privateKeyOf} produced
	 * @return the party, ready to decapsulate again
	 * @throws Exception
	 *             if the text is not a stored party or its keys cannot be read
	 */
	public static Party partyFrom(final String text) throws Exception
	{
		String[] parts = text == null ? new String[0] : text.trim().split("\\$");
		// the kind is read before the length: a public key is four parts long, and saying "this is
		// not a key of this tool" about one would send the user looking in the wrong place
		if (parts.length < 2 || !PREFIX.equals(parts[0]))
		{
			throw new IllegalArgumentException("this is not a stored key of this tool");
		}
		if (!"PRV".equals(parts[1]))
		{
			throw new IllegalArgumentException(
				"this is the public half; the private key of this side is needed here");
		}
		if (parts.length < 5)
		{
			throw new IllegalArgumentException("this stored key is incomplete");
		}
		String algorithm = parts[2];
		boolean hybrid = HYBRID.equals(algorithm);
		if (hybrid && parts.length < 7)
		{
			throw new IllegalArgumentException("a stored hybrid key carries two halves and this one carries one");
		}
		String first = hybrid ? "X25519" : algorithm;
		KeyPair firstPair = new KeyPair(toPublicKey(decode(parts[4]), first),
			toPrivateKey(decode(parts[3]), first));
		if (!hybrid)
		{
			return new Party(algorithm, firstPair, null);
		}
		KeyPair secondPair = new KeyPair(toPublicKey(decode(parts[6]), "ML-KEM-768"),
			toPrivateKey(decode(parts[5]), "ML-KEM-768"));
		return new Party(algorithm, firstPair, secondPair);
	}

	private static String encode(final byte[] bytes)
	{
		return Base64.getEncoder().encodeToString(bytes);
	}

	private static byte[] decode(final String text)
	{
		return Base64.getDecoder().decode(text);
	}

	private static java.security.PrivateKey toPrivateKey(final byte[] encoded,
		final String algorithm) throws Exception
	{
		return keyFactory(algorithm)
			.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(encoded));
	}

	private static java.security.KeyFactory keyFactory(final String algorithm) throws Exception
	{
		return java.security.KeyFactory
			.getInstance(algorithm.startsWith("ML-KEM") ? "ML-KEM" : algorithm);
	}

	private static String envelope(final String kind, final String algorithm, final byte[] first,
		final byte[] second)
	{
		StringBuilder text = new StringBuilder(PREFIX).append("$").append(kind).append("$")
			.append(algorithm).append("$").append(Base64.getEncoder().encodeToString(first));
		if (second != null)
		{
			text.append("$").append(Base64.getEncoder().encodeToString(second));
		}
		return text.toString();
	}

	private static String[] read(final String expectedKind, final String envelope)
	{
		if (envelope == null || envelope.isBlank())
		{
			throw new IllegalArgumentException("there is nothing to read");
		}
		String[] parts = envelope.trim().split("\\$");
		if (parts.length < 4 || !PREFIX.equals(parts[0]))
		{
			throw new IllegalArgumentException("'" + envelope + "' is not from this tool");
		}
		if (!expectedKind.equals(parts[1]))
		{
			throw new IllegalArgumentException("this is " + ("PUB".equals(parts[1])
				? "a public key, and a handshake was expected"
				: "a handshake, and a public key was expected"));
		}
		if (HYBRID.equals(parts[2]) && parts.length < 5)
		{
			throw new IllegalArgumentException("a hybrid " + expectedKind.toLowerCase(
				java.util.Locale.ROOT) + " carries two halves and this one carries one");
		}
		return parts;
	}

	private static PublicKey toPublicKey(final byte[] encoded, final String algorithm)
		throws Exception
	{
		return keyFactory(algorithm)
			.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));
	}

	private static KeyPairGeneratorAlgorithm mlKemAlgorithm(final String algorithm)
	{
		return switch (algorithm)
		{
			case ML_KEM_512 -> KeyPairGeneratorAlgorithm.ML_KEM_512;
			case ML_KEM_768 -> KeyPairGeneratorAlgorithm.ML_KEM_768;
			case ML_KEM_1024 -> KeyPairGeneratorAlgorithm.ML_KEM_1024;
			default -> throw new IllegalArgumentException("'" + algorithm + "' is not one of "
				+ String.join(", ", algorithms()));
		};
	}
}
