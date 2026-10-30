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
