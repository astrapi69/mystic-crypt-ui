/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.lock.IdleLockWatchdog;

/**
 * An edit typed into an open entry dialog is work the user has done, and the automatic lock and
 * close have to treat it as such (#303).
 * <p>
 * What is typed stays in the dialog until OK, which works on a copy of the entry (#881), and
 * nothing sets the dirty flag before then, while every decision downstream reads that flag: the
 * lock saves nothing, and the close that follows fifteen minutes later sees a clean model, wipes
 * every entry and drops it under the open dialog. The user walked away mid-edit and came back to a
 * vault without the change. So the close waits while the editor is open, and the typed change is
 * still there to be confirmed into the vault it belongs to.
 * <p>
 * Driven over a clock this test moves rather than fifteen real minutes, through the frame's own
 * workspace adapter, so the code under test is the real one.
 */
class OpenEditorSurvivesTheIdleLockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ORIGINAL_TITLE = "TheSecret";

	private static final String TYPED_TITLE = "TheSecretRenamedButNotConfirmed";

	@Test
	@DisplayName("an edit left open in the dialog survives the automatic lock and close")
	void anEditInAnOpenDialogIsNotLostToTheIdleTimeout() throws Exception
	{
		File databaseFile = new File(tempHome, "open-editor.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ORIGINAL_TITLE, "the-user", "the-password");
		application.saveDatabase();

		application.selectEntryRowByTitle(frame, ORIGINAL_TITLE);
		DialogFixture editor = application.openTheEditDialogAndType(frame, TYPED_TITLE);

		System.out.println("PROBE after typing: editor holds typed=" + theEditorHolds(editor)
			+ " model holds original=" + theModelStillHolds(ORIGINAL_TITLE) + " unsaved="
			+ GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
				.asLockableWorkspace().hasUnsavedChanges()));

		theIdleTimeoutRuns();

		boolean vaultOpen = GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().asLockableWorkspace().aVaultIsOpen());
		System.out.println("PROBE after the timeout: editor holds typed=" + theEditorHolds(editor)
			+ " model holds original=" + theModelStillHolds(ORIGINAL_TITLE) + " vaultOpen="
			+ vaultOpen);

		assertTrue(vaultOpen && theModelStillHolds(ORIGINAL_TITLE),
			"the vault the typed change belongs to has to be open afterwards, its entry intact: "
				+ "the close that does not wait for the open editor is the whole of #303 - it "
				+ "wiped every entry and dropped the vault under the dialog");
		assertEquals(TYPED_TITLE, theEditorHolds(editor),
			"the typed change has to be still in the open editor, to be confirmed");
	}

	/**
	 * What the open editor holds as the title
	 *
	 * @param editor
	 *            the open "Edit Crypt Entry" dialog
	 * @return the title typed into it
	 */
	private static String theEditorHolds(final DialogFixture editor)
	{
		return GuiActionRunner.execute(() -> editor.textBox("txtEntryName").target().getText());
	}

	/**
	 * Runs the real lock and the real close over a clock this test moves: the first check starts
	 * the idle clock, the second finds the idle timeout expired, the third finds the locked vault
	 * old enough to close
	 */
	private void theIdleTimeoutRuns()
	{
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(
			MysticCryptApplicationFrame.getInstance().asLockableWorkspace(), () -> 15, () -> 15,
			clock::get);
		GuiActionRunner.execute(() -> {
			watchdog.checkNow();
			clock.set(TimeUnit.MINUTES.toMillis(15));
			watchdog.checkNow();
			clock.set(TimeUnit.MINUTES.toMillis(30));
			watchdog.checkNow();
		});
		robot.waitForIdle();
	}

	/**
	 * Whether the model in memory still holds an entry with the given title, in both of the places
	 * an entry lives - the map of a node's entries and the tree the nodes hang in. Looking in only
	 * one of them reports a loss that did not happen
	 *
	 * @param title
	 *            the entry's title
	 * @return true if it is still in memory
	 */
	private static boolean theModelStillHolds(final String title)
	{
		return GuiActionRunner.execute(() -> {
			var model = MysticCryptApplicationFrame.getInstance().getModelObject();
			if (model == null)
			{
				return false;
			}
			if (model.getDataOfNodes() != null && model.getDataOfNodes().values().stream()
				.filter(entries -> entries != null).flatMap(List::stream).anyMatch(
					entry -> entry != null && Arrays.equals(title.toCharArray(), entry.getTitle())))
			{
				return true;
			}
			if (model.getRootTreeAsMap() == null)
			{
				return false;
			}
			for (var node : model.getRootTreeAsMap().values())
			{
				if (node == null || node.getValue() == null
					|| node.getValue().getDefaultContent() == null)
				{
					continue;
				}
				for (var entry : node.getValue().getDefaultContent())
				{
					if (entry != null && Arrays.equals(title.toCharArray(), entry.getTitle()))
					{
						return true;
					}
				}
			}
			return false;
		});
	}
}
