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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.mystic.crypt.app.file.JsonFileStore;
import io.github.astrapi69.mystic.crypt.app.file.TextFiles;
import io.github.astrapi69.mystic.crypt.app.file.UnreadableFileNotices;
import io.github.astrapi69.mystic.crypt.clipboard.ClipboardClearDecision;
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

	/**
	 * The UI language tag; defaults to English. No window offers it since #885 - the General tab's
	 * combo box stored a value nothing read - but the field stays, so a file written up to 8.14
	 * keeps its choice through every save and #150 can bring the setting back without losing it
	 */
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
	 * Whether locking writes pending changes to the file; defaults to OFF (#304).
	 * <p>
	 * Locking used to save, and no user asked it to: a timer committed changes somebody had not
	 * decided about yet. In a password manager writing is not a neutral act, so the lock keeps the
	 * changes in memory instead and whoever wants the old behaviour asks for it once, here.
	 */
	private boolean saveWhenLocking = false;

	/**
	 * After how many further minutes a LOCKED vault is closed altogether, so its decrypted content
	 * leaves memory; 0 turns it off (#242).
	 * <p>
	 * Locking keeps the vault decrypted so unlocking can rebuild the view without reading the file
	 * again, and nothing bounded that. This is the bound.
	 */
	private int closeLockedAfterMinutes = IdleLockDecision.DEFAULT_CLOSE_LOCKED_MINUTES;

	/**
	 * How many seconds after "Copy Password" or "Copy Username" the clipboard is cleared again; 0
	 * turns it off (#352).
	 * <p>
	 * On by default, at {@link ClipboardClearDecision#DEFAULT_CLEAR_AFTER_SECONDS}: without this, a
	 * copied secret is readable by every other process on the machine, every clipboard manager, and
	 * whatever the desktop environment syncs clipboards to, until the next lock or close -
	 * unbounded if the user keeps working.
	 */
	private int clipboardClearSeconds = ClipboardClearDecision.DEFAULT_CLEAR_AFTER_SECONDS;

	/**
	 * Whether the application keeps the paths of key files in the files of its configuration
	 * directory, the remembered sign-in and the remembered KeePass paths; defaults to on, as it has
	 * always been (#997).
	 * <p>
	 * The remembered sign-in offers the key file of each database again. Whoever does not want the
	 * assignment of a database to its key file on the disk turns this off: no key file path is
	 * written to those two files then, neither the one of the database signed in to last nor those
	 * of the others, and the paths already stored there are removed when the setting is switched
	 * off. The factors a database was opened with stay remembered, and so do the paths of the
	 * KeePass import and export files. Not covered: the vault itself, which keeps the key file
	 * paths it was opened with, and a copy of either file that was kept aside as
	 * {@code .unreadable-*} because it could not be read, which keeps its paths until the user
	 * deletes it.
	 */
	private boolean rememberKeyFiles = true;

	/**
	 * The theme of the JDK's Metal look, Steel for the menu's "Metal" and Ocean for its "Ocean", or
	 * {@code null} when none was saved: a file written before 8.17 has no such field, and Gson
	 * leaves it at {@code null}, which keeps the runtime's default theme (#1065)
	 */
	private MetalThemeChoice metalTheme;

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
		return JsonFileStore
			.read(new File(configurationDirectory, JSON_FILENAME), MysticCryptSettings.class)
			.orElseGet(MysticCryptSettings::new);
	}

	/** The settings files the user was already told about in this run (#997) */
	private static final Set<String> TOLD_NOT_REMEMBERED = ConcurrentHashMap.newKeySet();

	/**
	 * Whether key file paths may be written to the remembered files of the configuration directory.
	 * Fails closed: a settings file that exists and cannot be read is not "on", since the user may
	 * have switched the setting off in it, so no path is written until it can be read, and the user
	 * is told so, once per run and file (#997).
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return true if the setting is on, or if there is no settings file yet
	 */
	public static boolean rememberKeyFilesForWriting(File configurationDirectory)
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		Optional<MysticCryptSettings> read = JsonFileStore.read(file, MysticCryptSettings.class);
		if (read.isPresent())
		{
			return read.get().isRememberKeyFiles();
		}
		if (!file.exists())
		{
			return new MysticCryptSettings().isRememberKeyFiles();
		}
		if (TOLD_NOT_REMEMBERED.add(file.getAbsolutePath()))
		{
			UnreadableFileNotices.publish(new UnreadableFileNotices.KeyFilesNotRemembered(file));
		}
		return false;
	}

	/**
	 * Whether the settings file of the given directory exists and cannot be read
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return true if there is a settings file and it cannot be read
	 */
	public static boolean existsButCannotBeRead(File configurationDirectory)
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		return file.exists() && JsonFileStore.read(file, MysticCryptSettings.class).isEmpty();
	}

	/**
	 * Loads the settings for a change that is written back: a file that cannot be read is an error
	 * here, not defaults, since writing defaults over it would destroy what it holds
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the loaded settings, or fresh defaults when there is no settings file yet
	 * @throws java.io.IOException
	 *             if the settings file exists and cannot be read or parsed
	 */
	public static MysticCryptSettings loadForUpdate(File configurationDirectory)
		throws java.io.IOException
	{
		File file = new File(configurationDirectory, JSON_FILENAME);
		if (!file.exists())
		{
			return new MysticCryptSettings();
		}
		try
		{
			String json = TextFiles.readUtf8OrLatin1(file);
			MysticCryptSettings settings = JsonStringToObjectExtensions.toObject(json,
				MysticCryptSettings.class);
			if (settings == null)
			{
				throw new java.io.IOException("the settings file is empty: " + file);
			}
			return settings;
		}
		catch (java.io.IOException exception)
		{
			throw exception;
		}
		catch (RuntimeException exception)
		{
			throw new java.io.IOException("the settings file cannot be parsed: " + file, exception);
		}
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
			// a file that could not be read is kept aside first, not written over (#1082)
			JsonFileStore.write(this, file, MysticCryptSettings.class);
		}
		catch (Exception exception)
		{
			// ignore - persisting settings is best-effort
		}
	}
}
