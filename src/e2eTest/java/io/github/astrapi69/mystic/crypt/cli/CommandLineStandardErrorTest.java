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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pf4j.Plugin;

import io.github.astrapi69.mystic.crypt.StartMysticCryptApplication;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * What a successful {@code --cli} run writes to standard error (#986), for every command of the
 * command line, once without any plugin and once with the plugins the installer ships, each run a
 * JVM of its own started the way a user starts the application.
 * <p>
 * The plugins are the zips {@code src/main/izpack/install.xml} installs into
 * {@code ~/.config/mystic-crypt-ui/plugins}, taken from their builds; the suite declares them as
 * inputs (#836). Every command is called with input it succeeds on and that it answers on standard
 * output, so whatever arrives on standard error comes from the application around it. Nothing does,
 * unless the verbose switch asks for it; a warning or an error about the call, and what a command
 * reports on standard error itself, still show.
 */
class CommandLineStandardErrorTest
{

	/** The installer's target directory for a plugin zip, relative to the user's home */
	private static final String PLUGINS_UNDER_HOME = ".config/mystic-crypt-ui/plugins";

	/** A plugin target in the installer: the zip's source and where it is installed */
	private static final Pattern SHIPPED_PLUGIN = Pattern.compile(
		"<singlefile src=\"([^\"]+\\.zip)\"\\s+target=\"\\$\\{USER_HOME}/\\.config/mystic-crypt-ui/plugins/[^\"]+\"");

	/** The launcher variables that make every JVM announce itself on standard error */
	private static final List<String> JVM_ANNOUNCING_VARIABLES = List.of("JAVA_TOOL_OPTIONS",
		"_JAVA_OPTIONS", "JDK_JAVA_OPTIONS");

	@TempDir
	static Path inputs;

	@TempDir
	static Path homes;

	private static Path withoutPlugins;

	private static Path withShippedPlugins;

	private static List<Path> shippedPlugins;

	private static final String PASSPHRASE = TestPasswords.throwaway();

	private static String encodedHash;

	private static String sealedText;

	private static List<String> shares;

	/** How the plugins are installed for a run */
	enum Plugins
	{
		/** No plugins directory at all, as after an installation without the plugins pack */
		NONE,
		/** The installer's plugins in the directory the installer puts them in */
		SHIPPED
	}

	/** One command and the arguments it succeeds on; {in} and {out} are filled in per run */
	record Call(String name, String standardInput, List<String> args) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	/** What a run ended with and wrote */
	record Run(int exitCode, String out, String err) {
	}

	@BeforeAll
	static void prepareTheInputsAndTheHomes() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		shippedPlugins = shippedPluginZips();
		withoutPlugins = Files.createDirectories(homes.resolve("without-plugins"));
		withShippedPlugins = Files.createDirectories(homes.resolve("with-shipped-plugins"));
		Path pluginsDirectory = Files
			.createDirectories(withShippedPlugins.resolve(PLUGINS_UNDER_HOME));
		for (Path zip : shippedPlugins)
		{
			Files.copy(zip, pluginsDirectory.resolve(zip.getFileName()));
		}
		Files.writeString(inputs.resolve("plain.txt"), "a file to work on\n");
		writeRsaPrivateKeyAsDer();
		Path gnupgHome = Files.createDirectories(inputs.resolve("gnupg"));
		Files.setPosixFilePermissions(gnupgHome, PosixFilePermissions.fromString("rwx------"));
		library("", "der2pem", "--in=" + inputs.resolve("rsa.der"),
			"--out=" + inputs.resolve("rsa.pem"));
		library("", "keygen", "-a", "ML-DSA-65", "--out-private=" + inputs.resolve("mldsa.pem"),
			"--out-public=" + inputs.resolve("mldsa.pub.pem"));
		library("", "sign", "-a", "ML-DSA-65", "--in=" + inputs.resolve("plain.txt"),
			"--key=" + inputs.resolve("mldsa.pem"), "--signature=" + inputs.resolve("plain.sig"));
		library(PASSPHRASE + "\n", "keystore", "create", "-f",
			inputs.resolve("store.p12").toString(), "--password-stdin");
		encodedHash = library(PASSPHRASE + "\n", "hash", "--password-stdin").strip();
		sealedText = library(PASSPHRASE + "\n", "encrypt", "--text=a text to seal",
			"--passphrase-stdin").strip();
		shares = library("a secret to share\n", "share", "split", "--secret-stdin", "-n", "3", "-t",
			"2").strip().lines().toList();
	}

	static List<Call> everyCommand()
	{
		return List.of(call("hash", PASSPHRASE + "\n", "hash", "--password-stdin"),
			call("verify", PASSPHRASE + "\n", "verify", "--hash=" + encodedHash,
				"--password-stdin"),
			call("keygen", "", "keygen", "-a", "RSA"), call("kem", "", "kem"),
			call("checksum", "", "checksum", "{in}/plain.txt"),
			call("der2pem", "", "der2pem", "--in={in}/rsa.der"),
			call("obfuscate", "", "obfuscate", "--rule=a=x", "--text=banana"),
			call("disentangle", "", "disentangle", "--rule=a=x", "--text=bxnxnx"),
			call("cert", "", "cert", "--out={out}/cert.pem"),
			call("keystore create", PASSPHRASE + "\n", "keystore", "create", "-f", "{out}/new.p12",
				"--password-stdin"),
			call("keystore list", PASSPHRASE + "\n", "keystore", "list", "-f", "{in}/store.p12",
				"--password-stdin"),
			call("sign", "", "sign", "-a", "ML-DSA-65", "--in={in}/plain.txt",
				"--key={in}/mldsa.pem", "--signature={out}/plain.sig"),
			call("verify-signature", "", "verify-signature", "-a", "ML-DSA-65",
				"--in={in}/plain.txt", "--key={in}/mldsa.pub.pem", "--signature={in}/plain.sig"),
			call("encrypt", PASSPHRASE + "\n", "encrypt", "--text=a text to seal",
				"--passphrase-stdin"),
			call("decrypt", PASSPHRASE + "\n", "decrypt", "--text=" + sealedText,
				"--passphrase-stdin"),
			call("share split", "a secret to share\n", "share", "split", "--secret-stdin", "-n",
				"3", "-t", "2"),
			call("share combine", "", "share", "combine", "--share=" + shares.get(0),
				"--share=" + shares.get(1)),
			call("keyx new", "", "keyx", "new", "-k", "{out}/exchange.key"),
			call("convert", "", "convert", "-i", "{in}/rsa.pem", "--describe"),
			call("release-key status", "", "release-key", "status", "--home={in}/gnupg"),
			call("help", "", "help"));
	}

	static Stream<Arguments> everyCommandWithAndWithoutPlugins()
	{
		return everyCommand().stream().flatMap(command -> Stream
			.of(Arguments.of(command, Plugins.NONE), Arguments.of(command, Plugins.SHIPPED)));
	}

	@ParameterizedTest(name = "{0}, plugins: {1}")
	@MethodSource("everyCommandWithAndWithoutPlugins")
	void aSuccessfulRunWritesNothingToStandardError(Call command, Plugins plugins,
		@TempDir Path out) throws Exception
	{
		Run run = cli(home(plugins), false, command, out);

		assertEquals(0, run.exitCode(), run.err());
		assertEquals("", run.err(), "the application added this to a successful run");
	}

	@Test
	void theVerboseSwitchBringsThePluginsMessagesBack(@TempDir Path out) throws Exception
	{
		Run run = cli(withShippedPlugins, true, call("kem", "", "kem"), out);

		assertEquals(0, run.exitCode(), run.err());
		assertEquals(2 * shippedPlugins.size(), pluginMessages(run.err()),
			"one start and one stop message for every plugin: " + run.err());
	}

	@Test
	void theVerboseSwitchNamesTheMissingPluginsDirectory(@TempDir Path out) throws Exception
	{
		Run run = cli(withoutPlugins, true, call("kem", "", "kem"), out);

		assertEquals(0, run.exitCode(), run.err());
		assertTrue(run.err().contains(withoutPlugins.resolve(PLUGINS_UNDER_HOME).toString()),
			run.err());
	}

	@Test
	void aWarningAboutTheCallStillShows(@TempDir Path out) throws Exception
	{
		Run run = cli(withShippedPlugins, false,
			call("hash", "", "hash", "--password=" + TestPasswords.throwaway()), out);

		assertEquals(0, run.exitCode(), run.err());
		assertEquals(1, run.err().lines().count(), run.err());
		assertTrue(run.err().startsWith("warning: --password puts the password into the process"),
			run.err());
	}

	@Test
	void anErrorStillShows(@TempDir Path out) throws Exception
	{
		Run run = cli(withShippedPlugins, false, call("decrypt", PASSPHRASE + "\n", "decrypt",
			"--text=this is not base64", "--passphrase-stdin"), out);

		assertEquals(2, run.exitCode(), run.err());
		assertEquals(1, run.err().lines().count(), run.err());
		assertTrue(run.err().startsWith("error: "), run.err());
	}

	@Test
	void whatACommandReportsOnStandardErrorItselfStillShows(@TempDir Path out) throws Exception
	{
		Run run = cli(withShippedPlugins, false, call("encrypt", PASSPHRASE + "\n", "encrypt", "-i",
			"{in}/plain.txt", "-o", "{out}/sealed", "--passphrase-stdin"), out);

		assertEquals(0, run.exitCode(), run.err());
		assertEquals("encrypted 18 bytes to " + out.resolve("sealed") + System.lineSeparator(),
			run.err());
	}

	@Test
	void aPluginsWarningStillShows(@TempDir Path home, @TempDir Path out) throws Exception
	{
		installTheWarningPlugin(Files.createDirectories(home.resolve(PLUGINS_UNDER_HOME)));

		Run run = cli(home, false, call("kem", "", "kem"), out);

		assertEquals(0, run.exitCode(), run.err());
		assertTrue(run.err().contains("WARNING: " + WarningPlugin.WARNING), run.err());
	}

	/** A plugin that warns when it starts, the way a plugin reports a problem */
	public static class WarningPlugin extends Plugin
	{
		/** What it warns */
		static final String WARNING = "the warning plugin warns as it starts";

		@Override
		public void start()
		{
			Logger.getLogger(WarningPlugin.class.getName()).warning(WARNING);
		}
	}

	/** Packages {@link WarningPlugin} the way the plugin manager expects to find a plugin */
	private static void installTheWarningPlugin(final Path pluginsDirectory) throws IOException
	{
		try (ZipOutputStream zip = new ZipOutputStream(
			Files.newOutputStream(pluginsDirectory.resolve("warning-plugin-1.0.0.zip"))))
		{
			zip.putNextEntry(new ZipEntry("plugin.properties"));
			zip.write(("plugin.id=warning-plugin\nplugin.version=1.0.0\nplugin.class="
				+ WarningPlugin.class.getName() + "\n").getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			String path = WarningPlugin.class.getName().replace('.', '/') + ".class";
			zip.putNextEntry(new ZipEntry("classes/" + path));
			try (InputStream compiled = WarningPlugin.class.getClassLoader()
				.getResourceAsStream(path))
			{
				zip.write(compiled.readAllBytes());
			}
			zip.closeEntry();
		}
	}

	/** The start and stop messages of the plugins among the lines written */
	private static long pluginMessages(final String standardError)
	{
		return standardError.lines().filter(line -> line.startsWith("INFO: ")
			&& (line.endsWith(" started") || line.endsWith(" stopped"))).count();
	}

	private static Call call(final String name, final String standardInput, final String... args)
	{
		return new Call(name, standardInput, List.of(args));
	}

	private static Path home(final Plugins plugins)
	{
		return plugins == Plugins.NONE ? withoutPlugins : withShippedPlugins;
	}

	/**
	 * Starts the application with {@code --cli} in a JVM of its own, as {@code java -jar} does: the
	 * application's main class on this test's class path, its own home, the input on standard input
	 */
	static Run cli(final Path home, final boolean verbose, final Call command, final Path out)
		throws Exception
	{
		List<String> processArguments = new ArrayList<>();
		processArguments.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
		processArguments.add("-Duser.home=" + home);
		if (verbose)
		{
			processArguments.add("-D" + MysticCryptUiCli.VERBOSE_PROPERTY + "=true");
		}
		processArguments.add("-cp");
		processArguments.add(System.getProperty("java.class.path"));
		processArguments.add(StartMysticCryptApplication.class.getName());
		processArguments.add(MysticCryptUiCli.CLI_ARGUMENT);
		for (String arg : command.args())
		{
			processArguments
				.add(arg.replace("{in}", inputs.toString()).replace("{out}", out.toString()));
		}
		ProcessBuilder builder = new ProcessBuilder(processArguments).directory(out.toFile());
		Map<String, String> environment = builder.environment();
		JVM_ANNOUNCING_VARIABLES.forEach(environment::remove);
		Process process = builder.start();
		process.getOutputStream().write(command.standardInput().getBytes(StandardCharsets.UTF_8));
		process.getOutputStream().close();
		ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
		Thread outReader = Thread.ofVirtual()
			.start(() -> copy(process.getInputStream(), standardOutput));
		String standardError = new String(process.getErrorStream().readAllBytes(),
			StandardCharsets.UTF_8);
		if (!process.waitFor(90, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException(command + " did not finish: " + standardError);
		}
		outReader.join();
		return new Run(process.exitValue(), standardOutput.toString(StandardCharsets.UTF_8),
			standardError);
	}

	private static void copy(final InputStream from, final ByteArrayOutputStream to)
	{
		try
		{
			from.transferTo(to);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("standard output could not be read", exception);
		}
	}

	/**
	 * Runs a command of the library in this JVM to make an input, and returns what it printed
	 */
	private static String library(final String standardInput, final String... args)
	{
		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream printed = new ByteArrayOutputStream();
		ByteArrayOutputStream complained = new ByteArrayOutputStream();
		try
		{
			System.setIn(new ByteArrayInputStream(standardInput.getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(printed, true, StandardCharsets.UTF_8));
			System.setErr(new PrintStream(complained, true, StandardCharsets.UTF_8));
			int exitCode = MysticCryptCli.execute(args);
			if (exitCode != 0)
			{
				throw new IllegalStateException(
					"making an input with " + List.of(args) + " ended with " + exitCode + ": "
						+ complained.toString(StandardCharsets.UTF_8));
			}
		}
		finally
		{
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
		return printed.toString(StandardCharsets.UTF_8);
	}

	/** An RSA private key the JDK makes, written as the DER of its PKCS#8 encoding */
	private static void writeRsaPrivateKeyAsDer() throws Exception
	{
		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		Files.write(inputs.resolve("rsa.der"), keyPair.getPrivate().getEncoded());
	}

	/**
	 * The plugin zips the installer ships, read from its own description and required to be built:
	 * a run without them would not be the run this test is about
	 */
	private static List<Path> shippedPluginZips() throws IOException
	{
		Path installDescription = Path.of("src/main/izpack/install.xml");
		Matcher matcher = SHIPPED_PLUGIN.matcher(Files.readString(installDescription));
		List<Path> zips = new ArrayList<>();
		while (matcher.find())
		{
			// the installer resolves its sources against the build directory (gradle/izpack.gradle)
			Path zip = Path.of("build").resolve(matcher.group(1)).normalize();
			assertTrue(Files.isRegularFile(zip), zip + " is not built: run make plugins first");
			zips.add(zip);
		}
		assertFalse(zips.isEmpty(), "install.xml names no plugin zip");
		return zips;
	}
}
