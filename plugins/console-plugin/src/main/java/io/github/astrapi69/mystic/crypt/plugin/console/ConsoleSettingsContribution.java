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
package io.github.astrapi69.mystic.crypt.plugin.console;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the console plugin: how tall the docked console is and whether it can be
 * resized and moved. The console is docked into the bottom of the desktop, and how much room that
 * should take is a matter of taste and of screen size.
 */
@Extension
public class ConsoleSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "console-plugin";

	/** The console takes this fraction of the screen height: 4 means the bottom quarter */
	public static final String KEY_HEIGHT_DIVISOR = "dock.height.divisor";

	/** Whether the console frame can be resized and moved */
	public static final String KEY_RESIZABLE = "dock.resizable";

	/** The fraction used when nothing else is configured */
	public static final int DEFAULT_HEIGHT_DIVISOR = 4;

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Console";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_HEIGHT_DIVISOR, String.valueOf(DEFAULT_HEIGHT_DIVISOR));
		defaults.put(KEY_RESIZABLE, "false");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_HEIGHT_DIVISOR -> "4 docks the console into the bottom quarter of the screen";
			case KEY_RESIZABLE -> "true or false: whether the console frame can be resized and moved";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new ConsoleSettingsContribution().getDefaults());
	}

	/**
	 * The fraction of the screen height the console takes; a value below two would leave no room
	 * for anything else and is refused
	 *
	 * @return the divisor
	 */
	public static int heightDivisor()
	{
		int divisor = PluginSettings.asInt(current(), KEY_HEIGHT_DIVISOR, DEFAULT_HEIGHT_DIVISOR);
		return divisor < 2 ? DEFAULT_HEIGHT_DIVISOR : divisor;
	}

	/**
	 * Whether the console frame can be resized and moved
	 *
	 * @return true when it is resizable
	 */
	public static boolean resizable()
	{
		return Boolean.parseBoolean(current().get(KEY_RESIZABLE));
	}
}
