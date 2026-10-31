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
package io.github.astrapi69.mystic.crypt.plugin.releasekey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The status panel reads the configured home when it opens, holds what the user types in its model,
 * and reads the typed home when Read is pressed
 */
class ReleaseKeyStatusPanelTest
{

	@TempDir
	static Path directory;

	private static ThrowawayGnuPgHome home;

	private static String release;

	@TempDir
	File configurationDirectory;

	@BeforeAll
	static void writeTheHomeWithGpg() throws IOException
	{
		home = ThrowawayGnuPgHome.create(directory.resolve("home"));
		release = home.generate("Release Test <release@example.org>", "ed25519", "sign,cert",
			"0");
		home.addSubkey(release, "ed25519", "sign", "1y");
		home.takePrimaryOffline(release);
	}

	@AfterAll
	static void stopTheAgent() throws IOException
	{
		home.close();
	}

	@BeforeEach
	void configureTheThrowawayHome() throws IOException
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		ReleaseKeySettingsContribution contribution = new ReleaseKeySettingsContribution();
		PluginSettings.save(configurationDirectory, contribution.getPluginId(),
			contribution.getDefaults(), Map.of(ReleaseKeySettingsContribution.KEY_GNUPG_HOME,
				home.directory().toString(), ReleaseKeySettingsContribution.KEY_RELEASE_KEY, release));
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the configured home and release key are read as the panel opens")
	void opening_readsTheConfiguredHome()
	{
		ReleaseKeyStatusPanel panel = new ReleaseKeyStatusPanel();

		assertEquals(home.directory().toString(), named(panel, "txtGnuPgHome", JTextField.class)
			.getText());
		assertEquals(release, named(panel, "txtReleaseKey", JTextField.class).getText());
		JTable table = named(panel, "tblKeys", JTable.class);
		assertEquals(2, table.getRowCount());
		assertEquals("offline", table.getValueAt(0, 6));
		assertEquals("here", table.getValueAt(1, 6));
		assertEquals("valid", table.getValueAt(0, 7));
	}

	@Test
	@DisplayName("a typed home lands in the model, and Read reads it")
	void read_readsTheTypedHome()
	{
		ReleaseKeyStatusPanel panel = new ReleaseKeyStatusPanel();
		Path missing = directory.resolve("no-such-home");

		named(panel, "txtGnuPgHome", JTextField.class).setText(missing.toString());
		named(panel, "btnRead", JButton.class).doClick();

		assertEquals(missing.toString(), panel.getModelObject().getGnuPgHomePath());
		assertEquals(0, named(panel, "tblKeys", JTable.class).getRowCount());
		String result = named(panel, "lblResult", JLabel.class).getText();
		assertTrue(result.contains(missing.toString()), result);
	}

	@Test
	@DisplayName("a typed release key lands in the model, and blank shows every certificate")
	void read_readsTheTypedReleaseKey()
	{
		ReleaseKeyStatusPanel panel = new ReleaseKeyStatusPanel();

		named(panel, "txtReleaseKey", JTextField.class).setText("");
		named(panel, "btnRead", JButton.class).doClick();

		assertEquals("", panel.getModelObject().getReleaseKey());
		assertEquals(2, named(panel, "tblKeys", JTable.class).getRowCount());
		String result = named(panel, "lblResult", JLabel.class).getText();
		assertTrue(result.contains("1 certificate"), result);
	}

	private static <T extends Component> T named(final Container container, final String name,
		final Class<T> type)
	{
		T found = find(container, name, type);
		if (found == null)
		{
			throw new AssertionError("no " + type.getSimpleName() + " named " + name);
		}
		return found;
	}

	private static <T extends Component> T find(final Container container, final String name,
		final Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = find(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
