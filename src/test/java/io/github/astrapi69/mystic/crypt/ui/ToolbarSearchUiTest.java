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
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "search from the toolbar", the way KeePass users expect it: type and the tree
 * jumps to the first match, press Enter and it jumps to the next, lock the workspace and the field
 * goes dark and empty.
 */
class ToolbarSearchUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private JTextComponentFixture toolbarSearchField(final FrameFixture frame)
	{
		return frame.textBox("global.toolbar.menu.file.search.database");
	}

	/**
	 * Types into the given field the way the user does, through its own document, and waits until
	 * the application has reacted.
	 * <p>
	 * Not through the robot: driving the pointer onto a component needs a positioned window, which
	 * a build machine without a window manager does not reliably provide - and what is under test
	 * here is the field's listener, not whether a mouse can reach it.
	 *
	 * @param field
	 *            the field to type into
	 * @param text
	 *            what to type
	 */
	private void type(final JTextComponentFixture field, final String text)
	{
		GuiActionRunner.execute(() -> field.target().setText(text));
		robot.waitForIdle();
	}

	/**
	 * Presses Enter in the given field, which fires exactly the action listeners a real Enter fires
	 *
	 * @param field
	 *            the field to press Enter in
	 */
	private void pressEnter(final JTextComponentFixture field)
	{
		GuiActionRunner.execute(() -> ((javax.swing.JTextField)field.target()).postActionEvent());
		robot.waitForIdle();
	}

	@Test
	@DisplayName("typing jumps to the first match, enter to the next, and around again")
	void typingJumpsToTheFirstMatchAndEnterToTheNext() throws IOException
	{
		File databaseFile = new File(tempHome, "toolbar-search.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "FindMeFirst");
		application.addNodeToTreeRoot(frame, "FindMeSecond");

		JTextComponentFixture searchField = toolbarSearchField(frame);
		assertTrue(GuiActionRunner.execute(() -> searchField.target().isEnabled()),
			"the search field has to be usable once a database is open");

		type(searchField, "findme");
		assertEquals("FindMeFirst", application.selectedTreeNodeName(),
			"typing did not jump to the first match the user sees");

		pressEnter(searchField);
		assertEquals("FindMeSecond", application.selectedTreeNodeName(),
			"enter did not jump to the next match");

		pressEnter(searchField);
		assertEquals("FindMeFirst", application.selectedTreeNodeName(),
			"after the last match enter has to come around to the first again");
	}

	@Test
	@DisplayName("locking the workspace switches the field off and empties it")
	void lockingTheWorkspaceSwitchesTheFieldOffAndEmptiesIt() throws IOException
	{
		File databaseFile = new File(tempHome, "toolbar-search-lock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "SomethingToFind");

		JTextComponentFixture searchField = toolbarSearchField(frame);
		type(searchField, "something");

		application.lockWorkspace();

		assertFalse(GuiActionRunner.execute(() -> searchField.target().isEnabled()),
			"a locked workspace must not offer the search");
		assertEquals("", GuiActionRunner.execute(() -> searchField.target().getText()),
			"the term someone was searching for stayed on display in the locked application");
	}

}
