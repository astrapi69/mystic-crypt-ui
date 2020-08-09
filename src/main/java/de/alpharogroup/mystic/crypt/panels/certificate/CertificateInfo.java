package de.alpharogroup.mystic.crypt.panels.certificate;


import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateInfo
{
	int version;
	BigInteger serialNumber;
	String issuer;
	String subject;
	Valitidy valitidy;
	PublicKey publicKey;
	String signatureAlgorithm;
	Map<String, String> x509v3Extensions;
}
