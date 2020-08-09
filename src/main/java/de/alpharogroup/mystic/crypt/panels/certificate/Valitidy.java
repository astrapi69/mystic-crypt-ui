package de.alpharogroup.mystic.crypt.panels.certificate;


import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Use only ZoneId.of("UTC") for the values
 **/
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Valitidy
{
	ZonedDateTime notBefore;
	ZonedDateTime notAfter;
}
