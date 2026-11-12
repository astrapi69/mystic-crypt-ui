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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * What a locked workspace keeps of its key file instead of the key itself (#880): enough to
 * recognise the key when the key file is read again, nothing the key could be rebuilt from
 */
class KeyFileVerifierTest
{

	private static byte[] someKey()
	{
		byte[] key = new byte[64];
		new SecureRandom().nextBytes(key);
		return key;
	}

	@Test
	@DisplayName("the key it was made from matches, another key does not")
	void theSameKey_matches_andAnotherDoesNot()
	{
		byte[] key = someKey();
		KeyFileVerifier verifier = KeyFileVerifier.of(key.clone());

		assertTrue(verifier.matches(key));
		assertFalse(verifier.matches(someKey()));
	}

	@Test
	@DisplayName("a key that differs in its last byte does not match")
	void aKeyDifferingInOneByte_doesNotMatch()
	{
		byte[] key = someKey();
		KeyFileVerifier verifier = KeyFileVerifier.of(key.clone());
		key[key.length - 1] ^= 1;

		assertFalse(verifier.matches(key));
	}

	@ParameterizedTest(name = "\"{0}\" matches nothing")
	@NullAndEmptySource
	@DisplayName("no key at all matches nothing")
	void noKey_matchesNothing(final byte[] candidate)
	{
		assertFalse(KeyFileVerifier.of(someKey()).matches(candidate));
	}

	@ParameterizedTest(name = "\"{0}\" makes no verifier")
	@NullAndEmptySource
	@DisplayName("a verifier needs a key to be made from")
	void aVerifier_needsAKey(final byte[] key)
	{
		assertThrows(IllegalArgumentException.class, () -> KeyFileVerifier.of(key));
	}

	@Test
	@DisplayName("a wiped verifier matches nothing any more, not even its own key")
	void aWipedVerifier_matchesNothing()
	{
		byte[] key = someKey();
		KeyFileVerifier verifier = KeyFileVerifier.of(key.clone());

		verifier.wipe();

		assertFalse(verifier.matches(key));
	}

	@Test
	@DisplayName("two verifiers of one key hold different digests, because each has its own salt")
	void twoVerifiersOfOneKey_differ()
	{
		byte[] key = someKey();

		assertFalse(KeyFileVerifier.of(key.clone()).holdsTheSameDigestAs(KeyFileVerifier.of(key)));
	}
}
