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
package io.github.astrapi69.mystic.crypt.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The passphrase reaches {@link PassphraseBox} as characters, not as a {@link String} (#294).
 * <p>
 * A String cannot be overwritten and lives until the collector reaches it, so the sign-in panel
 * keeps the master password in a character array. Every caller above the vault format then turned
 * it back into a String to use it, which threw that away at the last moment - measured on #294 as a
 * copy of the master password still lying in the heap after the vault had been closed.
 * <p>
 * The String overloads stay: two library paths (the key file with a password, and the pre-8.3
 * format) take a String and cannot take anything else from here. What they must not do is disagree
 * with the character overloads, which is what the cross tests below pin.
 */
class PassphraseBoxTest
{

	private static final byte[] MAGIC = "TESTMC".getBytes(StandardCharsets.US_ASCII);

	private static final byte[] PLAINTEXT = "the vault's content".getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("what a character passphrase encrypted, the same characters decrypt")
	void encryptedWithCharacters_isReadBack_byTheSameCharacters() throws Exception
	{
		char[] passphrase = TestPasswords.throwawayChars();

		byte[] encrypted = PassphraseBox.encrypt(MAGIC, PLAINTEXT, passphrase);

		assertArrayEquals(PLAINTEXT, PassphraseBox.decrypt(MAGIC, encrypted, passphrase));
	}

	@Test
	@DisplayName("characters and String describe the same passphrase, in both directions")
	void theTwoOverloads_agree_soExistingFilesStayReadable() throws Exception
	{
		String passphrase = TestPasswords.throwaway();

		byte[] encryptedAsString = PassphraseBox.encrypt(MAGIC, PLAINTEXT, passphrase);
		byte[] encryptedAsChars = PassphraseBox.encrypt(MAGIC, PLAINTEXT, passphrase.toCharArray());

		assertArrayEquals(PLAINTEXT,
			PassphraseBox.decrypt(MAGIC, encryptedAsString, passphrase.toCharArray()),
			"a vault written before this change must open afterwards");
		assertArrayEquals(PLAINTEXT, PassphraseBox.decrypt(MAGIC, encryptedAsChars, passphrase),
			"and a vault written after it must open in a build from before");
	}

	@Test
	@DisplayName("the caller's passphrase array comes back untouched")
	void encryptAndDecrypt_leaveTheCallersArray_asItWas() throws Exception
	{
		char[] passphrase = TestPasswords.throwawayChars();
		char[] asHandedOver = passphrase.clone();

		byte[] encrypted = PassphraseBox.encrypt(MAGIC, PLAINTEXT, passphrase);
		PassphraseBox.decrypt(MAGIC, encrypted, passphrase);

		assertArrayEquals(asHandedOver, passphrase,
			"whoever owns the array decides when it is overwritten. A callee that wipes it takes "
				+ "the master password away from the next save");
	}

	@Test
	@DisplayName("a wrong character passphrase fails as a wrong passphrase")
	void aWrongPassphrase_fails_ratherThanReturningRubbish() throws Exception
	{
		byte[] encrypted = PassphraseBox.encrypt(MAGIC, PLAINTEXT, TestPasswords.throwawayChars());

		assertThrows(Exception.class,
			() -> PassphraseBox.decrypt(MAGIC, encrypted, TestPasswords.throwawayChars()));
	}
}
