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
package io.github.astrapi69.mystic.crypt.plugin.releasekey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * {@link ReleaseKeyStatus} reads a home for the view and says in one line what it read, against a
 * home gpg wrote with two certificates
 */
class ReleaseKeyStatusTest
{

	@TempDir
	static Path directory;

	private static ThrowawayGnuPgHome home;

	private static String release;

	@BeforeAll
	static void writeTheHomeWithGpg() throws IOException
	{
		home = ThrowawayGnuPgHome.create(directory.resolve("home"));
		release = home.generate("Release Test <release@example.org>", "ed25519", "sign,cert",
			"0");
		home.addSubkey(release, "ed25519", "sign", "1y");
		home.takePrimaryOffline(release);
		home.generate("Other Test <other@example.org>", "ed25519", "sign,cert", "0");
	}

	@AfterAll
	static void stopTheAgent() throws IOException
	{
		home.close();
	}

	@ParameterizedTest(name = "release key \"{0}\"")
	@CsvSource({ "''", "'   '" })
	@DisplayName("without a release key every certificate of the home is shown")
	void read_withoutAReleaseKey_showsEveryCertificate(final String noReleaseKey)
	{
		StatusReport report = ReleaseKeyStatus.read(home.directory(), noReleaseKey);

		assertFalse(report.failed(), report.message());
		assertEquals(2, report.certificates().size());
		assertTrue(report.message().contains("2"), report.message());
		assertTrue(report.message().contains(home.directory().toString()), report.message());
	}

	@Test
	@DisplayName("with a release key only that certificate is shown")
	void read_withAReleaseKey_showsOnlyIt()
	{
		StatusReport report = ReleaseKeyStatus.read(home.directory(), release);

		assertFalse(report.failed(), report.message());
		assertEquals(List.of(release),
			report.certificates().stream().map(KeyRingState::fingerprint).toList());
	}

	@Test
	@DisplayName("a release key that is not in the home is named as missing, with the home")
	void read_aReleaseKeyThatIsNotThere_isNamedAsMissing()
	{
		StatusReport report = ReleaseKeyStatus.read(home.directory(), "0000000000000000");

		assertTrue(report.failed());
		assertEquals(List.of(), report.certificates());
		assertTrue(report.message().contains("0000000000000000"), report.message());
		assertTrue(report.message().contains(home.directory().toString()), report.message());
	}

	@Test
	@DisplayName("a home that cannot be read gives the reason, never an empty view that looks read")
	void read_aHomeThatCannotBeRead_givesTheReason()
	{
		Path missing = directory.resolve("no-such-home");

		StatusReport report = ReleaseKeyStatus.read(missing, "");

		assertTrue(report.failed());
		assertEquals(List.of(), report.certificates());
		assertTrue(report.message().contains(missing.toString()), report.message());
	}
}
