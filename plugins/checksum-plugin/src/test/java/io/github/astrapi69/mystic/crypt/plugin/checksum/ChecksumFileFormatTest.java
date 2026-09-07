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

package io.github.astrapi69.mystic.crypt.plugin.checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A checksum file does not hold a bare hash. sha256sum and everything else following coreutils
 * write "<hash>  <filename>", and the tool used to compare that whole line against the hash it had
 * computed - so verifying this project's own release said "No Match" for a file that was intact
 * (#229).
 */
class ChecksumFileFormatTest
{

	private static final String HASH = "c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626";

	@ParameterizedTest(name = "the hash is read out of: {0}")
	@ValueSource(strings = {
			"c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626",
			"c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626  mystic-crypt-ui-8.2-installer.jar",
			"c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626 *mystic-crypt-ui-8.2-installer.jar",
			"  c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626  file.jar  \n",
			"SHA256 (mystic-crypt-ui-8.2-installer.jar) = c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626" })
	void theHashIsReadOutOfEveryFormatAChecksumFileComesIn(final String content)
	{
		assertEquals(HASH, ChecksumSupport.hashFrom(content));
	}

	@Test
	@DisplayName("the file this project published verifies against the file it belongs to")
	void theReleaseChecksumFileVerifies()
	{
		String published = HASH + "  mystic-crypt-ui-8.2-installer.jar";

		assertTrue(ChecksumSupport.matches(ChecksumSupport.hashFrom(published), HASH),
			"the checksum published next to the 8.2 installer must verify it");
	}

	@Test
	@DisplayName("an upper case hash from a download page verifies too")
	void anUpperCaseHashVerifies()
	{
		assertTrue(ChecksumSupport.matches(ChecksumSupport.hashFrom(HASH.toUpperCase()), HASH));
	}

	@Test
	void nothingIsNotAChecksum()
	{
		assertNull(ChecksumSupport.hashFrom(null));
		assertNull(ChecksumSupport.hashFrom("   "));
	}

	@Test
	@DisplayName("a wrong checksum still says so")
	void aWrongChecksumStillFails()
	{
		String wrong = "d00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626  file.jar";

		org.junit.jupiter.api.Assertions.assertFalse(
			ChecksumSupport.matches(ChecksumSupport.hashFrom(wrong), HASH));
	}
}
