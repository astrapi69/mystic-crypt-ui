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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrapi69.collection.pair.KeyValuePair;

/**
 * The custom properties of a vault entry as plain maps, for a feature that does not know the entry
 * model. The values are text, as the model holds them; the protected names are the model's own
 * set, so an export keeps the protection flag. Used by the lethenon wallet tools, which carry the
 * wallet's words as a protected property (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md).
 */
public final class VaultEntryProperties
{

	private VaultEntryProperties()
	{
	}

	/**
	 * The custom property values of an entry, in their order
	 *
	 * @param entry
	 *            the entry
	 * @return the values by name
	 */
	public static Map<String, String> valuesOf(final MysticCryptEntryModelBean entry)
	{
		Map<String, String> values = new LinkedHashMap<>();
		for (KeyValuePair<String, String> property : entry.getProperties())
		{
			values.put(property.getKey(), property.getValue());
		}
		return values;
	}

	/**
	 * The names of the custom properties the user marked as protected
	 *
	 * @param entry
	 *            the entry
	 * @return the protected names; empty when none is protected
	 */
	public static Set<String> protectedNamesOf(final MysticCryptEntryModelBean entry)
	{
		Set<String> names = entry.getProtectedPropertyKeys();
		return names == null ? Set.of() : new LinkedHashSet<>(names);
	}

	/**
	 * Replaces the custom properties of an entry with the given values and protected names
	 *
	 * @param entry
	 *            the entry, changed in place
	 * @param values
	 *            the values by name, in the order they should be stored
	 * @param protectedNames
	 *            the names among them that are protected
	 */
	public static void replace(final MysticCryptEntryModelBean entry, final Map<String, String> values,
		final Set<String> protectedNames)
	{
		List<KeyValuePair<String, String>> properties = entry.getProperties();
		properties.clear();
		for (Map.Entry<String, String> value : values.entrySet())
		{
			properties.add(new KeyValuePair<>(value.getKey(), value.getValue()));
		}
		entry.setProtectedPropertyKeys(new LinkedHashSet<>(protectedNames));
	}
}
