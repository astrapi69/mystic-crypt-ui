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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The notice of the third-party works the application ships (#1007): the silk icons ask for their
 * author and their license to be named, and so do the Apache-2.0 libraries that bring no license
 * file of their own
 */
class ThirdPartyNoticeTest
{

	/** The SHA-256 of the Apache License 2.0 as apache.org publishes it, LICENSE-2.0.txt */
	private static final String APACHE_2_0_TEXT_SHA_256 = "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30";

	/** Where the license text starts in the notice */
	private static final String SEPARATOR = "--------------------------------------------------------------------------------\n";

	/** The Apache-2.0 libraries the notice names, each with a class it brings */
	private static final Map<String, String> APACHE_LIBRARIES = Map.of("com.google.code.gson:gson",
		"com.google.gson.Gson", "info.picocli:picocli", "picocli.CommandLine",
		"com.google.code.findbugs:jsr305", "javax.annotation.Nonnull", "org.jspecify:jspecify",
		"org.jspecify.annotations.Nullable", "io.github.kostaskougios:cloning",
		"com.rits.cloning.Cloner");

	@Test
	@DisplayName("the notice credits the silk icons with their author, their page and their license")
	void theNoticeCreditsTheSilkIcons() throws IOException
	{
		String notice = notice();

		assertTrue(notice.contains("\"Silk\" icon set, version 1.3, by Mark James"), notice);
		assertTrue(notice.contains("http://www.famfamfam.com/lab/icons/silk/"),
			"the link back the author asks for in the credits");
		assertTrue(
			notice.contains("Creative Commons Attribution 2.5 License")
				&& notice.contains("http://creativecommons.org/licenses/by/2.5/"),
			"the license, by name and by address, as CC BY 2.5 section 4(a) asks");
	}

	@Test
	@DisplayName("the notice names the Apache-2.0 libraries the jar ships and carries the license text")
	void theNoticeNamesTheApacheLibrariesAndCarriesTheLicenseText()
		throws IOException, NoSuchAlgorithmException
	{
		String notice = notice();

		APACHE_LIBRARIES.forEach((coordinates, aClass) -> {
			assertTrue(notice.contains("(" + coordinates + ")"), coordinates + " is not named");
			assertDoesNotThrow(() -> Class.forName(aClass),
				coordinates + " is named but not shipped: " + aClass + " is not on the class path");
		});
		int start = notice.indexOf(SEPARATOR);
		assertTrue(start > 0, "the license text follows the separator");
		byte[] licenseText = notice.substring(start + SEPARATOR.length())
			.getBytes(StandardCharsets.UTF_8);
		assertEquals(APACHE_2_0_TEXT_SHA_256,
			HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(licenseText)),
			"the Apache License 2.0 text, word for word, as section 4(a) asks a copy of it");
	}

	@Test
	@DisplayName("the notice names where the curated licenses are (#1014)")
	void theNoticeNamesTheCuratedLicenses() throws IOException
	{
		assertTrue(notice().contains("legal/third-party/<group>/<artifact>/"),
			"where the curated licenses are");
	}

	private static String notice() throws IOException
	{
		try (InputStream in = ThirdPartyNoticeTest.class.getClassLoader()
			.getResourceAsStream("legal/NOTICE.txt"))
		{
			assertNotNull(in, "legal/NOTICE.txt is on the class path, so it is in the jar");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
