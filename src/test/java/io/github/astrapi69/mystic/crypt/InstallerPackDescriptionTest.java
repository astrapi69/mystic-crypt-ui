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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The sentence the installer shows for its plugin pack names what that pack installs, and nothing
 * else (#436).
 * <p>
 * It named the menu designer, which the pack deliberately does not carry: the menu designer is
 * development tooling, built and tested but not shipped, and reachable only with
 * {@code -Dmystic.crypt.ui.menu.designer=true} (architecture.md). Nothing installed wrongly - the
 * text did, and a user reading it during installation was promised a tool they would not get.
 * <p>
 * Both halves are checked mechanically rather than against a list somebody keeps in step by hand: a
 * plugin that exists but is not packed must not appear in the sentence, and the sentence must name
 * as many things as the pack installs.
 */
class InstallerPackDescriptionTest
{

	private static final Path INSTALL_XML = Path.of("src/main/izpack/install.xml");

	private static final Pattern PLUGINS_PACK = Pattern
		.compile("<pack name=\"plugins\".*?<description>(.*?)</description>", Pattern.DOTALL);

	private static final Pattern PACKED_PLUGIN = Pattern
		.compile("<singlefile src=\"\\.\\./plugins/([a-z-]+)-plugin/");

	@Test
	@DisplayName("a plugin the installer does not carry is not named in its description")
	void aPluginThatIsNotInstalled_isNotNamed() throws IOException
	{
		String description = pluginPackDescription();
		List<String> packed = packedPlugins();

		for (String plugin : pluginsInTheRepository())
		{
			if (packed.contains(plugin))
			{
				continue;
			}
			String asProse = plugin.replace('-', ' ');
			assertFalse(description.toLowerCase().contains(asProse),
				"'" + asProse + "' is named in the installer's plugin pack, which does not "
					+ "install it: " + description);
		}
	}

	@Test
	@DisplayName("the description names as many tools as the pack installs")
	void theDescriptionNamesEveryInstalledPlugin() throws IOException
	{
		String description = pluginPackDescription();
		String list = description.substring(description.indexOf(':') + 1);
		long named = Stream.of(list.split(",| and ")).map(String::trim)
			.filter(part -> !part.isEmpty()).count();

		assertEquals(packedPlugins().size(), named,
			"the sentence names " + named + " tools and the pack installs " + packedPlugins().size()
				+ " - a plugin added to the pack has to be added to the sentence with it: "
				+ description);
	}

	private static String pluginPackDescription() throws IOException
	{
		Matcher matcher = PLUGINS_PACK
			.matcher(Files.readString(INSTALL_XML, StandardCharsets.UTF_8));
		assertTrue(matcher.find(), "the installer configuration has a pack named 'plugins'");
		return matcher.group(1).replaceAll("\\s+", " ").trim();
	}

	private static List<String> packedPlugins() throws IOException
	{
		Matcher matcher = PACKED_PLUGIN
			.matcher(Files.readString(INSTALL_XML, StandardCharsets.UTF_8));
		List<String> packed = new ArrayList<>();
		while (matcher.find())
		{
			packed.add(matcher.group(1));
		}
		assertFalse(packed.isEmpty(), "the plugins pack installs something");
		return packed;
	}

	/** Every plugin that exists in the working tree, by its directory name without the suffix */
	private static List<String> pluginsInTheRepository() throws IOException
	{
		try (Stream<Path> directories = Files.list(Path.of("plugins")))
		{
			return directories.filter(Files::isDirectory).map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith("-plugin"))
				.map(name -> name.substring(0, name.length() - "-plugin".length())).sorted()
				.toList();
		}
	}
}
