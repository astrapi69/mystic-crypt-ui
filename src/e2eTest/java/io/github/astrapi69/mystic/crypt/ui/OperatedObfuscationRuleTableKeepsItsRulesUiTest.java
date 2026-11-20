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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.swing.table.model.GenericTableModel;

/**
 * What the rule table of "Operated Obfuscation" does to its rules, through the panel with a
 * database that has a key pair: Export writes the rules, Import reads them back, and the Delete
 * button of a row takes out that row and no other, which the next export shows. The twin of
 * {@link ObfuscationRuleTableKeepsItsRulesUiTest} for the second rule table of the plugin, built on
 * swing-table-components as well (#1047)
 */
class OperatedObfuscationRuleTableKeepsItsRulesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String TOOL = "Operated Obfuscation";

	private static final String DELETE_HEADER = "Delete";

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
	@DisplayName("the rules of the table, exported and imported into the emptied table, come back as they were, indexes and operation included")
	void exportedRules_comeBackThroughImport() throws Exception
	{
		FrameFixture frame = signedInWithAKeyAndTheTool();
		addTheThreeRules(frame);
		File exported = new File(tempHome, "operated-rules-roundtrip.obf");

		export(frame, exported);
		clearTheTable(frame);
		assertEquals(List.of(), rulesOf(frame), "precondition: the emptied table holds no rule");
		importFrom(frame, exported);

		assertEquals(theThreeRules(), rulesOf(frame),
			"the import gives back the exported rules, in order, with indexes and operation");
	}

	@Test
	@DisplayName("Delete on the second row takes out that rule and no other, in the table and in the next export")
	void deleteOfANonFirstRow_takesOutThatRule() throws Exception
	{
		FrameFixture frame = signedInWithAKeyAndTheTool();
		addTheThreeRules(frame);

		pressDeleteOfRow(frame, 1);

		List<String> left = List.of(theThreeRules().get(0), theThreeRules().get(2));
		assertEquals(left, rulesOf(frame), "the second rule is gone, the first and third are left");
		File exported = new File(tempHome, "operated-rules-after-delete.obf");
		export(frame, exported);
		clearTheTable(frame);
		importFrom(frame, exported);
		assertEquals(left, rulesOf(frame),
			"what the export wrote after the delete holds the two rules that were left");
	}

	private static List<String> theThreeRules()
	{
		return List.of("a=x|[0, 3]|UPPERCASE", "b=y|[]|NONE", "c=z|[2]|LOWERCASE");
	}

	private FrameFixture signedInWithAKeyAndTheTool() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "operated-obfuscation-rules.mcrdb");
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

	/** The rule table of the tool: the showing table whose last column is the Delete button */
	private JTable table(final FrameFixture frame)
	{
		return robot.finder().find(frame.target(), new GenericTypeMatcher<JTable>(JTable.class)
		{
			@Override
			protected boolean isMatching(final JTable candidate)
			{
				int last = candidate.getColumnCount() - 1;
				return last > 0 && DELETE_HEADER.equals(candidate.getColumnName(last));
			}
		});
	}

	@SuppressWarnings("unchecked")
	private GenericTableModel<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> modelOf(
		final FrameFixture frame)
	{
		return (GenericTableModel<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>>)table(
			frame).getModel();
	}

	private static KeyValuePair<Character, ObfuscationOperationRule<Character, Character>> row(
		final char original, final char replacement, final Set<Integer> indexes,
		final Operation operation)
	{
		return KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
			.key(original).value(new ObfuscationOperationRule<>(original, indexes, false,
				Optional.empty(), operation, replacement))
			.build();
	}

	private void addTheThreeRules(final FrameFixture frame)
	{
		GenericTableModel<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> model = modelOf(
			frame);
		GuiActionRunner.execute(() -> {
			model.add(row('a', 'x', new LinkedHashSet<>(List.of(0, 3)), Operation.UPPERCASE));
			model.add(row('b', 'y', new LinkedHashSet<>(), Operation.NONE));
			model.add(row('c', 'z', new LinkedHashSet<>(List.of(2)), Operation.LOWERCASE));
			return null;
		});
	}

	private void clearTheTable(final FrameFixture frame)
	{
		GenericTableModel<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> model = modelOf(
			frame);
		GuiActionRunner.execute(() -> {
			model.removeAll();
			return null;
		});
	}

	/** The rules of the table as shown: original=replacement|indexes|operation */
	private List<String> rulesOf(final FrameFixture frame)
	{
		JTable table = table(frame);
		return GuiActionRunner.execute(() -> {
			List<String> rules = new ArrayList<>();
			for (int row = 0; row < table.getRowCount(); row++)
			{
				rules.add(table.getValueAt(row, 0) + "=" + table.getValueAt(row, 1) + "|"
					+ table.getValueAt(row, 2) + "|" + table.getValueAt(row, 3));
			}
			return rules;
		});
	}

	private void pressDeleteOfRow(final FrameFixture frame, final int row)
	{
		JTable table = table(frame);
		int deleteColumn = table.getColumnCount() - 1;
		GuiActionRunner.execute(() -> {
			table.editCellAt(row, deleteColumn);
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
