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
package io.github.astrapi69.mystic.crypt.panel.certificate;

/**
 * Everything the {@link CertificatePanel} shows about one certificate: who it was issued to and by,
 * its version and serial number, the period it is valid in, the algorithm it was signed with, its
 * fingerprint together with the algorithm that fingerprint was taken with, and its public key.
 * <p>
 * The panel binds its components to this object, so what the panel shows is readable here at any
 * moment instead of having to be collected from nine widgets.
 * <p>
 * The certificate the panel is constructed with,
 * {@link io.github.astrapi69.crypt.data.model.X509CertificateV3Info}, is immutable and carries
 * neither a fingerprint nor a public key, which is why the values shown live in this object of
 * their own.
 */
public class CertificatePanelModel
{

	/** The subject the certificate was issued to */
	private String issuedTo = "";

	/** The issuer that signed the certificate */
	private String issuedBy = "";

	/** The X.509 version of the certificate */
	private String version = "";

	/** The serial number of the certificate */
	private String serialNumber = "";

	/** The first moment the certificate is valid at */
	private String validFrom = "";

	/** The last moment the certificate is valid at */
	private String validUntil = "";

	/** The algorithm the certificate was signed with */
	private String signatureAlgorithm = "";

	/** The fingerprint of the certificate */
	private String fingerprint = "";

	/** The algorithm the fingerprint was taken with */
	private String fingerprintAlgorithm = "";

	/** The public key the certificate carries */
	private String publicKey = "";

	/**
	 * Gets the subject the certificate was issued to
	 *
	 * @return the subject the certificate was issued to
	 */
	public String getIssuedTo()
	{
		return issuedTo;
	}

	/**
	 * Sets the subject the certificate was issued to
	 *
	 * @param issuedTo
	 *            the subject the certificate was issued to
	 */
	public void setIssuedTo(final String issuedTo)
	{
		this.issuedTo = issuedTo;
	}

	/**
	 * Gets the issuer that signed the certificate
	 *
	 * @return the issuer that signed the certificate
	 */
	public String getIssuedBy()
	{
		return issuedBy;
	}

	/**
	 * Sets the issuer that signed the certificate
	 *
	 * @param issuedBy
	 *            the issuer that signed the certificate
	 */
	public void setIssuedBy(final String issuedBy)
	{
		this.issuedBy = issuedBy;
	}

	/**
	 * Gets the X.509 version of the certificate
	 *
	 * @return the X.509 version of the certificate
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * Sets the X.509 version of the certificate
	 *
	 * @param version
	 *            the X.509 version of the certificate
	 */
	public void setVersion(final String version)
	{
		this.version = version;
	}

	/**
	 * Gets the serial number of the certificate
	 *
	 * @return the serial number of the certificate
	 */
	public String getSerialNumber()
	{
		return serialNumber;
	}

	/**
	 * Sets the serial number of the certificate
	 *
	 * @param serialNumber
	 *            the serial number of the certificate
	 */
	public void setSerialNumber(final String serialNumber)
	{
		this.serialNumber = serialNumber;
	}

	/**
	 * Gets the first moment the certificate is valid at
	 *
	 * @return the first moment the certificate is valid at
	 */
	public String getValidFrom()
	{
		return validFrom;
	}

	/**
	 * Sets the first moment the certificate is valid at
	 *
	 * @param validFrom
	 *            the first moment the certificate is valid at
	 */
	public void setValidFrom(final String validFrom)
	{
		this.validFrom = validFrom;
	}

	/**
	 * Gets the last moment the certificate is valid at
	 *
	 * @return the last moment the certificate is valid at
	 */
	public String getValidUntil()
	{
		return validUntil;
	}

	/**
	 * Sets the last moment the certificate is valid at
	 *
	 * @param validUntil
	 *            the last moment the certificate is valid at
	 */
	public void setValidUntil(final String validUntil)
	{
		this.validUntil = validUntil;
	}

	/**
	 * Gets the algorithm the certificate was signed with
	 *
	 * @return the algorithm the certificate was signed with
	 */
	public String getSignatureAlgorithm()
	{
		return signatureAlgorithm;
	}

	/**
	 * Sets the algorithm the certificate was signed with
	 *
	 * @param signatureAlgorithm
	 *            the algorithm the certificate was signed with
	 */
	public void setSignatureAlgorithm(final String signatureAlgorithm)
	{
		this.signatureAlgorithm = signatureAlgorithm;
	}

	/**
	 * Gets the fingerprint of the certificate
	 *
	 * @return the fingerprint of the certificate
	 */
	public String getFingerprint()
	{
		return fingerprint;
	}

	/**
	 * Sets the fingerprint of the certificate
	 *
	 * @param fingerprint
	 *            the fingerprint of the certificate
	 */
	public void setFingerprint(final String fingerprint)
	{
		this.fingerprint = fingerprint;
	}

	/**
	 * Gets the algorithm the fingerprint was taken with
	 *
	 * @return the algorithm the fingerprint was taken with
	 */
	public String getFingerprintAlgorithm()
	{
		return fingerprintAlgorithm;
	}

	/**
	 * Sets the algorithm the fingerprint was taken with
	 *
	 * @param fingerprintAlgorithm
	 *            the algorithm the fingerprint was taken with
	 */
	public void setFingerprintAlgorithm(final String fingerprintAlgorithm)
	{
		this.fingerprintAlgorithm = fingerprintAlgorithm;
	}

	/**
	 * Gets the public key the certificate carries
	 *
	 * @return the public key the certificate carries
	 */
	public String getPublicKey()
	{
		return publicKey;
	}

	/**
	 * Sets the public key the certificate carries
	 *
	 * @param publicKey
	 *            the public key the certificate carries
	 */
	public void setPublicKey(final String publicKey)
	{
		this.publicKey = publicKey;
	}
}
