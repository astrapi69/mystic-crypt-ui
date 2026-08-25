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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the file encryption plugin: whether the file that was just encrypted is
 * removed afterwards, and which of the two tabs the tool opens on.
 */
@Extension
public class FileCryptSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "file-crypt-plugin";

	/** Whether the source file is deleted once it has been encrypted */
	public static final String KEY_DELETE_SOURCE = "delete.source.after.encrypt";

	/** Which tab the tool starts on: "file" or "text" */
	public static final String KEY_START_TAB = "default.tab";

	/** The tab the tool starts on when nothing else is configured */
	public static final String DEFAULT_START_TAB = "file";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "File Encryption";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_DELETE_SOURCE, "false");
		defaults.put(KEY_START_TAB, DEFAULT_START_TAB);
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_DELETE_SOURCE -> "true or false: remove the original once it is encrypted - "
				+ "deleting is not wiping, the content can still be recoverable";
			case KEY_START_TAB -> "file or text";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new FileCryptSettingsContribution().getDefaults());
	}

	/**
	 * Whether the source file is deleted once it has been encrypted
	 *
	 * @return true when it is deleted
	 */
	public static boolean deleteSourceAfterEncrypt()
	{
		return Boolean.parseBoolean(current().get(KEY_DELETE_SOURCE));
	}

	/**
	 * The tab the tool starts on
	 *
	 * @return "file" or "text"
	 */
	public static String startTab()
	{
		String configured = current().get(KEY_START_TAB);
		return "text".equalsIgnoreCase(configured) ? "text" : DEFAULT_START_TAB;
	}
}
