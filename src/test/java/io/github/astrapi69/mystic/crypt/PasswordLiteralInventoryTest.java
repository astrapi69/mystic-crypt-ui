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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Passwords in tests are made up per run, never written as literals ({@code quality-checks.md}). A
 * literal guards only a fixture the test builds itself, yet it reads as a credential to anybody
 * looking and to the secret scanner on every pull request: GitGuardian failed #671 over a test
 * wallet's password, and it reports a removed literal line again.
 * <p>
 * The host's {@link TestPasswords#throwaway()} makes them up; a plugin's tests, which do not see
 * the host's test support, use a small helper of their own. The literals that exist today are
 * listed below with their count per file: the list is the work left, a file that gets one more, or
 * a new file with one, fails here, and a file that loses one has to lower its count, so the list
 * never claims more than is there.
 */
class PasswordLiteralInventoryTest
{

	/** A password-named variable or constant given a whole string literal, also as characters */
	private static final Pattern PASSWORD_LITERAL = Pattern.compile(
		"(?i)\\b\\w*(?:password|passphrase|passwd)\\w*\\s*=\\s*\"[^\"]*\"\\s*(?:\\.toCharArray\\(\\))?\\s*;");

	/** The literals that exist, per file, to be replaced when the file is next touched */
	private static final Map<String, Integer> NOT_REPLACED_YET = Map.ofEntries(Map.entry(
		"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/LethenonBalancePanelBindingTest.java",
		1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/LethenonMinePanelBindingTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/LethenonSendPanelBindingTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/LethenonSweepPanelBindingTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/LethenonWalletsTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/MiningSupportTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/NodeSupportTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/RememberWhatWorkedTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/SweepSupportTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/TransferSupportTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/TransferThroughANodeTest.java",
			1),
		Map.entry(
			"plugins/lethenon-plugin/src/test/java/io/github/astrapi69/mystic/crypt/plugin/lethenon/WalletBalanceSupportTest.java",
			1),
		Map.entry("src/e2eKdbx/java/io/github/astrapi69/mystic/crypt/keepass/KdbxFacts.java", 1),
		Map.entry(
			"src/e2eKdbx/java/io/github/astrapi69/mystic/crypt/ui/ClosingAVaultErasesAnImportedEntryUiTest.java",
			1),
		Map.entry(
			"src/e2eKdbx/java/io/github/astrapi69/mystic/crypt/ui/KdbxRoundTripKeepsEveryFieldUiTest.java",
			1),
		Map.entry(
			"src/e2eLock/java/io/github/astrapi69/mystic/crypt/ui/AutomaticLockHoldsTheInvariantUiTest.java",
			1),
		Map.entry(
			"src/e2eLock/java/io/github/astrapi69/mystic/crypt/ui/ClosingAVaultErasesItFromMemoryUiTest.java",
			1),
		Map.entry(
			"src/e2eLock/java/io/github/astrapi69/mystic/crypt/ui/LockingErasesTheKeyMaterialUiTest.java",
			1),
		Map.entry(
			"src/e2eLock/java/io/github/astrapi69/mystic/crypt/ui/PluginsUsableWhileLockedHoldTheLockUiTest.java",
			1),
		Map.entry(
			"src/e2eTest/java/io/github/astrapi69/mystic/crypt/ui/TreeCopyGivesTheClipboardOnlyTheNodeNameUiTest.java",
			1),
		Map.entry(
			"src/test/java/io/github/astrapi69/mystic/crypt/app/file/xml/ApplicationXmlFileWithPasswordAndKeyFactoryTest.java",
			1),
		Map.entry(
			"src/test/java/io/github/astrapi69/mystic/crypt/lock/MasterPasswordVerifierTest.java",
			1),
		Map.entry(
			"src/test/java/io/github/astrapi69/mystic/crypt/panel/signin/MasterPwFileModelBeanTest.java",
			1),
		Map.entry(
			"src/test/java/io/github/astrapi69/mystic/crypt/tooling/SigningPasswordsFromTheKeyringTest.java",
			2),
		Map.entry("src/test/java/io/github/astrapi69/mystic/crypt/vault/VaultCloseSupportTest.java",
			5));

	@Test
	@DisplayName("no test source has more password literals than the list allows, and the list is exact")
	void everyPasswordLiteralInTheTestsIsOnTheList() throws IOException
	{
		List<Path> sources = testSources();
		assertTrue(sources.size() > 300,
			"the scan has to find the test sources at all - an empty set is a broken scan, not a "
				+ "clean one; found " + sources.size());

		Map<String, Integer> found = new TreeMap<>();
		for (Path source : sources)
		{
			int count = countLiterals(Files.readString(source, StandardCharsets.UTF_8));
			if (count > 0)
			{
				found.put(source.toString().replace('\\', '/'), count);
			}
		}

		List<String> problems = new ArrayList<>();
		found.forEach((file, count) -> {
			int allowed = NOT_REPLACED_YET.getOrDefault(file, 0);
			if (count > allowed)
			{
				problems.add(file + " has " + count + " password literal(s), the list allows "
					+ allowed + ": make the password up per run (TestPasswords.throwaway() in "
					+ "the host, the plugin's own helper in a plugin)");
			}
		});
		NOT_REPLACED_YET.forEach((file, allowed) -> {
			int count = found.getOrDefault(file, 0);
			if (count < allowed)
			{
				problems.add(file + " now has " + count + " password literal(s), the list still "
					+ "says " + allowed + ": lower it, or remove the line when it reaches 0");
			}
		});
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static int countLiterals(final String content)
	{
		Matcher matcher = PASSWORD_LITERAL.matcher(content);
		int count = 0;
		while (matcher.find())
		{
			count++;
		}
		return count;
	}

	/**
	 * Every Java file of the host's test source sets and of every plugin's tests
	 *
	 * @return the files, relative to the module root this test runs in
	 * @throws IOException
	 *             if a tree cannot be walked
	 */
	private static List<Path> testSources() throws IOException
	{
		List<Path> roots = new ArrayList<>();
		try (Stream<Path> sourceSets = Files.list(Path.of("src")))
		{
			sourceSets.filter(Files::isDirectory)
				.filter(path -> !path.getFileName().toString().equals("main")).forEach(roots::add);
		}
		try (Stream<Path> plugins = Files.list(Path.of("plugins")))
		{
			plugins.map(plugin -> plugin.resolve("src").resolve("test")).filter(Files::isDirectory)
				.forEach(roots::add);
		}
		List<Path> sources = new ArrayList<>();
		for (Path root : roots)
		{
			try (Stream<Path> walk = Files.walk(root))
			{
				walk.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
			}
		}
		return sources;
	}
}
