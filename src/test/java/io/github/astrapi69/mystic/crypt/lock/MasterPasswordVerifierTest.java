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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locking keeps the master password in memory only because unlocking compares against it. This is
 * what replaces that: enough to recognise the password again, not enough to be it (#242)
 */
class MasterPasswordVerifierTest
{

	private static final char[] MASTER_PASSWORD = "correct horse battery staple".toCharArray();

	@Test
	@DisplayName("the password it was made from is recognised again")
	void recognisesTheSamePassword() throws Exception
	{
		assertTrue(MasterPasswordVerifier.of(MASTER_PASSWORD).matches(MASTER_PASSWORD.clone()));
	}

	@Test
	@DisplayName("any other password is refused")
	void refusesADifferentPassword() throws Exception
	{
		MasterPasswordVerifier verifier = MasterPasswordVerifier.of(MASTER_PASSWORD);

		assertFalse(verifier.matches("correct horse battery stapl".toCharArray()),
			"one character short is a different password");
		assertFalse(verifier.matches("Correct horse battery staple".toCharArray()), "case matters");
		assertFalse(verifier.matches(new char[0]), "the empty password is not it");
		assertFalse(verifier.matches(null), "no password at all is not it either");
	}

	@Test
	@DisplayName("the verifier does not carry the password it was made from")
	void doesNotCarryThePassword() throws Exception
	{
		MasterPasswordVerifier verifier = MasterPasswordVerifier.of(MASTER_PASSWORD);

		assertFalse(verifier.toString().contains("staple"),
			"a verifier that prints the secret would put it into every log line that logs it");
	}

	@Test
	@DisplayName("two verifiers for the same password are not the same bytes")
	void twoVerifiersForTheSamePasswordDiffer() throws Exception
	{
		MasterPasswordVerifier first = MasterPasswordVerifier.of(MASTER_PASSWORD);
		MasterPasswordVerifier second = MasterPasswordVerifier.of(MASTER_PASSWORD);

		assertNotEquals(first.fingerprint(), second.fingerprint(),
			"each verifier draws its own salt, so the same password derives to different bytes");
		assertTrue(second.matches(MASTER_PASSWORD.clone()), "and both still recognise it");
	}

	@Test
	@DisplayName("a verifier cannot be made without a password")
	void refusesToBeMadeWithoutAPassword()
	{
		assertThrows(IllegalArgumentException.class, () -> MasterPasswordVerifier.of(null));
		assertThrows(IllegalArgumentException.class, () -> MasterPasswordVerifier.of(new char[0]));
	}
}
