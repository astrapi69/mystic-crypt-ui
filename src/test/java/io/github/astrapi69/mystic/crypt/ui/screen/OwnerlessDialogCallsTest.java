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
package io.github.astrapi69.mystic.crypt.ui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * No dialog of the application or of a plugin opens without the window it belongs to (#63).
 * <p>
 * A dialog without a parent belongs to Swing's shared, invisible owner frame and opens on the first
 * screen, not on the one the application is on. The guard of the UI tests
 * ({@code SharedOwnerFrameGuard}) catches such a dialog only when a test opens it; the "Move not
 * possible" warnings of the database tree were missed that way, because no test opens them. This
 * check reads the main sources of the host and of every plugin instead, so a call is found whether
 * a test reaches it or not.
 * <p>
 * It looks for the calls that leave a dialog without its window or centre it on the screen:
 * {@code JOptionPane.show...(null}, {@code DialogExtensions.show...(null}, a dialog constructed
 * with a {@code null} owner or with none, {@code setLocationRelativeTo(null)}, and
 * {@code JOptionPaneExtensions.getSelectedOption} with a panel, which in swing-base-components
 * 5.2.1 takes the parent and then centres the dialog on the screen anyway
 * (astrapi69/swing-base-components#29; {@link OwnedDialogs} builds that dialog over its window).
 * Comments are left out, so a Javadoc that names such a call is not counted.
 */
class OwnerlessDialogCallsTest
{

	/** The calls that leave a dialog without its window, by the name a failure gives them */
	private static final Map<String, Pattern> OWNERLESS_CALLS = Map.of("JOptionPane.show...(null",
		Pattern.compile("\\bJOptionPane\\s*\\.\\s*show\\w*\\s*\\(\\s*null\\b"),
		"DialogExtensions.show...(null",
		Pattern.compile("\\bDialogExtensions\\s*\\.\\s*show\\w*\\s*\\(\\s*null\\b"),
		"a dialog constructed with a null owner",
		Pattern.compile("\\bnew\\s+[A-Z]\\w*Dialog\\s*\\(\\s*(?:\\(\\s*\\w+\\s*\\)\\s*)?null\\b"),
		"a dialog constructed without an owner", Pattern.compile("\\bnew\\s+JDialog\\s*\\(\\s*\\)"),
		"setLocationRelativeTo(null)",
		Pattern.compile("\\bsetLocationRelativeTo\\s*\\(\\s*null\\s*\\)"),
		"JOptionPaneExtensions.getSelectedOption with a panel, centred on the screen",
		Pattern.compile(
			"\\bJOptionPaneExtensions\\s*\\.\\s*getSelectedOption\\s*\\((?!\\s*\\w+\\s*\\))"));

	/** The calls that stay, per file, each with its reason */
	private static final Map<String, Allowed> ALLOWED = Map.of(
		"src/main/java/io/github/astrapi69/mystic/crypt/panel/privatekey/NewPrivateKeyFileDialog.java",
		new Allowed(1,
			"its main method shows the dialog on its own for development; no application window "
				+ "exists there"));

	/** The calls that exist today and are still to be fixed, per file */
	private static final Map<String, Integer> NOT_FIXED_YET = Map.of(
		"src/main/java/io/github/astrapi69/mystic/crypt/action/ExportKeePassDatabaseAction.java", 1,
		"src/main/java/io/github/astrapi69/mystic/crypt/action/ImportKeePassDatabaseAction.java", 1,
		"src/main/java/io/github/astrapi69/mystic/crypt/action/LockWorkspaceAction.java", 1,
		"src/main/java/io/github/astrapi69/mystic/crypt/action/SaveBeforeCloseConfirmation.java", 3,
		"src/main/java/io/github/astrapi69/mystic/crypt/action/SearchApplicationFileAction.java", 1,
		"src/main/java/io/github/astrapi69/mystic/crypt/panel/dbtree/SecretKeyTreeWithContentPanel.java",
		3);

	@Test
	@DisplayName("no main source of the host or of a plugin opens a dialog without its window, but the listed ones")
	void noDialogOpensWithoutItsWindow() throws IOException
	{
		List<Path> sources = mainSources();
		assertTrue(sources.size() > 400,
			"the main sources of the host and the plugins were not found where this test runs, "
				+ "so it would pass without checking anything; found " + sources.size());

		Map<String, List<String>> found = new TreeMap<>();
		for (Path source : sources)
		{
			List<String> calls = ownerlessCalls(Files.readString(source, StandardCharsets.UTF_8));
			if (!calls.isEmpty())
			{
				found.put(source.toString().replace('\\', '/'), calls);
			}
		}

		List<String> problems = new ArrayList<>();
		found.forEach((file, calls) -> {
			int expected = expectedCount(file);
			if (calls.size() != expected)
			{
				problems.add(file + " has " + calls.size() + " call(s) that leave a dialog "
					+ "without its window, the list expects " + expected + ": " + calls
					+ ". Give the dialog its caller as parent, or build it with OwnedDialogs");
			}
		});
		for (String file : expectedFiles())
		{
			if (!found.containsKey(file))
			{
				problems.add(file + " has no such call any more, the list still expects "
					+ expectedCount(file) + ": remove the line");
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("snippets")
	@DisplayName("a call is counted when it leaves the dialog without its window, and only then")
	void aCallIsCountedWhenItLeavesTheDialogWithoutItsWindow(final String description,
		final String snippet, final int expected)
	{
		assertEquals(expected, ownerlessCalls(snippet).size(), snippet);
	}

	static Stream<Arguments> snippets()
	{
		return Stream.of(
			Arguments.of("a message without parent", "JOptionPane.showMessageDialog(null, \"x\");",
				1),
			Arguments.of("a message with its parent", "JOptionPane.showMessageDialog(this, \"x\");",
				0),
			Arguments.of("the library message, the null on the next line",
				"DialogExtensions.showMessageDialog(\n\t\tnull, \"title\", \"text\", type);", 1),
			Arguments.of("a dialog without owner", "JDialog dialog = new JDialog();", 1),
			Arguments.of("a dialog with its owner", "JDialog dialog = new JDialog(owner);", 0),
			Arguments.of("a dialog of its own with a cast null owner",
				"new NewNodeDialog((Frame)null, \"title\", true);", 1),
			Arguments.of("centred on the screen", "dialog.setLocationRelativeTo(null);", 1),
			Arguments.of("centred over its window", "dialog.setLocationRelativeTo(owner);", 0),
			Arguments.of("the library option dialog with a panel",
				"JOptionPaneExtensions.getSelectedOption(panel, PLAIN, OK_CANCEL, frame, \"t\", f);",
				1),
			Arguments.of("the chosen option read from a pane",
				"return JOptionPaneExtensions.getSelectedOption(pane);", 0),
			Arguments.of("a line comment", "// JOptionPane.showMessageDialog(null, \"x\");", 0),
			Arguments.of("a Javadoc", "/** the library calls setLocationRelativeTo(null) */", 0));
	}

	private static List<String> ownerlessCalls(final String source)
	{
		String code = withoutComments(source);
		List<String> calls = new ArrayList<>();
		OWNERLESS_CALLS.forEach((name, pattern) -> {
			Matcher matcher = pattern.matcher(code);
			while (matcher.find())
			{
				int line = 1 + (int)code.substring(0, matcher.start()).chars()
					.filter(character -> character == '\n').count();
				calls.add("line " + line + ": " + name);
			}
		});
		calls.sort(null);
		return calls;
	}

	/**
	 * The source with its block and line comments blanked out; the line breaks stay, so the line
	 * numbers a failure names are those of the file
	 *
	 * @param source
	 *            the Java source
	 * @return the source without comments
	 */
	private static String withoutComments(final String source)
	{
		Matcher blocks = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(source);
		StringBuilder withoutBlocks = new StringBuilder();
		while (blocks.find())
		{
			blocks.appendReplacement(withoutBlocks,
				Matcher.quoteReplacement(blocks.group().replaceAll("[^\\n]", " ")));
		}
		blocks.appendTail(withoutBlocks);
		return withoutBlocks.toString().replaceAll("//[^\\n]*", "");
	}

	private static int expectedCount(final String file)
	{
		Allowed allowed = ALLOWED.get(file);
		return (allowed != null ? allowed.count() : 0) + NOT_FIXED_YET.getOrDefault(file, 0);
	}

	private static List<String> expectedFiles()
	{
		List<String> files = new ArrayList<>(ALLOWED.keySet());
		files.addAll(NOT_FIXED_YET.keySet());
		return files;
	}

	/**
	 * Every Java file of the host's main sources and of every plugin's main sources
	 *
	 * @return the files, relative to the module root this test runs in
	 * @throws IOException
	 *             if a tree cannot be walked
	 */
	private static List<Path> mainSources() throws IOException
	{
		List<Path> roots = new ArrayList<>();
		roots.add(Path.of("src", "main", "java"));
		try (Stream<Path> plugins = Files.list(Path.of("plugins")))
		{
			plugins.map(plugin -> plugin.resolve("src").resolve("main").resolve("java"))
				.filter(Files::isDirectory).forEach(roots::add);
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

	/**
	 * A call that stays
	 *
	 * @param count
	 *            how many such calls the file has
	 * @param reason
	 *            why they stay
	 */
	private record Allowed(int count, String reason) {
	}
}
