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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;

/**
 * What creating a second vault does while the first one is open and NOT locked (#279).
 * <p>
 * This test asserts the CURRENT behaviour, which is the defect: the new vault silently receives the
 * open vault's entries, and a change made to the open vault's own entry never reaches its file. It
 * is written this way on purpose - the behaviour was measured through the running application,
 * including the restart, and pinning it keeps the measurement reproducible while #279 is decided.
 * <p>
 * When #279 is fixed, this class becomes the regression test by inverting the assertions: whichever
 * way it is decided - refuse while a vault is open, or close the open one first - A must hold its
 * own change afterwards and B must not hold A's entries.
 */
class SecondVaultWhileSignedInProbeUiTest extends AbstractUiTest
{

	private static final String PW_A = TestPasswords.throwaway();
	private static final String PW_B = TestPasswords.throwaway() + "-b";

	private static void say(String line)
	{
		System.out.println("PROBE " + line);
	}

	private static java.util.List<String> tableTitles(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> {
			javax.swing.JTable table = frame.table().target();
			io.github.astrapi69.swing.table.model.GenericTableModel<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean> tableModel = (io.github.astrapi69.swing.table.model.GenericTableModel<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>)table
				.getModel();
			java.util.List<String> titles = new java.util.ArrayList<>();
			tableModel.getData().forEach(entry -> titles.add(entry.getTitle()));
			return titles;
		});
	}

	private static String treeDump()
	{
		return GuiActionRunner.execute(() -> {
			StringBuilder dump = new StringBuilder();
			io.github.astrapi69.gen.tree.BaseTreeNode<io.github.astrapi69.swing.renderer.tree.GenericTreeElement<java.util.List<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			root.traverse().forEach(node -> {
				String name = node.getValue() == null ? "?" : node.getValue().getName();
				java.util.List<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean> content = node
					.getValue() == null ? null : node.getValue().getDefaultContent();
				java.util.List<String> titles = new java.util.ArrayList<>();
				if (content != null)
				{
					content.forEach(entry -> titles.add(entry.getTitle()));
				}
				dump.append(name).append(titles).append(" ");
			});
			return dump.toString();
		});
	}

	@Test
	@DisplayName("the second vault receives the first one's entries, and the first loses its change")
	void measureWhereEntriesLandAndWhatEachFileHoldsAfterwards() throws Exception
	{
		File fileA = new File(tempHome, "vault-a.mcrdb");
		File fileB = new File(tempHome, "vault-b.mcrdb");
		createDatabaseFileHeadless(fileA, PW_A);

		ApplicationSteps application = signInWithExistingDatabase(fileA, PW_A);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "EntryOfA", "user-a", "secret-of-a");
		application.saveDatabase();
		say("A created and saved: " + fileA.getName() + " length=" + fileA.length());

		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New Application")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		javax.swing.JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(15, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(fileB);
			fileChooser.approveSelection();
		});
		DialogFixture masterKeyDialog = WindowFinder
			.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
			{
				@Override
				protected boolean isMatching(Dialog dialog)
				{
					return "Create your master key".equals(dialog.getTitle()) && dialog.isShowing();
				}
			}).withTimeout(15, TimeUnit.SECONDS).using(robot);
		new CreateMasterKeySteps(robot, masterKeyDialog).checkMasterPassword()
			.typeMasterPasswordWithRepeat(PW_B).okAndAwaitClose();

		say("after creating B ---------------------------------------");
		say("signedIn:            " + GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()));
		say("file on screen:      " + application.applicationFileOnScreen());
		say("tree top level:      " + application.treeTopLevelNames());
		say("EntryOfA visible:    " + application.entryExistsWithTitle("EntryOfA"));
		say("tree dump:           " + treeDump());
		say("B on disk length:    " + fileB.length());

		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "EntryMeantForB", "user-b", "secret-of-b");
		say("added EntryMeantForB, visible: " + application.entryExistsWithTitle("EntryMeantForB"));

		say("table titles right after adding: " + tableTitles(frame));
		say("tree dump after adding:          " + treeDump());
		application.selectTreeRowByName(frame, "mykeys");
		say("table titles with mykeys selected: " + tableTitles(frame));

		application.selectEntryRowByTitle(frame, "EntryOfA");
		application.editSelectedEntryTitle(frame, "EntryOfA-edited");
		say("renamed EntryOfA to EntryOfA-edited, visible: "
			+ application.entryExistsWithTitle("EntryOfA-edited"));

		application.saveDatabase();
		say("saved. A length=" + fileA.length() + " B length=" + fileB.length());
		say("tree dump before shutdown: " + treeDump());
		shutdownApplication();

		ApplicationSteps reopenedA = signInWithExistingDatabase(fileA, PW_A);
		say("reopened A with its own password ------------------------");
		say("A holds EntryOfA:         " + reopenedA.entryExistsWithTitle("EntryOfA"));
		say("A holds EntryOfA-edited:  " + reopenedA.entryExistsWithTitle("EntryOfA-edited"));
		say("A holds EntryMeantForB:   " + reopenedA.entryExistsWithTitle("EntryMeantForB"));
		assertTrue(reopenedA.entryExistsWithTitle("EntryOfA"),
			"A still holds the OLD title: the rename never reached A's file, which was not "
				+ "rewritten at all. This is the second half of #279");
		assertFalse(reopenedA.entryExistsWithTitle("EntryOfA-edited"),
			"and the change the user saw on screen is gone after the restart");
		assertFalse(reopenedA.entryExistsWithTitle("EntryMeantForB"),
			"the entry meant for the new vault did not land in A either");

		shutdownApplication();

		ApplicationSteps reopenedB = signInWithExistingDatabase(fileB, PW_B);
		say("reopened B with its own password ------------------------");
		say("B holds EntryOfA:         " + reopenedB.entryExistsWithTitle("EntryOfA"));
		say("B holds EntryOfA-edited:  " + reopenedB.entryExistsWithTitle("EntryOfA-edited"));
		say("B holds EntryMeantForB:   " + reopenedB.entryExistsWithTitle("EntryMeantForB"));

		assertTrue(reopenedB.entryExistsWithTitle("EntryOfA-edited"),
			"measured, and the heavier half of #279: the vault created as new and given its own "
				+ "master password holds the other vault's entry. Nobody asked for that copy");
		assertTrue(reopenedB.entryExistsWithTitle("EntryMeantForB"),
			"the entry added after the creation lands in B - inside a tree that is A's content");
	}
}
