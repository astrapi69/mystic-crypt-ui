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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Headless tests of {@link PluginSettings}: every declared key always has a value, what the user
 * stored wins over the default, and a key a plugin no longer declares does not survive a save.
 */
class PluginSettingsTest
{

	private static final String PLUGIN_ID = "test-plugin";

	private static Map<String, String> defaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put("first", "1");
		defaults.put("second", "two");
		return defaults;
	}

	@Test
	void withoutAStoredFileEveryDeclaredKeyHasItsDefault(@TempDir File configurationDirectory)
	{
		Map<String, String> loaded = PluginSettings.load(configurationDirectory, PLUGIN_ID,
			defaults());

		assertEquals(defaults(), loaded, "a fresh installation must see the declared defaults");
		assertEquals(List.of("first", "second"), List.copyOf(loaded.keySet()),
			"the order the plugin declares must be kept");
	}

	@Test
	void whatWasSavedIsWhatIsLoaded(@TempDir File configurationDirectory) throws Exception
	{
		Map<String, String> values = new LinkedHashMap<>(defaults());
		values.put("second", "changed");

		PluginSettings.save(configurationDirectory, PLUGIN_ID, defaults(), values);

		assertEquals("changed",
			PluginSettings.load(configurationDirectory, PLUGIN_ID, defaults()).get("second"));
		assertTrue(Files.exists(PluginSettings.file(configurationDirectory, PLUGIN_ID)),
			"saving must write the file of this plugin");
	}

	@Test
	void aKeyTheStoredFileDoesNotHaveFallsBackToItsDefault(@TempDir File configurationDirectory)
		throws Exception
	{
		Path file = PluginSettings.file(configurationDirectory, PLUGIN_ID);
		Files.createDirectories(file.getParent());
		// a file written by an older version of the plugin, which knew only one of the keys
		Files.writeString(file, "first=stored", StandardCharsets.UTF_8);

		Map<String, String> loaded = PluginSettings.load(configurationDirectory, PLUGIN_ID,
			defaults());

		assertEquals("stored", loaded.get("first"));
		assertEquals("two", loaded.get("second"), "the key that is missing must get its default");
	}

	@Test
	void aKeyThatIsNoLongerDeclaredDoesNotSurviveASave(@TempDir File configurationDirectory)
		throws Exception
	{
		Map<String, String> values = new LinkedHashMap<>(defaults());
		values.put("gone", "left over from an older version");

		PluginSettings.save(configurationDirectory, PLUGIN_ID, defaults(), values);

		assertFalse(Files.readString(PluginSettings.file(configurationDirectory, PLUGIN_ID))
			.contains("gone"), "only the declared keys belong in the file");
	}

	@Test
	void aValueThatIsMissingIsSavedAsItsDefault(@TempDir File configurationDirectory)
		throws Exception
	{
		PluginSettings.save(configurationDirectory, PLUGIN_ID, defaults(), Map.of("first", "1"));

		assertEquals("two",
			PluginSettings.load(configurationDirectory, PLUGIN_ID, defaults()).get("second"));
	}

	@Test
	void anUnreadableFileFallsBackToTheDefaults(@TempDir File configurationDirectory)
		throws Exception
	{
		Path file = PluginSettings.file(configurationDirectory, PLUGIN_ID);
		Files.createDirectories(file.getParent());
		// a directory where the properties file is expected: it exists, but cannot be read
		Files.createDirectories(file);

		assertEquals(defaults(), PluginSettings.load(configurationDirectory, PLUGIN_ID, defaults()),
			"an unusable file must never keep a plugin from starting");
	}

	@Test
	void resettingRemovesTheStoredValues(@TempDir File configurationDirectory) throws Exception
	{
		PluginSettings.save(configurationDirectory, PLUGIN_ID, defaults(),
			Map.of("first", "changed", "second", "changed"));

		assertTrue(PluginSettings.reset(configurationDirectory, PLUGIN_ID),
			"the stored file must be removed");
		assertEquals(defaults(), PluginSettings.load(configurationDirectory, PLUGIN_ID, defaults()),
			"after a reset the defaults apply again");
		assertFalse(PluginSettings.reset(configurationDirectory, PLUGIN_ID),
			"resetting twice is not an error, but nothing is removed the second time");
	}

	@Test
	void everyPluginHasItsOwnFile(@TempDir File configurationDirectory) throws Exception
	{
		PluginSettings.save(configurationDirectory, "one", defaults(), Map.of("first", "for one"));
		PluginSettings.save(configurationDirectory, "two", defaults(), Map.of("first", "for two"));

		assertEquals("for one",
			PluginSettings.load(configurationDirectory, "one", defaults()).get("first"));
		assertEquals("for two",
			PluginSettings.load(configurationDirectory, "two", defaults()).get("first"));
	}

	@ParameterizedTest
	@CsvSource({ "42,42", " 42 ,42", "not a number,7", "'',7" })
	void readsANumberAndFallsBackWhenTheValueIsNotOne(String stored, int expected)
	{
		assertEquals(expected, PluginSettings.asInt(Map.of("key", stored), "key", 7));
	}

	@Test
	void aMissingKeyReadAsANumberFallsBack()
	{
		assertEquals(7, PluginSettings.asInt(Map.of(), "key", 7));
	}

	@ParameterizedTest
	@ValueSource(strings = { "keystore-plugin", "menu-designer-plugin" })
	void theFileIsNamedAfterThePlugin(String pluginId, @TempDir File configurationDirectory)
	{
		Path file = PluginSettings.file(configurationDirectory, pluginId);

		assertEquals(pluginId + ".properties", file.getFileName().toString());
		assertEquals(PluginSettings.DIRECTORY_NAME, file.getParent().getFileName().toString());
	}

	@Test
	void aPluginCanLoadItsSettingsWithoutKnowingTheConfigurationDirectory(
		@TempDir File configurationDirectory) throws Exception
	{
		// this is the overload a plugin uses: it runs in its own class loader and has no access to
		// the application frame, so it resolves the directory itself
		String original = System.getProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
		try
		{
			System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
				configurationDirectory.getAbsolutePath());
			PluginSettings.save(configurationDirectory, PLUGIN_ID, defaults(),
				Map.of("first", "from the file"));

			Map<String, String> loaded = PluginSettings.load(PLUGIN_ID, defaults());

			assertEquals("from the file", loaded.get("first"));
			assertEquals("two", loaded.get("second"), "a missing key still gets its default");
		}
		finally
		{
			if (original != null)
			{
				System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY, original);
			}
			else
			{
				System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
			}
		}
	}

	@Test
	void theConfigurationDirectoryCanBePointedSomewhereElse()
	{
		String original = System.getProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
		try
		{
			System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY, "/tmp/some-config");
			assertEquals(new File("/tmp/some-config"), PluginSettings.configurationDirectory());

			System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
			assertTrue(
				PluginSettings.configurationDirectory().getPath()
					.endsWith(".config/mystic-crypt-ui"),
				"without the property the installed configuration directory is used, was: "
					+ PluginSettings.configurationDirectory());
		}
		finally
		{
			if (original != null)
			{
				System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY, original);
			}
			else
			{
				System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
			}
		}
	}
}
