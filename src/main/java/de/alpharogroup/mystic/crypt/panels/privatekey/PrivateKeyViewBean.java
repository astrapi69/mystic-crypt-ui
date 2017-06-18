package de.alpharogroup.mystic.crypt.panels.privatekey;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

import de.alpharogroup.crypto.key.KeySize;
import de.alpharogroup.crypto.key.PrivateKeyHexDecryptor;
import de.alpharogroup.crypto.key.PublicKeyHexEncryptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The class {@link PrivateKeyViewBean}.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PrivateKeyViewBean
{

	private KeySize keySize;

	private int keyLength;

	private PrivateKey privateKey;

	private PublicKey publicKey;

	private PublicKeyHexEncryptor encryptor;

	private PrivateKeyHexDecryptor decryptor;

	/** The private key file. */
	private File privateKeyFile;
}
