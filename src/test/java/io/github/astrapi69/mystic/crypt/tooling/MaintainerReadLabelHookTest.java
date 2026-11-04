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
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives {@code scripts/no-maintainer-read-label.py} the way Claude Code runs a PreToolUse hook:
 * one tool call as JSON on standard input, a refusal as JSON on standard output (#737). Only the
 * maintainer sets the label read-by-maintainer (#732), and every agent acts as his GitHub account,
 * so the hook is what keeps a Claude Code session, local or in the cloud, from setting or removing
 * it. It holds back nothing that does not run through Claude Code.
 */
class MaintainerReadLabelHookTest
{

	private static final File SCRIPT = new File("scripts/no-maintainer-read-label.py")
		.getAbsoluteFile();

	private static final File PROJECT_SETTINGS = new File(".claude/settings.json")
		.getAbsoluteFile();

	private static final String LABEL = "read-by-maintainer";

	private static final String DENY = "\"permissionDecision\": \"deny\"";

	static Stream<Arguments> bashCommands()
	{
		return Stream.of(//
			Arguments.of("gh adding the label is refused", "gh pr edit 12 --add-label " + LABEL,
				true),
			Arguments.of("gh removing the label is refused",
				"gh pr edit 12 --remove-label " + LABEL, true),
			Arguments.of("a pull request created with the label is refused",
				"gh pr create --title x --label " + LABEL + " --body-file b.md", true),
			Arguments.of("the REST labels endpoint with the label is refused",
				"gh api -X POST repos/o/r/issues/12/labels -f 'labels[]=" + LABEL + "'", true),
			Arguments.of("deleting the label through REST is refused",
				"gh api -X DELETE repos/o/r/issues/12/labels/" + LABEL, true),
			Arguments.of("a GraphQL label mutation naming the label is refused",
				"gh api graphql -f query='mutation { addLabelsToLabelable(input: {labelIds: [\""
					+ LABEL + "\"]}) { clientMutationId } }'",
				true),
			Arguments.of("another label passes", "gh pr edit 12 --add-label P3", false),
			Arguments.of("reading the label's events passes",
				"gh api repos/o/r/issues/12/events --jq '.[]" + " | select(.label.name == \""
					+ LABEL + "\")'",
				false),
			Arguments.of("a grep for the label passes",
				"grep -n " + LABEL + " .github/workflows/maintainer-read.yml", false),
			Arguments.of("a commit passes", "git commit -F msg.txt", false));
	}

	/**
	 * A Bash command, as the session would run it
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("bashCommands")
	void aBashCommand(String caseName, String command, boolean refused) throws Exception
	{
		Run run = hook(
			"{\"tool_name\": \"Bash\", \"tool_input\": {\"command\": " + json(command) + "}}");

		assertEquals(0, run.exit(), caseName + ": " + run.output());
		assertEquals(refused, run.output().contains(DENY), caseName + ": " + run.output());
	}

	static Stream<Arguments> waysOfNamingAScript()
	{
		return Stream.of(//
			Arguments.of("run by bash is refused", "bash %s", true),
			Arguments.of("run as the command itself is refused", "%s", true),
			Arguments.of("run after another command is refused", "cd /tmp && %s", true),
			Arguments.of("sourced is refused", "source %s", true),
			Arguments.of("read by grep passes", "grep -n label %s", false),
			Arguments.of("shown by cat passes", "cat %s", false),
			Arguments.of("copied passes", "cp %s /tmp/copy.sh", false));
	}

	/**
	 * A script that sets the label counts when the command runs it, not when it only reads, shows
	 * or copies it
	 */
	@ParameterizedTest(name = "a script that sets the label, {0}")
	@MethodSource("waysOfNamingAScript")
	void aScriptThatSetsTheLabel(String caseName, String form, boolean refused,
		@TempDir File directory) throws Exception
	{
		File script = new File(directory, "set.sh");
		Files.writeString(script.toPath(),
			"#!/bin/bash\nset -e\ngh pr edit 5 --add-label " + LABEL + "\n",
			StandardCharsets.UTF_8);
		assertTrue(script.setExecutable(true));

		Run run = hook(bash(form.formatted(script.getAbsolutePath())));

		assertEquals(refused, run.output().contains(DENY), caseName + ": " + run.output());
	}

	@Test
	@DisplayName("an input file the command hands to a REST write is read, and the label in it is refused")
	void anInputFileWithTheLabel_isRefused(@TempDir File directory) throws Exception
	{
		File input = new File(directory, "labels.json");
		Files.writeString(input.toPath(), "{\"labels\": [\"" + LABEL + "\"]}",
			StandardCharsets.UTF_8);

		Run run = hook(
			bash("gh api -X POST repos/o/r/issues/1/labels --input " + input.getAbsolutePath()));

		assertTrue(run.output().contains(DENY), run.output());
	}

	@Test
	@DisplayName("a script whose text describes the label and the gh call on other lines than its write passes")
	void aScriptThatOnlyDescribesTheLabel_passes(@TempDir File directory) throws Exception
	{
		File script = new File(directory, "issue.py");
		Files.writeString(script.toPath(), "BODY = '''The hook refuses `gh pr edit\n--add-label "
			+ LABEL
			+ "`.'''\nimport subprocess\nsubprocess.run(['gh', 'issue', 'create', '--body', BODY])\n",
			StandardCharsets.UTF_8);

		Run run = hook(bash("python3 " + script.getAbsolutePath()));

		assertEquals(0, run.exit(), run.output());
		assertEquals("", run.output(), "describing the label is not setting it");
	}

	static Stream<Arguments> mcpCalls()
	{
		return Stream.of(//
			Arguments.of("an MCP issue write with the label is refused", "mcp__github__issue_write",
				"{\"owner\": \"astrapi69\", \"repo\": \"r\", \"issue_number\": 5, \"labels\": [\""
					+ LABEL + "\"]}",
				true),
			Arguments.of("an MCP pull request update with the label is refused",
				"mcp__GitHub__update_pull_request",
				"{\"pullNumber\": 5, \"labels\": [\"P3\", \"" + LABEL + "\"]}", true),
			Arguments.of("an MCP label removal is refused", "mcp__github__remove_issue_label",
				"{\"issue_number\": 5, \"label\": \"" + LABEL + "\"}", true),
			Arguments.of("an MCP write with another label passes", "mcp__github__issue_write",
				"{\"issue_number\": 5, \"labels\": [\"P3\"]}", false),
			Arguments.of("an MCP read that filters by the label passes", "mcp__github__list_issues",
				"{\"labels\": [\"" + LABEL + "\"]}", false),
			Arguments.of("an MCP tool of another server passes", "mcp__claude-mem__search",
				"{\"query\": \"" + LABEL + "\"}", false));
	}

	/**
	 * A GitHub MCP tool call, the way a session in the cloud is likely to set a label
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("mcpCalls")
	void anMcpToolCall(String caseName, String tool, String input, boolean refused) throws Exception
	{
		Run run = hook("{\"tool_name\": \"" + tool + "\", \"tool_input\": " + input + "}");

		assertEquals(0, run.exit(), caseName + ": " + run.output());
		assertEquals(refused, run.output().contains(DENY), caseName + ": " + run.output());
	}

	@Test
	@DisplayName("input that is not a tool call is let through, not a crash that blocks every tool")
	void notAToolCall_passes() throws Exception
	{
		Run run = hook("not json");

		assertEquals(0, run.exit(), run.output());
		assertEquals("", run.output());
	}

	@Test
	@DisplayName("the project settings run the hook for Bash and for every MCP tool, from the repository")
	void theProjectSettingsRunTheHook() throws Exception
	{
		String settings = Files.readString(PROJECT_SETTINGS.toPath(), StandardCharsets.UTF_8);
		Matcher entry = Pattern.compile(
			"\"matcher\"\\s*:\\s*\"([^\"]+)\"[^\\]]*?\"command\"\\s*:\\s*\""
				+ "((?:[^\"\\\\]|\\\\.)*no-maintainer-read-label\\.py(?:[^\"\\\\]|\\\\.)*)\"",
			Pattern.DOTALL).matcher(settings);

		assertTrue(entry.find(), "a PreToolUse entry runs the hook: " + settings);
		Pattern matcher = Pattern.compile(entry.group(1));
		for (String tool : List.of("Bash", "mcp__github__issue_write", "mcp__GitHub__add_labels"))
		{
			assertTrue(matcher.matcher(tool).matches(), tool + " is matched by " + entry.group(1));
		}
		assertTrue(entry.group(2).contains("$CLAUDE_PROJECT_DIR"),
			"the script is found from the project, wherever the session starts: " + entry.group(2));
		assertTrue(settings.contains("\"sessionUrl\": false"),
			"the attribution settings stay: " + settings);
	}

	private record Run(int exit, String output) {
	}

	private static String bash(String command)
	{
		return "{\"tool_name\": \"Bash\", \"tool_input\": {\"command\": " + json(command) + "}}";
	}

	private static String json(String text)
	{
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}

	private static Run hook(String input) throws IOException, InterruptedException
	{
		ProcessBuilder builder = new ProcessBuilder("python3", "-I", SCRIPT.getPath())
			.redirectError(ProcessBuilder.Redirect.DISCARD);
		// a home without .claude/hooks, so the refusals of the test do not land in the real log
		builder.environment().put("HOME", new File("build/tmp/no-home").getAbsolutePath());
		Process process = builder.start();
		process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
		process.getOutputStream().close();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
			.trim();
		if (!process.waitFor(30, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException(SCRIPT + " did not finish");
		}
		return new Run(process.exitValue(), output);
	}
}
