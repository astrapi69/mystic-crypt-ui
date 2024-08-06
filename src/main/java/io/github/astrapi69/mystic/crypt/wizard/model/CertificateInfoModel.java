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
package io.github.astrapi69.mystic.crypt.wizard.model;

import java.math.BigInteger;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * The class {@link CertificateInfoModel} represents all the information for an X.509 certificate
 */
@Data
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateInfoModel
{
	/**
	 * Information about the private key
	 */
	KeyInfoModel privateKeyInfo;

	/**
	 * Information about the public key
	 */
	KeyInfoModel publicKeyInfo;

	/**
	 * The distinguished name information of the issuer
	 */
	DistinguishedNameInfoModel issuer;

	/**
	 * The serial number of the certificate
	 */
	BigInteger serial;

	/**
	 * The validity period of the certificate
	 */
	ValidityModel validityModel;

	/**
	 * The distinguished name information of the subject
	 */
	DistinguishedNameInfoModel subject;

	/**
	 * The signature algorithm used to sign the certificate
	 */
	String signatureAlgorithm;

	/**
	 * The version of the certificate
	 */
	Integer version;

	/**
	 * The extensions added in X.509 V3 certificate
	 */
	ExtensionInfoModel[] extensions;
}
