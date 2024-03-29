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
package io.github.astrapi69.mystic.crypt.panel.privatekey;

import java.io.File;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyHexDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyHexEncryptor;

/**
 * The class {@link PrivateKeyModelBean}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrivateKeyModelBean implements Serializable
{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	PrivateKeyHexDecryptor decryptor;

	PublicKeyHexEncryptor encryptor;

	int keyLength;

	KeySize keySize;

	PrivateKey privateKey;

	/** The key file. */
	File privateKeyFile;

	PublicKey publicKey;
}
