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
package io.github.astrapi69.mystic.crypt.panels.obfuscate.character;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import javax.swing.*;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import com.google.common.collect.BiMap;

import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.collections.pairs.KeyValuePair;
import io.github.astrapi69.collections.set.SetFactory;
import io.github.astrapi69.crypto.obfuscation.character.ObfuscatorExtensions;
import io.github.astrapi69.crypto.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.crypto.obfuscation.rule.Operation;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panels.keygen.EnDecryptPanel;
import io.github.astrapi69.mystic.crypt.panels.obfuscate.ModeContext;
import io.github.astrapi69.swing.base.BasePanel;

@Getter
public class OperationRulePanel extends BasePanel<ObfuscationOperationModelBean>
{

	private static final long serialVersionUID = 1L;
	private EnDecryptPanel enDecryptPanel;
	private ObfuscationOperationRulePanel simpleRulePanel;
	private ObfuscationOperationRuleTablePanel simpleRuleTablePanel;

	public OperationRulePanel()
	{
		this(BaseModel.of(ObfuscationOperationModelBean.builder()
			.tableModel(EditableCharacterObfuscationOperationRulesTableModel.builder().build())
			.build()));
	}

	public OperationRulePanel(final IModel<ObfuscationOperationModelBean> model)
	{
		super(model);
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
		if (simpleRulePanel.getTxtOriginalChar().getText().isEmpty())
		{

			String title = "Original character is empty";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Please choose a value for the original character";
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (simpleRulePanel.getTxtRelpaceWith().getText().isEmpty())
		{

			String title = "Replace with character is empty";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Please choose a value for the replace with character";
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.WARNING_MESSAGE);
			return;
		}
		final Character origChar = simpleRulePanel.getTxtOriginalChar().getText().charAt(0);
		final Character replaceWith = simpleRulePanel.getTxtRelpaceWith().getText().charAt(0);
		Map<Character, ObfuscationOperationRule<Character, Character>> map = getModelObject()
			.getTableModel().toMap();
		KeyValuePair<Character, ObfuscationOperationRule<Character, Character>> keyValuePair;
		if (getModelObject().getSelected() != null
			&& getModelObject().getSelected().getCharacter().equals(origChar)
			&& ModeContext.UPDATE.equals(getModelObject().getProccessMode()))
		{
			// get entry from table model
			Optional<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> optional = getModelObject()
				.getTableModel().indexOf(origChar);
			if (optional.isPresent())
			{
				keyValuePair = optional.get();
				final List<Character> replaceWithChars = ListFactory.newArrayList();
				for (Entry<Character, ObfuscationOperationRule<Character, Character>> entry : map
					.entrySet())
				{
					replaceWithChars.add(entry.getValue().getReplaceWith());
				}
				if (replaceWithChars.contains(replaceWith)
					&& !keyValuePair.getValue().getReplaceWith().equals(replaceWith))
				{
					String title = "Replace with character already exists";
					String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
						+ "<p> Please choose a character that is not in use. <br><br> "
						+ "<p>Disentangle process can not be executed if same characters exists";
					JOptionPane.showMessageDialog(this, htmlMessage, title,
						JOptionPane.WARNING_MESSAGE);
					return;
				}

				String indexesAsString = simpleRulePanel.getTxtIndexes().getText();
				Set<Integer> indexes = SetFactory.newTreeSet();
				Object selectedItem = simpleRulePanel.getCmbOperation().getSelectedItem();
				Operation selectedOperation = (Operation)selectedItem;
				String[] strings = indexesAsString.split(",");
				IntStream.range(0, strings.length).forEach(i -> {
					String index = strings[i];
					if (!index.isEmpty())
					{
						indexes.add(Integer.valueOf(strings[i]));
					}
				});

				keyValuePair.getValue().setReplaceWith(replaceWith);
				keyValuePair.getValue().setIndexes(indexes);
				keyValuePair.getValue().setOperation(selectedOperation);
				getModelObject().getTableModel().fireTableDataChanged();
			}
		}
		else
		{
			if (map.containsKey(origChar))
			{
				String title = "Original character already exists";
				String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
					+ "<p> Please choose a character that is not in use. <br><br> "
					+ "<p>Disentangle process can not be executed if same characters exists";
				JOptionPane.showMessageDialog(this, htmlMessage, title,
					JOptionPane.WARNING_MESSAGE);
				return;
			}
			final List<Character> replaceWithChars = ListFactory.newArrayList();
			for (Entry<Character, ObfuscationOperationRule<Character, Character>> entry : map
				.entrySet())
			{
				replaceWithChars.add(entry.getValue().getReplaceWith());
			}
			if (replaceWithChars.contains(replaceWith))
			{
				String title = "Replace with character already exists";
				String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
					+ "<p> Please choose a character that is not in use. <br><br> "
					+ "<p>Disentangle process can not be executed if same characters exists";
				JOptionPane.showMessageDialog(this, htmlMessage, title,
					JOptionPane.WARNING_MESSAGE);
				return;
			}
			String indexesAsString = simpleRulePanel.getTxtIndexes().getText();
			Set<Integer> indexes = SetFactory.newTreeSet();
			Object selectedItem = simpleRulePanel.getCmbOperation().getSelectedItem();
			Operation selectedOperation = (Operation)selectedItem;
			String[] strings = indexesAsString.split(",");
			for (String index : strings)
			{
				if (!index.isEmpty())
				{
					indexes.add(Integer.valueOf(index));
				}
			}
			keyValuePair = KeyValuePair
				.<Character, ObfuscationOperationRule<Character, Character>> builder().key(origChar)
				.value(ObfuscationOperationRule.<Character, Character> builder().character(origChar)
					.replaceWith(replaceWith).indexes(indexes).operation(selectedOperation).build())
				.build();
			getModelObject().getTableModel().add(keyValuePair);
		}
		getModelObject().setProccessMode(ModeContext.CREATE);
		simpleRulePanel.getTxtOriginalChar().setText("");
		simpleRulePanel.getTxtRelpaceWith().setText("");

		simpleRulePanel.getTxtIndexes().setText("");

		simpleRulePanel.getCmbOperation().setSelectedIndex(0);
		simpleRulePanel.revalidate();

		simpleRuleTablePanel.getTblKeyRules().setModel(getModelObject().getTableModel());
		simpleRuleTablePanel.revalidate();

	}

