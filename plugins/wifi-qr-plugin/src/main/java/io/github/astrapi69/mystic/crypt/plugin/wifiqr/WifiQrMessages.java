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
package io.github.astrapi69.mystic.crypt.plugin.wifiqr;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import io.github.astrapi69.resourcebundle.locale.ResourceBundleExtensions;

/**
 * The plugin's own texts, from {@code wifiqr/messages.properties}, each with a default for a
 * missing key (.claude/rules/i18n.md)
 */
final class WifiQrMessages
{

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
		.getBundle("wifiqr.messages");

	private WifiQrMessages()
	{
	}

	/**
	 * Gets the string for the given key, falling back to the given default when the key is missing
	 *
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            what to return when the key is not in the bundle
	 * @return the string
	 */
	static String getString(final String key, final String defaultValue)
	{
		return ResourceBundleExtensions.getStringQuietly(RESOURCE_BUNDLE, key, defaultValue);
	}

	/**
	 * The string for the given key with its arguments filled in
	 *
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the pattern when the key is not in the bundle
	 * @param arguments
	 *            what fills the pattern
	 * @return the formatted string
	 */
	static String format(final String key, final String defaultValue, final Object... arguments)
	{
		return MessageFormat.format(getString(key, defaultValue), arguments);
	}
}
