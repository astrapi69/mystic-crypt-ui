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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.swing.table.model.GenericTableModel;

/**
 * What the rule table of "Simple Obfuscation" does to its rules, through the panel with a database
 * that has a key pair: Export writes the rules, Import reads them back into a fresh table, and the
 * Delete button of a row takes out that row and no other, which the next export shows. The table is
 * built on swing-table-components, whose 4.0 release is #1047; the export, the import and the
 * delete button were reached by no test (the e2e of the tool only opened it, the key tests only
 * checked that a file appears).
 */
class ObfuscationRuleTableKeepsItsRulesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String TOOL = "Simple Obfuscation";

	private static final int DELETE_COLUMN = 3;

	/**
	 * The tool's frame stays on the desktop of the next test, whose lookup would then find two
	 * tables
	 */
	@AfterEach
	void closeTheTool()
	{
		new ApplicationSteps(robot).closeInternalFrame(TOOL);
	}

	@Test
	@DisplayName("the rules of the table, exported and imported into the emptied table, come back as they were")
	void exportedRules_comeBackThroughImport() throws Exception
	{
		FrameFixture frame = signedInWithAKeyAndTheTool();
		addRules(frame, "a=x", "b=y", "c=z");
		File exported = new File(tempHome, "rules-roundtrip.obf");

		export(frame, exported);
		clearTheTable(frame);
		assertEquals(List.of(), rulesOf(frame), "precondition: the emptied table holds no rule");
		importFrom(frame, exported);

		assertEquals(List.of("a=x", "b=y", "c=z"), rulesOf(frame),
			"the import gives back the exported rules, in order");
	}

	@Test
	@DisplayName("Delete on the second row takes out that rule and no other, in the table and in the next export")
	void deleteOfANonFirstRow_takesOutThatRule() throws Exception
	{
		FrameFixture frame = signedInWithAKeyAndTheTool();
		addRules(frame, "a=x", "b=y", "c=z");

		pressDeleteOfRow(frame, 1);

		assertEquals(List.of("a=x", "c=z"), rulesOf(frame),
			"the second rule is gone, the first and the third are left");
		File exported = new File(tempHome, "rules-after-delete.obf");
		export(frame, exported);
		clearTheTable(frame);
		importFrom(frame, exported);
		assertEquals(List.of("a=x", "c=z"), rulesOf(frame),
			"what the export wrote after the delete holds the two rules that were left");
	}

	private FrameFixture signedInWithAKeyAndTheTool() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "obfuscation-rules.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		KeyModel privateKeyInfo = KeyModelExtensions.toKeyModel(keyPair.getPrivate());
		GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject()
			.getMasterPwFileModelBean().setPrivateKeyInfo(privateKeyInfo));
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool(TOOL, TOOL);
		return frame;
	}

	@SuppressWarnings("unchecked")
	private static GenericTableModel<KeyValuePair<Character, ObfuscationRule<Character, Character>>> modelOf(
		final FrameFixture frame)
	{
		return (GenericTableModel<KeyValuePair<Character, ObfuscationRule<Character, Character>>>)frame
			.table("tblKeyRules").target().getModel();
	}

	/** Adds rules written "original=replacement" to the table's model */
	private static void addRules(final FrameFixture frame, final String... rules)
	{
		GuiActionRunner.execute(() -> {
			for (String rule : rules)
			{
				Character original = rule.charAt(0);
				Character replacement = rule.charAt(2);
				modelOf(frame).add(
					new KeyValuePair<>(original, ObfuscationRule.<Character, Character> builder()
						.character(original).replaceWith(replacement).build()));
			}
			return null;
		});
	}

	private static void clearTheTable(final FrameFixture frame)
	{
		GuiActionRunner.execute(() -> {
			modelOf(frame).removeAll();
			return null;
		});
	}

	/** The rules of the table as shown, written "original=replacement" */
	private static List<String> rulesOf(final FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> {
			JTable table = frame.table("tblKeyRules").target();
			List<String> rules = new ArrayList<>();
			for (int row = 0; row < table.getRowCount(); row++)
			{
				rules.add(table.getValueAt(row, 0) + "=" + table.getValueAt(row, 1));
			}
			return rules;
		});
	}

	/** The Delete button of the row, pressed the way the table's cell editor offers it */
	private void pressDeleteOfRow(final FrameFixture frame, final int row)
	{
		GuiActionRunner.execute(() -> {
			JTable table = frame.table("tblKeyRules").target();
			table.editCellAt(row, DELETE_COLUMN);
			((javax.swing.JButton)table.getEditorComponent()).doClick();
			return null;
		});
		robot.waitForIdle();
	}

	private void export(final FrameFixture frame, final File target)
	{
		SwingUtilities.invokeLater(() -> frame.button("btnExport").target().doClick());
		choose(target);
		Pause.pause(new Condition("exported rule file written")
		{
			@Override
			public boolean test()
			{
				return target.exists() && target.length() > 0;
			}
		}, 10000);
		assertTrue(target.length() > 0, "the export wrote a file");
	}

	private void importFrom(final FrameFixture frame, final File source)
	{
		SwingUtilities.invokeLater(() -> frame.button("btnImport").target().doClick());
		choose(source);
		Pause.pause(new Condition("imported rules shown")
		{
			@Override
			public boolean test()
			{
				return !rulesOf(frame).isEmpty();
			}
		}, 10000);
	}

	private void choose(final File file)
	{
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(file);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
	}
}
