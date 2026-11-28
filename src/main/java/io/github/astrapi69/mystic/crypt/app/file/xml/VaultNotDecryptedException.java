/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.security.GeneralSecurityException;

/**
 * A decryption that authenticates nothing did not decrypt the database (#1188): its padding check
 * refused the key, or let it through and what came out is content a wrong key could leave - no xml
 * at all, or a root the reader fails on before it converts anything.
 * <p>
 * Such a decryption ends with a padding check and nothing else, and a wrong key passes that check
 * about once in 256 tries, measured in {@code WrongPasswordsWithoutAuthenticationTest}. What comes
 * out then is noise, and calling it a database that was decrypted but cannot be read (#624) told
 * whoever typed a wrong password that the password was right. So it is a failed decryption, which
 * the sign-in reports in the unlock prompt's words, as it does when the padding check refuses the
 * key (#1005). Without authentication a damaged file reads the same way, and there is nothing that
 * could tell the two apart.
 * <p>
 * It carries no cause and always the same message.
 */
public class VaultNotDecryptedException extends GeneralSecurityException
{

	private static final long serialVersionUID = 1L;

	/** A decryption without authentication that did not decrypt the database */
	public VaultNotDecryptedException()
	{
		super("A decryption that authenticates nothing did not decrypt the database: the password "
			+ "or the key is wrong, or the file is damaged.");
	}
}
