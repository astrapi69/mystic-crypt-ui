/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;

/**
 * A published release's application jar, and a probe source run against it in a child JVM with only
 * that jar on its class path - the release itself, not a model of it (#402, #490).
 * <p>
 * The jar is taken out of the installer published on GitHub and checked against the sha256
 * published beside it ({@code scripts/fetch-release-jar.sh}); Gradle passes its path and version as
 * system properties. <b>A missing jar fails in CI and skips locally</b>, decided by the maintainer
 * in #402: CI fetches and caches it before the build and must not report a compatibility it did not
 * check, the same rule as for the plugin zips; a local run without network skips and says why.
 */
final class ReleaseProbe
{

	/**
	 * The release a vault this build writes has to stay readable in, as long as it holds nothing
	 * that release has no field for ({@code formatCompatibilityRelease}, #402)
	 */
	static final ReleaseProbe FORMAT_COMPATIBILITY = new ReleaseProbe("mystic.crypt.ui.release.jar",
		"mystic.crypt.ui.release.version");

	/**
	 * The last release that sealed its vault with the application's own {@code PassphraseBox},
	 * before the construction moved into mystic-crypt ({@code envelopeMoveRelease}, #490)
	 */
	static final ReleaseProbe ENVELOPE_MOVE = new ReleaseProbe(
		"mystic.crypt.ui.envelope.release.jar", "mystic.crypt.ui.envelope.release.version");

	private final String jarProperty;

	private final String versionProperty;

	private ReleaseProbe(final String jarProperty, final String versionProperty)
	{
		this.jarProperty = jarProperty;
		this.versionProperty = versionProperty;
	}

	/**
	 * The release jar, or the end of the test: failed in CI, skipped locally
	 *
	 * @return the jar
	 */
	File requireJar()
	{
		File releaseJar = new File(System.getProperty(jarProperty, ""));
		if (releaseJar.isFile())
		{
			return releaseJar;
		}
		String reason = "the application jar of " + name() + " is not at " + releaseJar
			+ ". 'make release-jar' downloads the published installer, checks its sha256 and "
			+ "extracts it (needs network)";
		if ("true".equals(System.getenv("GITHUB_ACTIONS")))
		{
			throw new AssertionError(reason + ". CI fetches it before the build, so its absence "
				+ "here is a broken step, not a skip (#402)");
		}
		Assumptions.abort("SKIPPED locally: " + reason + ". In CI this fails (#402)");
		return releaseJar;
	}

	/**
	 * @return "release" and the version, for messages
	 */
	String name()
	{
		return "release " + System.getProperty(versionProperty, "(unknown version)");
	}

	/**
	 * Runs a probe source from the test resources in a child JVM with only the release jar on its
	 * class path, the password on its standard input
	 *
	 * @param probeResource
	 *            the probe, e.g. {@code /compat/OpenWithTheRelease.java}
	 * @param directory
	 *            where the probe source is copied to
	 * @param password
	 *            what the probe reads from standard input
	 * @param arguments
	 *            the probe's arguments
	 * @return its exit code and everything it printed
	 * @throws IOException
	 *             if the probe cannot be copied or started
	 * @throws InterruptedException
	 *             if waiting for it is interrupted
	 */
	ProbeRun run(final String probeResource, final File directory, final char[] password,
		final String... arguments) throws IOException, InterruptedException
	{
		File releaseJar = requireJar();
		File probe = new File(directory,
			probeResource.substring(probeResource.lastIndexOf('/') + 1));
		try (InputStream source = ReleaseProbe.class.getResourceAsStream(probeResource))
		{
			Files.copy(source, probe.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		List<String> command = new ArrayList<>(List.of(
			ProcessHandle.current().info().command().orElse("java"), "-Djava.awt.headless=true",
			"-cp", releaseJar.getAbsolutePath(), probe.getAbsolutePath()));
		command.addAll(List.of(arguments));
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		try (OutputStream toProcess = process.getOutputStream())
		{
			toProcess.write((new String(password) + "\n").getBytes(StandardCharsets.UTF_8));
		}
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!process.waitFor(2, TimeUnit.MINUTES))
		{
			process.destroyForcibly();
			throw new IllegalStateException(name() + " did not finish the probe: " + output);
		}
		return new ProbeRun(process.exitValue(), output);
	}

	/**
	 * What a probe did
	 *
	 * @param exitCode
	 *            0 when it succeeded
	 * @param output
	 *            everything it printed
	 */
	record ProbeRun(int exitCode, String output) {

		/**
		 * @return the lines describing one entry each, in the order printed
		 */
		List<String> entryLines()
		{
			return output.lines().filter(line -> line.startsWith("ENTRY ")).toList();
		}
	}
}
