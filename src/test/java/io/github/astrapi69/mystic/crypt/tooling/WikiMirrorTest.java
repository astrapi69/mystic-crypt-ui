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
package io.github.astrapi69.mystic.crypt.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@code scripts/mirror-wiki.sh}, which the workflow {@code wiki-mirror.yml} runs, against a
 * real throwaway wiki: a bare repository with pages, and a source directory as {@code docs/wiki/}
 * is in this repository (#869). The wiki gets exactly the source's pages: a page new or changed in
 * the source is written, a page the source no longer has is deleted, an unchanged page is not
 * touched, and what is not a page (a .gitignore, an uploaded image) stays. The run names every page
 * it wrote or deleted, and it fails when it cannot write the wiki or has no pages to write, instead
 * of reporting success
 */
class WikiMirrorTest
{

	private static final File SCRIPT = new File("scripts/mirror-wiki.sh").getAbsoluteFile();

	@TempDir
	File root;

	private File wiki;

	private File source;

	/**
	 * A wiki with three pages and a .gitignore, and a source that changes one, adds one, drops one
	 */
	@BeforeEach
	void aWikiAndASourceThatDiffer() throws Exception
	{
		wiki = new File(root, "wiki.git");
		source = new File(root, "docs-wiki");
		File seed = new File(root, "seed");
		git(root, "init", "-q", "--bare", "-b", "master", wiki.getPath());
		git(root, "clone", "-q", wiki.getPath(), seed.getPath());
		write(seed, "Home.md", "old home");
		write(seed, "Same.md", "same");
		write(seed, "Old-Page.md", "a page the source no longer has");
		write(seed, ".gitignore", "*.tmp");
		git(seed, "add", ".");
		git(seed, "commit", "-q", "-m", "the wiki as it was");
		git(seed, "push", "-q", "origin", "master");
		Files.createDirectories(source.toPath());
		write(source, "Home.md", "new home");
		write(source, "Same.md", "same");
		write(source, "New-Page.md", "a page new in the source");
	}

	@Test
	@DisplayName("the wiki gets exactly the source's pages, and the run names every page it wrote or deleted")
	void theWiki_getsExactlyTheSourcesPages() throws Exception
	{
		Run run = run(source.getPath(), wiki.getPath());

		assertEquals(0, run.exit(), run.output());
		File mirrored = clone("mirrored");
		assertEquals(Set.of("Home.md", "Same.md", "New-Page.md"), pagesIn(mirrored), run.output());
		assertEquals("new home", read(mirrored, "Home.md"));
		assertTrue(new File(mirrored, ".gitignore").isFile(), "what is not a page stays");
		assertTrue(run.output().contains("written: Home.md"), run.output());
		assertTrue(run.output().contains("written: New-Page.md"), run.output());
		assertTrue(run.output().contains("deleted: Old-Page.md"), run.output());
		assertFalse(run.output().contains("Same.md"),
			"an unchanged page is not named: " + run.output());
		assertTrue(run.output().contains("2 written, 1 deleted, 3 pages"), run.output());
	}

	@Test
	@DisplayName("a source name with %3A is written to the wiki with a colon, and stays on a rerun")
	void aSourceNameWithAnEncodedColon_isWrittenUnderTheWikiName() throws Exception
	{
		write(source, "Help%3A-Create.md", "a redirect page for released versions");

		Run run = run(source.getPath(), wiki.getPath());

		assertEquals(0, run.exit(), run.output());
		File mirrored = clone("mirrored-colon");
		assertTrue(new File(mirrored, "Help:-Create.md").isFile(),
			"the wiki page carries the colon: " + run.output());
		assertEquals("a redirect page for released versions", read(mirrored, "Help:-Create.md"));
		assertTrue(run.output().contains("written: Help:-Create.md"), run.output());

		Run again = run(source.getPath(), wiki.getPath());
		assertEquals(0, again.exit(), again.output());
		assertTrue(again.output().contains("0 written, 0 deleted"),
			"the mapped page is unchanged, not deleted and rewritten: " + again.output());
	}

	@Test
	@DisplayName("a wiki page with a colon is deleted only when its encoded source file goes")
	void aWikiPageWithAColon_isDeletedOnlyWhenItsEncodedSourceGoes() throws Exception
	{
		write(source, "Help%3A-Create.md", "kept while the source holds the encoded name");
		run(source.getPath(), wiki.getPath());

		Files.delete(new File(source, "Help%3A-Create.md").toPath());
		Run run = run(source.getPath(), wiki.getPath());

		assertEquals(0, run.exit(), run.output());
		File mirrored = clone("mirrored-colon-gone");
		assertFalse(new File(mirrored, "Help:-Create.md").exists(), run.output());
		assertTrue(run.output().contains("deleted: Help:-Create.md"), run.output());
	}

	@Test
	@DisplayName("a wiki that already holds the source's pages gets no commit, and the run says so")
	void aWikiThatAlreadyMatches_getsNoCommit() throws Exception
	{
		run(source.getPath(), wiki.getPath());
		String before = output(wiki, "git", "rev-parse", "master");

		Run again = run(source.getPath(), wiki.getPath());

		assertEquals(0, again.exit(), again.output());
		assertEquals(before, output(wiki, "git", "rev-parse", "master"));
		assertTrue(again.output().contains("0 written, 0 deleted, 3 pages"), again.output());
	}

	@Test
	@DisplayName("a source without a page is refused, and the wiki keeps its pages")
	void aSourceWithoutAPage_isRefused() throws Exception
	{
		File empty = new File(root, "empty");
		Files.createDirectories(empty.toPath());
		String before = output(wiki, "git", "rev-parse", "master");

		Run run = run(empty.getPath(), wiki.getPath());

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains(empty.getPath()), "it names the source: " + run.output());
		assertEquals(before, output(wiki, "git", "rev-parse", "master"), "nothing was deleted");
	}

	@Test
	@DisplayName("a wiki that refuses the push makes the run fail, and says it was not written")
	void aWikiThatRefusesThePush_failsTheRun() throws Exception
	{
		File hook = new File(wiki, "hooks/pre-receive");
		Files.writeString(hook.toPath(), "#!/bin/sh\necho refused by the test >&2\nexit 1\n",
			StandardCharsets.UTF_8);
		Files.setPosixFilePermissions(hook.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));

		Run run = run(source.getPath(), wiki.getPath());

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("not written"), run.output());
	}

	@Test
	@DisplayName("a wiki that cannot be reached makes the run fail")
	void aWikiThatCannotBeReached_failsTheRun() throws Exception
	{
		Run run = run(source.getPath(), new File(root, "no-such-wiki.git").getPath());

		assertNotEquals(0, run.exit(), run.output());
	}

	private File clone(final String name) throws Exception
	{
		File copy = new File(root, name);
		git(root, "clone", "-q", wiki.getPath(), copy.getPath());
		return copy;
	}

	private static Set<String> pagesIn(final File directory)
	{
		Set<String> pages = new TreeSet<>();
		for (String name : directory.list())
		{
			if (name.endsWith(".md"))
			{
				pages.add(name);
			}
		}
		return pages;
	}

	private static void write(final File directory, final String name, final String content)
		throws Exception
	{
		Files.writeString(new File(directory, name).toPath(), content, StandardCharsets.UTF_8);
	}

	private static String read(final File directory, final String name) throws Exception
	{
		return Files.readString(new File(directory, name).toPath(), StandardCharsets.UTF_8);
	}

	private record Run(int exit, String output) {
	}

	private Run run(final String sourceDirectory, final String wikiRemote) throws Exception
	{
		List<String> command = List.of(SCRIPT.getPath(), sourceDirectory, wikiRemote);
		Process process = start(root, command);
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		finish(process, command);
		return new Run(process.exitValue(), output);
	}

	private static void git(final File directory, final String... arguments) throws Exception
	{
		List<String> command = new ArrayList<>(List.of("git"));
		command.addAll(List.of(arguments));
		Process process = start(directory, command);
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		finish(process, command);
		if (process.exitValue() != 0)
		{
			throw new IllegalStateException(String.join(" ", command) + " failed with exit code "
				+ process.exitValue() + " in " + directory + ": " + output);
		}
	}

	private static String output(final File directory, final String... command) throws Exception
	{
		Process process = start(directory, List.of(command));
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		finish(process, List.of(command));
		return output.trim();
	}

	private static Process start(final File directory, final List<String> command) throws Exception
	{
		ProcessBuilder builder = new ProcessBuilder(command).directory(directory)
			.redirectErrorStream(true);
		// the machine's own git configuration must not leak in: no identity, no hooks, no
		// defaults that change what a clone or a push does
		builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
		builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
		builder.environment().put("GIT_AUTHOR_NAME", "Test");
		builder.environment().put("GIT_AUTHOR_EMAIL", "test@example.org");
		builder.environment().put("GIT_COMMITTER_NAME", "Test");
		builder.environment().put("GIT_COMMITTER_EMAIL", "test@example.org");
		builder.environment().put("LC_ALL", "C");
		return builder.start();
	}

	private static void finish(final Process process, final List<String> command) throws Exception
	{
		if (!process.waitFor(30, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException(String.join(" ", command) + " did not finish");
		}
	}
}
