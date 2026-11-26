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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives {@code scripts/closing-references.py} the way the closing-references workflow does: the
 * pull request's body, its commit messages and the references GitHub reports, as JSON on standard
 * input (#555). A pull request may close an issue only when a line says so and nothing else.
 * <p>
 * The 8.7 and 8.8 release pull requests closed #518 and #535 through "KNOWN AND NOT FIXED: #NN";
 * those are the first cases here, once with GitHub's list filled and once with it still empty, as
 * it is for a few seconds after a pull request opens.
 */
class ClosingReferencesCheckTest
{

	private static final File SCRIPT = new File("scripts/closing-references.py").getAbsoluteFile();

	private static final String REPOSITORY = "astrapi69/mystic-crypt-ui";

	record Case(String name, String body, String commit, String reported, int exit) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Case> cases()
	{
		return Stream.of(
			new Case("'KNOWN AND NOT FIXED: #535' with GitHub reporting #535 is refused (8.8)",
				"- **KNOWN AND NOT FIXED: #535**, as asked.", "docs: x",
				"\"" + REPOSITORY + "#535\"", 1),
			new Case("the same text before GitHub has filled its list is refused too",
				"- **KNOWN AND NOT FIXED: #535**, as asked.", "docs: x", "", 1),
			new Case("'Closes #547' on a line of its own passes", "text\n\nCloses #547", "build: x",
				"\"" + REPOSITORY + "#547\"", 0),
			new Case("a closing line in a commit message passes", "text", "fix: x\n\nCloses #9",
				"\"" + REPOSITORY + "#9\"", 0),
			new Case("several references on one closing line pass", "Fixes #1, #2 and closes #3.",
				"chore: x",
				"\"" + REPOSITORY + "#1\", \"" + REPOSITORY + "#2\", \"" + REPOSITORY + "#3\"", 0),
			new Case("a closing line as a list item passes", "- Closes #12", "chore: x",
				"\"" + REPOSITORY + "#12\"", 0),
			new Case("any case of the keyword passes", "resolves #7", "chore: x",
				"\"" + REPOSITORY + "#7\"", 0),
			new Case("an issue of another repository named with its repository passes",
				"Closes astrapi69/lethenon#3", "chore: x", "\"astrapi69/lethenon#3\"", 0),
			new Case("an issue of another repository named by number only is refused", "Closes #3",
				"chore: x", "\"astrapi69/lethenon#3\"", 1),
			new Case("a reference without a closing keyword closes nothing and passes",
				"Refs #535, as before", "docs: x\n\nRefs #535", "", 0),
			new Case("a closing keyword in the middle of prose is refused",
				"This fixes #44 along the way.", "fix: x", "", 1),
			new Case("a reported reference that is not a reference is unreadable input",
				"Closes #1", "chore: x", "\"junk\"", 2),
			new Case("a reported reference without a number is unreadable input", "Closes #1",
				"chore: x", "\"astrapi69/lethenon#\"", 2),
			new Case("the repository is compared in any case: the same issue passes",
				"Closes Astrapi69/Mystic-Crypt-UI#5", "chore: x", "\"" + REPOSITORY + "#5\"", 0),
			new Case("the same number in another repository is not the same issue",
				"Closes Astrapi69/Mystic-Crypt-UI#5", "chore: x", "\"astrapi69/lethenon#5\"", 1),
			new Case("a colon after the keyword is part of a closing line", "Closes: #12",
				"chore: x", "\"" + REPOSITORY + "#12\"", 0),
			new Case("a colon after the keyword does not hide prose on the line",
				"Closes: #12 as asked", "chore: x", "\"" + REPOSITORY + "#12\"", 1),
			new Case("an issue that stays open, in parentheses in a commit subject, closes nothing",
				"text", "fix: the thing (#962)", "", 0),
			new Case("an issue that stays open, written '#NN stays open', closes nothing",
				"#962 stays open", "chore: x", "", 0),
			new Case("'fix: #NN stays open' reads as a closing keyword and is refused (AG-069)",
				"fix: #962 stays open", "chore: x", "", 1),
			new Case("'Fixes #NN stays open' is refused, the issue is not named alone",
				"Fixes #962 stays open", "chore: x", "\"" + REPOSITORY + "#962\"", 1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void theCheck_decidesAsTheRuleSays(final Case closing) throws Exception
	{
		String input = "{\"repository\": \"" + REPOSITORY + "\", \"body\": " + json(closing.body())
			+ ", \"commits\": [" + json(closing.commit()) + "], \"closing\": [" + closing.reported()
			+ "]}";

		Run run = run(input);

		assertEquals(closing.exit(), run.exit(), run.output());
		assertTrue(run.output().startsWith("closing-references: "),
			"the check says what it read: " + run.output());
	}

	@org.junit.jupiter.api.Test
	@org.junit.jupiter.api.DisplayName("input it cannot read is a failure, not a pass")
	void unreadableInput_isAFailure() throws Exception
	{
		Run run = run("{\"repository\": \"" + REPOSITORY + "\"}");

		assertEquals(2, run.exit(), run.output());
	}

	@org.junit.jupiter.api.Test
	@org.junit.jupiter.api.DisplayName("a reference of this repository is shown as '#n' in the NOT NAMED line")
	void notNamedLine_showsTheNumberAlone_forThisRepository() throws Exception
	{
		Run run = run(input("\"This fixes #44 along the way.\"", "[\"chore: x\"]", "[]"));

		assertEquals(1, run.exit(), run.output());
		assertTrue(run.output().contains("NOT NAMED: " + REPOSITORY + "#44 would be closed"),
			run.output());
		assertTrue(run.output().contains("says only 'Closes #44'."), run.output());
	}

	@org.junit.jupiter.api.Test
	@org.junit.jupiter.api.DisplayName("a reference of another repository is shown as 'owner/name#n' in the NOT NAMED line")
	void notNamedLine_showsTheRepository_forAnotherRepository() throws Exception
	{
		Run run = run(input("\"This fixes astrapi69/lethenon#8 too.\"", "[\"chore: x\"]", "[]"));

		assertEquals(1, run.exit(), run.output());
		assertTrue(run.output().contains("NOT NAMED: astrapi69/lethenon#8 would be closed"),
			run.output());
		assertTrue(run.output().contains("says only 'Closes astrapi69/lethenon#8'."), run.output());
		assertFalse(run.output().contains("'Closes #8'"), run.output());
	}

	@org.junit.jupiter.api.Test
	@org.junit.jupiter.api.DisplayName("the summary counts what GitHub reported, what the text says and what is named")
	void summary_countsReportedFoundAndNamed() throws Exception
	{
		String reported = "[\"" + REPOSITORY + "#1\", \"" + REPOSITORY + "#2\"]";

		Run run = run(input("\"Closes #1\\nThis fixes #3 too.\"", "[\"chore: x\"]", reported));

		assertEquals(1, run.exit(), run.output());
		assertTrue(
			run.output().startsWith("closing-references: 3 issue(s) this pull request would "
				+ "close (2 reported by GitHub, 2 found in the text), 1 named on a closing line"),
			run.output());
	}

	@org.junit.jupiter.api.Test
	@org.junit.jupiter.api.DisplayName("when every reference is named, the summary says so and nothing is refused")
	void summary_countsAllNamed_whenEveryReferenceIsNamed() throws Exception
	{
		Run run = run(input("\"Closes #1\"", "[\"chore: x\"]", "[\"" + REPOSITORY + "#1\"]"));

		assertEquals(0, run.exit(), run.output());
		assertTrue(
			run.output().startsWith("closing-references: 1 issue(s) this pull request would "
				+ "close (1 reported by GitHub, 1 found in the text), 1 named on a closing line"),
			run.output());
		assertFalse(run.output().contains("NOT NAMED"), run.output());
	}

	record Raw(String name, String repository, String body, String commits, String closing,
		int exit) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Raw> readableInputs()
	{
		String repository = "\"" + REPOSITORY + "\"";
		return Stream.of(
			new Raw("a body of null is read as an empty text: its commit still names the issue",
				repository, "null", "[\"fix: x\\n\\nCloses #4\"]", "[\"" + REPOSITORY + "#4\"]", 0),
			new Raw("a body of null is read as an empty text: nothing names the reported issue",
				repository, "null", "[\"fix: x\"]", "[\"" + REPOSITORY + "#4\"]", 1),
			new Raw("a pull request without commits and without references passes", repository,
				"\"text\"", "[]", "[]", 0));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("readableInputs")
	void readableInput_isDecidedByItsContent(final Raw raw) throws Exception
	{
		Run run = run(input(raw.repository(), raw.body(), raw.commits(), raw.closing()));

		assertEquals(raw.exit(), run.exit(), run.output());
		assertTrue(run.output().startsWith("closing-references: "), run.output());
	}

	static Stream<String[]> notJson()
	{
		return Stream.of(new String[] { "text that is not JSON", "closing references" },
			new String[] { "empty input", "" }, new String[] { "a JSON array", "[]" },
			new String[] { "a JSON null", "null" },
			new String[] { "JSON cut off in the middle", "{\"repository\": \"a/b\", \"body\"" });
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("notJson")
	void inputThatIsNoJsonObject_isAFailure(final String name, final String input) throws Exception
	{
		Run run = run(input);

		assertEquals(2, run.exit(), run.output());
		assertTrue(run.output().startsWith("closing-references: cannot read the input"),
			run.output());
	}

	/**
	 * Wrong types in the input: the docstring promises exit 2 "on input it cannot read", and an
	 * input of the wrong type is such an input. Before the fix most rows ended in a traceback with
	 * exit 1 and the rest were read as something else without a word (#555).
	 */
	static Stream<Raw> wrongTypes()
	{
		String repository = "\"" + REPOSITORY + "\"";
		return Stream.of(
			new Raw("a body that is a number is unreadable", repository, "5", "[]", "[]", 2),
			new Raw("a body that is true is unreadable", repository, "true", "[]", "[]", 2),
			new Raw("a body that is a list is unreadable", repository, "[\"Closes #1\"]", "[]",
				"[]", 2),
			new Raw("a commit message of null is unreadable", repository, "\"\"", "[null]", "[]",
				2),
			new Raw("a commit message that is a number is unreadable", repository, "\"\"", "[5]",
				"[]", 2),
			new Raw("a reported reference of null is unreadable", repository, "\"\"", "[]",
				"[null]", 2),
			new Raw("a reported reference that is a number is unreadable", repository, "\"\"", "[]",
				"[5]", 2),
			new Raw("a repository that is a list is unreadable", "[\"a\"]", "\"\"", "[]",
				"[\"#1\"]", 2),
			new Raw("a repository of null with a reference to read is unreadable", "null",
				"\"Closes #1\"", "[]", "[]", 2),
			new Raw("a repository of null is unreadable", "null", "\"\"", "[]", "[]", 2),
			new Raw("commits that are one string are unreadable", repository, "\"\"",
				"\"fix: x\\n\\nCloses #1\"", "[]", 2),
			new Raw("commits that are an object are unreadable", repository, "\"\"",
				"{\"Closes #2\": 1}", "[]", 2),
			new Raw("reported references that are an object are unreadable", repository, "\"\"",
				"[]", "{\"" + REPOSITORY + "#1\": 1}", 2),
			new Raw("a body that is an empty object is unreadable", repository, "{}", "[]", "[]",
				2),
			new Raw("a body that is the number 0 is unreadable", repository, "0", "[]", "[]", 2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("wrongTypes")
	void inputOfTheWrongType_isAFailureNotATraceback(final Raw raw) throws Exception
	{
		Run run = run(input(raw.repository(), raw.body(), raw.commits(), raw.closing()));

		assertEquals(raw.exit(), run.exit(), run.output());
		assertTrue(run.output().startsWith("closing-references: cannot read the input"),
			run.output());
		assertFalse(run.output().contains("Traceback"), run.output());
	}

	private static String input(final String body, final String commits, final String closing)
	{
		return input("\"" + REPOSITORY + "\"", body, commits, closing);
	}

	private static String input(final String repository, final String body, final String commits,
		final String closing)
	{
		return "{\"repository\": " + repository + ", \"body\": " + body + ", \"commits\": "
			+ commits + ", \"closing\": " + closing + "}";
	}

	private static String json(final String text)
	{
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
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
