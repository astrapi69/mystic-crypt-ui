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
package io.github.astrapi69.mystic.crypt.panel.info;

/**
 * What the Info dialog shows. The version comes from the running jar's own manifest
 * ({@code Implementation-Version}, written by the build from {@code projectVersion} - see
 * gradle/packaging.gradle) rather than from a hand-maintained constant, which is exactly what let
 * the dialog show "8.1-SNAPSHOT" long after the project had moved past it.
 */
public record ApplicationInfo(String applicationName, String version, String copyright,
	String licenseSummary, String githubUrl, String wikiUrl) {

	private static final String DEFAULT_VERSION = "development";
	private static final String GITHUB_URL = "https://github.com/astrapi69/mystic-crypt-ui";
	private static final String WIKI_URL = "https://github.com/astrapi69/mystic-crypt-ui/wiki";

	/**
	 * The application's current info, read from the running jar's manifest where available
	 *
	 * @param applicationName
	 *            the application name to show
	 * @param copyright
	 *            the copyright line to show
	 * @param licenseSummary
	 *            the one-line license summary to show
	 * @return the info to show in the dialog
	 */
	public static ApplicationInfo current(final String applicationName, final String copyright,
		final String licenseSummary)
	{
		final String implementationVersion = ApplicationInfo.class.getPackage()
			.getImplementationVersion();
		return new ApplicationInfo(applicationName,
			implementationVersion != null ? implementationVersion : DEFAULT_VERSION, copyright,
			licenseSummary, GITHUB_URL, WIKI_URL);
	}
}
