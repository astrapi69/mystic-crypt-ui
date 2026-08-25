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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the menu designer: whether the current menu is exported into the editor as
 * soon as the tool opens. Someone who edits a layout in an external editor and only pastes it here
 * does not want the field overwritten every time.
 */
@Extension
public class MenuDesignerSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "menu-designer-plugin";

	/** Whether the current menu is exported into the editor when the tool opens */
	public static final String KEY_EXPORT_ON_OPEN = "export.on.open";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Menu Designer";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_EXPORT_ON_OPEN, "true");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return KEY_EXPORT_ON_OPEN.equals(key)
			? "true or false: fill the editor with the current menu when the tool opens"
			: null;
	}

	/**
	 * Whether the current menu is exported into the editor when the tool opens
	 *
	 * @return true when the editor is filled on open
	 */
	public static boolean exportOnOpen()
	{
		return !"false".equalsIgnoreCase(PluginSettings
			.load(PLUGIN_ID, new MenuDesignerSettingsContribution().getDefaults())
			.get(KEY_EXPORT_ON_OPEN));
	}
}
