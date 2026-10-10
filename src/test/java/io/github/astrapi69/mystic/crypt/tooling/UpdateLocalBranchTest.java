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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@code scripts/update-local-branch.sh}, the last step of {@code scripts/merge-pr.sh},
 * against real throwaway repositories: a remote, a clone standing on the target branch, a linked
 * worktree on a feature branch, and a second clone that moves the remote the way a merge does.
 * <p>
 * The step fast-forwarded the local target with {@code git fetch origin develop:develop} whenever
 * the target was not checked out in the current worktree. git refuses that form when the branch is
 * checked out in any worktree, so a merge started from a linked worktree ended with exit 128 after
 * the pull request had been merged (#543), and a chain written with {@code &&} stopped after a
 * merge that happened.
 */
class UpdateLocalBranchTest
{

	private static final File SCRIPT = new File("scripts/update-local-branch.sh").getAbsoluteFile();

	@TempDir
	File root;

	private File main;

	private File work;

	private String merged;

	/**
	 * A remote with one commit on develop, a clone on develop, a linked worktree of it on a feature
	 * branch, and a second commit on the remote's develop that the clone has not fetched yet
	 */
	@BeforeEach
	void aMergeTheLocalCopiesHaveNotSeen() throws Exception
	{
		File origin = new File(root, "origin.git");
		main = new File(root, "main");
		work = new File(root, "work");
		File elsewhere = new File(root, "elsewhere");
		git(root, "init", "-q", "--bare", "-b", "develop", origin.getPath());
		git(root, "clone", "-q", origin.getPath(), main.getPath());
		commit(main, "base.txt");
		git(main, "push", "-q", "origin", "develop");
		git(main, "worktree", "add", "-q", "-b", "feature/x", work.getPath(), "develop");
		git(root, "clone", "-q", origin.getPath(), elsewhere.getPath());
		commit(elsewhere, "merged.txt");
		git(elsewhere, "push", "-q", "origin", "develop");
		merged = output(elsewhere, "git", "rev-parse", "HEAD");
	}

	@Test
	@DisplayName("the target checked out in another worktree is brought up to the merge, and the exit is 0")
	void theTargetCheckedOutInAnotherWorktree_isFastForwarded() throws Exception
	{
		Run run = run(work, "develop");

		assertEquals(0, run.exit(), run.output());
		assertEquals(merged, output(main, "git", "rev-parse", "develop"), run.output());
		assertEquals("feature/x", output(work, "git", "rev-parse", "--abbrev-ref", "HEAD"),
			"the worktree the merge was started from stays where it was");
		assertTrue(new File(main, "merged.txt").exists(),
			"the other worktree's files follow its branch: " + run.output());
	}

	@Test
	@DisplayName("a worktree on the target with uncommitted work is left as it is, and the exit is still 0")
	void aTargetCheckedOutWithUncommittedWork_isLeftAsItIs() throws Exception
	{
		String before = output(main, "git", "rev-parse", "develop");
		Files.writeString(new File(main, "base.txt").toPath(), "work in progress",
			StandardCharsets.UTF_8);

		Run run = run(work, "develop");

		assertEquals(0, run.exit(), run.output());
		assertEquals(before, output(main, "git", "rev-parse", "develop"),
			"somebody's work in progress is not where a merge script moves a branch");
		assertEquals("work in progress",
			Files.readString(new File(main, "base.txt").toPath(), StandardCharsets.UTF_8));
		assertTrue(run.output().contains(main.getCanonicalPath()),
			"it says which worktree it left alone: " + run.output());
		assertTrue(run.output().contains("git merge --ff-only origin/develop"),
			"and how to bring it up later: " + run.output());
	}

	@Test
	@DisplayName("the target checked out here is brought up to the merge")
	void theTargetCheckedOutHere_isFastForwarded() throws Exception
	{
		Run run = run(main, "develop");

		assertEquals(0, run.exit(), run.output());
		assertEquals(merged, output(main, "git", "rev-parse", "HEAD"), run.output());
		assertEquals("develop", output(main, "git", "rev-parse", "--abbrev-ref", "HEAD"));
	}

	@Test
	@DisplayName("a target checked out nowhere is brought up to the merge")
	void aTargetCheckedOutNowhere_isFastForwarded() throws Exception
	{
		git(main, "switch", "-q", "-c", "another");

		Run run = run(work, "develop");

		assertEquals(0, run.exit(), run.output());
		assertEquals(merged, output(main, "git", "rev-parse", "develop"), run.output());
		assertEquals("another", output(main, "git", "rev-parse", "--abbrev-ref", "HEAD"));
	}

	@Test
	@DisplayName("a local target with a commit of its own is not updated, and the exit is still 0")
	void aLocalTargetThatHasMovedOnItsOwn_isNotUpdated() throws Exception
	{
		commit(main, "local.txt");
		String local = output(main, "git", "rev-parse", "develop");
		git(main, "switch", "-q", "-c", "another");

		Run run = run(work, "develop");

		assertEquals(0, run.exit(), run.output());
		assertEquals(local, output(main, "git", "rev-parse", "develop"),
			"a fast-forward that is not one is refused, not forced: " + run.output());
		assertTrue(run.output().contains("not updated"), run.output());
	}

	@Test
	@DisplayName("a target checked out elsewhere with a commit of its own is not updated, and the exit is still 0")
	void aCheckedOutTargetThatHasMovedOnItsOwn_isNotUpdated() throws Exception
	{
		commit(main, "local.txt");
		String local = output(main, "git", "rev-parse", "develop");

		Run run = run(work, "develop");

		assertEquals(0, run.exit(), run.output());
		assertEquals(local, output(main, "git", "rev-parse", "develop"), run.output());
		assertTrue(run.output().contains("not updated"), run.output());
	}

	private record Run(int exit, String output) {
	}

	private static Run run(File directory, String branch) throws Exception
	{
		List<String> command = List.of(SCRIPT.getPath(), branch);
		Process process = start(directory, command);
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		finish(process, command);
		return new Run(process.exitValue(), output);
	}

	private static void commit(File repository, String fileName) throws Exception
	{
		Files.writeString(new File(repository, fileName).toPath(), fileName,
			StandardCharsets.UTF_8);
		git(repository, "add", fileName);
		git(repository, "commit", "-q", "-m", "chore: " + fileName);
	}

	private static void git(File directory, String... arguments) throws Exception
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

	private static String output(File directory, String... command) throws Exception
	{
		Process process = start(directory, List.of(command));
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		finish(process, List.of(command));
		return output.trim();
	}

	private static Process start(File directory, List<String> command) throws Exception
	{
		ProcessBuilder builder = new ProcessBuilder(command).directory(directory)
			.redirectErrorStream(true);
		// the machine's own git configuration must not leak in: no identity, no hooks, no
		// defaults that change what a fetch or a merge does
		builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
		builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
		builder.environment().put("GIT_AUTHOR_NAME", "Test");
		builder.environment().put("GIT_AUTHOR_EMAIL", "test@example.org");
		builder.environment().put("GIT_COMMITTER_NAME", "Test");
		builder.environment().put("GIT_COMMITTER_EMAIL", "test@example.org");
		builder.environment().put("LC_ALL", "C");
		return builder.start();
	}

	private static void finish(Process process, List<String> command) throws Exception
	{
		if (!process.waitFor(30, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException(String.join(" ", command) + " did not finish");
		}
	}
}
