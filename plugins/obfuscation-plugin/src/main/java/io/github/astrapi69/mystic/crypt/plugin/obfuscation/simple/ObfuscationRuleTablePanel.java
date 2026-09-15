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
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.file.write.StoreFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyStringDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyStringEncryptor;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.ObfuscationKeyAvailability;
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
public class ObfuscationRuleTablePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;

	/** How many rows of the rule table are visible before the window is made taller */
	private static final int VISIBLE_RULE_ROWS = 10;

	private Map<String, Class<?>> aliases;
	private javax.swing.JButton btnExport;
	private javax.swing.JButton btnImport;
	private FileNameExtensionFilter fileNameExtensionFilter;
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

	public ObfuscationRuleTablePanel(final IModel<ObfuscationModelBean> model)
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

	/**
	 * Encrypts the rule table with the signed-in database's key pair and writes it to a file the
	 * user chooses.
	 * <p>
	 * Refuses first, before the file chooser even opens, when the database has no key - reading a
	 * null {@code privateKeyInfo} used to reach {@code KeyModelExtensions.toPrivateKey} and throw a
	 * {@link NullPointerException} on the ordinary case of a master-password-only database (#357).
	 * The button is disabled for the same reason in {@link #onInitializeComponents()}; this is the
	 * second line, for a keyboard shortcut or a caller added later, the same shape #269/#270 use.
	 *
	 * @param actionEvent
	 *            the click that triggered this
	 */
	protected void onExport(final ActionEvent actionEvent)
	{
		ApplicationModelBean modelObject = MysticCryptApplicationFrame.getInstance()
			.getModelObject();
		if (!ObfuscationKeyAvailability.keyIsAvailable(modelObject))
		{
			JOptionPane.showMessageDialog(this, ObfuscationKeyAvailability.refusalMessage(),
				ObfuscationKeyAvailability.refusalTitle(), JOptionPane.WARNING_MESSAGE);
			return;
		}
		fileChooser.setFileFilter(fileNameExtensionFilter);
		final int returnVal = fileChooser.showSaveDialog(ObfuscationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			KeyModel privateKeyInfo = modelObject.getMasterPwFileModelBean().getPrivateKeyInfo();
			PrivateKey privateKey = KeyModelExtensions.toPrivateKey(privateKeyInfo);
			PublicKey publicKey = RuntimeExceptionDecorator
				.decorate(() -> PrivateKeyExtensions.generatePublicKey(privateKey));
			PublicKeyStringEncryptor encryptor = new PublicKeyStringEncryptor(publicKey);

			List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> data = getModelObject()
				.getTableModel().getData();
			String xml = ObjectToXmlExtensions.toXml(data);
			byte[] encrypted = RuntimeExceptionDecorator.decorate(() -> encryptor.encrypt(xml));

			final File selectedFile = fileChooser.getSelectedFile();
			RuntimeExceptionDecorator
				.decorate(() -> StoreFileExtensions.toFile(selectedFile, encrypted));
		}
	}

	/**
	 * Reads a file the user chooses and decrypts it with the signed-in database's key pair.
	 * <p>
	 * Refuses first, before the file chooser even opens, when the database has no key - the same
	 * guard as {@link #onExport(ActionEvent)}, for the same reason (#357)
	 *
	 * @param actionEvent
	 *            the click that triggered this
	 */
	protected void onImport(final ActionEvent actionEvent)
	{
		ApplicationModelBean modelObject = MysticCryptApplicationFrame.getInstance()
			.getModelObject();
		if (!ObfuscationKeyAvailability.keyIsAvailable(modelObject))
		{
			JOptionPane.showMessageDialog(this, ObfuscationKeyAvailability.refusalMessage(),
				ObfuscationKeyAvailability.refusalTitle(), JOptionPane.WARNING_MESSAGE);
			return;
		}
		fileChooser.setFileFilter(fileNameExtensionFilter);
		final int returnVal = fileChooser.showOpenDialog(ObfuscationRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedFile = fileChooser.getSelectedFile();
			try
			{
				KeyModel privateKeyInfo = modelObject.getMasterPwFileModelBean()
					.getPrivateKeyInfo();
				PrivateKey privateKey = KeyModelExtensions.toPrivateKey(privateKeyInfo);

				byte[] encrypted = ReadFileExtensions.readFileToBytearray(selectedFile);

				PrivateKeyStringDecryptor decryptor = new PrivateKeyStringDecryptor(privateKey);

				String xml = RuntimeExceptionDecorator.decorate(() -> decryptor.decrypt(encrypted));

				List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> data = XmlToObjectExtensions
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

		String editText = CharacterObfuscationRulesTableModel.EDIT;
		String deleteText = CharacterObfuscationRulesTableModel.DELETE;

		lblKeyRules = new javax.swing.JLabel();
		tblKeyRules = new GenericJTable<>(getModelObject().getTableModel());
		// the table opens showing ten rows and takes every further row from the height the window
		// is given, instead of the fixed 217 px the hand written GroupLayout pinned it to
		tblKeyRules.setPreferredScrollableViewportSize(new Dimension(
			tblKeyRules.getPreferredSize().width, tblKeyRules.getRowHeight() * VISIBLE_RULE_ROWS));
		scpKeyRules = ToolForm.scrolled(tblKeyRules);
		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();
		btnImport.setName("btnImport");
		btnExport.setName("btnExport");

		lblKeyRules.setText("Table of key rules for obfuscate");

		btnImport.setText("Import");

		btnExport.setText("Export");

		// disabled while the signed-in database has no key to encrypt or decrypt with, rather than
		// enabled and throwing on the ordinary case of a master-password-only database (#357).
		// Whichever database was signed in when this tool window was opened decides the state for
		// this window's whole lifetime - a fresh tool window is what a
		// new menu click builds, never reused across a sign-in change
		boolean keyAvailable = ObfuscationKeyAvailability
			.keyIsAvailable(MysticCryptApplicationFrame.getInstance().getModelObject());
		btnImport.setEnabled(keyAvailable);
		btnExport.setEnabled(keyAvailable);
		btnImport.setToolTipText(keyAvailable
			? ObfuscationMessages.getString("obfuscation.rule.table.tooltip.import.button",
				"loads rules from a file, decrypted with the signed-in database's key pair - only works while signed in")
			: ObfuscationKeyAvailability.disabledTooltip());
		btnExport.setToolTipText(keyAvailable
			? ObfuscationMessages.getString("obfuscation.rule.table.tooltip.export.button",
				"saves the rules below to a file, encrypted with the signed-in database's key pair - only works while signed in")
			: ObfuscationKeyAvailability.disabledTooltip());

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
