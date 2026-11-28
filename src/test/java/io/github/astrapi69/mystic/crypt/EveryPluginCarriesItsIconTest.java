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

/**
 * Every internal plugin under {@code plugins/} carries its own icon and sets it on its menu items
 * (#1192), so a new plugin without one turns this red instead of showing a bare menu entry.
 * <p>
 * The test reads the plugin directories, the way {@link PublicPluginGrantTest} does: the plugins
 * are not on this suite's class path. Per plugin it checks the two images next to its menu
 * contribution class, the SVG they were rendered from, and that the contribution reads both of them
 * next to itself. The end-to-end test {@code EveryPluginShowsAnIconOfItsOwnUiTest} checks the same
 * through the built zips and the menu a user sees; this one answers in the everyday build.
 */
class EveryPluginCarriesItsIconTest
{

	/** Where the internal plugins live, one directory each */
	private static final Path PLUGINS = Path.of("plugins");

	/** Where the sources of the plugin icons live, one SVG each */
	private static final Path DESIGN = Path.of("design/plugin-icons");

	/** The sizes every plugin carries: 16 px for the menu, 32 px for a scaled screen */
	private static final List<Integer> SIZES = List.of(16, 32);

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
		Path resources = resourcesNextTo(plugin, menuContributionOf(plugin));
		for (int size : SIZES)
		{
			Path file = resources.resolve(imageName(size));
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

	@ParameterizedTest(name = "{0} reads both images next to its menu contribution, once each")
	@MethodSource("plugins")
	void everyPlugin_readsItsImagesNextToItsContribution(final String plugin) throws IOException
	{
		String contribution = Files.readString(menuContributionOf(plugin));
		for (int size : SIZES)
		{
			Matcher matcher = Pattern.compile("getClass\\(\\)\\s*\\.\\s*getResource\\(\\s*\""
				+ Pattern.quote(imageName(size)) + "\"\\s*\\)").matcher(contribution);
			int reads = 0;
			while (matcher.find())
			{
				reads++;
			}
			assertEquals(1, reads, plugin + " reads " + imageName(size) + " " + reads
				+ " times next to its menu contribution, once is right");
		}
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

	private static String imageName(final int size)
	{
		return "icon-" + size + ".png";
	}

	/**
	 * The resource directory of the contribution's package, where {@code getClass().getResource}
	 * finds a name without a path
	 */
	private static Path resourcesNextTo(final String plugin, final Path contribution)
	{
		Path pluginSources = PLUGINS.resolve(plugin).resolve("src/main/java");
		return PLUGINS.resolve(plugin).resolve("src/main/resources")
			.resolve(pluginSources.relativize(contribution.getParent()));
	}

	private static Path menuContributionOf(final String plugin) throws IOException
	{
		Path main = PLUGINS.resolve(plugin).resolve("src/main/java");
		try (Stream<Path> files = Files.walk(main))
		{
			List<Path> contributions = files
				.filter(path -> path.getFileName().toString().endsWith("MenuContribution.java"))
				.toList();
			assertEquals(1, contributions.size(),
				plugin + " has " + contributions.size() + " menu contributions: " + contributions);
			return contributions.get(0);
		}
	}
}
