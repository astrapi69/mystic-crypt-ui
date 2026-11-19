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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * The active look and feel is what ends up in the settings (#1059)
 */
class LookAndFeelChoiceTest
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
	void aDifferentActiveLook_isWritten() throws Exception
	{
		UIManager.setLookAndFeel(new FlatDarkLaf());

		assertTrue(LookAndFeelChoice.persistActive(configurationDirectory));

		assertEquals("FlatLaf Dark",
			MysticCryptSettings.load(configurationDirectory).getLookAndFeel());
	}

	@Test
	void aJdkLook_isWritten() throws Exception
	{
		UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

		assertTrue(LookAndFeelChoice.persistActive(configurationDirectory));

		assertEquals("Metal", MysticCryptSettings.load(configurationDirectory).getLookAndFeel());
	}

	@Test
	void theLookTheSettingsAlreadyHold_isNotWrittenAgain() throws Exception
	{
		UIManager.setLookAndFeel(new FlatDarkLaf());
		LookAndFeelChoice.persistActive(configurationDirectory);

		assertFalse(LookAndFeelChoice.persistActive(configurationDirectory));
	}

	@Test
	void otherSettings_surviveTheWrite() throws Exception
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setAutoLockMinutes(7);
		settings.save(configurationDirectory);
		UIManager.setLookAndFeel(new FlatDarkLaf());

		LookAndFeelChoice.persistActive(configurationDirectory);

		assertEquals(7, MysticCryptSettings.load(configurationDirectory).getAutoLockMinutes());
	}

	@Test
	void unreadableSettings_areLeftAsTheyAre() throws Exception
	{
		File settingsFile = new File(configurationDirectory, MysticCryptSettings.JSON_FILENAME);
		String corrupt = "{ this is not json";
		Files.writeString(settingsFile.toPath(), corrupt);
		UIManager.setLookAndFeel(new FlatDarkLaf());

		assertFalse(LookAndFeelChoice.persistActive(configurationDirectory));

		assertEquals(corrupt, Files.readString(settingsFile.toPath()),
			"the unreadable file was replaced by defaults plus the look");
	}
}
