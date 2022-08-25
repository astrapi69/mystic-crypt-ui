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
package io.github.astrapi69.mystic.crypt.panel.obfuscate.character;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.GroupLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import org.apache.commons.codec.DecoderException;

import io.github.astrapi69.collection.map.MapFactory;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.table.editor.DeleteRowButtonEditor;
import io.github.astrapi69.swing.table.editor.TableCellButtonEditor;
import io.github.astrapi69.swing.table.renderer.TableCellButtonRendererFactory;
import io.github.astrapi69.xml.crypto.file.XmlDecryptionExtensions;
import io.github.astrapi69.xml.crypto.file.XmlEncryptionExtensions;


@Getter
@Log
public class ObfuscationOperationRuleTablePanel extends BasePanel<ObfuscationOperationModelBean>
{

	private static final long serialVersionUID = 1L;
	private Map<String, Class<?>> aliases;
	private javax.swing.JButton btnExport;
	private javax.swing.JButton btnImport;
	private JFileChooser fileChooser;
	private JLabel lblKeyRules;
	private JScrollPane scpKeyRules;
	private GenericJXTable<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> tblKeyRules;

	{
		aliases = MapFactory.newLinkedHashMap();
		aliases.put("KeyValuePair", KeyValuePair.class);
		aliases.put("ObfuscationOperationRule", ObfuscationOperationRule.class);
	}

	public ObfuscationOperationRuleTablePanel()
	{
		this(BaseModel.of(ObfuscationOperationModelBean.builder().build()));

	}

	public ObfuscationOperationRuleTablePanel(
		@NonNull final IModel<ObfuscationOperationModelBean> model)
	{
		super(model);
	}

	protected void onDeleteObfuscationOperationRule(
		ObfuscationOperationRule<Character, Character> selected)
	{
	}

	protected void onEditObfuscationOperationRule(
		ObfuscationOperationRule<Character, Character> selected)
	{
	}

	@SneakyThrows
	protected void onExport(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(ObfuscationOperationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = getModelObject()
				.getTableModel().getData();
			final File selectedFile = fileChooser.getSelectedFile();
			XmlEncryptionExtensions.writeToFileAsXmlAndHex(aliases, data, selectedFile);
		}
	}

	protected void onImport(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showOpenDialog(ObfuscationOperationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedFile = fileChooser.getSelectedFile();
			try
			{
				List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = XmlDecryptionExtensions
					.readFromFileAsXmlAndHex(aliases, selectedFile, "io.github.astrapi69.**");
				getModelObject().getTableModel().setData(data);
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

		String editText = EditableCharacterObfuscationOperationRulesTableModel.EDIT;
		String deleteText = EditableCharacterObfuscationOperationRulesTableModel.DELETE;

		lblKeyRules = new JLabel();
		scpKeyRules = new JScrollPane();
		tblKeyRules = new GenericJXTable<>(getModelObject().getTableModel());
		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();

		scpKeyRules.setViewportView(tblKeyRules);

		lblKeyRules.setText("Table of key rules for obfuscate");

		btnImport.setText("Import");
		btnExport.setText("Export");

		final TableColumn editValueColumn = tblKeyRules.getColumn(editText);

		editValueColumn
			.setCellRenderer(TableCellButtonRendererFactory.newTableCellButtonRenderer(editText));

		editValueColumn.setCellEditor(new TableCellButtonEditor()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onGetCellEditorValue()
			{
				@SuppressWarnings("unchecked")
				ObfuscationOperationRule<Character, Character> selected = (ObfuscationOperationRule<Character, Character>)this
					.getValue();
				onEditObfuscationOperationRule(selected);
			}

			@Override
			protected String onSetText()
			{
				return editText;
			}
		});

		final TableColumn deleteValueColumn = tblKeyRules.getColumn(deleteText);

		deleteValueColumn.setCellEditor(new DeleteRowButtonEditor());

		deleteValueColumn
			.setCellRenderer(TableCellButtonRendererFactory.newTableCellButtonRenderer(deleteText));

		btnImport.addActionListener(this::onImport);
		btnExport.addActionListener(this::onExport);

		fileChooser = new JFileChooser(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory());
	}

	protected void onInitializeGroupLayout()
	{
		final GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(lblKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 280,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(scpKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 1188,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup()
							.addComponent(btnImport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(3, 3, 3)))
				.addContainerGap(31, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(29, 29, 29).addComponent(lblKeyRules)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(scpKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 217,
					javax.swing.GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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
