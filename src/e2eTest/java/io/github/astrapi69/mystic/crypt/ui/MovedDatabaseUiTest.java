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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A database that was moved since it was last saved must be written where it now is.
 * <p>
 * The database carries its own path inside its encrypted xml. Opening a file from a new location
 * and saving it used to write to the old path instead: a moved file was recreated at the location
 * the user had emptied, holding everything they had just added, while the file in front of them
 * stayed as it was. Nothing said so, because the save reported success either way.
 * <p>
 * This runs through the real application, because that is the only place where the whole chain -
 * sign-in, model, save action, file - is present at once.
 */
class MovedDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String NODE_NAME = "Node added after the move";

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "renamed in the same directory", "moved into another directory" })
	void aDatabaseThatWasMovedIsSavedWhereItNowIs(final String movement) throws Exception
	{
		File original = new File(tempHome, "moved-source.mcrdb");
		createDatabaseFileHeadless(original, MASTER_PASSWORD);
		File targetDirectory = movement.startsWith("moved into")
			? Files.createDirectories(tempHome.toPath().resolve("elsewhere")).toFile()
			: tempHome;
		File moved = new File(targetDirectory, "moved-target.mcrdb");
		Files.move(original.toPath(), moved.toPath());
		long lengthBeforeSave = moved.length();

		ApplicationSteps application = signInWithExistingDatabase(moved, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, NODE_NAME);
		application.saveDatabase();

		// the file is the oracle here, not the save indicator: the dirty flag is cleared before
		// anything is written, so it goes green whichever file the bytes land in
		Pause.pause(new Condition("the file that was opened has been written")
		{
			@Override
			public boolean test()
			{
				return moved.length() != lengthBeforeSave;
			}
		}, 20000);

		assertFalse(original.exists(),
			"the save brought back the file at the old path, where the user will not look");
		assertTrue(moved.length() != lengthBeforeSave,
			"the file that was opened was not written to");

		shutdownApplication();
		ApplicationSteps reopened = signInWithExistingDatabase(moved, MASTER_PASSWORD);
		reopened.showMainFrame();

		assertTrue(reopened.treeContainsNodeStartingWith(NODE_NAME),
			"what was added after the move is missing from the file that was open on screen");
		assertEquals(moved.getAbsolutePath(), reopened.applicationFileOnScreen().getAbsolutePath(),
			"the application points at a different file than the one it opened");
	}
}
