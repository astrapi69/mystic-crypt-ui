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
package io.github.astrapi69.mystic.crypt.panel.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * What the "Properties" tab shows of a property the entry marks as protected: the words the
 * lethenon plugin keeps there, or a custom field KeePass protected
 */
class PropertiesPanelProtectedValuesTest
{

	private static final String WORDS_NAME = "lethenon.mnemonic";

	private static final String WORDS = "made up words that stand in for the twenty four";

	private static MysticCryptEntryModelBean anEntryWithAProtectedAndAPlainProperty()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.setProperties(new ArrayList<>(List.of(new KeyValuePair<>(WORDS_NAME, WORDS),
			new KeyValuePair<>("note", "plain text"))));
		entry.setProtectedPropertyKeys(Set.of(WORDS_NAME));
		return entry;
	}

	/**
	 * A panel that keeps the dialog it was asked to show, types the given name into it and answers
	 * it with the given option
	 */
	private static PropertiesPanel recording(final MysticCryptEntryModelBean entry,
		final AtomicReference<PropertiesNewEntryPanel> shown, final String typedKey,
		final int answer)
	{
		return new PropertiesPanel(BaseModel.of(entry))
		{
			@Override
			protected int askForProperty(final PropertiesNewEntryPanel panel, final String title)
			{
				shown.set(panel);
				if (typedKey != null)
				{
					panel.getTxtKey().setText(typedKey);
				}
				return answer;
			}
		};
	}

	private static PropertiesNewEntryPanel dialogOfRow(final MysticCryptEntryModelBean entry,
		final int row)
	{
		AtomicReference<PropertiesNewEntryPanel> shown = new AtomicReference<>();
		PropertiesPanel panel = recording(entry, shown, null, JOptionPane.CANCEL_OPTION);
		panel.getTblProperties().setRowSelectionInterval(row, row);
		panel.onEditSelected();
		return shown.get();
	}

	private static boolean showsInClear(final JTextComponent field)
	{
		return !(field instanceof JPasswordField passwordField) || passwordField.getEchoChar() == 0;
	}

	@Test
	@DisplayName("the table shows a protected value as it is (pinned)")
	void theTableShowsAProtectedValueAsItIs()
	{
		PropertiesPanel panel = new PropertiesPanel(
			BaseModel.of(anEntryWithAProtectedAndAPlainProperty()));

		assertEquals(WORDS, panel.getTblProperties().getValueAt(0, 1));
	}

	@Test
	@DisplayName("the table shows a value that is not protected as it is")
	void theTableShowsAPlainValueAsItIs()
	{
		PropertiesPanel panel = new PropertiesPanel(
			BaseModel.of(anEntryWithAProtectedAndAPlainProperty()));

		assertEquals("plain text", panel.getTblProperties().getValueAt(1, 1));
	}

	@Test
	@DisplayName("the dialog of a protected property shows its value in clear (pinned)")
	void theDialogOfAProtectedPropertyShowsItsValueInClear()
	{
		PropertiesNewEntryPanel dialog = dialogOfRow(anEntryWithAProtectedAndAPlainProperty(), 0);

		assertTrue(showsInClear(dialog.getTxtValue()), "the value field masks the words");
	}

	@Test
	@DisplayName("the dialog of a property that is not protected shows its value in clear")
	void theDialogOfAPlainPropertyShowsItsValueInClear()
	{
		PropertiesNewEntryPanel dialog = dialogOfRow(anEntryWithAProtectedAndAPlainProperty(), 1);

		assertTrue(showsInClear(dialog.getTxtValue()), "the value field masks a plain value");
	}

	@Test
	@DisplayName("renaming a protected property leaves the mark on the old name, and the table shows the value (pinned)")
	void renamingAProtectedProperty_leavesTheMarkOnTheOldName()
	{
		MysticCryptEntryModelBean entry = anEntryWithAProtectedAndAPlainProperty();
		PropertiesPanel panel = recording(entry, new AtomicReference<>(), "renamed words",
			JOptionPane.OK_OPTION);
		panel.getTblProperties().setRowSelectionInterval(0, 0);

		panel.onEditSelected();

		assertEquals(Set.of(WORDS_NAME), VaultEntryProperties.protectedNamesOf(entry));
		assertEquals(WORDS, panel.getTblProperties().getValueAt(0, 1));
	}
}
