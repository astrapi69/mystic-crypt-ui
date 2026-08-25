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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginCommandContribution;
import picocli.CommandLine;

/**
 * Headless tests of {@link MysticCryptUiCli}: the command line is built from the library's own root
 * command plus whatever the plugins contribute, and a broken contribution must never take the whole
 * command line down.
 */
class MysticCryptUiCliTest
{

	/** A contribution like a plugin provides one */
	static class WorkingContribution implements PluginCommandContribution
	{
		@Override
		public List<Object> getCommands()
		{
			return List.of(new TestCommand());
		}
	}

	/** A contribution that fails the way a plugin with a missing class would */
	static class BrokenContribution implements PluginCommandContribution
	{
		@Override
		public List<Object> getCommands()
		{
			throw new IllegalStateException("this plugin is broken");
		}
	}

	@CommandLine.Command(name = "test-command", description = "a contributed command")
	static class TestCommand implements Runnable
	{
		@Override
		public void run()
		{
			System.out.print("");
		}
	}

	@ParameterizedTest
	@CsvSource({ "--cli,true", "-cli,false", "cli,false", "'',false" })
	void recognizesTheCliInvocationOnlyByTheExactSwitch(String argument, boolean expected)
	{
		assertEquals(expected, MysticCryptUiCli.isCliInvocation(new String[] { argument }));
	}

	@Test
	void withoutArgumentsTheApplicationStartsItsUserInterface()
	{
		assertFalse(MysticCryptUiCli.isCliInvocation(new String[0]),
			"an application started without arguments must show its window");
		assertFalse(MysticCryptUiCli.isCliInvocation(null));
	}

	@Test
	void stripsTheCliSwitchAndKeepsTheCommand()
	{
		String[] remaining = MysticCryptUiCli
			.stripCliArgument(new String[] { "--cli", "hash", "--text", "secret" });

		assertEquals(List.of("hash", "--text", "secret"), List.of(remaining));
	}

	@Test
	void theLibraryCommandsAreAvailableWithoutAnyContribution()
	{
		CommandLine commandLine = MysticCryptUiCli.newCommandLine(List.of());

		assertNotNull(commandLine.getCommandSpec().userObject(),
			"picocli must be able to instantiate the library's root command");
		assertFalse(commandLine.getSubcommands().isEmpty(),
			"the library's own subcommands must be reachable, was: "
				+ commandLine.getSubcommands().keySet());
	}

	/** The commands the library brings today; they must not need a contribution */
	@ParameterizedTest
	@ValueSource(strings = { "hash", "verify", "keygen", "checksum", "obfuscate" })
	void aLibraryCommandIsReachableThroughTheApplicationsCommandLine(String command)
	{
		assertTrue(MysticCryptUiCli.newCommandLine(List.of()).getSubcommands().containsKey(command),
			"'" + command + "' must be reachable without a plugin");
	}

	@Test
	void aContributedCommandIsAddedToTheLibraryCommands()
	{
		CommandLine commandLine = MysticCryptUiCli
			.newCommandLine(List.of(new WorkingContribution()));

		assertTrue(commandLine.getSubcommands().containsKey("test-command"),
			"the contributed command must be reachable, was: "
				+ commandLine.getSubcommands().keySet());
		assertTrue(commandLine.getSubcommands().containsKey("hash"),
			"a contribution must not replace the library commands");
	}

	@Test
	void aBrokenContributionIsSkippedInsteadOfBreakingTheCommandLine()
	{
		List<Object> commands = MysticCryptUiCli
			.commands(List.of(new BrokenContribution(), new WorkingContribution()));

		assertEquals(1, commands.size(),
			"the working contribution must survive a broken one next to it");
	}

	@Test
	void noContributionsAtAllIsNotAnError()
	{
		assertTrue(MysticCryptUiCli.commands(null).isEmpty());
		assertTrue(MysticCryptUiCli.commands(List.of()).isEmpty());
	}

	@Test
	void aLibraryCommandRunsThroughTheApplicationsCommandLine()
	{
		StringWriter out = new StringWriter();
		CommandLine commandLine = MysticCryptUiCli.newCommandLine(List.of());
		commandLine.setOut(new PrintWriter(out));

		int exitCode = commandLine.execute("checksum", "--help");

		assertEquals(0, exitCode, "the library command must run, wrote: " + out);
		assertFalse(out.toString().isBlank(), "the library command must produce output");
	}

	@Test
	void runsACommandEndToEndWithoutAnyPluginInstalled(
		@org.junit.jupiter.api.io.TempDir File emptyPluginsDirectory)
	{
		String original = System.getProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		try
		{
			System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY,
				emptyPluginsDirectory.getAbsolutePath());

			// the whole path: load the plugins, build the command line, run, stop the plugins - the
			// exit code must be the one of the command, whatever happens while stopping
			assertEquals(0, MysticCryptUiCli.execute("checksum", "--help"));
			assertEquals(2, MysticCryptUiCli.execute("does-not-exist"),
				"an unknown command must end with picocli's usage exit code");
		}
		finally
		{
			if (original != null)
			{
				System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY, original);
			}
			else
			{
				System.clearProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
			}
		}
	}

	@Test
	void quietingTheLoggingWorksWithAndWithoutABackend()
	{
		// must never throw, whatever backend is on the classpath
		MysticCryptUiCli.quietPluginLogging();
		System.setProperty(MysticCryptUiCli.VERBOSE_PROPERTY, "true");
		try
		{
			MysticCryptUiCli.quietPluginLogging();
		}
		finally
		{
			System.clearProperty(MysticCryptUiCli.VERBOSE_PROPERTY);
		}
	}

	@Test
	void thePluginsDirectoryCanBePointedSomewhereElse()
	{
		String original = System.getProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		try
		{
			System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY, "/tmp/some-plugins");
			assertEquals(Path.of("/tmp/some-plugins"), MysticCryptUiCli.pluginsDirectory());

			System.clearProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
			assertTrue(
				MysticCryptUiCli.pluginsDirectory().toString()
					.endsWith(".config/mystic-crypt-ui/plugins"),
				"without the property the installed plugins directory is used, was: "
					+ MysticCryptUiCli.pluginsDirectory());
		}
		finally
		{
			if (original != null)
			{
				System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY, original);
			}
			else
			{
				System.clearProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
			}
		}
	}
}
