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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A vault entry as the wallet tools see it: its custom properties, with the protection flag each
 * carries. The host's entry model is the place the entry lives; this is the part the wallet needs.
 */
final class WalletEntry
{

	/** The network hint, a plain property: testnet or mainnet */
	static final String NETWORK = "lethenon.network";

	/** The address the wallet had when the entry was made, a plain property, for the backup check */
	static final String ADDRESS = "lethenon.address";

	private final Map<String, Property> properties = new LinkedHashMap<>();

	private WalletEntry()
	{
	}

	/**
	 * A new wallet entry for a network
	 *
	 * @param network
	 *            "testnet" or "mainnet"
	 * @return the entry, with no words stored in it
	 */
	static WalletEntry newEntry(final String network)
	{
		WalletEntry entry = new WalletEntry();
		entry.properties.put(NETWORK, new Property(network.toCharArray(), false));
		return entry;
	}

	/** An entry with no properties, for a host entry that has none yet */
	static WalletEntry empty()
	{
		return new WalletEntry();
	}

	/** The names of the properties, in the order they were set */
	java.util.Set<String> propertyNames()
	{
		return properties.keySet();
	}

	/** The value of a property as text, for the host's model, which holds text */
	String textOf(final String name)
	{
		return new String(properties.get(name).value);
	}

	boolean hasProperty(final String name)
	{
		return properties.containsKey(name);
	}

	boolean isProtected(final String name)
	{
		Property property = properties.get(name);
		return property != null && property.protectedValue;
	}

	/** The address the entry records for its wallet, or empty when none is recorded */
	String addressForCheck()
	{
		Property property = properties.get(ADDRESS);
		return property == null ? "" : new String(property.value);
	}

	/** The buffer that holds a protected property's value, the one the entry itself keeps */
	char[] protectedBuffer(final String name)
	{
		return properties.get(name).value;
	}

	void setProperty(final String name, final char[] value, final boolean protectedValue)
	{
		properties.put(name, new Property(value, protectedValue));
	}

	/** Removes a property and overwrites the buffer that held its value */
	void removeProperty(final String name)
	{
		Property property = properties.remove(name);
		if (property != null)
		{
			Arrays.fill(property.value, '\0');
		}
	}

	private static final class Property
	{

		private final char[] value;

		private final boolean protectedValue;

		private Property(final char[] value, final boolean protectedValue)
		{
			this.value = value;
			this.protectedValue = protectedValue;
		}
	}
}
