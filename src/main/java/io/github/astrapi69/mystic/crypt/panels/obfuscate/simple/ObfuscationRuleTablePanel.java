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
package io.github.astrapi69.mystic.crypt.panels.obfuscate.simple;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;

import io.github.astrapi69.mystic.crypt.SpringBootSwingApplication;
import org.apache.commons.codec.DecoderException;

import de.alpharogroup.collections.map.MapFactory;
import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.xml.crypto.file.XmlDecryptionExtensions;
import de.alpharogroup.xml.crypto.file.XmlEncryptionExtensions;
import io.github.astrapi69.crypto.obfuscation.rule.ObfuscationRule;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import io.github.astrapi69.swing.GenericJTable;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.renderer.TableCellButtonRendererFactory;
import io.github.astrapi69.swing.table.editor.DeleteRowButtonEditor;
import io.github.astrapi69.swing.table.editor.TableCellButtonEditor;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

@Getter
@Log
public class ObfuscationRuleTablePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;
	private Map<String, Class<?>> aliases;
	private javax.swing.JButton btnExport;
	private javax.swing.JButton btnImport;
	private JFileChooser fileChooser;
	private JLabel lblKeyRules;
	private JScrollPane scpKeyRules;
	private GenericJTable<KeyValuePair<Character, ObfuscationRule<Character, Character>>> tblKeyRules;

	{
		aliases = MapFactory.newLinkedHashMap();
		aliases.put("KeyValuePair", KeyValuePair.class);
		aliases.put("ObfuscationRule", ObfuscationRule.class);
	}

	public ObfuscationRuleTablePanel()
	{
		this(BaseModel.of(ObfuscationModelBean.builder().build()));
	}

	public ObfuscationRuleTablePanel(final Model<ObfuscationModelBean> model)
	{
		super(model);
	}

	protected TableCellButtonEditor newUpdateTableCellButtonEditor(final @NonNull String editorText)
	{
		return new TableCellButtonEditor(new JCheckBox())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onGetCellEditorValue()
			{
				@SuppressWarnings("unchecked")
				ObfuscationRule<Character, Character> selected = (ObfuscationRule<Character, Character>)this
					.getValue();
				ObfuscationRuleTablePanel.this.onEditObfuscationRule(selected);
			}

			@Override
			protected String onSetText()
			{
				return editorText;
			}
		};
	}

	protected void onEditObfuscationRule(ObfuscationRule<Character, Character> selected)
	{
	}

	@SneakyThrows
	protected void onExport(final ActionEvent actionEvent)
	{

		final int returnVal = fileChooser.showSaveDialog(ObfuscationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> data = getModelObject()
				.getTableModel().getData();
			final File selectedFile = fileChooser.getSelectedFile();
			XmlEncryptionExtensions.writeToFileAsXmlAndHex(aliases, data, selectedFile);
		}
	}

	protected void onImport(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showOpenDialog(ObfuscationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedFile = fileChooser.getSelectedFile();
			try
			{
				getModelObject().getTableModel().setData(XmlDecryptionExtensions
					.readFromFileAsXmlAndHex(aliases, selectedFile, "de.alpharogroup.**"));
				getModelObject().getTableModel().fireTableDataChanged();
			}
			catch (final IOException | DecoderException e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		String editText = CharacterObfuscationRulesTableModel.EDIT;
		String deleteText = CharacterObfuscationRulesTableModel.DELETE;

		lblKeyRules = new javax.swing.JLabel();
		scpKeyRules = new javax.swing.JScrollPane();
		tblKeyRules = new GenericJTable<>(getModelObject().getTableModel());
		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();

		lblKeyRules.setText("Table of key rules for obfuscate");

		scpKeyRules.setViewportView(tblKeyRules);

		btnImport.setText("Import");

		btnExport.setText("Export");

		final TableColumn editValueColumn = tblKeyRules.getColumn(editText);

		editValueColumn
			.setCellRenderer(TableCellButtonRendererFactory.newTableCellButtonRenderer(editText));

		editValueColumn.setCellEditor(newUpdateTableCellButtonEditor(editText));

		tblKeyRules.getColumn(deleteText).setCellEditor(new DeleteRowButtonEditor());

		tblKeyRules.getColumn(deleteText)
			.setCellRenderer(TableCellButtonRendererFactory.newTableCellButtonRenderer(deleteText));

		btnImport.addActionListener(this::onImport);
		btnExport.addActionListener(this::onExport);

		fileChooser = new JFileChooser(
			SpringBootSwingApplication.getInstance().getConfigurationDirectory());
	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(layout
						.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE)
						.addComponent(btnImport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(
							btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
						layout.createSequentialGroup().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
							.addComponent(scpKeyRules, javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.PREFERRED_SIZE, 1000,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addComponent(lblKeyRules, javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.PREFERRED_SIZE, 280,
								javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGap(0, 0, Short.MAX_VALUE)))
					.addContainerGap()));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(29, 29, 29).addComponent(lblKeyRules)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(scpKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 217,
					javax.swing.GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(btnImport).addComponent(btnExport))
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

}
