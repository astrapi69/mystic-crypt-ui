package de.alpharogroup.mystic.crypt.panels.certificate;

import org.junit.Test;

import static org.junit.Assert.*;

public class CertificateAttributesTest {

    @Test
    public void testToRepresentableString() {
        String actual;
        String expected;
        CertificateAttributes certificateAttributes;

        certificateAttributes = CertificateAttributes.builder().build();
        actual = certificateAttributes.toRepresentableString();
        expected = "";
        assertEquals(actual, expected);

        certificateAttributes = CertificateAttributes.builder()
                .countryCode("GR")
                .build();
        actual = certificateAttributes.toRepresentableString();
        expected = "C=GR";
        assertEquals(actual, expected);

        certificateAttributes = CertificateAttributes.builder()
                .countryCode("GR")
                .state("Pieria")
                .build();
        actual = certificateAttributes.toRepresentableString();
        expected = "C=GR, ST=Pieria";
        assertEquals(actual, expected);

        certificateAttributes = CertificateAttributes.builder()
                .countryCode("GR")
                .state("Pieria")
                .organisation("Alpha Ro Group Ltd")
                .build();
        actual = certificateAttributes.toRepresentableString();
        expected = "C=GR, ST=Pieria, O=Alpha Ro Group Ltd";
        assertEquals(actual, expected);

        certificateAttributes = CertificateAttributes.builder()
                .countryCode("GR")
                .state("Pieria")
                .organisation("Alpha Ro Group Ltd")
                .organisationUnit("Certificate Authority")
                .build();
        actual = certificateAttributes.toRepresentableString();
        expected = "C=GR, ST=Pieria, O=Alpha Ro Group Ltd, OU=Certificate Authority";
        assertEquals(actual, expected);

        certificateAttributes = CertificateAttributes.builder()
                .countryCode("GR")
                .state("Pieria")
                .organisation("Alpha Ro Group Ltd")
                .organisationUnit("Certificate Authority")
                .commonName("asterios.raptis@web.de")
                .build();
        actual = certificateAttributes.toRepresentableString();
        expected = "C=GR, ST=Pieria, O=Alpha Ro Group Ltd, OU=Certificate Authority, CN=asterios.raptis@web.de";
        assertEquals(actual, expected);
    }
}
