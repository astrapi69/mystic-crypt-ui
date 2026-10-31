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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.gnupg.KeyRingState;
import io.github.astrapi69.mystic.crypt.gnupg.KeyState;
import io.github.astrapi69.mystic.crypt.gnupg.RevocationCertificate;
import io.github.astrapi69.mystic.crypt.gnupg.SecretPart;

/**
 * {@link KeyRows} turns certificates into the rows of the status table. The certificate here has
 * the release key's shape and its dates.
 */
class KeyRowsTest
{

	private static final LocalDate TODAY = LocalDate.parse("2026-10-29");

	private static final KeyState PRIMARY = new KeyState(
		"5B2FA6B11E298BC09287F423D8C403518C49CA75", true, "rsa2048", "SC",
		Instant.parse("2016-05-01T12:56:24Z"), null, SecretPart.OFFLINE);

	private static final KeyState ENCRYPTION = new KeyState(
		"346DAC645023B91D7CD5FCBD1C664D394E31A6E2", false, "rsa2048", "E",
		Instant.parse("2016-05-01T12:56:24Z"), null, SecretPart.HERE);

	private static final KeyState SIGNING = new KeyState(
		"6D67F8442A6FCC96BD7C1B5E9FCF7C9710E2BD8D", false, "rsa4096", "S",
		Instant.parse("2026-10-10T09:22:23Z"), Instant.parse("2028-10-09T09:22:23Z"),
		SecretPart.HERE);

	private static final KeyRingState RELEASE = new KeyRingState(PRIMARY.fingerprint(),
		"Release Test <release@example.org>", List.of(PRIMARY, ENCRYPTION, SIGNING),
		RevocationCertificate.VALID);

	@Test
	@DisplayName("one row per key, the primary key first, in the table's columns")
	void of_writesOneRowPerKey()
	{
		List<List<String>> rows = KeyRows.of(List.of(RELEASE), TODAY);

		assertEquals(List.of(
			List.of("primary", "5B2F A6B1 1E29 8BC0 9287 F423 D8C4 0351 8C49 CA75", "[SC]",
				"rsa2048", "2016-05-01", "never", "offline", "valid",
				"Release Test <release@example.org>"),
			List.of("subkey", "346D AC64 5023 B91D 7CD5 FCBD 1C66 4D39 4E31 A6E2", "[E]",
				"rsa2048", "2016-05-01", "never", "here", "", ""),
			List.of("subkey", "6D67 F844 2A6F CC96 BD7C 1B5E 9FCF 7C97 10E2 BD8D", "[S]",
				"rsa4096", "2026-10-10", "2028-10-09", "here", "", "")), rows);
		assertEquals(KeyRows.COLUMNS.size(), rows.get(0).size());
	}

	@ParameterizedTest(name = "expires {0}, today {1}: {2}")
	@CsvSource({ "2028-10-09T09:22:23Z, 2026-10-29, 2028-10-09",
			"2026-10-29T23:59:59Z, 2026-10-29, 2026-10-29",
			"2026-10-28T23:59:59Z, 2026-10-29, expired 2026-10-28",
			"2016-05-02T00:00:00Z, 2026-10-29, expired 2016-05-02" })
	@DisplayName("a key that expired says so; one that expires today still counts as valid")
	void of_saysWhenAKeyExpired(final String expires, final String today, final String expected)
	{
		KeyState expiring = new KeyState(SIGNING.fingerprint(), false, "rsa4096", "S",
			SIGNING.created(), Instant.parse(expires), SecretPart.HERE);
		KeyRingState ring = new KeyRingState(PRIMARY.fingerprint(), "", List.of(PRIMARY, expiring),
			RevocationCertificate.ABSENT);

		List<List<String>> rows = KeyRows.of(List.of(ring), LocalDate.parse(today));

		assertEquals(expected, rows.get(1).get(5));
	}

	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource({ "HERE, here", "OFFLINE, offline", "UNKNOWN, unknown" })
	@DisplayName("where the secret part is, in words")
	void of_namesWhereTheSecretPartIs(final SecretPart secret, final String expected)
	{
		KeyState key = new KeyState(PRIMARY.fingerprint(), true, "ed25519", "SC",
			PRIMARY.created(), null, secret);

		List<List<String>> rows = KeyRows.of(
			List.of(new KeyRingState(key.fingerprint(), "", List.of(key),
				RevocationCertificate.ABSENT)), TODAY);

		assertEquals(expected, rows.get(0).get(6));
	}

	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource({ "VALID, valid", "ABSENT, absent", "NOT_VALID, not valid" })
	@DisplayName("the revocation certificate, in words, on the primary key's row")
	void of_namesTheRevocationCertificate(final RevocationCertificate certificate,
		final String expected)
	{
		KeyRingState ring = new KeyRingState(PRIMARY.fingerprint(), "", List.of(PRIMARY),
			certificate);

		assertEquals(expected, KeyRows.of(List.of(ring), TODAY).get(0).get(7));
	}

	@Test
	@DisplayName("a key that states no use shows empty brackets rather than nothing")
	void of_aKeyWithoutUse_showsEmptyBrackets()
	{
		KeyState key = new KeyState(PRIMARY.fingerprint(), true, "rsa2048", "", PRIMARY.created(),
			null, SecretPart.HERE);
		KeyRingState ring = new KeyRingState(key.fingerprint(), "", List.of(key),
			RevocationCertificate.ABSENT);

		assertEquals("[]", KeyRows.of(List.of(ring), TODAY).get(0).get(2));
	}

	@Test
	@DisplayName("no certificates, no rows")
	void of_noCertificates_noRows()
	{
		assertEquals(List.of(), KeyRows.of(List.of(), TODAY));
	}
}
