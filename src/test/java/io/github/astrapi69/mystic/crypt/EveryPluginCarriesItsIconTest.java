/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.ui.icon.PluginIcon;

/**
 * Every internal plugin under {@code plugins/} carries its own icon and sets it on its menu items
 * (#1192), so a new plugin without one turns this red instead of showing a bare menu entry.
 * <p>
 * The test reads the plugin directories, the way {@link PublicPluginGrantTest} does: the plugins
 * are not on this suite's class path. Per plugin it checks the two images {@link PluginIcon} reads,
 * the SVG they were rendered from, and that the plugin's menu contribution hands its items to
 * {@link PluginIcon#onEvery} under its own name - under another name it would find no image and its
 * items would stay without an icon, which only a log line would say.
 */
class EveryPluginCarriesItsIconTest
{

	/** Where the internal plugins live, one directory each */
	private static final Path PLUGINS = Path.of("plugins");

	/** Where the sources of the plugin icons live, one SVG each */
	private static final Path DESIGN = Path.of("design/plugin-icons");

	/** A call of {@link PluginIcon#onEvery} and the plugin name it passes, in one match */
	private static final Pattern ON_EVERY = Pattern.compile(
		"PluginIcon\\s*\\.\\s*onEvery\\s*\\([^;]*?,\\s*getClass\\(\\)\\s*,\\s*\"([a-z0-9-]+)\"\\s*\\)",
		Pattern.DOTALL);

	@Test
	@DisplayName("the scan reads every plugin, so an empty answer cannot pass for a clean one")
	void theScan_readsEveryPlugin()
	{
		List<String> scanned = plugins().toList();

		assertTrue(scanned.size() >= 15, "only " + scanned.size()
			+ " plugin directories were scanned, and this repository has 15: " + scanned);
		assertTrue(scanned.contains("checksum-plugin"), scanned.toString());
		assertTrue(scanned.contains("lethenon-plugin"), scanned.toString());
	}

	@ParameterizedTest(name = "{0} carries its icon in 16 and 32 px, square, with a transparent corner")
	@MethodSource("plugins")
	void everyPlugin_carriesBothImages(final String plugin) throws IOException
	{
		for (int size : PluginIcon.SIZES)
		{
			Path file = PLUGINS.resolve(plugin).resolve("src/main/resources")
				.resolve(PluginIcon.resource(plugin, size));
			assertTrue(Files.isRegularFile(file), "missing " + file);
			BufferedImage image = ImageIO.read(file.toFile());
			assertNotNull(image, file + " is not an image ImageIO can read");
			assertEquals(size, image.getWidth(), file.toString());
			assertEquals(size, image.getHeight(), file.toString());
			assertTrue(image.getColorModel().hasAlpha(), file + " has no alpha channel");
			assertEquals(0, image.getRGB(0, 0) >>> 24, file + ": the corner outside the tile");
			assertEquals(255, image.getRGB(size / 2, size / 2) >>> 24,
				file + ": the middle of the tile");
		}
	}

	@ParameterizedTest(name = "{0} keeps the SVG its images were rendered from")
	@MethodSource("plugins")
	void everyPlugin_hasItsSource(final String plugin)
	{
		Path source = DESIGN.resolve(plugin + ".svg");

		assertTrue(Files.isRegularFile(source), "missing " + source);
	}

	/**
	 * A plugin that brings menu items hands them to {@link PluginIcon#onEvery}. A plugin that
	 * brings no menu and only an action on an entry (#1120) needs no call: the host sets its icon
	 * on that item by its plugin id, in {@code EntryActions}
	 */
	@ParameterizedTest(name = "{0} sets its icon on its menu items, under its own name")
	@MethodSource("plugins")
	void everyPlugin_setsItsIconOnItsMenuItems(final String plugin) throws IOException
	{
		List<Path> contributions = sourcesOf(plugin, "MenuContribution.java");
		if (contributions.isEmpty())
		{
			assertTrue(
				sourcesOf(plugin, ".java").stream().map(EveryPluginCarriesItsIconTest::read)
					.anyMatch(source -> source.contains("implements EntryActionContribution")),
				plugin + " has neither a menu contribution nor an entry action");
			return;
		}
		assertEquals(1, contributions.size(),
			plugin + " has " + contributions.size() + " menu contributions: " + contributions);
		String contribution = read(contributions.get(0));
		Matcher matcher = ON_EVERY.matcher(contribution);

		assertTrue(matcher.find(),
			plugin + ": getMenuItems() does not hand its items to PluginIcon.onEvery");
		assertEquals(plugin, matcher.group(1),
			plugin + " passes another plugin's name, so it would load no image");
		assertFalse(matcher.find(), plugin + " sets its icon twice");
	}

	static Stream<String> plugins()
	{
		assertTrue(Files.isDirectory(PLUGINS), "no plugins directory at " + PLUGINS.toAbsolutePath()
			+ "; this test runs from the project directory");
		try (Stream<Path> entries = Files.list(PLUGINS))
		{
			return entries.filter(Files::isDirectory).map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith("-plugin")).sorted().toList().stream();
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}

	private static List<Path> sourcesOf(final String plugin, final String ending) throws IOException
	{
		Path main = PLUGINS.resolve(plugin).resolve("src/main/java");
		try (Stream<Path> files = Files.walk(main))
		{
			return files.filter(path -> path.getFileName().toString().endsWith(ending)).toList();
		}
	}

	private static String read(final Path source)
	{
		try
		{
			return Files.readString(source);
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}
}
