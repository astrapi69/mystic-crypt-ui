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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The look and feel on the screen and the look and feel in the settings
 */
public final class LookAndFeelChoice
{

	private static final Logger LOG = Logger.getLogger(LookAndFeelChoice.class.getName());

	private LookAndFeelChoice()
	{
	}

	/**
	 * The settings the Settings dialog opens on: the saved ones, with the look and feel of the
	 * screen put in. The dialog shows what the screen shows, so closing it cannot write another
	 * look down; the two differ when the saved look could not be applied at start, and then the
	 * saved one is replaced by the one that is in use (#1058)
	 * <p>
	 * A look that is not among the installed ones has no name the settings could hold, and the
	 * saved look stays
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the settings to show in the dialog
	 */
	public static MysticCryptSettings settingsForTheDialog(final File configurationDirectory)
	{
		MysticCryptSettings settings = MysticCryptSettings.load(configurationDirectory);
		String activeName = GeneralSettingsPanel.installedNameOfActiveLookAndFeel();
		if (activeName != null)
		{
			settings.setLookAndFeel(activeName);
		}
		return settings;
	}

	/**
	 * Writes the active look and feel into the settings of the given configuration directory
	 * <p>
	 * A look that is not among the installed ones has no name the settings could hold and is not
	 * written; nothing is written either when the settings already hold the active look. Of the two
	 * looks of the JDK that share a class, Metal and Ocean, the settings hold "Metal" and the next
	 * start shows the runtime's default theme of it, which is Ocean: the theme is not saved (#1065)
	 * <p>
	 * A settings file that exists and cannot be read is left as it is, and the failure is logged:
	 * writing the look into defaults would replace everything else the file holds
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return {@code true} if a write was attempted, whether or not it reached the disk
	 */
	public static boolean persistActive(final File configurationDirectory)
	{
		String activeName = GeneralSettingsPanel.installedNameOfActiveLookAndFeel();
		if (activeName == null)
		{
			return false;
		}
		MysticCryptSettings settings;
		try
		{
			settings = MysticCryptSettings.loadForUpdate(configurationDirectory);
		}
		catch (IOException exception)
		{
			LOG.log(Level.WARNING, "The look and feel was not saved, the settings could not be "
				+ "read: " + exception.getMessage(), exception);
			return false;
		}
		if (activeName.equals(settings.getLookAndFeel()))
		{
			return false;
		}
		settings.setLookAndFeel(activeName);
		settings.save(configurationDirectory);
		return true;
	}
}
