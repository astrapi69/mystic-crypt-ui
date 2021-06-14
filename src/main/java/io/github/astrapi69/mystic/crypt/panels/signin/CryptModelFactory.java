/**
 * The MIT License
 * <p>
 * Copyright (C) 2015 Asterios Raptis
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panels.signin;

import io.github.astrapi69.crypto.algorithm.Algorithm;
import io.github.astrapi69.crypto.model.CryptModel;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * The factory class {@link CryptModelFactory} holds methods for creating {@link CryptModel} objects
 * @deprecated replace with same name class when version 7.6 crypt-data comes out
 */
@Deprecated public class CryptModelFactory
{

	private CryptModelFactory()
	{
	}

	/**
	 * Factory method for create a new {@link CryptModel} from the given key
	 *
	 * @param <C>
	 *            the generic type of the cipher
	 * @param <K>
	 *            the generic type of the key
	 * @param <T>
	 *            the generic type of the decorator objects
	 * @param key
	 *            the key
	 * @return the new created {@link CryptModel} object
	 */
	public static <C, K, T> CryptModel<C, K, T> of(K key)
	{
		return CryptModel.<C, K, T>builder().key(key).build();
	}

	/**
	 * Factory method for create a new {@link CryptModel} from the given {@link PublicKey}
	 *
	 * @param publicKey
	 *            the public key
	 * @return the new created {@link CryptModel} object
	 */
	public static CryptModel<Cipher, PublicKey, byte[]> newCryptModel(PublicKey publicKey)
	{
		return of(publicKey);
	}

	/**
	 * Factory method for create a new {@link CryptModel} from the given {@link PrivateKey}
	 *
	 * @param privateKey
	 *            the private key
	 * @return the new created {@link CryptModel} object
	 */
	public static CryptModel<Cipher, PrivateKey, byte[]> newCryptModel(PrivateKey privateKey)
	{
		return of(privateKey);
	}

	/**
	 * Factory method for create a new {@link CryptModel} from the given {@link Algorithm} and the
	 * given key as {@link String} object. The key have to fulfil the requirements for instance the
	 * length have to be 16 characters
	 *
	 * Examples: for the key: 'D1D15ED36B887AF1' <code>
	 *     String key;
	 *     key = "1234567890123456";
	 *     // or
	 *     key = "D1D15ED36B887AF1";
	 * </code>
	 *
	 * @param key
	 *            the key
	 * @param algorithm
	 *            the algorithm
	 * @return the new created {@link CryptModel} object
	 */
	public static CryptModel<Cipher, String, String> newCryptModel(Algorithm algorithm, String key)
	{
		return CryptModel.<Cipher, String, String>builder().key(key).algorithm(algorithm).build();
	}

}
