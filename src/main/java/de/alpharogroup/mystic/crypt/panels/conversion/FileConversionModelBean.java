package de.alpharogroup.mystic.crypt.panels.conversion;

import java.io.File;
import java.io.Serializable;

import de.alpharogroup.crypto.key.KeyType;
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
 * The class {@link FileConversionModelBean}.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileConversionModelBean implements Serializable
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The der file. */
	File derFile;

	/** The pem file. */
	File pemFile;

	/** The key type. */
	@Builder.Default
	KeyType keyType = KeyType.PRIVATE_KEY;

}
