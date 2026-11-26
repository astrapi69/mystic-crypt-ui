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
import org.junit.jupiter.params.provider.ValueSource;

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

	private static final String REFUSAL_BY_12 = "release freeze: #12 (release/8.9) is open;"
		+ " nothing else merges until it is tagged";

	private static final String RELEASE_ITSELF = "the release pull request itself";

	private static final String NO_FREEZE = "no release pull request is open";

	private static final String PART_OF_RELEASE = "labelled release-content: part of the release";

	private static final String LONG_BRANCH = "release/" + "x".repeat(200);

	private static final String LONG_REFUSAL = "release freeze: #2 (" + LONG_BRANCH
		+ ") is open; nothing else merges until it is tagged";

	private static String line(final int number, final String state, final String description)
	{
		return "sha" + number + "\t" + state + "\t" + number + "\t" + description;
	}

	record Exact(String name, List<Pull> pulls, List<String> lines) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Exact> exactLines()
	{
		Pull feature = new Pull(10, "feature/x", null);
		Pull release = new Pull(12, "release/8.9", null);
		Pull partOfTheRelease = new Pull(13, "docs/changelog", "release-content");
		return Stream.of(
			new Exact("no freeze: success with the no-freeze description", List.of(feature),
				List.of(line(10, "success", NO_FREEZE))),
			new Exact("freeze: failure naming the release pull request, the release passes",
				List.of(feature, release),
				List.of(line(10, "failure", REFUSAL_BY_12), line(12, "success", RELEASE_ITSELF))),
			new Exact("exemption: success with the release-content description",
				List.of(release, partOfTheRelease),
				List.of(line(12, "success", RELEASE_ITSELF), line(13, "success", PART_OF_RELEASE))),
			new Exact("release-content without a freeze keeps the exemption description",
				List.of(partOfTheRelease), List.of(line(13, "success", PART_OF_RELEASE))),
			new Exact("a release pull request labelled release-content is the release itself",
				List.of(new Pull(12, "release/8.9", "release-content"), feature),
				List.of(line(12, "success", RELEASE_ITSELF), line(10, "failure", REFUSAL_BY_12))),
			new Exact("a pull request from a hotfix branch into develop is refused during a freeze",
				List.of(new Pull(11, "hotfix/8.9.1", null), release),
				List.of(line(11, "failure", REFUSAL_BY_12), line(12, "success", RELEASE_ITSELF))),
			new Exact("a pull request from a hotfix branch passes during a freeze with the label",
				List.of(new Pull(11, "hotfix/8.9.1", "release-content"), release),
				List.of(line(11, "success", PART_OF_RELEASE), line(12, "success", RELEASE_ITSELF))),
			new Exact("a pull request from a hotfix branch passes without a freeze",
				List.of(new Pull(11, "hotfix/8.9.1", null)),
				List.of(line(11, "success", NO_FREEZE))),
			new Exact("the release prefix is case sensitive",
				List.of(new Pull(11, "Release/8.9", null), feature),
				List.of(line(11, "success", NO_FREEZE), line(10, "success", NO_FREEZE))),
			new Exact("an unrelated label is no exemption",
				List.of(release, new Pull(13, "docs/x", "documentation")),
				List.of(line(12, "success", RELEASE_ITSELF), line(13, "failure", REFUSAL_BY_12))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("exactLines")
	void theStatusLines_carryStateAndDescription(final Exact exact) throws Exception
	{
		Run run = run("{\"pulls\": ["
			+ String.join(", ", exact.pulls().stream().map(Pull::json).toList()) + "]}");

		assertEquals(0, run.exit(), run.output());
		assertEquals(exact.lines(), run.output().lines().toList());
	}

	@Test
	@DisplayName("with two release pull requests open the refusal names the lowest number, in either input order")
	void twoReleasePullRequests_refusalNamesTheLowestNumber() throws Exception
	{
		Pull feature = new Pull(10, "feature/x", null);
		Pull lower = new Pull(5, "release/8.8", null);
		Pull higher = new Pull(12, "release/8.9", null);
		String refusal = "release freeze: #5 (release/8.8) is open;"
			+ " nothing else merges until it is tagged";

		for (List<Pull> pulls : List.of(List.of(feature, lower, higher),
			List.of(feature, higher, lower)))
		{
			Run run = run("{\"pulls\": ["
				+ String.join(", ", pulls.stream().map(Pull::json).toList()) + "]}");

			assertEquals(0, run.exit(), run.output());
			assertEquals(line(10, "failure", refusal), run.output().lines().toList().get(0));
			assertEquals(2, run.output().lines().filter(l -> l.contains(RELEASE_ITSELF)).count(),
				"both release pull requests are the release itself: " + run.output());
		}
	}

	@Test
	@DisplayName("a description longer than 140 characters is cut to 140 and the line keeps its four fields")
	void aLongDescription_isCutToRoom() throws Exception
	{
		Run run = run("{\"pulls\": [" + new Pull(1, "feature/x", null).json() + ", "
			+ new Pull(2, LONG_BRANCH, null).json() + "]}");

		assertEquals(0, run.exit(), run.output());
		String[] fields = run.output().lines().toList().get(0).split("\t", -1);
		assertEquals(4, fields.length, run.output());
		assertEquals("failure", fields[1]);
		assertTrue(LONG_REFUSAL.length() > 140, "the uncut description is longer than the room");
		assertEquals(LONG_REFUSAL.substring(0, 140), fields[3]);
	}

	@Test
	@DisplayName("a description of at most 140 characters is not cut")
	void aShortDescription_isNotCut() throws Exception
	{
		Run run = run("{\"pulls\": [" + new Pull(1, "feature/x", null).json() + ", "
			+ new Pull(12, "release/8.9", null).json() + "]}");

		assertEquals(line(1, "failure", REFUSAL_BY_12), run.output().lines().toList().get(0));
	}

	@Test
	@DisplayName("an empty list of pull requests prints nothing and exits 0")
	void noPullRequests_printNothing() throws Exception
	{
		Run run = run("{\"pulls\": []}");

		assertEquals(0, run.exit(), run.output());
		assertEquals("", run.output());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "not json at all", "", "[]", "{}", "{\"pulls\": 5}",
			"{\"pulls\": null}", "{\"pulls\": {\"a\": 1}}", "{\"pulls\": [5]}",
			"{\"pulls\": [{\"number\": 1, \"head_ref\": \"a\", \"head_sha\": \"s\"}]}" })
	@DisplayName("input that is no pulls document exits 2 and prints no status")
	void inputThatIsNoPullsDocument_exits2(final String input) throws Exception
	{
		Run run = run(input);

		assertEquals(2, run.exit(), run.output());
		assertFalse(run.output().contains("\t"), "no status line: " + run.output());
	}

	private static final String GOOD_PULL = "{\"number\": 1, \"head_ref\": \"feature/a\","
		+ " \"head_sha\": \"s1\", \"labels\": []}";

	/**
	 * The script's docstring says exit 2 on input it cannot read and then no status is set: a field
	 * of the wrong type is such input, and no status is printed for the pull requests before it.
	 */
	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {
			"{\"number\": 2, \"head_ref\": \"feature/b\", \"head_sha\": \"s2\", \"labels\": null}",
			"{\"number\": 2, \"head_ref\": \"feature/b\", \"head_sha\": \"s2\", \"labels\": 5}",
			"{\"number\": 2, \"head_ref\": null, \"head_sha\": \"s2\", \"labels\": []}",
			"{\"number\": 2, \"head_ref\": 5, \"head_sha\": \"s2\", \"labels\": []}",
			"{\"number\": null, \"head_ref\": \"release/c\", \"head_sha\": \"s2\", \"labels\": []}",
			"{\"number\": \"x\", \"head_ref\": \"release/c\", \"head_sha\": \"s2\", \"labels\": []}",
			"{\"number\": 2, \"head_ref\": \"feature/b\", \"head_sha\": null, \"labels\": []}" })
	@DisplayName("a pull request with a field of the wrong type exits 2 and sets no status")
	void aFieldOfTheWrongType_exits2WithoutAnyStatus(final String badPull) throws Exception
	{
		Run run = run("{\"pulls\": [" + GOOD_PULL + ", " + badPull + "]}");

		assertEquals(2, run.exit(), run.output());
		assertFalse(run.output().contains("\t"),
			"no status line, not even for the first pull request: " + run.output());
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
