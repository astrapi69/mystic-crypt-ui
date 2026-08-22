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

import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
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
			: "SHA256withRSA";

		return CertFactory.newX509CertificateV3(keyPair, issuer, serial, notBefore, notAfter,
			subject, signatureAlgorithm);
	}
}
