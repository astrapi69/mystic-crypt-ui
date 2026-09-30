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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives {@code scripts/check-commit-provenance.sh} the way CI and the commit-msg hook do: against
 * a real throwaway git repository, through the script's own command line.
 * <p>
 * The script refused a non-human co-author trailer (#373) but never looked at who a commit is
 * authored or committed by. A session committing under the default identity {@code Claude
 * <noreply@anthropic.com>} passed it, and GitHub turned that author into exactly the forbidden
 * trailer when it squash-merged the pull request (#444).
 */
class CommitProvenanceCheckTest
{

	private static final String HUMAN = "Asterios Raptis <asteri.raptis@gmail.com>";

	private static final String TOOL = "Claude <noreply@anthropic.com>";

	private static final String GITHUB = "GitHub <noreply@github.com>";

	private static final File SCRIPT = new File("scripts/check-commit-provenance.sh")
		.getAbsoluteFile();

	static Stream<Arguments> commitsInARange()
	{
		return Stream.of(//
			Arguments.of("a human author and committer pass", HUMAN, HUMAN, "feat: x", 0),
			Arguments.of("an author that is the tool is refused", TOOL, HUMAN, "feat: x", 1),
			Arguments.of("a committer that is the tool is refused", HUMAN, TOOL, "feat: x", 1),
			Arguments.of("GitHub as the committer of a web merge passes", HUMAN, GITHUB, "feat: x",
				0),
			Arguments.of("a co-author trailer naming the tool is refused", HUMAN, HUMAN,
				"feat: x\n\nCo-Authored-By: " + TOOL, 1),
			Arguments.of("a written exception lets a tool author through", TOOL, HUMAN,
				"feat: x\n\nCo-Author-Exception: generated fixture, reviewed by hand", 0),
			Arguments.of("prose that mentions the tool is not an identity", HUMAN, HUMAN,
				"docs: what Claude got wrong here", 0));
	}

	/**
	 * The {@code --range} mode, which CI runs over the commits of a pull request
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("commitsInARange")
	void rangeModeJudgesTheIdentityAndTheTrailers(String caseName, String author, String committer,
		String message, int expectedExit, @TempDir File repository) throws Exception
	{
		git(repository, Map.of(), "init", "-q", "-b", "develop");
		commit(repository, HUMAN, HUMAN, "chore: base", "base.txt");
		git(repository, Map.of(), "branch", "base");
		commit(repository, author, committer, message, "change.txt");

		int exit = run(repository, Map.of(), SCRIPT.getPath(), "--range", "base..HEAD");

		assertEquals(expectedExit, exit, caseName);
	}

	static Stream<Arguments> identitiesAboutToCommit()
	{
		return Stream.of(//
			Arguments.of("a human identity passes the hook", HUMAN, HUMAN, 0),
			Arguments.of("the tool as author is refused by the hook", TOOL, HUMAN, 1),
			Arguments.of("the tool as committer is refused by the hook", HUMAN, TOOL, 1));
	}

	/**
	 * The {@code --message-file} mode, which the commit-msg hook runs before the commit exists: the
	 * identity it checks is the one git is about to use
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("identitiesAboutToCommit")
	void messageFileModeJudgesTheIdentityAboutToCommit(String caseName, String author,
		String committer, int expectedExit, @TempDir File repository) throws Exception
	{
		git(repository, Map.of(), "init", "-q", "-b", "develop");
		File message = new File(repository, "COMMIT_EDITMSG");
		Files.writeString(message.toPath(), "feat: x\n", StandardCharsets.UTF_8);

		int exit = run(repository, identityEnvironment(author, committer), SCRIPT.getPath(),
			"--message-file", message.getAbsolutePath());

		assertEquals(expectedExit, exit, caseName);
	}

	private static void commit(File repository, String author, String committer, String message,
		String fileName) throws Exception
	{
		Files.writeString(new File(repository, fileName).toPath(), fileName,
			StandardCharsets.UTF_8);
		git(repository, Map.of(), "add", fileName);
		git(repository, identityEnvironment(author, committer), "commit", "-q", "-m", message);
	}

	private static Map<String, String> identityEnvironment(String author, String committer)
	{
		return Map.of("GIT_AUTHOR_NAME", nameOf(author), "GIT_AUTHOR_EMAIL", emailOf(author),
			"GIT_COMMITTER_NAME", nameOf(committer), "GIT_COMMITTER_EMAIL", emailOf(committer));
	}

	private static String nameOf(String identity)
	{
		return identity.substring(0, identity.indexOf(" <"));
	}

	private static String emailOf(String identity)
	{
		return identity.substring(identity.indexOf('<') + 1, identity.indexOf('>'));
	}

	private static void git(File repository, Map<String, String> environment, String... arguments)
		throws Exception
	{
		List<String> command = new ArrayList<>(List.of("git"));
		command.addAll(List.of(arguments));
		int exit = run(repository, environment, command.toArray(String[]::new));
		if (exit != 0)
		{
			throw new IllegalStateException("git " + String.join(" ", arguments)
				+ " failed with exit code " + exit + " in " + repository);
		}
	}

	private static int run(File directory, Map<String, String> environment, String... command)
		throws Exception
	{
		ProcessBuilder builder = new ProcessBuilder(command).directory(directory)
			.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
		builder.environment().putAll(environment);
		// the test's own identity must not leak in from the machine that runs it
		builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
		builder.environment().putIfAbsent("GIT_AUTHOR_NAME", nameOf(HUMAN));
		builder.environment().putIfAbsent("GIT_AUTHOR_EMAIL", emailOf(HUMAN));
		builder.environment().putIfAbsent("GIT_COMMITTER_NAME", nameOf(HUMAN));
		builder.environment().putIfAbsent("GIT_COMMITTER_EMAIL", emailOf(HUMAN));
		Process process = builder.start();
		if (!process.waitFor(30, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException(String.join(" ", command) + " did not finish");
		}
		return process.exitValue();
	}
}
