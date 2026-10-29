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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * One reading of a GnuPG home for the status view: every certificate, or only the release key when
 * one is named, and a line that says what was read - or why nothing could be, so that an empty
 * table is never mistaken for an empty keyring
 */
final class ReleaseKeyStatus
{

	private ReleaseKeyStatus()
	{
	}

	/**
	 * Reads the home
	 *
	 * @param home
	 *            the GnuPG home
	 * @param releaseKey
	 *            the fingerprint or long key id of the release key, blank for every certificate
	 * @return what to show
	 */
	static StatusReport read(final Path home, final String releaseKey)
	{
		List<KeyRingState> rings;
		try
		{
			rings = GnuPgHomeSupport.read(home);
		}
		catch (IOException exception)
		{
			return new StatusReport(List.of(), String.format(
				ReleaseKeyMessages.getString("releasekey.result.unreadable",
					"Could not read the GnuPG home %s: %s"),
				home, exception.getMessage()), true);
		}
		if (releaseKey == null || releaseKey.isBlank())
		{
			return new StatusReport(rings, String.format(ReleaseKeyMessages
				.getString("releasekey.result.every", "%d certificate(s) in %s"), rings.size(),
				home), false);
		}
		return releaseKeyOf(rings, home, releaseKey.trim());
	}

	private static StatusReport releaseKeyOf(final List<KeyRingState> rings, final Path home,
		final String releaseKey)
	{
		Optional<KeyRingState> found = GnuPgHomeSupport.find(rings, releaseKey);
		if (found.isEmpty())
		{
			return new StatusReport(List.of(), String.format(
				ReleaseKeyMessages.getString("releasekey.result.missing",
					"The release key %s is not in %s"),
				releaseKey, home), true);
		}
		return new StatusReport(List.of(found.get()), String.format(
			ReleaseKeyMessages.getString("releasekey.result.release", "The release key, read from %s"),
			home), false);
	}
}
