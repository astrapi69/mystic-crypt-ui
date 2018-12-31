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
package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.commons.codec.DecoderException;

import com.thoughtworks.xstream.XStream;

import de.alpharogroup.collections.map.MapFactory;
import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.crypto.hex.HexExtensions;
import de.alpharogroup.crypto.obfuscation.rule.ObfuscationOperationRule;
import de.alpharogroup.file.read.ReadFileExtensions;
import de.alpharogroup.file.write.WriteFileExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.mystic.crypt.panels.obfuscate.ModeContext;
import de.alpharogroup.swing.GenericJTable;
import de.alpharogroup.swing.base.BasePanel;
import de.alpharogroup.swing.renderer.TableCellButtonRenderer;
import de.alpharogroup.swing.table.editor.DeleteRowButtonEditor;
import de.alpharogroup.swing.table.editor.TableCellButtonEditor;
import de.alpharogroup.xml.ObjectToXmlExtensions;
import de.alpharogroup.xml.XmlToObjectExtensions;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;


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
	private GenericJTable<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> tblKeyRules;
	private XStream xStream;

	{
		xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.allowTypesByWildcard(new String[] { "de.alpharogroup.**" });
		aliases = MapFactory.newLinkedHashMap();
		aliases.put("KeyValuePair", KeyValuePair.class);
		aliases.put("ObfuscationOperationRule", ObfuscationOperationRule.class);
	}

	public ObfuscationOperationRuleTablePanel()
	{
		this(BaseModel
			.<ObfuscationOperationModelBean> of(ObfuscationOperationModelBean.builder().build()));

	}

	public ObfuscationOperationRuleTablePanel(
		@NonNull final Model<ObfuscationOperationModelBean> model)
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
		List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = getModelObject()
			.getTableModel().getData();

		final int returnVal = fileChooser.showSaveDialog(ObfuscationOperationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File obfuscationRules = fileChooser.getSelectedFile();
			String xmlString = ObjectToXmlExtensions.toXmlWithXStream(xStream, data, aliases);
			final String hexXmlString = HexExtensions.encodeHex(xmlString, Charset.forName("UTF-8"),
				true);
			WriteFileExtensions.writeStringToFile(obfuscationRules, hexXmlString, "UTF-8");
		}
	}

	protected void onImport(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showOpenDialog(ObfuscationOperationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File obfuscationRules = fileChooser.getSelectedFile();
			try
			{
				final String hexXmlString = ReadFileExtensions.readFromFile(obfuscationRules);
				String xmlString = HexExtensions.decodeHex(hexXmlString);

				List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = XmlToObjectExtensions
					.toObjectWithXStream(xStream, xmlString, aliases);

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
		tblKeyRules = new GenericJTable<>(getModelObject().getTableModel());
		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();

		scpKeyRules.setViewportView(tblKeyRules);

		lblKeyRules.setText("Table of key rules for obfuscate");

		btnImport.setText("Import");
		btnExport.setText("Export");


		final TableColumn editValueColumn = tblKeyRules.getColumn(editText);

		editValueColumn.setCellRenderer(new TableCellButtonRenderer(null, null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value,
				final boolean isSelected, final boolean hasFocus, final int row, final int column)
			{
				if (isSelected)
				{
					setForeground(newSelectionForeground(table));
					setBackground(newSelectionBackround(table));
				}
				else
				{
					setForeground(newForeground(table));
					setBackground(newBackround(table));
				}
				final String text = editText;
				setText(text);
				return this;
			}
		});

		editValueColumn.setCellEditor(new TableCellButtonEditor(new JCheckBox())
		{
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("unchecked")
			@Override
			public Object getCellEditorValue()
			{
				ObfuscationOperationRule<Character, Character> selected = (ObfuscationOperationRule<Character, Character>)this
					.getValue();
				getModelObject().setSelected(selected);
				getModelObject().setProccessMode(ModeContext.UPDATE);
				onEditObfuscationOperationRule(selected);
				final String text = editText;
				return text;

			}

			@Override
			public Component getTableCellEditorComponent(final JTable table, final Object value,
				final boolean isSelected, final int row, final int column)
			{
				setRow(row);
				setColumn(column);
				setValue(value);
				if (isSelected)
				{
					getButton().setForeground(table.getSelectionForeground());
					getButton().setBackground(table.getSelectionBackground());
				}
				else
				{
					getButton().setForeground(table.getForeground());
					getButton().setBackground(table.getBackground());
				}
				final String text = editText;
				getButton().setText(text);
				setClicked(true);
				return getButton();
			}
		});

		final TableColumn deleteValueColumn = tblKeyRules.getColumn(deleteText);
		deleteValueColumn.setCellEditor(new DeleteRowButtonEditor(new JCheckBox()));

		deleteValueColumn.setCellRenderer(new TableCellButtonRenderer(null, null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value,
				final boolean isSelected, final boolean hasFocus, final int row, final int column)
			{
				if (isSelected)
				{
					setForeground(newSelectionForeground(table));
					setBackground(newSelectionBackround(table));
				}
				else
				{
					setForeground(newForeground(table));
					setBackground(newBackround(table));
				}
				final String text = deleteText;
				setText(text);
				return this;
			}
		});


		btnImport.addActionListener(actionEvent -> onImport(actionEvent));
		btnExport.addActionListener(actionEvent -> onExport(actionEvent));

		fileChooser = new JFileChooser();
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
