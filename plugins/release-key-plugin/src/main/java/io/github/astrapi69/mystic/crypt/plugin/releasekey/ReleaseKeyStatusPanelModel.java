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

import java.util.List;

/**
 * Everything the {@link ReleaseKeyStatusPanel} holds: the GnuPG home and the release key as typed,
 * and the report of the last reading.
 * <p>
 * The panel binds its fields to this object, so what the user typed is readable here at any moment
 * - the Read button does not ask a widget for its content.
 */
public class ReleaseKeyStatusPanelModel
{

	/** The GnuPG home as typed or chosen, blank for the one gpg uses */
	private String gnuPgHomePath = "";

	/** The fingerprint or long key id of the release key, blank for every certificate */
	private String releaseKey = "";

	/** What the last reading gave, nothing read before the first one */
	private StatusReport report = new StatusReport(List.of(), "", false);

	/**
	 * Gets the GnuPG home as typed
	 *
	 * @return the GnuPG home as typed
	 */
	public String getGnuPgHomePath()
	{
		return gnuPgHomePath;
	}

	/**
	 * Sets the GnuPG home as typed
	 *
	 * @param gnuPgHomePath
	 *            the GnuPG home as typed
	 */
	public void setGnuPgHomePath(final String gnuPgHomePath)
	{
		this.gnuPgHomePath = gnuPgHomePath;
	}

	/**
	 * Gets the release key as typed
	 *
	 * @return the release key as typed
	 */
	public String getReleaseKey()
	{
		return releaseKey;
	}

	/**
	 * Sets the release key as typed
	 *
	 * @param releaseKey
	 *            the release key as typed
	 */
	public void setReleaseKey(final String releaseKey)
	{
		this.releaseKey = releaseKey;
	}

	/**
	 * Gets the report of the last reading
	 *
	 * @return the report of the last reading
	 */
	public StatusReport getReport()
	{
		return report;
	}

	/**
	 * Sets the report of the last reading
	 *
	 * @param report
	 *            the report of the last reading
	 */
	public void setReport(final StatusReport report)
	{
		this.report = report;
	}
}
