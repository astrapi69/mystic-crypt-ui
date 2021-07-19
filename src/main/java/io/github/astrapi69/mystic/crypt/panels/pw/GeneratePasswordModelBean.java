package io.github.astrapi69.mystic.crypt.panels.pw;

import io.github.astrapi69.mystic.crypt.panels.signin.MasterPwFileModelBean;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * The bean class {@link GeneratePasswordModelBean} is holding the data for generate custom password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratePasswordModelBean
{
	int passwordLength;
	/** The password char array. */
	char[] password;
	boolean lowercase;
	boolean uppercase;
	boolean special;
	boolean moreSpecial;
	boolean plus;
	boolean digits;
	boolean underscore;
	boolean brackets;
	boolean whitespace;
	boolean minus;
}
