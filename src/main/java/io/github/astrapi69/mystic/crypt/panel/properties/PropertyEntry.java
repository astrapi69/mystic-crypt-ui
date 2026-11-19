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
package io.github.astrapi69.mystic.crypt.panel.properties;

import java.util.Map;
import java.util.Objects;

import io.github.astrapi69.collection.pair.KeyValuePair;

/**
 * A row of the "Properties" table: a live view of one of the entry's own {@link KeyValuePair}s as a
 * {@link Map.Entry}, which is the row type of the table models of swing-table-components since 4.0
 * (its models no longer use the types of silly-bean). The vault stores {@link KeyValuePair}s, so
 * the pair stays what is persisted and this class only shows it: a change of the pair is seen in
 * the row at once, and the other way round.
 * <p>
 * Equality follows the contract of {@link Map.Entry}: the same key and the same value
 */
public final class PropertyEntry implements Map.Entry<String, String>
{

	private final KeyValuePair<String, String> pair;

	/**
	 * Creates the view
	 *
	 * @param pair
	 *            the entry's own property, not a copy
	 */
	public PropertyEntry(final KeyValuePair<String, String> pair)
	{
		this.pair = Objects.requireNonNull(pair, "pair");
	}

	/**
	 * The property this row shows
	 *
	 * @return the entry's own pair
	 */
	public KeyValuePair<String, String> getPair()
	{
		return pair;
	}

	@Override
	public String getKey()
	{
		return pair.getKey();
	}

	@Override
	public String getValue()
	{
		return pair.getValue();
	}

	@Override
	public String setValue(final String value)
	{
		String previous = pair.getValue();
		pair.setValue(value);
		return previous;
	}

	@Override
	public boolean equals(final Object other)
	{
		return other instanceof Map.Entry<?, ?> entry && Objects.equals(getKey(), entry.getKey())
			&& Objects.equals(getValue(), entry.getValue());
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
	}

	@Override
	public String toString()
	{
		return getKey() + "=" + getValue();
	}
}
