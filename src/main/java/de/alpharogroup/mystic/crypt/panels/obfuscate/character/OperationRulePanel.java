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
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

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
			.keyRulesTableModel(CharacterObfuscationOperationRulesTableModel.builder().build())
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
		String indexesAsString = simpleRulePanel.getTxtIndexes().getText();
		Set<Integer> indexes = SetFactory.newTreeSet();
		Object selectedItem = simpleRulePanel.getCmbOperation().getSelectedItem();
		Operation selectedOperation = (Operation)selectedItem;
		String[] strings = indexesAsString.split(",");
		for (int i = 0; i < strings.length; i++)
		{
			indexes.add(Integer.valueOf(strings[i]));
		}
		getModelObject().getKeyRulesTableModel()
			.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
				.key(origChar)
				.value(ObfuscationOperationRule.<Character, Character> newRule().character(origChar)
					.replaceWith(replaceWith).indexes(indexes).operation(selectedOperation).build())
				.build());
		simpleRulePanel.getTxtOriginalChar().setText("");
		simpleRulePanel.getTxtRelpaceWith().setText("");
		simpleRulePanel.getTxtIndexes().setText("");
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
			.getKeyRulesTableModel().toMap();
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
		simpleRuleTablePanel = new ObfuscationOperationRuleTablePanel(getModel());

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