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
package de.alpharogroup.mystic.crypt.panels.obfuscate.simple;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.JOptionPane;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.alpharogroup.collections.list.ListFactory;
import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.crypto.obfuscation.api.Obfuscatable;
import de.alpharogroup.crypto.obfuscation.rule.ObfuscationRule;
import de.alpharogroup.crypto.obfuscation.simple.SimpleCharacterObfuscator;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.mystic.crypt.panels.keygen.EnDecryptPanel;
import de.alpharogroup.mystic.crypt.panels.obfuscate.ModeContext;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

@Getter
public class RulePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;
	private EnDecryptPanel enDecryptPanel;
	private ObfuscationRulePanel simpleRulePanel;
	private ObfuscationRuleTablePanel simpleRuleTablePanel;

	public RulePanel()
	{
		this(BaseModel.<ObfuscationModelBean> of(ObfuscationModelBean.builder()
			.tableModel(CharacterObfuscationRulesTableModel.builder().build()).build()));
	}

	public RulePanel(final Model<ObfuscationModelBean> model)
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
		Map<Character, ObfuscationRule<Character, Character>> map = getModelObject().getTableModel()
			.toMap();
		KeyValuePair<Character, ObfuscationRule<Character, Character>> keyValuePair = null;
		if (getModelObject().getSelected() != null
			&& getModelObject().getSelected().getCharacter().equals(origChar)
			&& ModeContext.UPDATE.equals(getModelObject().getProccessMode()))
		{
			// get entry from table model
			Optional<KeyValuePair<Character, ObfuscationRule<Character, Character>>> optional = getModelObject()
				.getTableModel().indexOf(origChar);
			if (optional.isPresent())
			{
				keyValuePair = optional.get();
				final List<Character> replaceWithChars = ListFactory.newArrayList();
				for (Entry<Character, ObfuscationRule<Character, Character>> entry : map.entrySet())
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

				keyValuePair.getValue().setReplaceWith(replaceWith);
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
			for (Entry<Character, ObfuscationRule<Character, Character>> entry : map.entrySet())
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

			keyValuePair = KeyValuePair.<Character, ObfuscationRule<Character, Character>> builder()
				.key(origChar).value(ObfuscationRule.<Character, Character> builder()
					.character(origChar).replaceWith(replaceWith).build())
				.build();
			getModelObject().getTableModel().add(keyValuePair);
		}
		getModelObject().setProccessMode(ModeContext.CREATE);
		simpleRulePanel.getTxtOriginalChar().setText("");
		simpleRulePanel.getTxtRelpaceWith().setText("");

		simpleRulePanel.revalidate();

		simpleRuleTablePanel.getTblKeyRules().setModel(getModelObject().getTableModel());
		simpleRuleTablePanel.revalidate();

	}

	protected void onDecrypt(final ActionEvent actionEvent)
	{
		final String disentangledKey = getModelObject().getObfuscator().disentangle();
		getEnDecryptPanel().getTxtToEncrypt().setText(disentangledKey);
		getEnDecryptPanel().getTxtEncrypted().setText("");
	}

	protected void onEditObfuscationRule(ObfuscationRule<Character, Character> selected)
	{
		simpleRulePanel.onEditObfuscationRule(selected);
	}

	protected void onEncrypt(final ActionEvent actionEvent)
	{
		final String toObfuscatedString = getEnDecryptPanel().getTxtToEncrypt().getText();
		final Map<Character, ObfuscationRule<Character, Character>> keymap = getModelObject()
			.getTableModel().toMap();
		// create the rule
		BiMap<Character, ObfuscationRule<Character, Character>> biMap = HashBiMap.create(keymap);
		// TODO FIXME
		// obfuscate the key
		 final Obfuscatable obfuscator = new SimpleCharacterObfuscator(biMap, toObfuscatedString);
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
		simpleRulePanel = new ObfuscationRulePanel(getModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAdd(final ActionEvent actionEvent)
			{
				RulePanel.this.onAdd(actionEvent);
			}

		};
		simpleRuleTablePanel = new ObfuscationRuleTablePanel(getModel())
		{
			@Override
			protected void onEditObfuscationRule(ObfuscationRule<Character, Character> selected)
			{
				RulePanel.this.onEditObfuscationRule(selected);
			}
		};

		enDecryptPanel = new EnDecryptPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onDecrypt(final ActionEvent actionEvent)
			{
				RulePanel.this.onDecrypt(actionEvent);
			}

			@Override
			protected void onEncrypt(final ActionEvent actionEvent)
			{
				RulePanel.this.onEncrypt(actionEvent);
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
