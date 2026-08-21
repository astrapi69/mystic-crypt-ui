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
package io.github.astrapi69.mystic.crypt.settings;

import java.io.File;

import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.gson.ObjectToJsonFileExtensions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The user-configurable application settings, persisted as JSON in the configuration directory.
 * Plugin enable/disable state is <em>not</em> stored here - pf4j persists that itself.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MysticCryptSettings
{
	public static final String JSON_FILENAME = "settings.json";

	/** The look and feel name (as reported by {@link javax.swing.UIManager}); defaults to Nimbus */
	private String lookAndFeel = "Nimbus";

	/** The UI language tag; defaults to English */
	private String language = "en";

	/**
	 * Loads the settings from the given configuration directory, or a fresh, default settings
	 * object if none exists yet or it could not be read
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the loaded, or a fresh default, {@link MysticCryptSettings}
	 */
	public static MysticCryptSettings load(File configurationDirectory)
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		if (file.exists())
		{
			try
			{
				String json = ReadFileExtensions.fromFile(file);
				return JsonStringToObjectExtensions.toObject(json, MysticCryptSettings.class);
			}
			catch (Exception exception)
			{
				// ignore, fall through to fresh defaults
			}
		}
		return new MysticCryptSettings();
	}

	/**
	 * Saves these settings into the given configuration directory
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 */
	public void save(File configurationDirectory)
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		try
		{
			ObjectToJsonFileExtensions.toJsonFile(this, file);
		}
		catch (Exception exception)
		{
			// ignore - persisting settings is best-effort
		}
	}
}
