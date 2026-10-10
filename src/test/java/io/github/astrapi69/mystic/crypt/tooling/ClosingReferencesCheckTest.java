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
				"This fixes #44 along the way.", "fix: x", "", 1));
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
