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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * The settings the Settings dialog opens on show the look on the screen (#1058)
 */
class SettingsForTheDialogTest
{

	@TempDir
	File configurationDirectory;

	private LookAndFeel original;

	@BeforeEach
	void rememberTheLookAndFeel()
	{
		original = UIManager.getLookAndFeel();
		FlatLafTheme.installAll();
	}

	@AfterEach
	void restoreTheLookAndFeel() throws Exception
	{
		UIManager.setLookAndFeel(original);
	}

	@Test
	void theLookOnTheScreen_replacesTheSavedOne() throws Exception
	{
		MysticCryptSettings saved = new MysticCryptSettings();
		saved.setLookAndFeel("FlatLaf Light");
		saved.setAutoLockMinutes(7);
		saved.save(configurationDirectory);
		UIManager.setLookAndFeel(new FlatDarkLaf());

		MysticCryptSettings shown = LookAndFeelChoice.settingsForTheDialog(configurationDirectory);

		assertEquals("FlatLaf Dark", shown.getLookAndFeel());
		assertEquals(7, shown.getAutoLockMinutes(), "the other saved settings are kept");
	}

	@Test
	void aLookThatIsNotInstalled_keepsTheSavedOne() throws Exception
	{
		MysticCryptSettings saved = new MysticCryptSettings();
		saved.setLookAndFeel("FlatLaf Light");
		saved.save(configurationDirectory);
		UIManager.setLookAndFeel(new MetalLookAndFeel()
		{
			private static final long serialVersionUID = 1L;
		});

		MysticCryptSettings shown = LookAndFeelChoice.settingsForTheDialog(configurationDirectory);

		assertEquals("FlatLaf Light", shown.getLookAndFeel());
	}
}
