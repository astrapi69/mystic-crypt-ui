package io.github.astrapi69.mystic.crypt.app.file;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

public class KeyModelExtensions
{

	/**
	 * Reads the given byte array with the given algorithm and returns the {@link PrivateKey}
	 * object.
	 *
	 * @param keyModel
	 *            the info model for create the private key
	 * @return the {@link PrivateKey} object
	 * @throws NoSuchAlgorithmException
	 *             is thrown if instantiation of the cypher object fails.
	 * @throws InvalidKeySpecException
	 *             is thrown if generation of the SecretKey object fails.
	 * @deprecated only temporary method. will be replaced from module crypt-data
	 */
	public static PrivateKey readPrivateKey(KeyModel keyModel)
	{
		Objects.requireNonNull(keyModel);
		if (!keyModel.getKeyType().equals(KeyType.PRIVATE_KEY))
		{
			throw new RuntimeException(
				"Given KeyModel:" + keyModel.toString() + "\n is not a private key");
		}
		return RuntimeExceptionDecorator.decorate(
			() -> PrivateKeyReader.readPrivateKey(keyModel.getEncoded(), keyModel.getAlgorithm()));
	}

	public static KeyModel toKeyModel(PrivateKey privateKey)
	{
		return KeyModel.builder().encoded(privateKey.getEncoded()).keyType(KeyType.PRIVATE_KEY)
			.algorithm(privateKey.getAlgorithm()).build();
	}
}
