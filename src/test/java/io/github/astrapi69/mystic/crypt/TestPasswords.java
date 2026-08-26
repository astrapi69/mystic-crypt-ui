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
package io.github.astrapi69.mystic.crypt;

import java.util.UUID;

/**
 * Throwaway credentials for tests.
 * <p>
 * A test that needs a password almost always builds the thing that password protects: a database in
 * a temporary directory, a key store, an encrypted file. Such a password guards nothing beyond the
 * test run, and writing one into the source makes it look exactly like a credential that does guard
 * something - to a reader, and to the secret scanner that reads every pull request.
 * <p>
 * So they are made up per run. The one exception is the password of a fixture that is checked in
 * and therefore cannot change; it lives here, in one place, with its reason next to it.
 */
public final class TestPasswords
{

	/**
	 * The password of the checked-in KeePass fixture {@code src/test/resources/test-db.kdbx}.
	 * <p>
	 * This one cannot be made up: the file it opens is in the repository and was encrypted with it.
	 * It protects a database of invented entries and guards nothing else.
	 */
	public static final String KEEPASS_FIXTURE = "foo-secret-bar-1969-?";

	private TestPasswords()
	{
	}

	/**
	 * A password for something the test itself creates, different on every run
	 *
	 * @return the password
	 */
	public static String throwaway()
	{
		return "throwaway-" + UUID.randomUUID();
	}

	/**
	 * The same as {@link #throwaway()}, for the interfaces that take a character array
	 *
	 * @return the password
	 */
	public static char[] throwawayChars()
	{
		return throwaway().toCharArray();
	}
}
