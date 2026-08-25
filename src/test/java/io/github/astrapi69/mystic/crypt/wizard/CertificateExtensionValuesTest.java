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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the extension values a certificate can carry: what someone types has to become real DER,
 * and what cannot be made sense of has to be refused rather than written into a certificate as
 * rubbish.
 */
class CertificateExtensionValuesTest
{

	@ParameterizedTest
	@CsvSource({ "CA:true,true,-1", "CA:false,false,-1", "CA:true pathlen:2,true,2",
			"ca:TRUE,true,-1", " CA:true , true,-1" })
	void readsBasicConstraints(String value, boolean certificateAuthority, int pathLength)
	{
		BasicConstraints constraints = CertificateExtensionValues
			.basicConstraints(value.replace(" pathlen", ",pathlen"));

		assertEquals(certificateAuthority, constraints.isCA());
		if (0 <= pathLength)
		{
			assertEquals(pathLength, constraints.getPathLenConstraint().intValue());
		}
	}

	@Test
	void aPathLengthWithoutBeingACertificateAuthorityIsIgnored()
	{
		BasicConstraints constraints = CertificateExtensionValues
			.basicConstraints("CA:false,pathlen:3");

		assertFalse(constraints.isCA());
		assertEquals(null, constraints.getPathLenConstraint(),
			"a path length only means something for a certificate authority");
	}

	@ParameterizedTest
	@ValueSource(strings = { "CA", "CA:true,pathlen:x", "nonsense:true", "" })
	void refusesBasicConstraintsThatMakeNoSense(String value)
	{
		assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.basicConstraints(value));
	}

	@ParameterizedTest
	@CsvSource({ "digitalSignature", "keyCertSign", "cRLSign", "keyEncipherment", "keyAgreement",
			"nonRepudiation", "contentCommitment", "dataEncipherment", "encipherOnly",
			"decipherOnly" })
	void readsEveryKeyUsageByName(String name)
	{
		assertTrue(CertificateExtensionValues.keyUsage(name)
			.hasUsages(CertificateExtensionValues.keyUsage(name).getBytes()[0] & 0xff));
	}

	@Test
	void readsSeveralKeyUsagesAtOnce()
	{
		KeyUsage usage = CertificateExtensionValues
			.keyUsage("digitalSignature,keyCertSign,cRLSign");

		assertTrue(usage.hasUsages(KeyUsage.digitalSignature));
		assertTrue(usage.hasUsages(KeyUsage.keyCertSign));
		assertTrue(usage.hasUsages(KeyUsage.cRLSign));
		assertFalse(usage.hasUsages(KeyUsage.keyAgreement));
	}

	@ParameterizedTest
	@ValueSource(strings = { "somethingElse", "", "digitalSignature,nope" })
	void refusesAKeyUsageThatDoesNotExist(String value)
	{
		assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.keyUsage(value));
	}

	@ParameterizedTest
	@CsvSource({ "DNS:example.com,2", "IP:10.0.0.1,7", "email:me@example.com,1",
			"URI:https://example.com,6" })
	void readsEverySubjectAlternativeNameType(String value, int expectedTag)
	{
		GeneralNames names = CertificateExtensionValues.subjectAlternativeName(value);

		assertEquals(1, names.getNames().length);
		assertEquals(expectedTag, names.getNames()[0].getTagNo());
	}

	@Test
	void readsSeveralAlternativeNamesAtOnce()
	{
		GeneralNames names = CertificateExtensionValues
			.subjectAlternativeName("DNS:example.com,DNS:www.example.com,IP:10.0.0.1");

		assertEquals(3, names.getNames().length);
		assertEquals(GeneralName.dNSName, names.getNames()[0].getTagNo());
		assertEquals(GeneralName.iPAddress, names.getNames()[2].getTagNo());
	}

	@ParameterizedTest
	@ValueSource(strings = { "example.com", "FTP:example.com", "" })
	void refusesAnAlternativeNameThatIsNotTyped(String value)
	{
		assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.subjectAlternativeName(value));
	}

	@Test
	void buildsARealExtensionForEachUnderstoodKind()
	{
		Extension basicConstraints = CertificateExtensionValues
			.toExtension(CertificateExtensionValues.BASIC_CONSTRAINTS, true, "CA:true,pathlen:0");
		assertEquals(Extension.basicConstraints, basicConstraints.getExtnId());
		assertTrue(basicConstraints.isCritical());
		assertTrue(BasicConstraints.getInstance(basicConstraints.getParsedValue()).isCA(),
			"the extension must parse back to what was asked for");

		Extension keyUsage = CertificateExtensionValues.toExtension(
			CertificateExtensionValues.KEY_USAGE, true, "digitalSignature,keyCertSign");
		assertTrue(KeyUsage.getInstance(keyUsage.getParsedValue()).hasUsages(KeyUsage.keyCertSign));

		Extension alternativeName = CertificateExtensionValues.toExtension(
			CertificateExtensionValues.SUBJECT_ALTERNATIVE_NAME, false, "DNS:example.com");
		assertFalse(alternativeName.isCritical());
		assertEquals(1,
			GeneralNames.getInstance(alternativeName.getParsedValue()).getNames().length);
	}

	@ParameterizedTest
	@ValueSource(strings = { "30030101ff", "30 03 01 01 ff", "0x30030101ff", "30:03:01:01:ff" })
	void takesAnUnknownExtensionAsHex(String value)
	{
		Extension extension = CertificateExtensionValues.toExtension("1.3.6.1.4.1.99999.1", false,
			value);

		assertEquals("1.3.6.1.4.1.99999.1", extension.getExtnId().getId());
		assertEquals(5, extension.getExtnValue().getOctets().length);
	}

	@ParameterizedTest
	@ValueSource(strings = { "not hex", "abc", "" })
	void refusesAnUnknownExtensionThatIsNotHex(String value)
	{
		assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.toExtension("1.3.6.1.4.1.99999.1", false, value));
	}

	@Test
	void refusesAnExtensionWithoutAnObjectId()
	{
		assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.toExtension("", false, "CA:true"));
		assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.toExtension(null, false, "CA:true"));
	}

	@Test
	void saysWhatItUnderstandsAndHowItLooks()
	{
		assertEquals(3, CertificateExtensionValues.understoodExtensionIds().size());
		assertEquals("Basic Constraints",
			CertificateExtensionValues.displayName(CertificateExtensionValues.BASIC_CONSTRAINTS));
		assertEquals("1.2.3", CertificateExtensionValues.displayName("1.2.3"),
			"an extension it does not know is shown by its object id");
		assertTrue(CertificateExtensionValues.valueHint(CertificateExtensionValues.KEY_USAGE)
			.contains("digitalSignature"));
		assertTrue(CertificateExtensionValues.valueHint("1.2.3").contains("hex"));
	}

	@Test
	void theMessageOfARefusalSaysWhatWasExpected()
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> CertificateExtensionValues.toExtension(CertificateExtensionValues.KEY_USAGE, true,
				"nonsense"));

		assertTrue(exception.getMessage().contains("Key Usage"), exception.getMessage());
		assertTrue(exception.getMessage().contains("digitalSignature"), exception.getMessage());
	}
}
