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
import io.github.astrapi69.mystic.crypt.lock.IdleLockDecision;
import io.github.astrapi69.swing.enumeration.FrameMode;
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

	/**
	 * The look and feel name (as reported by {@link javax.swing.UIManager}); defaults to FlatLaf
	 * Light (#125)
	 */
	private String lookAndFeel = "FlatLaf Light";

	/** The UI language tag; defaults to English */
	private String language = "en";

	/**
	 * The view the application opens in. Defaults to the panel view, which is what the application
	 * has always shown after signing in.
	 */
	private FrameMode viewMode = FrameMode.APPLICATION_PANEL;

	/** Whether tooltips are shown across the application; defaults to on */
	private boolean tooltipsEnabled = true;

	/**
	 * After how many idle minutes the workspace locks itself; 0 turns it off (#241).
	 * <p>
	 * On by default, at {@link IdleLockDecision#DEFAULT_TIMEOUT_MINUTES}: an open vault used to
	 * stay open for as long as the application ran, and the realistic case for a password manager
	 * is the one where nobody remembers to lock it.
	 */
	private int autoLockMinutes = IdleLockDecision.DEFAULT_TIMEOUT_MINUTES;

	/**
	 * After how many further minutes a LOCKED vault is closed altogether, so its decrypted content
	 * leaves memory; 0 turns it off (#242).
	 * <p>
	 * Locking keeps the vault decrypted so unlocking can rebuild the view without reading the file
	 * again, and nothing bounded that. This is the bound.
	 */
	private int closeLockedAfterMinutes = IdleLockDecision.DEFAULT_CLOSE_LOCKED_MINUTES;

	/**
	 * The view the application opens in.
	 * <p>
	 * Written by hand rather than left to lombok because a settings file can say
	 * {@code "viewMode": null} or name a mode this version does not know, and json turns both into
	 * a null field. A view mode nobody can read is the default view, not a crash on start.
	 *
	 * @return the view mode, never null
	 */
	public FrameMode getViewMode()
	{
		return viewMode == null ? FrameMode.APPLICATION_PANEL : viewMode;
	}

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
