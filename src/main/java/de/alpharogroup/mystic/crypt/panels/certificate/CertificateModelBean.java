package de.alpharogroup.mystic.crypt.panels.certificate;


import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateModelBean
{
	String issuedTo;
	String issuedBy;
	String version;
	String serialNumber;
	String validFrom;
	String validUntil;
	String signatureAlgorithm;
	String fingerprint;
	String publicKey;
}
