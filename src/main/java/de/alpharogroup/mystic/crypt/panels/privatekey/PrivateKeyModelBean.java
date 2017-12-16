package de.alpharogroup.mystic.crypt.panels.privatekey;

import java.io.File;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

import de.alpharogroup.crypto.key.KeySize;
import de.alpharogroup.crypto.key.PrivateKeyHexDecryptor;
import de.alpharogroup.crypto.key.PublicKeyHexEncryptor;
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
 * The class {@link PrivateKeyModelBean}.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level=AccessLevel.PRIVATE)
public class PrivateKeyModelBean implements Serializable
{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	KeySize keySize;

	int keyLength;

	PrivateKey privateKey;

	PublicKey publicKey;

	PublicKeyHexEncryptor encryptor;

	PrivateKeyHexDecryptor decryptor;

	/** The key file. */
	File privateKeyFile;
}
