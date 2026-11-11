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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every page of the GitHub wiki the application links to exists in {@code docs/wiki/}, the wiki's
 * source in this repository, which a workflow mirrors into the wiki (#868, #869). The links are
 * read where they are written: the Java sources of the host and of every plugin, and README.md,
 * DEPLOYMENT-INFO.md and the documents directly in {@code docs/}. A help button that opens a page
 * nobody can find is what this keeps from happening again: the sign-in dialogs' two help buttons
 * open pages that were renamed in 2023 (#868), which is why they stand in {@link #KNOWN_MISSING}
 * until #868 is decided
 */
class WikiLinksTest
{

	/** The wiki's address; the page name follows it after a slash, nothing following is Home */
	private static final Pattern WIKI_LINK = Pattern.compile(
		"https://github\\.com/astrapi69/mystic-crypt-ui/wiki(?:/(?<page>[^\\s\"')>#\\]]*))?"
			+ "(?:\"\\s*\\+\\s*\"(?<continued>[^\"#]*))?");

	/** The wiki's source in this repository */
	private static final Path WIKI_SOURCE = Path.of("docs", "wiki");

	/**
	 * Pages the application links to that the wiki does not have, each with the issue that decides
	 * what happens to the link. A page leaves this list when its issue is done; the list cannot
	 * outlive its cause, see {@link #theKnownMissingPages_areStillLinkedAndStillMissing}
	 */
	private static final Map<String, String> KNOWN_MISSING = Map.of(
		"Help:-open-existing-mystic-crypt-database", "#868",
		"Help:-create-new-mystic-crypt-database", "#868");

	@Test
	@DisplayName("every wiki page the application and its documents link to is in docs/wiki/")
	void everyLinkedPage_isInTheWikiSource()
	{
		Map<String, Set<Path>> linked = linkedPages();

		assertFalse(linked.isEmpty(), "no wiki link found in " + scannedFiles().size()
			+ " files: a scan that finds nothing has checked nothing");
		List<String> missing = new ArrayList<>();
		linked.forEach((page, where) -> {
			if (!KNOWN_MISSING.containsKey(page) && !Files.isRegularFile(sourceOf(page)))
			{
				missing.add(page + " (linked from " + where + ")");
			}
		});
		assertEquals(List.of(), missing, "pages linked but not in " + WIKI_SOURCE + ", out of "
			+ linked.size() + " linked pages in " + scannedFiles().size() + " files");
	}

	@Test
	@DisplayName("a page known to be missing is still linked and still missing, so the list cannot outlive its issue")
	void theKnownMissingPages_areStillLinkedAndStillMissing()
	{
		Set<String> linked = linkedPages().keySet();

		KNOWN_MISSING.forEach((page, issue) -> {
			assertTrue(linked.contains(page),
				page + " is no longer linked: take it out of KNOWN_MISSING (" + issue + ")");
			assertFalse(Files.exists(sourceOf(page)),
				page + " is in the wiki source now: take it out of KNOWN_MISSING (" + issue + ")");
		});
	}

	@ParameterizedTest(name = "{argumentSetName}")
	@MethodSource("linkForms")
	@DisplayName("a wiki link is read in each form the sources write it")
	void aWikiLink_isReadInEachForm(final String text, final String page)
	{
		assertEquals(Set.of(page), pagesIn(text));
	}

	static Stream<Arguments> linkForms()
	{
		return Stream.of(
			Arguments.argumentSet("the wiki itself is its Home page",
				"WIKI_URL = \"https://github.com/astrapi69/mystic-crypt-ui/wiki\";", "Home"),
			Arguments.argumentSet("a Java literal continued on the next line",
				"String helpLink = \"https://github.com/astrapi69/mystic-crypt-ui/wiki/\"\n\t\t\t+ "
					+ "\"Help:-open-existing-mystic-crypt-database\";",
				"Help:-open-existing-mystic-crypt-database"),
			Arguments.argumentSet("a Markdown link",
				"the [wiki izpack section](https://github.com/astrapi69/mystic-crypt-ui/wiki/"
					+ "How-to-create-izpack-installer-with-gradle).",
				"How-to-create-izpack-installer-with-gradle"),
			Arguments.argumentSet("an autolink in angle brackets",
				"<https://github.com/astrapi69/mystic-crypt-ui/wiki/Getting-Started>",
				"Getting-Started"),
			Arguments.argumentSet("a link to a section of a page",
				"(https://github.com/astrapi69/mystic-crypt-ui/wiki/The-Command-Line#options)",
				"The-Command-Line"));
	}

	/** The linked pages, each with the files that link to it */
	private static Map<String, Set<Path>> linkedPages()
	{
		Map<String, Set<Path>> linked = new TreeMap<>();
		for (Path file : scannedFiles())
		{
			for (String page : pagesIn(read(file)))
			{
				linked.computeIfAbsent(page, key -> new TreeSet<>()).add(file);
			}
		}
		return linked;
	}

	static Set<String> pagesIn(final String text)
	{
		Set<String> pages = new TreeSet<>();
		Matcher link = WIKI_LINK.matcher(text);
		while (link.find())
		{
			String page = (link.group("page") == null ? "" : link.group("page"))
				+ (link.group("continued") == null ? "" : link.group("continued"));
			pages.add(page.isEmpty() ? "Home" : page);
		}
		return pages;
	}

	/**
	 * The Java sources of the host and of every plugin, README.md, DEPLOYMENT-INFO.md and the
	 * documents directly in docs/; the wiki source itself is not read here
	 */
	private static List<Path> scannedFiles()
	{
		List<Path> files = new ArrayList<>(
			List.of(Path.of("README.md"), Path.of("DEPLOYMENT-INFO.md")));
		files.addAll(filesUnder(Path.of("src", "main", "java"), ".java", Integer.MAX_VALUE));
		files.addAll(filesUnder(Path.of("plugins"), ".java", Integer.MAX_VALUE).stream()
			.filter(file -> file.toString().contains("/src/main/java/")).toList());
		files.addAll(filesUnder(Path.of("docs"), ".md", 1));
		return files;
	}

	private static List<Path> filesUnder(final Path directory, final String extension,
		final int depth)
	{
		try (Stream<Path> found = Files.walk(directory, depth))
		{
			return found.filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().endsWith(extension)).sorted()
				.toList();
		}
		catch (IOException unreadable)
		{
			throw new UncheckedIOException("cannot list " + directory, unreadable);
		}
	}

	private static String read(final Path file)
	{
		try
		{
			return Files.readString(file, StandardCharsets.UTF_8);
		}
		catch (IOException unreadable)
		{
			throw new UncheckedIOException("cannot read " + file, unreadable);
		}
	}

	private static Path sourceOf(final String page)
	{
		return WIKI_SOURCE.resolve(page + ".md");
	}
}
