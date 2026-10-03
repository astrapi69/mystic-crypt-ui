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
package io.github.astrapi69.mystic.crypt.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A KDBX written on a machine whose JVM default encoding is not UTF-8 must still be readable.
 * <p>
 * Up to KeePassJava2 2.2.5 it was not: the Jackson serializer wrote the database XML in the JVM's
 * default encoding, so an entry containing {@code ü} or {@code é} produced a file that the library
 * itself refused to load - {@code JsonMappingException: Invalid UTF-8 start byte 0xfc}. That is
 * data loss on export, not a cosmetic defect, and nothing in this application could see it coming:
 * the exporting JVM decides. Fixed upstream in 2.2.6 (jorabin/KeePassJava2#104), and this is the
 * guard, because the next bump could bring it back.
 * <p>
 * It runs in a CHILD process, because {@code file.encoding} is read when a JVM starts: the test JVM
 * here is UTF-8 and cannot ask itself this question. Same shape as the release-compatibility probe
 * in {@code VaultOpensInTheLastReleaseTest}.
 */
class NonAsciiTextSurvivesANonUtf8JvmTest
{

	/** A single-byte encoding that cannot represent the text the probe writes */
	private static final String FOREIGN_ENCODING = "ISO-8859-1";

	@Test
	@DisplayName("a database written on a non-UTF-8 JVM reads back with its text intact")
	void aNonUtf8Jvm_writesAReadableDatabase() throws Exception
	{
		String java = ProcessHandle.current().info().command().orElse("java");
		Process probe = new ProcessBuilder(java, "-Dfile.encoding=" + FOREIGN_ENCODING, "-cp",
			System.getProperty("java.class.path"), NonAsciiKdbxProbe.class.getName())
				.redirectErrorStream(true).start();
		String output;
		try (InputStream fromProbe = probe.getInputStream())
		{
			output = new String(fromProbe.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertTrue(probe.waitFor(2, TimeUnit.MINUTES), "the probe did not finish: " + output);

		assertTrue(output.contains("file.encoding=" + FOREIGN_ENCODING),
			"the child has to run with the encoding this test is about, or it proves nothing: "
				+ output);
		assertEquals(0, probe.exitValue(),
			"a KDBX written with a non-UTF-8 default encoding could not be read back. That is the "
				+ "upstream defect jorabin/KeePassJava2#104, fixed in 2.2.6 - check which version "
				+ "this build binds. The probe said: " + output);
		assertTrue(output.contains("INTACT"), output);
	}
}
