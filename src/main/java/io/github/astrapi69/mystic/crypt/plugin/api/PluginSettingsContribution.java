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
package io.github.astrapi69.mystic.crypt.plugin.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.ExtensionPoint;

/**
 * Extension point that lets a plugin have its own configuration. What a plugin declares here shows
 * up in the settings dialog as an editable list of keys and values and is stored in its own file,
 * next to the application settings.
 * <p>
 * The declared defaults are the contract: a key that is not declared is not offered for editing,
 * and a key that is declared always has a value, even in a fresh installation.
 */
public interface PluginSettingsContribution extends ExtensionPoint
{

	/**
	 * Gets the id of the plugin these settings belong to, the same id the plugin declares in its
	 * {@code plugin.properties}. It decides which file the values are stored in
	 *
	 * @return the plugin id
	 */
	String getPluginId();

	/**
	 * Gets the settings this plugin understands, with the value each one has until the user changes
	 * it. Use a {@link LinkedHashMap} to decide the order they are shown in
	 *
	 * @return the settings keys with their default values
	 */
	Map<String, String> getDefaults();

	/**
	 * Gets the name shown for this plugin in the settings dialog; the plugin id is used when this
	 * is not overridden
	 *
	 * @return the display name
	 */
	default String getDisplayName()
	{
		return getPluginId();
	}

	/**
	 * Gets a short explanation of a single setting, shown as a tool tip
	 *
	 * @param key
	 *            the settings key
	 * @return the explanation, or {@code null} when there is none
	 */
	default String getDescription(String key)
	{
		return null;
	}

}
