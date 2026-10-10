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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives {@code scripts/release-freeze.py} the way the release-freeze workflow does: the open pull
 * requests into develop as JSON on standard input, one status per pull request out (#516). While a
 * pull request from a release/* branch is open, every other pull request into develop is refused,
 * except one labelled release-content.
 */
class ReleaseFreezeCheckTest
{

	private static final File SCRIPT = new File("scripts/release-freeze.py").getAbsoluteFile();

	record Pull(int number, String headRef, String label) {
		String json()
		{
			return "{\"number\": " + number + ", \"head_ref\": \"" + headRef
				+ "\", \"head_sha\": \"sha" + number + "\", \"labels\": ["
				+ (label == null ? "" : "\"" + label + "\"") + "]}";
		}
	}

	record Case(String name, List<Pull> pulls, List<String> states) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Case> cases()
	{
		Pull feature = new Pull(10, "feature/x", null);
		Pull fix = new Pull(11, "fix/y", null);
		Pull release = new Pull(12, "release/8.9", null);
		Pull partOfTheRelease = new Pull(13, "docs/changelog", "release-content");
		return Stream.of(
			new Case("without a release pull request every pull request passes",
				List.of(feature, fix), List.of("success", "success")),
			new Case("with a release pull request open the others are refused, the release passes",
				List.of(feature, release, fix), List.of("failure", "success", "failure")),
			new Case("a pull request labelled release-content passes during the freeze",
				List.of(release, partOfTheRelease, feature),
				List.of("success", "success", "failure")),
			new Case("the release-content label means nothing without a freeze",
				List.of(partOfTheRelease, feature), List.of("success", "success")),
			new Case("a branch that only contains the word release is no release branch",
				List.of(new Pull(14, "feature/release-notes", null), feature),
				List.of("success", "success")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void theStatuses_followTheOpenReleasePullRequest(final Case freeze) throws Exception
	{
		Run run = run("{\"pulls\": ["
			+ String.join(", ", freeze.pulls().stream().map(Pull::json).toList()) + "]}");

		assertEquals(0, run.exit(), run.output());
		List<String> lines = run.output().lines().toList();
		assertEquals(freeze.pulls().size(), lines.size(), "one status per pull request: " + lines);
		for (int index = 0; index < lines.size(); index++)
		{
			String[] fields = lines.get(index).split("\t", -1);
			assertEquals("sha" + freeze.pulls().get(index).number(), fields[0]);
			assertEquals(freeze.states().get(index), fields[1], lines.get(index));
			assertTrue(fields[3].length() <= 140,
				"a status description has room for 140 characters");
		}
	}

	@Test
	@DisplayName("a refused pull request is told which release pull request holds the freeze")
	void theRefusal_namesTheReleasePullRequest() throws Exception
	{
		Run run = run("{\"pulls\": [" + new Pull(10, "feature/x", null).json() + ", "
			+ new Pull(12, "release/8.9", null).json() + "]}");

		assertTrue(run.output().contains("#12 (release/8.9)"), run.output());
	}

	@Test
	@DisplayName("input it cannot read sets no status at all, and a required status never set blocks the merge")
	void unreadableInput_setsNoStatus() throws Exception
	{
		Run run = run("{\"pulls\": [{\"number\": 1}]}");

		assertEquals(2, run.exit(), run.output());
		assertTrue(run.output().lines().noneMatch(line -> line.contains("\tsuccess\t")),
			run.output());
	}

	record Run(int exit, String output) {
	}

	private static Run run(final String input) throws IOException, InterruptedException
	{
		Process process = new ProcessBuilder("python3", SCRIPT.getPath()).redirectErrorStream(true)
			.start();
		process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
		process.getOutputStream().close();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the script ended");
		return new Run(process.exitValue(), output);
	}
}
