package de.alpharogroup.mystic.crypt.panels.signin;

import java.io.File;
import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * The bean class {@link MasterPwFileModelBean} is for holding the sign in data
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterPwFileModelBean implements Serializable
{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The key file. */
	File keyFile;

	/** The master password char array. */
	char[] masterPw;

	/** The flag if the key file will be used in the authentication. */
	boolean withKeyFile;

	/** The flag if the master password will be used in the authentication. */
	boolean withMasterPw;
	
	/** The flag if the master password is displayed in plain text. */
	boolean showMasterPw;
}