	protected void onDecrypt(final ActionEvent actionEvent)
	{
		BiMap<Character, ObfuscationOperationRule<Character, Character>> biMap = getModelObject()
			.getTableModel().toBiMap();
		String text = getEnDecryptPanel().getTxtEncrypted().getText();
		String disentangled = ObfuscatorExtensions.disentangleImproved(biMap, text);
		getEnDecryptPanel().getTxtToEncrypt().setText(disentangled);
		getEnDecryptPanel().getTxtEncrypted().setText("");
	}

	protected void onEditObfuscationOperationRule(
		ObfuscationOperationRule<Character, Character> selected)
	{
		simpleRulePanel.onEditObfuscationOperationRule(selected);
	}

	protected void onEncrypt(final ActionEvent actionEvent)
	{
		final String toObfuscatedString = getEnDecryptPanel().getTxtToEncrypt().getText();
		// create the rule
		BiMap<Character, ObfuscationOperationRule<Character, Character>> biMap = getModelObject()
			.getTableModel().toBiMap();
		final String result = ObfuscatorExtensions.obfuscateWith(biMap, toObfuscatedString);
		getEnDecryptPanel().getTxtEncrypted().setText(result);
		getEnDecryptPanel().getTxtToEncrypt().setText("");
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		simpleRulePanel = new ObfuscationOperationRulePanel(getModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAdd(final ActionEvent actionEvent)
			{
				OperationRulePanel.this.onAdd(actionEvent);
			}

		};
		simpleRuleTablePanel = new ObfuscationOperationRuleTablePanel(getModel())
		{
			@Override
			protected void onEditObfuscationOperationRule(
				ObfuscationOperationRule<Character, Character> selected)
			{
				OperationRulePanel.this.onEditObfuscationOperationRule(selected);
			}
		};

		enDecryptPanel = new EnDecryptPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onDecrypt(final ActionEvent actionEvent)
			{
				OperationRulePanel.this.onDecrypt(actionEvent);
			}

			@Override
			protected void onEncrypt(final ActionEvent actionEvent)
			{
				OperationRulePanel.this.onEncrypt(actionEvent);
			}
		};
		enDecryptPanel.getBtnEncrypt().setText("Obfuscate >");
		enDecryptPanel.getBtnDecrypt().setText("< Disentangle");
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeMigLayout();
	}

	protected void onInitializeMigLayout()
	{
		setLayout(new MigLayout());
		add(simpleRulePanel, "wrap");
		add(simpleRuleTablePanel, "wrap");
		add(enDecryptPanel);
	}

}
