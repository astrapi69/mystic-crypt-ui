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
package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.alpharogroup.collections.list.ListFactory;
import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.collections.set.SetFactory;
import de.alpharogroup.crypto.obfuscation.CharacterObfuscator;
import de.alpharogroup.crypto.obfuscation.api.Obfuscatable;
import de.alpharogroup.crypto.obfuscation.rule.ObfuscationOperationRule;
import de.alpharogroup.crypto.obfuscation.rule.Operation;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.mystic.crypt.panels.keygen.EnDecryptPanel;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

@Getter
public class OperationRulePanel extends BasePanel<ObfuscationOperationModelBean>
{

	private static final long serialVersionUID = 1L;
	private ObfuscationOperationRulePanel simpleRulePanel;
	private ObfuscationOperationRuleTablePanel simpleRuleTablePanel;
	private EnDecryptPanel enDecryptPanel;

	public OperationRulePanel()
	{
		this(BaseModel.<ObfuscationOperationModelBean> of(ObfuscationOperationModelBean.builder()
			.tableModel(EditableCharacterObfuscationOperationRulesTableModel.builder().build())
			.build()));
	}

	public OperationRulePanel(final Model<ObfuscationOperationModelBean> model)
	{
		super(model);
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
		final Character origChar = simpleRulePanel.getTxtOriginalChar().getText().charAt(0);
		final Character replaceWith = simpleRulePanel.getTxtRelpaceWith().getText().charAt(0);
		Map<Character, ObfuscationOperationRule<Character, Character>> map = getModelObject()
			.getTableModel().toMap();
		if (map.containsKey(origChar))
		{
			String title = "Original character already exists";
			String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
				+ "<p> Please choose a character that is not in use. <br><br> "
				+ "<p>Disentangle process can not be executed if same characters exists";
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.WARNING_MESSAGE);
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
			JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.WARNING_MESSAGE);
			return;
		}
		String indexesAsString = simpleRulePanel.getTxtIndexes().getText();
		Set<Integer> indexes = SetFactory.newTreeSet();
		Object selectedItem = simpleRulePanel.getCmbOperation().getSelectedItem();
		Operation selectedOperation = (Operation)selectedItem;
		String[] strings = indexesAsString.split(",");
		for (int i = 0; i < strings.length; i++)
		{
			String index = strings[i];
			if (!index.isEmpty())
			{
				indexes.add(Integer.valueOf(strings[i]));
			}
		}
		getModelObject().getTableModel()
			.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
				.key(origChar)
				.value(ObfuscationOperationRule.<Character, Character> newRule().character(origChar)
					.replaceWith(replaceWith).indexes(indexes).operation(selectedOperation).build())
				.build());
		simpleRulePanel.getTxtOriginalChar().setText("");
		simpleRulePanel.getTxtRelpaceWith().setText("");
		
		Document document = simpleRulePanel.getTxtIndexes().getDocument();
		try
		{
			document.remove(0, document.getLength());
		}
		catch (BadLocationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		simpleRulePanel.getCmbOperation().setSelectedIndex(0);
	}

	protected void onDecrypt(final ActionEvent actionEvent)
	{
		final String disentangledKey = getModelObject().getObfuscator().disentangle();
		getEnDecryptPanel().getTxtToEncrypt().setText(disentangledKey);
		getEnDecryptPanel().getTxtEncrypted().setText("");
	}

	protected void onEncrypt(final ActionEvent actionEvent)
	{
		// TODO FIXME
		final String toObfuscatedString = getEnDecryptPanel().getTxtToEncrypt().getText();
		final Map<Character, ObfuscationOperationRule<Character, Character>> keymap = getModelObject()
			.getTableModel().toMap();
		// create the rule
		BiMap<Character, ObfuscationOperationRule<Character, Character>> biMap = HashBiMap
			.create(keymap);
		// obfuscate the key
		final Obfuscatable obfuscator = new CharacterObfuscator(biMap, toObfuscatedString);
		getModelObject().setObfuscator(obfuscator);
		final String result = getModelObject().getObfuscator().obfuscate();
		getEnDecryptPanel().getTxtEncrypted().setText(result);
		getEnDecryptPanel().getTxtToEncrypt().setText("");
	}

	@SuppressWarnings("serial")
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
		simpleRuleTablePanel = new ObfuscationOperationRuleTablePanel(getModel()) {
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
	
	protected void onEditObfuscationOperationRule(
		ObfuscationOperationRule<Character, Character> selected)
	{
		simpleRulePanel.onEditObfuscationOperationRule(selected);
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
