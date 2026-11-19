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

/**
 * Keeps the look and feel that is on the screen in the settings, so that the next start applies it
 * (#1059). Used by the View > Look and Feel menu, whose items only switch the look of the running
 * application
 */
public final class LookAndFeelChoice
{

	private LookAndFeelChoice()
	{
	}

	/**
	 * Writes the active look and feel into the settings of the given configuration directory
	 * <p>
	 * A look that is not among the installed ones has no name the settings could hold and is not
	 * written; nothing is written either when the settings already hold the active look
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return {@code true} if the settings were written
	 */
	public static boolean persistActive(final File configurationDirectory)
	{
		String activeName = GeneralSettingsPanel.installedNameOfActiveLookAndFeel();
		if (activeName == null)
		{
			return false;
		}
		MysticCryptSettings settings = MysticCryptSettings.load(configurationDirectory);
		if (activeName.equals(settings.getLookAndFeel()))
		{
			return false;
		}
		settings.setLookAndFeel(activeName);
		settings.save(configurationDirectory);
		return true;
	}
}
