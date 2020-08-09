package de.alpharogroup.mystic.crypt.panels.certificate;


import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.Date;

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
	Date validFrom;
	Date validUntil;
	String signatureAlgorithm;
	String fingerprint;
	String publicKey;
}
