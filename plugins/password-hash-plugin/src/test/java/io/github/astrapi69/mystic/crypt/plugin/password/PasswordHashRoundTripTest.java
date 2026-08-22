/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.plugin.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.pw.PasswordEncryptor;

/**
 * Headless round-trip test of the password hashing the plugin's panel drives: hashing then
 * verifying with the correct and a wrong password, for both Argon2id and PBKDF2.
 */
class PasswordHashRoundTripTest
{

	@Test
	void argon2idHashesAndVerifies()
	{
		PasswordEncryptor passwordEncryptor = PasswordEncryptor.getInstance();
		String encodedHash = passwordEncryptor.hashPasswordArgon2id("secret-password-1");

		assertNotNull(encodedHash);
		assertTrue(passwordEncryptor.matchArgon2id("secret-password-1", encodedHash),
			"the correct password must verify");
		assertFalse(passwordEncryptor.matchArgon2id("wrong-password", encodedHash),
			"a wrong password must not verify");
	}

	@Test
	void pbkdf2HashesAndVerifies()
	{
		PasswordEncryptor passwordEncryptor = PasswordEncryptor.getInstance();
		String encodedHash = passwordEncryptor.hashPasswordPbkdf2("secret-password-2");

		assertNotNull(encodedHash);
		assertTrue(passwordEncryptor.matchPbkdf2("secret-password-2", encodedHash),
			"the correct password must verify");
		assertFalse(passwordEncryptor.matchPbkdf2("wrong-password", encodedHash),
			"a wrong password must not verify");
	}
}
