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

import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ConsensusRules;

/**
 * Which chain a genesis block starts: the test network or the main chain. It matters only where
 * there is no chain yet; on an existing chain its genesis block decides, as it does for lethenon's
 * command line, whose {@code mine --testnet} makes the same choice (#518).
 * <p>
 * The identifiers come from the chain library rather than from this plugin: they go into signed
 * bytes, and the library owns that wire.
 */
public enum ChainKind
{

	/** the test network, {@code lethenon-test-2} since lethenon 0.4.0: where every new scheme runs first */
	TEST_NETWORK(Chain.TEST_IDENTIFIER, "the test network"),

	/**
	 * the main chain, {@code lethenon-2} since lethenon 0.4.0: started only from the genesis block
	 * fixed in the library's code, which lethenon carries from 1.0.0 on (lethenon ADR 0005)
	 */
	MAIN_CHAIN(Chain.IDENTIFIER, "the main chain");

	private final String identifier;

	private final String description;

	ChainKind(final String identifier, final String description)
	{
		this.identifier = identifier;
		this.description = description;
	}

	/**
	 * The chain identifier a genesis block of this kind carries
	 *
	 * @return e.g. {@code lethenon-test-2}
	 */
	public String identifier()
	{
		return identifier;
	}

	/**
	 * Whether a chain of this kind can be started with this build of the library: a test network
	 * always, the main chain only once the library fixes its genesis block in the code (#535)
	 * @return true where a new chain of this kind can be started
	 */
	public boolean canStart()
	{
		return this != MAIN_CHAIN || ConsensusRules.LETHENON.anchorFor(identifier).isPresent();
	}

	/**
	 * What this kind is called in the mine window
	 *
	 * @return the description, e.g. "the test network"
	 */
	public String description()
	{
		return description;
	}
}
