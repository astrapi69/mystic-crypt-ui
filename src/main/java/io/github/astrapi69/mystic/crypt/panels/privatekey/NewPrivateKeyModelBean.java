package io.github.astrapi69.mystic.crypt.panels.privatekey;

import java.io.File;
import java.security.PrivateKey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import io.github.astrapi69.crypto.key.KeySize;
import lombok.experimental.SuperBuilder;

/**
 * The class {@link PrivateKeyModelBean}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewPrivateKeyModelBean
{
	int keyLength;

	KeySize keySize;

	PrivateKey privateKey;

	/** The private key directory */
	File privateKeyDirectory;

	String filenameOfPrivateKey;

	/** The private key file */
	File privateKeyFile;
}
