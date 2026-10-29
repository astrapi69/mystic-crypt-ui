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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * The rows of the status table: one per key, the primary key of each certificate first. What
 * belongs to the certificate as a whole - its revocation certificate and its user ID - stands on
 * the primary key's row only. Dates are days in UTC.
 */
final class KeyRows
{

	/** The table's column names, in the order every row fills them */
	static final List<String> COLUMNS = List.of(
		ReleaseKeyMessages.getString("releasekey.column.key", "key"),
		ReleaseKeyMessages.getString("releasekey.column.fingerprint", "fingerprint"),
		ReleaseKeyMessages.getString("releasekey.column.use", "use"),
		ReleaseKeyMessages.getString("releasekey.column.algorithm", "algorithm"),
		ReleaseKeyMessages.getString("releasekey.column.created", "created"),
		ReleaseKeyMessages.getString("releasekey.column.expires", "expires"),
		ReleaseKeyMessages.getString("releasekey.column.secret", "secret part"),
		ReleaseKeyMessages.getString("releasekey.column.revocation", "revocation certificate"),
		ReleaseKeyMessages.getString("releasekey.column.user.id", "user ID"));

	private KeyRows()
	{
	}

	/**
	 * The rows for the given certificates
	 *
	 * @param rings
	 *            the certificates
	 * @param today
	 *            the day a key counts as expired after
	 * @return one row per key, each with one value per column
	 */
	static List<List<String>> of(final List<KeyRingState> rings, final LocalDate today)
	{
		List<List<String>> rows = new ArrayList<>();
		for (KeyRingState ring : rings)
		{
			for (KeyState key : ring.keys())
			{
				rows.add(rowOf(ring, key, today));
			}
		}
		return rows;
	}

	private static List<String> rowOf(final KeyRingState ring, final KeyState key,
		final LocalDate today)
	{
		return List.of(
			key.primary()
				? ReleaseKeyMessages.getString("releasekey.key.primary", "primary")
				: ReleaseKeyMessages.getString("releasekey.key.subkey", "subkey"),
			grouped(key.fingerprint()), "[" + key.usage() + "]", key.algorithm(),
			dayOf(key.created()).toString(), expiryOf(key.expires(), today),
			secretOf(key.secret()), key.primary() ? revocationOf(ring.revocationCertificate()) : "",
			key.primary() ? ring.userId() : "");
	}

	/** The fingerprint in groups of four, the way gpg prints it to be compared by eye */
	private static String grouped(final String fingerprint)
	{
		return fingerprint.replaceAll("(.{4})(?!$)", "$1 ");
	}

	private static LocalDate dayOf(final Instant instant)
	{
		return LocalDate.ofInstant(instant, ZoneOffset.UTC);
	}

	private static String expiryOf(final Instant expires, final LocalDate today)
	{
		if (expires == null)
		{
			return ReleaseKeyMessages.getString("releasekey.expires.never", "never");
		}
		LocalDate day = dayOf(expires);
		return day.isBefore(today)
			? ReleaseKeyMessages.getString("releasekey.expires.expired", "expired") + " " + day
			: day.toString();
	}

	private static String secretOf(final SecretPart secret)
	{
		return switch (secret)
		{
			case HERE -> ReleaseKeyMessages.getString("releasekey.secret.here", "here");
			case OFFLINE -> ReleaseKeyMessages.getString("releasekey.secret.offline", "offline");
			case UNKNOWN -> ReleaseKeyMessages.getString("releasekey.secret.unknown", "unknown");
		};
	}

	private static String revocationOf(final RevocationCertificate certificate)
	{
		return switch (certificate)
		{
			case VALID -> ReleaseKeyMessages.getString("releasekey.revocation.valid", "valid");
			case ABSENT -> ReleaseKeyMessages.getString("releasekey.revocation.absent", "absent");
			case NOT_VALID -> ReleaseKeyMessages.getString("releasekey.revocation.not.valid",
				"not valid");
		};
	}
}
