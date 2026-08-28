/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyHexDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyHexEncryptor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Everything the key generation tool holds: the algorithm and the parameters a key pair is
 * generated with, the generated key pair itself with the PEM text of both keys, the format a
 * private key file is written in and the hex encryptor and decryptor the RSA demo runs on.
 * <p>
 * The panels bind their components to this object, so what the user chose is readable here at any
 * moment - no button handler has to ask a widget for its content.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateKeysModelBean implements Serializable
{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	PrivateKeyHexDecryptor decryptor;

	PublicKeyHexEncryptor encryptor;

	/** The key size a size based algorithm generates its key pair with */
	KeySize keySize;

	/** The algorithm a key pair is generated with */
	KeyPairGeneratorAlgorithm algorithm;

	/** The curve an EC key sits on */
	String curve;

	/** The format a private key file is written in */
	KeyFormat keyFormat;

	PrivateKey privateKey;

	PublicKey publicKey;

	/** The generated private key as PEM, the text the private key area shows */
	String privateKeyPem;

	/** The generated public key as PEM, the text the public key area shows */
	String publicKeyPem;

	String password;
	String signatureAlgorithm;
	Date start;
	Date end;
}
