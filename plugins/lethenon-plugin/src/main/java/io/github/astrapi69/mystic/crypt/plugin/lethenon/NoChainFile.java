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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.nio.file.Path;

/**
 * There is no chain file at the place named (#710): a kind of its own, so a window can say so in
 * its own words instead of reading the message. It stays an {@link IllegalArgumentException}, and
 * its message is the one the plugin always gave, so every caller that caught it still does
 */
final class NoChainFile extends IllegalArgumentException
{

	private static final long serialVersionUID = 1L;

	private final transient Path chainFile;

	/**
	 * Instantiates a new {@link NoChainFile}
	 *
	 * @param chainFile
	 *            the place where the chain file was looked for
	 */
	NoChainFile(final Path chainFile)
	{
		super("there is no chain file at " + chainFile.toAbsolutePath()
			+ "; a chain starts when its genesis block is mined");
		this.chainFile = chainFile;
	}

	/**
	 * @return the place where the chain file was looked for
	 */
	Path chainFile()
	{
		return chainFile;
	}
}
