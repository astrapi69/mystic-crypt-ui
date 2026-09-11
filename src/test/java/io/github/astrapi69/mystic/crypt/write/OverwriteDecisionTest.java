package io.github.astrapi69.mystic.crypt.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The display-free half of the overwrite question (#300): whether one has to be asked at all, and
 * in which words.
 */
class OverwriteDecisionTest
{

	@Test
	@DisplayName("a file that is not there needs no question")
	void nothingToReplaceMeansNothingToAsk(@TempDir File directory)
	{
		assertFalse(OverwriteDecision.needsAnAnswer(new File(directory, "not-there.mcrdb")));
		assertFalse(OverwriteDecision.needsAnAnswer(null), "and a missing target is not a target");
	}

	@Test
	@DisplayName("a directory is not something this question can answer")
	void aDirectoryIsNotAFileToReplace(@TempDir File directory)
	{
		assertFalse(OverwriteDecision.needsAnAnswer(directory),
			"asking 'replace this?' about a directory would promise something the writer cannot "
				+ "do, and the write fails for a different reason anyway");
	}

	@Test
	@DisplayName("an existing file is what the question exists for")
	void anExistingFileNeedsAnAnswer(@TempDir File directory) throws Exception
	{
		File target = new File(directory, "already-there.mcrdb");
		Files.writeString(target.toPath(), "somebody's database");

		assertTrue(OverwriteDecision.needsAnAnswer(target));
	}

	@Test
	@DisplayName("the irreplaceable case says what is lost, the derived case does not")
	void theWordsFollowWhatTheFileHolds()
	{
		String vault = OverwriteDecision.messageFor(DataClass.IRREPLACEABLE,
			"somebody-elses.mcrdb");
		String derived = OverwriteDecision.messageFor(DataClass.DERIVED, "download.bin.sha256");

		assertTrue(vault.contains("somebody-elses.mcrdb"), "the file is named, not described");
		assertTrue(vault.contains("nothing can produce it again"),
			"a vault has nothing behind it, and the question has to say so: that is the whole "
				+ "difference between the two halves of the rule");
		assertTrue(derived.contains("download.bin.sha256"), "the derived case names it too");
		assertFalse(derived.contains("nothing can produce it again"),
			"a checksum can be recomputed, and dressing that up as a loss teaches people to click "
				+ "through the warning that matters");
	}

	@Test
	@DisplayName("the title says which decision is being taken")
	void theTitleDistinguishesTheTwoCases()
	{
		assertEquals("Replace this database?", OverwriteDecision.titleFor(DataClass.IRREPLACEABLE));
		assertEquals("Replace the existing file?", OverwriteDecision.titleFor(DataClass.DERIVED));
	}
}
