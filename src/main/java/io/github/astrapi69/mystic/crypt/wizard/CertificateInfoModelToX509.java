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
package io.github.astrapi69.mystic.crypt.wizard;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;

import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ExtensionInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;

/**
 * Builds an X.509 v3 certificate from the {@link CertificateInfoModel} the certificate wizard
 * collects: the key pair is restored from the model's key infos, issuer and subject from its
 * distinguished names and the validity window from its validity model.
 */
public final class CertificateInfoModelToX509
{

	private CertificateInfoModelToX509()
	{
	}

	/**
	 * Generates the X.509 certificate described by the given model
	 *
	 * @param model
	 *            the collected certificate info
	 * @return the generated {@link X509Certificate}
	 * @throws Exception
	 *             if the keys cannot be restored or the certificate cannot be generated
	 */
	public static X509Certificate toX509Certificate(CertificateInfoModel model) throws Exception
	{
		PrivateKey privateKey = KeyInfoExtensions
			.toPrivateKey(KeyInfoModel.toKeyInfo(model.getPrivateKeyInfo()));
		PublicKey publicKey = KeyInfoExtensions
			.toPublicKey(KeyInfoModel.toKeyInfo(model.getPublicKeyInfo()));
		KeyPair keyPair = new KeyPair(publicKey, privateKey);

		X500Name issuer = DistinguishedNameInfoModel.toDistinguishedNameInfo(model.getIssuer())
			.toX500Name();
		X500Name subject = DistinguishedNameInfoModel.toDistinguishedNameInfo(model.getSubject())
			.toX500Name();

		ValidityModel validity = model.getValidityModel();
		Date notBefore = Date.from(validity.getNotBefore().toInstant());
		Date notAfter = Date.from(validity.getNotAfter().toInstant());
		BigInteger serial = model.getSerial() != null
			? model.getSerial()
			: BigInteger.valueOf(notBefore.getTime());
		String signatureAlgorithm = model.getSignatureAlgorithm() != null
			? model.getSignatureAlgorithm()
			: defaultSignatureAlgorithmFor(privateKey);
		// a signature algorithm that the key cannot produce fails deep inside the certificate
		// builder with a message nobody can act on; saying it plainly here is worth the two lines
		if (!signatureAlgorithmFits(signatureAlgorithm, privateKey.getAlgorithm()))
		{
			throw new IllegalArgumentException(
				"a " + privateKey.getAlgorithm() + " key cannot sign with '" + signatureAlgorithm
					+ "' - use " + defaultSignatureAlgorithmFor(privateKey)
					+ " or another algorithm of that family");
		}

		// what the wizard collected on its "Extensions" step used to stop here: the extensions were
		// built into the model and then never handed to the certificate, so key usage, subject
		// alternative names and basic constraints were silently missing from every certificate
		Extension[] extensions = toExtensions(model.getExtensions());

		return CertFactory.newX509CertificateV3(keyPair, issuer, serial, notBefore, notAfter,
			subject, signatureAlgorithm, extensions);
	}

	/**
	 * Builds the extensions of a certificate from what the wizard collected
	 *
	 * @param extensionInfoModels
	 *            the extensions from the wizard, which may be null or empty
	 * @return the extensions, empty when there are none
	 * @throws IllegalArgumentException
	 *             if one of them cannot be built
	 */
	public static Extension[] toExtensions(final ExtensionInfoModel[] extensionInfoModels)
	{
		if (extensionInfoModels == null || extensionInfoModels.length == 0)
		{
			return new Extension[0];
		}
		Extension[] extensions = new Extension[extensionInfoModels.length];
		for (int index = 0; index < extensionInfoModels.length; index++)
		{
			ExtensionInfoModel extensionInfoModel = extensionInfoModels[index];
			extensions[index] = CertificateExtensionValues.toExtension(
				extensionInfoModel.getExtensionId(), extensionInfoModel.isCritical(),
				extensionInfoModel.getValue());
		}
		return extensions;
	}

	/**
	 * The signature algorithm to use when none was chosen, for a key of the given kind
	 *
	 * @param keyAlgorithm
	 *            the algorithm of the private key, as the key itself reports it
	 * @return the signature algorithm
	 */
	public static String defaultSignatureAlgorithmFor(final String keyAlgorithm)
	{
		return switch (keyAlgorithm.toUpperCase(java.util.Locale.ROOT))
		{
			case "EC", "ECDSA" -> "SHA256withECDSA";
			case "DSA" -> "SHA256withDSA";
			case "ED25519" -> "Ed25519";
			case "ED448" -> "Ed448";
			// an RSASSA-PSS key signed the PKCS#1 v1.5 way produces a certificate that contradicts
			// RFC 4055 and that openssl refuses to verify
			case "RSASSA-PSS" -> "SHA256withRSAandMGF1";
			default -> "SHA256withRSA";
		};
	}

	/**
	 * The signature algorithm to use when none was chosen, for the given private key.
	 * <p>
	 * A real JDK-generated Ed25519/Ed448 key reports the generic family name "EdDSA" through
	 * {@link PrivateKey#getAlgorithm()}, not "Ed25519"/"Ed448" specifically - BouncyCastle refuses
	 * the bare "EdDSA" as a signature algorithm name ("Unknown signature type requested: EdDSA"),
	 * it needs the exact curve. {@link java.security.interfaces.EdECKey#getParams()} still knows
	 * which curve the key actually is, which {@link #defaultSignatureAlgorithmFor(String)} alone
	 * cannot recover from the algorithm name (#142).
	 *
	 * @param privateKey
	 *            the private key that will do the signing
	 * @return the signature algorithm
	 */
	public static String defaultSignatureAlgorithmFor(final PrivateKey privateKey)
	{
		if (privateKey instanceof java.security.interfaces.EdECKey edECKey)
		{
			return edECKey.getParams().getName();
		}
		return defaultSignatureAlgorithmFor(privateKey.getAlgorithm());
	}

	/**
	 * Whether a key of the given kind can sign with the given signature algorithm
	 *
	 * @param signatureAlgorithm
	 *            the signature algorithm that was chosen
	 * @param keyAlgorithm
	 *            the algorithm of the private key
	 * @return true if the two fit together
	 */
	public static boolean signatureAlgorithmFits(final String signatureAlgorithm,
		final String keyAlgorithm)
	{
		String signature = signatureAlgorithm.toUpperCase(java.util.Locale.ROOT).replace("-", "");
		String key = keyAlgorithm.toUpperCase(java.util.Locale.ROOT).replace("-", "");
		return switch (key)
		{
			case "RSA" -> signature.contains("RSA");
			case "RSASSAPSS" -> signature.contains("RSA") && signature.contains("MGF1")
				|| signature.contains("PSS");
			case "EC", "ECDSA" -> signature.contains("ECDSA");
			case "DSA" -> signature.contains("DSA") && !signature.contains("ECDSA");
			case "ED25519" -> signature.contains("ED25519");
			case "ED448" -> signature.contains("ED448");
			// the generic family name a real Ed25519/Ed448 key actually reports (#142) - either
			// curve-specific signature name fits it
			case "EDDSA" -> signature.contains("ED25519") || signature.contains("ED448");
			// anything else - the post-quantum families among them - names itself in its signature
			default -> signature.contains(key);
		};
	}
}
