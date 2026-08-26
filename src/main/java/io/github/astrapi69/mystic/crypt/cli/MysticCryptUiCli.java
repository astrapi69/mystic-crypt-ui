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

import java.io.File;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginCommandContribution;
import picocli.CommandLine;

/**
 * The command line side of the application: the commands of the mystic-crypt library plus whatever
 * the installed plugins contribute through {@link PluginCommandContribution}.
 * <p>
 * Nothing is reimplemented here. The library's own root command is used as it is, so every command
 * it gains is available here the moment the library is updated, and a plugin only has to contribute
 * what the library does not cover.
 * <p>
 * This class never touches Swing, so it works on a machine without a display.
 */
public final class MysticCryptUiCli
{

	/**
	 * System property that points at the plugins directory; without it the installed one is used
	 */
	public static final String PLUGINS_DIRECTORY_PROPERTY = "mystic.crypt.ui.plugins.dir";

	/** The argument that switches the application from the user interface to the command line */
	public static final String CLI_ARGUMENT = "--cli";

	/** System property that keeps the logging of the plugin system as it is */
	public static final String VERBOSE_PROPERTY = "mystic.crypt.ui.cli.verbose";

	private MysticCryptUiCli()
	{
	}

	/**
	 * Whether the given application arguments ask for the command line instead of the user
	 * interface
	 *
	 * @param args
	 *            the arguments the application was started with
	 * @return true if the first argument is {@value #CLI_ARGUMENT}
	 */
	public static boolean isCliInvocation(final String[] args)
	{
		return args != null && args.length > 0 && CLI_ARGUMENT.equals(args[0]);
	}

	/**
	 * Removes the {@value #CLI_ARGUMENT} switch, leaving the command and its options
	 *
	 * @param args
	 *            the arguments the application was started with
	 * @return the arguments for the command line
	 */
	public static String[] stripCliArgument(final String[] args)
	{
		String[] remaining = new String[args.length - 1];
		System.arraycopy(args, 1, remaining, 0, remaining.length);
		return remaining;
	}

	/**
	 * The plugins directory the command line reads its commands from: the one named by
	 * {@value #PLUGINS_DIRECTORY_PROPERTY}, or the installed one under the user's home directory,
	 * which is exactly where the installer and the user interface put the plugin zips
	 *
	 * @return the plugins directory
	 */
	public static Path pluginsDirectory()
	{
		String configured = System.getProperty(PLUGINS_DIRECTORY_PROPERTY);
		if (configured != null && !configured.isBlank())
		{
			return Path.of(configured);
		}
		return new File(System.getProperty("user.home"),
			".config/" + MysticCryptApplicationFrame.APPLICATION_NAME + "/plugins").toPath();
	}

	/**
	 * Runs a command and returns its exit code
	 *
	 * @param args
	 *            the command and its options, without the {@value #CLI_ARGUMENT} switch
	 * @return the exit code of the command
	 */
	public static int execute(final String... args)
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		quietPluginLogging();
		PluginManager pluginManager = new DefaultPluginManager(pluginsDirectory());
		try
		{
			pluginManager.loadPlugins();
			pluginManager.startPlugins();
			return newCommandLine(pluginManager.getExtensions(PluginCommandContribution.class))
				.execute(args);
		}
		finally
		{
			try
			{
				pluginManager.stopPlugins();
			}
			catch (RuntimeException exception)
			{
				// pf4j trips over its own list of started plugins while stopping them; the command
				// has run at this point, so this must not turn a successful run into a failed one
				System.err.println("the plugins could not be stopped cleanly: " + exception);
			}
		}
	}

	/**
	 * Turns the logging of the plugin system down to warnings.
	 * <p>
	 * The command line writes its result to standard output, and so does the logging backend by
	 * default - which would put pf4j's debug output into the very same stream and make
	 * {@code ... --cli keystore list > entries.txt} unusable. Set {@value #VERBOSE_PROPERTY} to
	 * {@code true} to keep the logging as it is.
	 * <p>
	 * Done through reflection deliberately: this way the class needs no compile time dependency on
	 * a particular logging backend, and simply does nothing when another one is in use.
	 */
	public static void quietPluginLogging()
	{
		if (Boolean.getBoolean(VERBOSE_PROPERTY))
		{
			return;
		}
		try
		{
			Object root = Class.forName("org.slf4j.LoggerFactory")
				.getMethod("getLogger", String.class).invoke(null, "ROOT");
			Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
			Object warn = levelClass.getField("WARN").get(null);
			root.getClass().getMethod("setLevel", levelClass).invoke(root, warn);
		}
		catch (ReflectiveOperationException | RuntimeException exception)
		{
			// another backend, or none at all - then there is nothing to quiet down
		}
	}

	/**
	 * Builds the command line: the library's root command with the contributed plugin commands
	 * added to it
	 *
	 * @param contributions
	 *            the contributions of the started plugins
	 * @return the command line to execute
	 */
	public static CommandLine newCommandLine(final List<PluginCommandContribution> contributions)
	{
		// the library's root command carries all of its own subcommands; picocli instantiates it
		// itself, which also works with the non-public constructor it declares
		CommandLine commandLine = new CommandLine(
			io.github.astrapi69.mystic.crypt.cli.MysticCryptCli.class);
		for (Object command : commands(contributions))
		{
			addUnlessTheNameIsTaken(commandLine, command);
		}
		return commandLine;
	}


	/**
	 * Adds a contributed command, unless the library already has one under that name.
	 * <p>
	 * A plugin can be installed from anywhere, and a library release can take a name that was free
	 * yesterday, so a clash is a situation to survive rather than to prevent. Both ways it can go
	 * wrong are bad: picocli throws while the command line is still being assembled, which used to
	 * cost every other command including the library's own, and where it does not throw it replaces
	 * the library command silently, so {@code hash} would quietly become something else.
	 * <p>
	 * The name is therefore checked before the command is added, and the library keeps it.
	 *
	 * @param commandLine
	 *            the command line being assembled
	 * @param command
	 *            the contributed command
	 */
	private static void addUnlessTheNameIsTaken(final CommandLine commandLine, final Object command)
	{
		try
		{
			String name = CommandLine.Model.CommandSpec.forAnnotatedObject(command).name();
			if (commandLine.getSubcommands().containsKey(name))
			{
				// the command line is a terminal tool, so this belongs where the user can see it
				System.err.println("the plugin command '" + name
					+ "' was left out: the name is already in use by mystic-crypt");
				return;
			}
			commandLine.addSubcommand(command);
		}
		catch (RuntimeException notUsable)
		{
			System.err.println("a plugin command is not usable and was left out: " + notUsable);
		}
	}

	/**
	 * Collects the commands of all contributions, skipping a plugin that fails instead of letting
	 * it take the whole command line down
	 *
	 * @param contributions
	 *            the contributions of the started plugins
	 * @return the contributed commands
	 */
	public static List<Object> commands(final List<PluginCommandContribution> contributions)
	{
		List<Object> commands = new ArrayList<>();
		if (contributions == null)
		{
			return commands;
		}
		for (PluginCommandContribution contribution : contributions)
		{
			try
			{
				List<Object> contributed = contribution.getCommands();
				if (contributed != null)
				{
					commands.addAll(contributed);
				}
			}
			catch (RuntimeException exception)
			{
				System.err.println("the commands of " + contribution.getClass().getName()
					+ " are not available: " + exception);
			}
		}
		return commands;
	}
}
