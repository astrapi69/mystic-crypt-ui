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
package io.github.astrapi69.mystic.crypt;

import java.util.ResourceBundle;

import io.github.astrapi69.resourcebundle.locale.ResourceBundleExtensions;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * The Class Messages.
 */
@Log
@UtilityClass
public class Messages
{

	/** The Constant BUNDLE_NAME. */
	private static final String BUNDLE_NAME = "ui.messages"; //$NON-NLS-1$

	/** The Constant RESOURCE_BUNDLE. */
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	/**
	 * Gets the string.
	 *
	 * @param key
	 *            the key
	 * @return the string
	 */
	public static String getString(final String key)
	{
		return ResourceBundleExtensions.getStringQuietly(RESOURCE_BUNDLE, key);
	}

	/**
	 * Gets the string of the key, or the given default when the bundle has no such key. The default
	 * is the literal default also when it is empty: the library hands back its warning text for a
	 * missing key when the default is empty ({@code ResourceBundleExtensions.getStringQuietly}
	 * treats an empty default as none).
	 *
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the text shown when the bundle has no such key
	 * @return the string
	 */
	public static String getString(final String key, final String defaultValue)
	{
		// TODO(resourcebundle-core#60): remove the guard once the library returns an empty default
		if (defaultValue != null && key != null && !key.isEmpty()
			&& !RESOURCE_BUNDLE.containsKey(key))
		{
			return defaultValue;
		}
		return ResourceBundleExtensions.getStringQuietly(RESOURCE_BUNDLE, key, defaultValue);
	}

}
