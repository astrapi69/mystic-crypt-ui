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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Every wiki link in the sources points at a page that exists: the wiki is mirrored from
 * docs/wiki/, a page the mirror does not carry is deleted from the wiki, and the sign-in help
 * buttons pointed at pages renamed in 2023 until this pinned it (#868)
 */
class HelpLinksPointAtWikiPagesTest
{

	private static final Pattern WIKI_LINK = Pattern
		.compile("github\\.com/astrapi69/mystic-crypt-ui/wiki/\"\\s*\\+\\s*\"([^\"]+)\"");

	@Test
	void everyWikiLinkInTheSourcesNamesAPageOfTheMirror() throws IOException
	{
		List<Path> sources = Files.walk(Path.of("src/main/java"))
			.filter(path -> path.toString().endsWith(".java")).toList();
		int found = 0;
		StringBuilder missing = new StringBuilder();
		for (Path source : sources)
		{
			Matcher matcher = WIKI_LINK.matcher(Files.readString(source));
			while (matcher.find())
			{
				found++;
				String page = matcher.group(1);
				if (!Files.isRegularFile(Path.of("docs/wiki", page + ".md")))
				{
					missing.append(source.getFileName()).append(" points at wiki page '")
						.append(page).append("', which docs/wiki/ does not carry\n");
				}
			}
		}
		assertTrue(found >= 2, "the two sign-in help links exist, or this test measures nothing");
		assertTrue(missing.isEmpty(), missing.toString());
	}
}
