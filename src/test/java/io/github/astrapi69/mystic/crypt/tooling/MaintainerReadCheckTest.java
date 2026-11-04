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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives {@code scripts/maintainer-read.py} the way the maintainer-read workflow does: the base
 * branch's CODEOWNERS, the changed files, the labels and the issue events of a pull request as JSON
 * on standard input, one status out (#732).
 * <p>
 * A pull request that changes a path CODEOWNERS lists stays red until the maintainer has set the
 * label read-by-maintainer himself: not through a GitHub App, not from another account, and the
 * last time it was set. "Require review from Code Owners" cannot say that here, because every pull
 * request is opened by the account that would be the code owner. A check that has nothing to
 * compare does not pass.
 */
class MaintainerReadCheckTest
{

	private static final File SCRIPT = new File("scripts/maintainer-read.py").getAbsoluteFile();

	private static final String MAINTAINER = "astrapi69";

	private static final String LABEL = "read-by-maintainer";

	private static final String VAULT_FORMAT = "/src/main/java/io/github/astrapi69/mystic/crypt/app/file/xml/";

	private static final String VAULT_FILE = VAULT_FORMAT.substring(1) + "PasswordVaultFormat.java";

	/**
	 * The paths #732 names: the vault format, and the check's own files, so that a change to the
	 * check needs the label as well
	 */
	private static final List<String> LISTED = List.of(VAULT_FORMAT, "/.github/CODEOWNERS",
		"/.github/workflows/maintainer-read.yml", "/scripts/maintainer-read.py");

	private static final String CODEOWNERS = "# the paths of #732\n\n"
		+ LISTED.stream().map(path -> path + " @" + MAINTAINER + "\n").collect(joining());

	private static final String PATTERN_FORMS = "/docs/ @astrapi69\n/build.gradle @astrapi69\n"
		+ "/scripts/*.py @astrapi69\n";

	record Event(String event, String label, String login, String app, String createdAt) {
		String json()
		{
			return "{\"event\": \"" + event + "\", \"label\": {\"name\": \"" + label
				+ "\"}, \"actor\": {\"login\": \"" + login + "\"}, \"performed_via_github_app\": "
				+ (app == null ? "null" : "{\"id\": 1, \"slug\": \"" + app + "\"}")
				+ ", \"created_at\": \"" + createdAt + "\"}";
		}
	}

	/**
	 * One pull request and the status it gets; {@code state} is empty where the script cannot
	 * decide and sets none, and {@code says} is what its output names
	 */
	record Case(String name, String codeowners, List<String> files, List<String> labels,
		List<Event> events, String state, String says) {
		int exit()
		{
			return state.isEmpty() ? 2 : 0;
		}

		String json()
		{
			return "{\"codeowners\": " + quoted(codeowners) + ", \"files\": " + quotedAll(files)
				+ ", \"labels\": " + quotedAll(labels) + ", \"events\": ["
				+ events.stream().map(Event::json).collect(joining(", ")) + "], \"maintainer\": \""
				+ MAINTAINER + "\"}";
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	private static final String UNDECIDED = "";

	static Stream<Case> cases()
	{
		Event byTheMaintainer = new Event("labeled", LABEL, MAINTAINER, null,
			"2026-11-04T10:00:00Z");
		Event throughAnApp = new Event("labeled", LABEL, MAINTAINER, "some-app",
			"2026-11-04T11:00:00Z");
		Event byAnotherAccount = new Event("labeled", LABEL, "someone-else", null,
			"2026-11-04T10:00:00Z");
		Event removed = new Event("unlabeled", LABEL, MAINTAINER, null, "2026-11-04T11:00:00Z");
		Event anotherLabelLater = new Event("labeled", "bug", "someone-else", "some-app",
			"2026-11-04T12:00:00Z");
		List<String> labelled = List.of("bug", LABEL);
		return Stream.of(
			new Case("nothing on a listed path changed: success, saying what it compared",
				CODEOWNERS, List.of("README.md", "src/main/java/Other.java"), List.of(), List.of(),
				"success", "none of the 2 changed files is on one of the 4 listed paths"),
			new Case("a changed vault format file without the label fails and names the file",
				CODEOWNERS, List.of("README.md", VAULT_FILE), List.of(), List.of(), "failure",
				"no " + LABEL + " label: " + VAULT_FILE),
			new Case("the label set by the maintainer through the web passes", CODEOWNERS,
				List.of(VAULT_FILE), labelled, List.of(byTheMaintainer), "success",
				"read by " + MAINTAINER),
			new Case("the label set through an app fails, even under the maintainer's login",
				CODEOWNERS, List.of(VAULT_FILE), labelled, List.of(throughAnApp), "failure",
				"through the app some-app"),
			new Case("the label set by another account fails", CODEOWNERS, List.of(VAULT_FILE),
				labelled, List.of(byAnotherAccount), "failure",
				"set by someone-else, not by " + MAINTAINER),
			new Case("the last labeling counts: earlier by the maintainer, last through an app",
				CODEOWNERS, List.of(VAULT_FILE), labelled,
				List.of(byTheMaintainer, removed, throughAnApp), "failure",
				"through the app some-app"),
			new Case("the last labeling is the latest in time, not the last in the list",
				CODEOWNERS, List.of(VAULT_FILE), labelled, List.of(throughAnApp, byTheMaintainer),
				"failure", "through the app some-app"),
			new Case("a label of another name set later changes nothing", CODEOWNERS,
				List.of(VAULT_FILE), labelled, List.of(byTheMaintainer, anotherLabelLater),
				"success", "read by " + MAINTAINER),
			new Case("a label removed after it was set does not count", CODEOWNERS,
				List.of(VAULT_FILE), List.of("bug"), List.of(byTheMaintainer, removed), "failure",
				"no " + LABEL + " label"),
			new Case("the label on the pull request with no event saying who set it fails",
				CODEOWNERS, List.of(VAULT_FILE), labelled, List.of(), "failure",
				"no event says who set it"),
			new Case("a change to CODEOWNERS itself needs the label", CODEOWNERS,
				List.of(".github/CODEOWNERS"), List.of(), List.of(), "failure",
				"no " + LABEL + " label: .github/CODEOWNERS"),
			new Case("a change to the check's workflow needs the label", CODEOWNERS,
				List.of("README.md", ".github/workflows/maintainer-read.yml"), List.of(), List.of(),
				"failure", ".github/workflows/maintainer-read.yml"),
			new Case("a file moved out of the vault format counts by its old name", CODEOWNERS,
				List.of("src/main/java/Moved.java", VAULT_FILE), List.of(), List.of(), "failure",
				VAULT_FILE),
			new Case("a path that only shares a prefix with a listed directory is not in it",
				CODEOWNERS,
				List.of(VAULT_FORMAT.substring(1).replace("/xml/", "/xmlx/") + "Other.java"),
				List.of(), List.of(), "success", "none of the 1 changed files"),
			new Case("a directory pattern covers a file at any depth below it", PATTERN_FORMS,
				List.of("docs/guide/deep/page.md"), List.of(), List.of(), "failure",
				"docs/guide/deep/page.md"),
			new Case("a file pattern covers that file", PATTERN_FORMS, List.of("build.gradle"),
				List.of(), List.of(), "failure", "build.gradle"),
			new Case("a file pattern does not cover a file that only begins with its name",
				PATTERN_FORMS, List.of("build.gradle.kts"), List.of(), List.of(), "success",
				"none of the 1 changed files is on one of the 3 listed paths"),
			new Case("a * matches within one path segment", PATTERN_FORMS,
				List.of("scripts/release-freeze.py"), List.of(), List.of(), "failure",
				"scripts/release-freeze.py"),
			new Case("a * does not match across a /", PATTERN_FORMS,
				List.of("scripts/fixtures/helper.py"), List.of(), List.of(), "success",
				"none of the 1 changed files"),
			new Case("a CODEOWNERS that lists no path cannot decide", "# nothing listed\n\n",
				List.of(VAULT_FILE), List.of(), List.of(), UNDECIDED, "lists no path"),
			new Case("an empty list of changed files cannot decide", CODEOWNERS, List.of(),
				List.of(), List.of(), UNDECIDED, "no changed file"),
			new Case("a ** pattern is not read: it cannot decide",
				"/src/**/xml/ @" + MAINTAINER + "\n", List.of(VAULT_FILE), List.of(), List.of(),
				UNDECIDED, "a form the check does not read"),
			new Case("a negated pattern is not read: it cannot decide",
				"!/docs/ @" + MAINTAINER + "\n", List.of("docs/a.md"), List.of(), List.of(),
				UNDECIDED, "a form the check does not read"),
			new Case("a pattern that is not anchored at the root cannot decide",
				"docs/ @" + MAINTAINER + "\n", List.of("docs/a.md"), List.of(), List.of(),
				UNDECIDED, "not anchored"),
			new Case("a pattern without an owner cannot decide", "/docs/\n", List.of("docs/a.md"),
				List.of(), List.of(), UNDECIDED, "no owner"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void theStatus_followsTheListedPathsAndTheLabel(final Case check) throws Exception
	{
		Run run = run(check.json());

		assertEquals(check.exit(), run.exit(), run.toString());
		List<String> lines = run.stdout().lines().toList();
		assertTrue(lines.size() <= 1, "at most one status: " + lines);
		String[] status = String.join("", lines).split("\t", 2);
		assertEquals(check.state(), status[0], run.toString());
		assertTrue(status[status.length - 1].length() <= 140,
			"a status description has room for 140 characters: " + run);
		assertTrue((run.stdout() + run.stderr()).contains(check.says()), run.toString());
	}

	@Test
	@DisplayName("a description longer than 140 characters keeps what is missing and the end of the path")
	void aLongPath_isShortenedFromTheFront() throws Exception
	{
		String deep = VAULT_FORMAT.substring(1) + "a/very/deep/package/that/makes/the/path/long/"
			+ "EnoughToOverflowTheDescription.java";
		Run run = run(
			new Case("", CODEOWNERS, List.of(deep, VAULT_FILE), List.of(), List.of(), "failure", "")
				.json());

		String description = run.stdout().strip().split("\t", 2)[1];
		assertTrue(description.length() <= 140, description);
		assertTrue(description.startsWith("no " + LABEL + " label: ..."), description);
		assertTrue(description.endsWith("EnoughToOverflowTheDescription.java and 1 more"),
			description);
	}

	@Test
	@DisplayName("input it cannot read sets no status at all, and a required status never set blocks the merge")
	void unreadableInput_setsNoStatus() throws Exception
	{
		Run run = run("{\"codeowners\": \"/docs/ @astrapi69\"}");

		assertEquals(2, run.exit(), run.toString());
		assertEquals("", run.stdout(), run.toString());
		assertTrue(run.stderr().startsWith("maintainer-read: cannot read the input"),
			"the log says why: " + run);
	}

	@Test
	@DisplayName("CODEOWNERS lists exactly the paths #732 names, each for the maintainer, and each exists on this branch")
	void theRepositorysCodeowners_listsThePathsOfTheIssue() throws IOException
	{
		List<String> entries = Files
			.readAllLines(Path.of(".github/CODEOWNERS"), StandardCharsets.UTF_8).stream()
			.map(String::strip).filter(line -> !line.isEmpty() && !line.startsWith("#"))
			.map(line -> String.join(" ", line.split("\\s+"))).toList();

		assertEquals(LISTED.stream().map(path -> path + " @" + MAINTAINER).toList(), entries);
		for (String path : LISTED)
		{
			Path onTheBranch = Path.of(path.substring(1));
			assertTrue(
				path.endsWith("/")
					? Files.isDirectory(onTheBranch)
					: Files.isRegularFile(onTheBranch),
				path + " is on this branch, so a rename cannot drop it from the list unseen");
		}
		for (String vaultFormat : List.of("PasswordVaultFormat.java", "VaultXmlCodec.java",
			"VaultFileWriter.java"))
		{
			assertTrue(Files.isRegularFile(Path.of(VAULT_FORMAT.substring(1), vaultFormat)),
				vaultFormat + " is still inside the listed directory");
		}
	}

	@Test
	@DisplayName("the script reads the repository's CODEOWNERS, and a vault format change needs the label")
	void theScript_readsTheRepositorysCodeowners() throws Exception
	{
		String codeowners = Files.readString(Path.of(".github/CODEOWNERS"), StandardCharsets.UTF_8);

		Run run = run(
			new Case("", codeowners, List.of(VAULT_FILE), List.of(), List.of(), "failure", "")
				.json());

		assertEquals(0, run.exit(), run.toString());
		assertTrue(run.stdout().startsWith("failure\tno " + LABEL + " label: "), run.toString());
	}

	@Test
	@DisplayName("the workflow runs the base branch's script, names no job like the status, and counts a renamed file by its old name")
	void theWorkflow_judgesWithTheBaseBranchsFilesOnly() throws IOException
	{
		String workflow = Files.readString(Path.of(".github/workflows/maintainer-read.yml"),
			StandardCharsets.UTF_8);

		assertTrue(workflow.contains("pull_request_target:"), "runs as pull_request_target");
		assertTrue(workflow.contains("ref: ${{ github.base_ref }}"), "checks out the base branch");
		assertFalse(workflow.contains("pull_request.head.ref")
			|| workflow.contains("pull_request.head.repo"), "checks out nothing of the head");
		assertFalse(
			Pattern.compile("(?m)^\\s+(?:name:\\s*)?maintainer-read:?\\s*$").matcher(workflow)
				.find(),
			"a job of the status's name would satisfy the required status with its own check run");
		assertTrue(workflow.contains("previous_filename"),
			"a file moved out of a listed path counts by its old name");
		assertFalse(
			workflow.contains("pull_request.title") || workflow.contains("pull_request.body"),
			"no title or body near a shell line");
	}

	private static String quoted(final String text)
	{
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}

	private static String quotedAll(final List<String> texts)
	{
		return "[" + texts.stream().map(MaintainerReadCheckTest::quoted).collect(joining(", "))
			+ "]";
	}

	record Run(int exit, String stdout, String stderr) {
		@Override
		public String toString()
		{
			return "exit " + exit + ", stdout: " + stdout + ", stderr: " + stderr;
		}
	}

	private static Run run(final String input) throws IOException, InterruptedException
	{
		Process process = new ProcessBuilder("python3", "-I", SCRIPT.getPath()).start();
		process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
		process.getOutputStream().close();
		String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the script ended");
		return new Run(process.exitValue(), stdout, stderr);
	}
}
