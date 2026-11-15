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

import java.util.List;
import java.util.Locale;

/**
 * What a test passes to a JVM it starts itself, so that the forked JVM speaks the test JVM's
 * language. Every test JVM speaks English in the United States form whatever the desktop speaks
 * (gradle/test-jvms.gradle, #615), but through system properties, and a forked JVM does not inherit
 * them: it takes its language from the environment. On a German desktop java.util.logging then
 * writes {@code WARNUNG:} and {@code INFORMATION:} where the assertions read {@code WARNING:} and
 * {@code INFO:} (#990, and the command line's standard error test after it).
 */
public final class ForkedJvm
{

	private ForkedJvm()
	{
	}

	/**
	 * The options that give a forked JVM this JVM's language and country
	 *
	 * @return {@code -Duser.language} and {@code -Duser.country} of {@link Locale#getDefault()}
	 */
	public static List<String> localeOptions()
	{
		Locale locale = Locale.getDefault();
		return List.of("-Duser.language=" + locale.getLanguage(),
			"-Duser.country=" + locale.getCountry());
	}
}
