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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which plugins may be used without a vault, asserted as the GRANT rather than as the offering.
 * <p>
 * {@code PublicMenuInventoryUiTest} checks that nothing reachable in the locked state is missing
 * from the host's allow-list - the dangerous direction. It cannot see a plugin that joins that
 * list: the list is built from what the plugins declare ({@code DesktopMenu:638-642}), so a new
 * contribution returning true from {@code isUsableWithoutAVault()} adds itself to the very set it
 * would be measured against, and every test stays green while a tool becomes reachable without a
 * sign-in. That is how #232 happened, and #232's answer was the list, not a rule about who may be
 * on it (#469).
 * <p>
 * So the expected grants are written down HERE, each with the reason it is allowed, and the test
 * reads the plugin sources. A new opt-in turns this red, and whoever wants it adds a line and its
 * justification in the same diff.
 * <p>
 * It reads sources rather than classes because a plugin is its own Gradle build: its classes are
 * not on this suite's classpath, and the e2e suites that install zips need a display. The same
 * trade as {@link InstallerPackDescriptionTest}, which reads the installer's XML.
 */
class PublicPluginGrantTest
{

	/** Where the internal plugins live, one directory each */
	private static final Path PLUGINS = Path.of("plugins");

	/**
	 * Who is allowed to be reachable without a vault, and why. A tool belongs here only when it
	 * touches no vault at all - the same category as a password generator in a password manager,
	 * which every comparable application offers while locked.
	 */
	private static final Map<String, String> ALLOWED = allowed();

	private static Map<String, String> allowed()
	{
		Map<String, String> allowed = new LinkedHashMap<>();
		allowed.put("checksum-plugin", "computes checksums of files the user picks; it reads no "
			+ "entry, writes no vault and needs no master password (#232)");
		// the second opt-in, which brought the locked-state harness with it (#301, #468)
		allowed.put("password-hash-plugin", "hashes and verifies a typed password; it reads no "
			+ "entry, writes no vault, opens no file and needs no master password (#301)");
		return allowed;
	}

	/** An override of the method, and the value it returns, in one match */
	private static final Pattern GRANT = Pattern.compile(
		"public\\s+boolean\\s+isUsableWithoutAVault\\s*\\(\\s*\\)\\s*\\{\\s*return\\s+(true|false)\\s*;",
		Pattern.DOTALL);

	@Test
	@DisplayName("exactly the plugins written down here may be used without a vault")
	void onlyTheWrittenDownPlugins_areUsableWithoutAVault() throws IOException
	{
		Map<String, String> declared = declaredGrants();

		assertEquals(ALLOWED.keySet(), declared.keySet(),
			"the set of plugins reachable without a vault changed. Every entry needs a reason in "
				+ "ALLOWED above, and a tool that touches a vault does not belong there at all "
				+ "(#469). What the sources declare: " + declared);
	}

	@Test
	@DisplayName("the scan reads every plugin, so an empty answer cannot pass for a clean one")
	void theScan_readsEveryPlugin() throws IOException
	{
		Set<String> scanned = pluginDirectories();

		assertTrue(scanned.size() >= 13,
			"only " + scanned.size() + " plugin directories were "
				+ "scanned, and this repository has more - a scan that cannot scan must not report "
				+ "green: " + scanned);
		assertTrue(scanned.contains("checksum-plugin"), scanned.toString());
		assertTrue(scanned.contains("lethenon-plugin"), scanned.toString());
	}

	@Test
	@DisplayName("a plugin that only names the method in its javadoc has not granted anything")
	void aJavadocMention_isNotAGrant() throws IOException
	{
		Path lethenon = PLUGINS.resolve("lethenon-plugin");
		String sources = sourcesOf(lethenon);

		assertTrue(sources.contains("isUsableWithoutAVault"),
			"precondition: the lethenon plugin discusses the method, which is what makes it the "
				+ "case worth pinning");
		assertEquals(Set.of(), grantsIn(sources),
			"it declines deliberately - its submenu carries a wallet operation beside a read-only "
				+ "verifier, and the grant is per contribution rather than per item (#301)");
	}

	private Map<String, String> declaredGrants() throws IOException
	{
		Map<String, String> declared = new LinkedHashMap<>();
		for (String plugin : pluginDirectories())
		{
			if (grantsIn(sourcesOf(PLUGINS.resolve(plugin))).contains("true"))
			{
				declared.put(plugin, "declared in " + plugin);
			}
		}
		return declared;
	}

	private static Set<String> grantsIn(final String sources)
	{
		Set<String> values = new TreeSet<>();
		Matcher matcher = GRANT.matcher(sources);
		while (matcher.find())
		{
			values.add(matcher.group(1));
		}
		return values;
	}

	private static String sourcesOf(final Path plugin) throws IOException
	{
		Path main = plugin.resolve("src/main/java");
		if (!Files.isDirectory(main))
		{
			return "";
		}
		StringBuilder sources = new StringBuilder();
		try (Stream<Path> files = Files.walk(main))
		{
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList())
			{
				sources.append(Files.readString(file)).append('\n');
			}
		}
		return sources.toString();
	}

	private static Set<String> pluginDirectories() throws IOException
	{
		assertTrue(Files.isDirectory(PLUGINS), "no plugins directory at " + PLUGINS.toAbsolutePath()
			+ "; this test runs from the " + "project directory");
		try (Stream<Path> entries = Files.list(PLUGINS))
		{
			return new TreeSet<>(
				entries.filter(Files::isDirectory).map(path -> path.getFileName().toString())
					.filter(name -> name.endsWith("-plugin")).toList());
		}
	}
}
