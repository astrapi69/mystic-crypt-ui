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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.mystic.crypt.obfuscation.simple.SimpleObfuscatorExtensions;

/**
 * Tests that the two fields of {@link ObfuscationRulePanel} are bound to
 * {@link ObfuscationRulePanelModel}: what the user types is what the Add button turns into a rule,
 * and the rule that comes out obfuscates a text the way the typed characters describe.
 * <p>
 * The Add button of the running application is wired by
 * {@link io.github.astrapi69.mystic.crypt.plugin.obfuscation.simple.RulePanel}, which cannot be
 * built here: it creates the rule table panel, and that one asks the application frame singleton
 * for the configuration directory of its file chooser. The button is therefore given the same
 * handler the application gives it - it reads the rule from the model and from nowhere else - and
 * the assertion is on the obfuscated text the collected rules produce.
 */
class ObfuscationRulePanelBindingTest
{

	/** The rules the Add button built from the model, in the order they were added */
	private final BiMap<Character, ObfuscationRule<Character, Character>> addedRules = HashBiMap
		.create();

	/**
	 * The component of the tree below the given container that carries the given name
	 *
	 * @param <T>
	 *            the type of the component
	 * @param container
	 *            the root of the component tree to walk
	 * @param name
	 *            the name the component carries
	 * @param type
	 *            the type of the component
	 * @return the component, null when the tree holds none of that name and type
	 */
	private static <T extends Component> T named(Container container, String name, Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = named(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * Types the given text into the named field of the panel, the way a user does
	 *
	 * @param panel
	 *            the panel under test
	 * @param name
	 *            the name of the field
	 * @param text
	 *            the text to type
	 */
	private static void type(Container panel, String name, String text)
	{
		JTextComponent field = named(panel, name, JTextComponent.class);
		assertNotNull(field, "the panel holds no field named " + name);
		field.setText(text);
	}

	/**
	 * A rule editor whose Add button does what the application's does: it builds the rule from the
	 * model the fields write into, never from the fields themselves
	 *
	 * @return the panel under test
	 */
	private ObfuscationRulePanel newPanelAddingWhatTheModelHolds()
	{
		return new ObfuscationRulePanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAdd(final ActionEvent actionEvent)
			{
				ObfuscationRulePanelModel typedRule = getRuleModelObject();
				Character original = typedRule.getOriginalCharacter().charAt(0);
				Character replacement = typedRule.getReplaceWith().charAt(0);
				addedRules.put(original, ObfuscationRule.<Character, Character> builder()
					.character(original).replaceWith(replacement).build());
			}
		};
	}

	/**
	 * Typing two rules and pressing Add for each one obfuscates a text exactly as the typed
	 * characters describe, and disentangles it back - the proof that both characters travelled
	 * through the model into the rule
	 */
	@Test
	void addObfuscatesWithTheCharactersTypedIntoTheBoundFields()
	{
		ObfuscationRulePanel panel = newPanelAddingWhatTheModelHolds();
		JButton add = named(panel, "btnAddRule", JButton.class);
		assertNotNull(add, "the panel holds no button named btnAddRule");

		type(panel, "txtOriginalChar", "a");
		type(panel, "txtRelpaceWith", "x");
		add.doClick();

		type(panel, "txtOriginalChar", "b");
		type(panel, "txtRelpaceWith", "y");
		add.doClick();

		String obfuscated = SimpleObfuscatorExtensions.obfuscateWith(addedRules, "cabbage");
		assertEquals("cxyyxge", obfuscated,
			"a and b must be replaced by what was typed as their replacement");
		assertEquals("cabbage", SimpleObfuscatorExtensions
			.disentangleBiMap(SimpleObfuscatorExtensions.toCharacterBiMap(addedRules), obfuscated),
			"the rules built from the model must invert again");
	}

	/**
	 * What was typed is readable from the model at any moment, without pressing anything
	 */
	@Test
	void theModelHoldsWhatWasTypedWithoutPressingAnything()
	{
		ObfuscationRulePanel panel = newPanelAddingWhatTheModelHolds();

		type(panel, "txtOriginalChar", "q");
		type(panel, "txtRelpaceWith", "7");

		assertEquals("q", panel.getRuleModelObject().getOriginalCharacter());
		assertEquals("7", panel.getRuleModelObject().getReplaceWith());
	}

	/**
	 * Editing an existing rule fills the fields, and through them the model, so that pressing Add
	 * works with the rule that was picked in the table
	 */
	@Test
	void editingARulePutsItIntoTheModelTheAddButtonReads()
	{
		ObfuscationRulePanel panel = newPanelAddingWhatTheModelHolds();

		panel.onEditObfuscationRule(ObfuscationRule.<Character, Character> builder().character('m')
			.replaceWith('n').build());

		assertEquals("m", panel.getRuleModelObject().getOriginalCharacter());
		assertEquals("n", panel.getRuleModelObject().getReplaceWith());

		named(panel, "btnAddRule", JButton.class).doClick();
		assertEquals("nn", SimpleObfuscatorExtensions.obfuscateWith(addedRules, "mm"),
			"the rule picked for editing must be the one the Add button built");
	}

	/**
	 * A field of this editor takes one character; a second one is refused by its document and the
	 * model keeps the first - the field is still limited although it is now model backed
	 */
	@Test
	void aSecondCharacterIsRefusedAndTheModelKeepsTheFirst() throws Exception
	{
		ObfuscationRulePanel panel = newPanelAddingWhatTheModelHolds();

		type(panel, "txtOriginalChar", "a");
		named(panel, "txtOriginalChar", JTextComponent.class).getDocument().insertString(1, "b",
			null);

		assertEquals("a", panel.getRuleModelObject().getOriginalCharacter(),
			"the second character must not reach the model");
	}
}
