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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;

/**
 * Headless proof of the checksum computation the plugin's {@link ChecksumPanel} performs: it must
 * produce the well-known MD5 and SHA-256 digests for a file whose content is the ASCII string
 * "abc". This guards the exact library call ({@link FileChecksumExtensions#getChecksum}) the panel
 * uses against the selected {@link ChecksumAlgorithm}
 */
class ChecksumComputationTest
{

	@Test
	void fileChecksumMatchesTheWellKnownDigestsForAbc(@TempDir File tempDir) throws Exception
	{
		File file = new File(tempDir, "abc.txt");
		Files.write(file.toPath(), "abc".getBytes(StandardCharsets.UTF_8));

		assertEquals("900150983cd24fb0d6963f7d28e17f72",
			FileChecksumExtensions.getChecksum(file, ChecksumAlgorithm.MD5),
			"MD5 of the bytes of \"abc\"");
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
			FileChecksumExtensions.getChecksum(file, ChecksumAlgorithm.SHA_256),
			"SHA-256 of the bytes of \"abc\"");
	}
}
