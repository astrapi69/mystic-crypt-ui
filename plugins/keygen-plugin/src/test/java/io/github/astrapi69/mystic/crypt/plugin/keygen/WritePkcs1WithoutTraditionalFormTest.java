package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;

/**
 * What {@code KeygenSupport.writePrivateKey} does today when PKCS#1 is asked for a key that has no
 * traditional form of its own, and where that answer comes from.
 * <p>
 * The PEM path hands the request to crypt-data's {@code PrivateKeyWriter.write(key, out, PEM,
 * format)}. Up to crypt-data 12.3 it answered with the PKCS#8 file - the caller got a format it had
 * not asked for and was told nothing - and this test pinned that deliberately, so the change would
 * be noticed rather than arrive unannounced. It was: crypt-data 13.0 refuses with an
 * {@code InvalidKeySpecException} naming the algorithm, before anything is written (crypt-data#42),
 * and this pin failed on the bump to mystic-crypt 13.3, which brings it. What is pinned now is the
 * refusal.
 * <p>
 * Reaching this through the user interface is not possible - {@code GenerateKeysPanel} forces the
 * format box to PKCS#8 for exactly these algorithms - so no user meets the exception. A direct
 * caller of {@code writePrivateKey} does, which is the better of the two outcomes: a refusal that
 * names the algorithm beats a file in a format nobody asked for.
 */
class WritePkcs1WithoutTraditionalFormTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static PrivateKey newPrivateKey(final KeyPairGeneratorAlgorithm algorithm)
		throws Exception
	{
		return KeyPairGenerator
			.getInstance(algorithm.getAlgorithm(), BouncyCastleProvider.PROVIDER_NAME)
			.generateKeyPair().getPrivate();
	}

	/**
	 * Since crypt-data 13.0: the request is refused, and the message names the algorithm.
	 *
	 * @param algorithm
	 *            an algorithm whose private key has no traditional form
	 * @param directory
	 *            the directory the key is written to
	 * @throws Exception
	 *             if the key cannot be generated or written
	 */
	@ParameterizedTest(name = "{0} asked for PKCS#1 as PEM")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class,
		names = { "ML_DSA_65", "ML_KEM_768", "X25519", "X448" })
	void pkcs1AsPemIsRefusedWithTheAlgorithmNamed(final KeyPairGeneratorAlgorithm algorithm,
		@TempDir File directory) throws Exception
	{
		PrivateKey privateKey = newPrivateKey(algorithm);
		File file = new File(directory, "key.pem");

		InvalidKeySpecException refused = assertThrows(InvalidKeySpecException.class,
			() -> KeygenSupport.writePrivateKey(privateKey, file, KeyFormat.PKCS_1,
				KeyFileFormat.PEM),
			algorithm + " has no traditional form, so PKCS#1 cannot be written for it");

		assertTrue(refused.getMessage().contains(privateKey.getAlgorithm()),
			"the refusal names the algorithm, which is what makes it actionable: "
				+ refused.getMessage());
	}

	/**
	 * The counterpart, so the case above cannot be satisfied by writing PKCS#8 for everything.
	 *
	 * @param algorithm
	 *            an algorithm whose private key has a traditional form
	 * @param directory
	 *            the directory the key is written to
	 * @throws Exception
	 *             if the key cannot be generated or written
	 */
	@ParameterizedTest(name = "{0} asked for PKCS#1 as PEM gets its own header")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "DSA" })
	void pkcs1AsPemKeepsWorkingWhereATraditionalFormExists(
		final KeyPairGeneratorAlgorithm algorithm, @TempDir File directory) throws Exception
	{
		File file = new File(directory, "key.pem");

		KeygenSupport.writePrivateKey(newPrivateKey(algorithm), file, KeyFormat.PKCS_1,
			KeyFileFormat.PEM);

		String header = Files.readAllLines(file.toPath()).get(0);
		assertTrue(header.endsWith(" PRIVATE KEY-----") && !header.equals("-----BEGIN PRIVATE KEY-----"),
			algorithm + " has a traditional form and must write its own header, was: " + header);
	}

	/**
	 * DER ignores the key format for every algorithm, and crypt-data#42 leaves that as it is, so
	 * naming PKCS#1 there has to keep working rather than start throwing.
	 *
	 * @param algorithm
	 *            an algorithm whose private key has no traditional form
	 * @param directory
	 *            the directory the key is written to
	 * @throws Exception
	 *             if the key cannot be generated or written
	 */
	@ParameterizedTest(name = "{0} as DER with PKCS#1 named")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class,
		names = { "ML_DSA_65", "ML_KEM_768", "X25519", "X448" })
	void derIgnoresTheKeyFormat(final KeyPairGeneratorAlgorithm algorithm, @TempDir File directory)
		throws Exception
	{
		PrivateKey privateKey = newPrivateKey(algorithm);
		File file = new File(directory, "key.der");

		KeygenSupport.writePrivateKey(privateKey, file, KeyFormat.PKCS_1, KeyFileFormat.DER);

		assertEquals(privateKey.getEncoded().length, Files.readAllBytes(file.toPath()).length,
			algorithm + " as DER must stay the encoded key");
	}
}
