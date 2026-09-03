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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;

import io.github.astrapi69.collection.map.MapFactory;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.PrivateKeyExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.file.write.StoreFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyStringDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyStringEncryptor;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.ObfuscationMessages;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.table.GenericJTable;
import io.github.astrapi69.swing.table.editor.DeleteRowButtonEditor;
import io.github.astrapi69.swing.table.editor.TableCellButtonEditor;
import io.github.astrapi69.swing.table.renderer.TableCellButtonRendererFactory;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import io.github.astrapi69.xstream.ObjectToXmlExtensions;
import io.github.astrapi69.xstream.XmlToObjectExtensions;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;


@Getter
@Log
public class ObfuscationOperationRuleTablePanel extends BasePanel<ObfuscationOperationModelBean>
{

	private static final long serialVersionUID = 1L;

	/** How many rows of the rule table are visible before the window is made taller */
	private static final int VISIBLE_RULE_ROWS = 10;

	private Map<String, Class<?>> aliases;
	private javax.swing.JButton btnExport;
	private javax.swing.JButton btnImport;
	private JFileChooser fileChooser;
	private FileNameExtensionFilter fileNameExtensionFilter;
	private JLabel lblKeyRules;
	private JScrollPane scpKeyRules;
	private GenericJTable<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> tblKeyRules;

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

	protected void onExport(final ActionEvent actionEvent)
	{
		fileChooser.setFileFilter(fileNameExtensionFilter);
		final int returnVal = fileChooser.showSaveDialog(ObfuscationOperationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = getModelObject()
				.getTableModel().getData();
			final File selectedFile = fileChooser.getSelectedFile();

			ApplicationModelBean modelObject = MysticCryptApplicationFrame.getInstance()
				.getModelObject();
			KeyModel privateKeyInfo = modelObject.getMasterPwFileModelBean().getPrivateKeyInfo();
			PrivateKey privateKey = KeyModelExtensions.toPrivateKey(privateKeyInfo);
			PublicKey publicKey = RuntimeExceptionDecorator
				.decorate(() -> PrivateKeyExtensions.generatePublicKey(privateKey));
			PublicKeyStringEncryptor encryptor = new PublicKeyStringEncryptor(publicKey);

			String xml = ObjectToXmlExtensions.toXml(data);
			byte[] encrypted = RuntimeExceptionDecorator.decorate(() -> encryptor.encrypt(xml));

			RuntimeExceptionDecorator
				.decorate(() -> StoreFileExtensions.toFile(selectedFile, encrypted));
		}
	}

	protected void onImport(final ActionEvent actionEvent)
	{
		fileChooser.setFileFilter(fileNameExtensionFilter);
		final int returnVal = fileChooser.showOpenDialog(ObfuscationOperationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedFile = fileChooser.getSelectedFile();
			try
			{
				KeyModel privateKeyInfo = MysticCryptApplicationFrame.getInstance().getModelObject()
					.getMasterPwFileModelBean().getPrivateKeyInfo();
				PrivateKey privateKey = KeyModelExtensions.toPrivateKey(privateKeyInfo);

				byte[] encrypted = ReadFileExtensions.readFileToBytearray(selectedFile);

				PrivateKeyStringDecryptor decryptor = new PrivateKeyStringDecryptor(privateKey);

				String xml = RuntimeExceptionDecorator.decorate(() -> decryptor.decrypt(encrypted));

				List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = XmlToObjectExtensions
					.toObject(xml);

				getModelObject().getTableModel().setData(data);
				getModelObject().getTableModel().fireTableDataChanged();
			}
			catch (final IOException e)
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
		tblKeyRules = new GenericJTable<>(getModelObject().getTableModel());
		// the table opens showing ten rows and takes every further row from the height the window
		// is given, instead of the fixed 217 px the hand written GroupLayout pinned it to
		tblKeyRules.setPreferredScrollableViewportSize(new Dimension(
			tblKeyRules.getPreferredSize().width, tblKeyRules.getRowHeight() * VISIBLE_RULE_ROWS));
		scpKeyRules = ToolForm.scrolled(tblKeyRules);
		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();

		lblKeyRules.setText("Table of key rules for obfuscate");

		btnImport.setText("Import");
		btnExport.setText("Export");

		btnImport.setToolTipText(ObfuscationMessages.getString("obfuscation.rule.table.tooltip.import.button",
			"loads rules from a file, decrypted with the signed-in database's key pair - only works while signed in"));
		btnExport.setToolTipText(ObfuscationMessages.getString("obfuscation.rule.table.tooltip.export.button",
			"saves the rules below to a file, encrypted with the signed-in database's key pair - only works while signed in"));

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

		fileNameExtensionFilter = new FileNameExtensionFilter(
			"Mystic crypt obfuscation files (*.obf)", "obf");
	}

	/**
	 * Lays this panel out with the shared tool window form: the caption over the whole width, the
	 * table of rules taking the height the window has left, and the buttons under it
	 */
	protected void onInitializeToolFormLayout()
	{
		setLayout(ToolForm.newLayout());

		add(lblKeyRules, ToolForm.WIDE);
		add(scpKeyRules, ToolForm.GROWING);
		add(ToolForm.buttons(btnImport, btnExport), ToolForm.BUTTON_ROW);
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeToolFormLayout();
	}

}
