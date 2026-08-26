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
package io.github.astrapi69.mystic.crypt.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.cli.clash.ClashingContribution;
import io.github.astrapi69.mystic.crypt.cli.clash.FreeNameCommand;
import io.github.astrapi69.mystic.crypt.cli.clash.TakenNameCommand;

/**
 * The command line, run the way a user runs it: with plugins installed on disk, loaded by the real
 * plugin manager, assembled into the real command line.
 * <p>
 * This is the test that was missing. A dependency bump gave the library a command named
 * {@code keyx} while a plugin here contributed one too, and the whole command line stopped starting
 * - every command, not just that one. Nothing caught it, because the plugin command tests drove
 * their own command directly and the host tests assembled the command line from objects they handed
 * it themselves. Neither ever put a plugin on disk.
 */
class CommandLineWithInstalledPluginsTest
{

	private static final String PLUGIN_ID = "clashing-test-plugin";

	private String originalPluginsDirectory;

	@AfterEach
	void restoreThePluginsDirectory()
	{
		if (originalPluginsDirectory == null)
		{
			System.clearProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		}
		else
		{
			System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY,
				originalPluginsDirectory);
		}
	}

	private void pluginsLiveIn(final File directory)
	{
		originalPluginsDirectory = System.getProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY,
			directory.getAbsolutePath());
	}

	/**
	 * Packages the two commands and their contribution the way the plugin manager expects to find
	 * them: {@code plugin.properties} at the root, the classes underneath, and the extension named
	 * in {@code extensions.idx}
	 */
	private static void installTheClashingPlugin(final File directory) throws Exception
	{
		List<Class<?>> classes = List.of(ClashingContribution.class, TakenNameCommand.class,
			FreeNameCommand.class);
		try (ZipOutputStream zip = new ZipOutputStream(
			Files.newOutputStream(new File(directory, PLUGIN_ID + "-1.0.0.zip").toPath())))
		{
			zip.putNextEntry(new ZipEntry("plugin.properties"));
			zip.write(("plugin.id=" + PLUGIN_ID + "\nplugin.version=1.0.0\n"
				+ "plugin.description=a plugin that exists to take a name the library uses\n")
					.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.putNextEntry(new ZipEntry("classes/META-INF/extensions.idx"));
			zip.write(
				(ClashingContribution.class.getName() + "\n").getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			for (Class<?> type : classes)
			{
				String path = type.getName().replace('.', '/') + ".class";
				zip.putNextEntry(new ZipEntry("classes/" + path));
				try (java.io.InputStream compiled = type.getClassLoader().getResourceAsStream(path))
				{
					zip.write(compiled.readAllBytes());
				}
				zip.closeEntry();
			}
		}
	}

	private static String outputOf(final Runnable runnable)
	{
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try (PrintStream stream = new PrintStream(captured, true, StandardCharsets.UTF_8))
		{
			System.setOut(stream);
			System.setErr(stream);
			runnable.run();
		}
		finally
		{
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
		return captured.toString(StandardCharsets.UTF_8);
	}

	/**
	 * A plugin on disk taking a name the library uses must cost that one command and nothing else.
	 * Before the guard this failed with
	 * {@code DuplicateNameException: Another subcommand named 'hash' already exists}, thrown while
	 * the command line was still being assembled.
	 */
	@Test
	@DisplayName("an installed plugin that takes a library name does not stop the command line")
	void anInstalledClashDoesNotStopTheCommandLine(@TempDir File directory) throws Exception
	{
		installTheClashingPlugin(directory);
		pluginsLiveIn(directory);

		int[] exitCode = new int[1];
		String output = outputOf(() -> exitCode[0] = MysticCryptUiCli.execute("hash", "--help"));

		assertEquals(0, exitCode[0], "the command line did not run: " + output);
		assertTrue(output.contains("Usage:") && output.contains("hash"),
			"the library's hash command did not answer: " + output);
		assertFalse(output.contains("the plugin command ran, which it must not"),
			"the plugin took over a name the library documents: " + output);
	}

	/**
	 * And the neighbour with a free name survives, or the guard would be trading one outage for a
	 * smaller one
	 */
	@Test
	@DisplayName("the plugin's other command still works after the clash was left out")
	void theFreelyNamedCommandOfTheSamePluginStillWorks(@TempDir File directory) throws Exception
	{
		installTheClashingPlugin(directory);
		pluginsLiveIn(directory);

		int[] exitCode = new int[1];
		String output = outputOf(
			() -> exitCode[0] = MysticCryptUiCli.execute("a-name-nobody-uses"));

		assertEquals(0, exitCode[0], "the contributed command did not run: " + output);
		assertTrue(output.contains("the free command ran"), output);
	}

	/**
	 * With nothing installed the library's commands are still all there, so a failure above means
	 * the plugin and not the harness
	 */
	@Test
	@DisplayName("with an empty plugins directory the library commands run")
	void withoutAnyPluginTheLibraryCommandsRun(@TempDir File directory)
	{
		pluginsLiveIn(directory);

		int[] exitCode = new int[1];
		String output = outputOf(() -> exitCode[0] = MysticCryptUiCli.execute("keygen", "--help"));

		assertEquals(0, exitCode[0], output);
		assertTrue(output.contains("Usage:"), output);
	}

	/**
	 * The plugins directory the test points at is the one that was used, so this test cannot pass
	 * by quietly reading the developer's own installed plugins
	 */
	@Test
	@DisplayName("the plugins directory under test is the one that is read")
	void theDirectoryUnderTestIsTheOneThatIsRead(@TempDir File directory)
	{
		pluginsLiveIn(directory);

		assertEquals(Path.of(directory.getAbsolutePath()), MysticCryptUiCli.pluginsDirectory());
	}
}
