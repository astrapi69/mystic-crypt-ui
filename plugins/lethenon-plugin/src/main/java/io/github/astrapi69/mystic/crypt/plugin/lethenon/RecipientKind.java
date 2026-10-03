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

/**
 * What the recipient of a transfer was given as: an account key, which the chain shows as it is, or
 * a published address, from which a one-time destination is derived for this payment alone. The two
 * are told apart by the person sending rather than guessed from the text, the way lethenon's
 * command line takes {@code --to} or {@code --to-address} and never both
 */
public enum RecipientKind
{

	/** an account key in hexadecimal; the chain names it as the recipient */
	ACCOUNT_KEY("an account key"),

	/**
	 * a published address, {@code <view key>:<spend key>} in hexadecimal; the chain names a
	 * one-time destination nobody but the recipient can connect to it
	 */
	PUBLISHED_ADDRESS("a published address");

	private final String description;

	RecipientKind(final String description)
	{
		this.description = description;
	}

	/**
	 * What this kind is called in the send window
	 *
	 * @return the description, e.g. "a published address"
	 */
	public String description()
	{
		return description;
	}
}
