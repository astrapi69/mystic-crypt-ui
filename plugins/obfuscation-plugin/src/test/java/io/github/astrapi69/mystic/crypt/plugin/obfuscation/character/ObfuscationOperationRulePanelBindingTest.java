/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.obfuscation.character.ObfuscatorExtensions;

/**
 * Tests that the components of {@link ObfuscationOperationRulePanel} are bound to
 * {@link ObfuscationOperationRulePanelModel}: what the user types and chooses is what the Add
 * button turns into a rule, and the rule that comes out obfuscates a text the way the four
 * components describe it.
 * <p>
 * The Add button of the running application is wired by
 * {@link io.github.astrapi69.mystic.crypt.plugin.obfuscation.character.OperationRulePanel}, which
 * cannot be built here: it creates the rule table panel, and that one asks the application frame
 * singleton for the configuration directory of its file chooser. The button is therefore given the
 * same handler the application gives it - it reads the rule from the model and from nowhere else -
 * and the assertion is on the obfuscated text the collected rules produce.
 */
class ObfuscationOperationRulePanelBindingTest
{

	/** The rules the Add button built from the model */
	private final BiMap<Character, ObfuscationOperationRule<Character, Character>> rules = HashBiMap
		.create();

	/**
	 * The indexes the given comma separated text names
	 *
	 * @param indexesAsText
	 *            the indexes as the user typed them
	 * @return the indexes as numbers
	 */
	private static Set<Integer> toIndexes(final String indexesAsText)
	{
		Set<Integer> indexes = new TreeSet<>();
		for (String index : indexesAsText.split(","))
		{
			if (!index.isEmpty())
			{
				indexes.add(Integer.valueOf(index));
			}
		}
		return indexes;
	}

	/**
	 * A rule editor whose Add button does what the application's does: it builds the rule from the
	 * model the components write into, never from the components themselves
	 *
	 * @return the panel under test
	 */
	private ObfuscationOperationRulePanel newPanelAddingWhatTheModelHolds()
	{
		return new ObfuscationOperationRulePanel(
			BaseModel.of(ObfuscationOperationModelBean.builder().build()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAdd(final ActionEvent actionEvent)
			{
				ObfuscationOperationRulePanelModel typedRule = getRuleModelObject();
				Character original = typedRule.getOriginalCharacter().charAt(0);
				Character replacement = typedRule.getReplaceWith().charAt(0);
				rules.put(original,
					ObfuscationOperationRule.<Character, Character> builder().character(original)
						.replaceWith(replacement).indexes(toIndexes(typedRule.getIndexes()))
						.operation(typedRule.getOperation()).build());
			}
		};
	}

	/**
	 * Typing a rule, naming an index and choosing an operation makes the Add button build exactly
	 * that rule: outside the named index the replacement character is used, at the named index the
	 * chosen operation is applied to the original one
	 */
	@Test
	void addObfuscatesWithWhatWasTypedAndChosen()
	{
		ObfuscationOperationRulePanel panel = newPanelAddingWhatTheModelHolds();

		panel.getTxtOriginalChar().setText("a");
		panel.getTxtRelpaceWith().setText("x");
		panel.getTxtIndexes().setText("1");
		panel.getCmbOperation().setSelectedItem(Operation.UPPERCASE);

		panel.getBtnAdd().doClick();

		assertEquals("xAx", ObfuscatorExtensions.obfuscateWith(rules, "aaa"),
			"the replacement, the index and the operation must all come out of the model");
	}

	/**
	 * The model carries the operation the combo box shows from the start, so that a rule added
	 * without touching the combo box is built with the operation the user sees
	 */
	@Test
	void theModelHoldsTheOperationTheComboBoxShowsFromTheStart()
	{
		ObfuscationOperationRulePanel panel = newPanelAddingWhatTheModelHolds();

		assertNotNull(panel.getCmbOperation().getSelectedItem(),
			"binding the combo box must not clear the selection it starts with");
		assertEquals(panel.getCmbOperation().getSelectedItem(),
			panel.getRuleModelObject().getOperation());
	}

	/**
	 * Editing an existing rule fills every component, and through them the model, so that pressing
	 * Add works with the rule that was picked in the table
	 */
	@Test
	void editingARulePutsEveryPartOfItIntoTheModel()
	{
		ObfuscationOperationRulePanel panel = newPanelAddingWhatTheModelHolds();

		panel.onEditObfuscationOperationRule(ObfuscationOperationRule
			.<Character, Character> builder().character('m').replaceWith('n')
			.indexes(toIndexes("1,2")).operation(Operation.LOWERCASE).build());

		ObfuscationOperationRulePanelModel typedRule = panel.getRuleModelObject();
		assertEquals("m", typedRule.getOriginalCharacter());
		assertEquals("n", typedRule.getReplaceWith());
		assertEquals("1,2", typedRule.getIndexes());
		assertEquals(Operation.LOWERCASE, typedRule.getOperation());
	}

	/**
	 * The indexes field takes numbers and separators only; a letter is refused by its document and
	 * the model keeps what was typed before - the field is still limited although it is now model
	 * backed
	 */
	@Test
	void aLetterIsRefusedByTheIndexesFieldAndTheModelKeepsTheNumbers() throws Exception
	{
		ObfuscationOperationRulePanel panel = newPanelAddingWhatTheModelHolds();

		panel.getTxtIndexes().setText("1,2");
		panel.getTxtIndexes().getDocument().insertString(3, "a", null);

		assertEquals("1,2", panel.getRuleModelObject().getIndexes(),
			"a letter must not reach the model");
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	/**
	 * The original/replacement fields silently cap what is typed to one character, and the
	 * indexes field only accepts comma separated numbers - neither is obvious from the label
	 * alone (#162)
	 */
	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		ObfuscationOperationRulePanel panel = newPanelAddingWhatTheModelHolds();

		assertHasTooltip(panel.getTxtOriginalChar(), "original character");
		assertHasTooltip(panel.getTxtRelpaceWith(), "replace with");
		assertHasTooltip(panel.getTxtIndexes(), "indexes");
		assertHasTooltip(panel.getCmbOperation(), "operation");
		assertHasTooltip(panel.getBtnAdd(), "add button");
	}
}
