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
package io.github.astrapi69.mystic.crypt.wizard;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link ExtensionsPanel} provides a user interface for managing extensions in a
 * certificate wizard. Users can add, edit, and delete extensions, as well as mark them as critical.
 * The inputs for extension ID and value are validated to ensure they conform to ASN.1 standards
 * <p>
 * What the entry form holds is kept in an {@link ExtensionsPanelModel}: every input component is
 * bound to it, so the form state can be read at any moment instead of being fished out of the
 * widgets when a button is pressed
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtensionsPanel extends BasePanel<BaseWizardStateMachineModel<CertificateInfoModel>>
{

	/** What the choice is called when the extension is not one of the understood ones */
	public static final String OTHER_EXTENSION = "Other (object id)";


	JLabel lblHeader;
	JMComboBox<String, ComboBoxModel<String>> cmbExtensionKind;
	JLabel lblValueHint;
	JTable tblExtensions;
	DefaultTableModel tableModel;
	JMTextField txtExtensionId;
	JMTextField txtExtensionValue;
	JMCheckBox chkCritical;
	JButton btnAddExtension;
	JButton btnEditExtension;
	JButton btnDeleteExtension;
	JScrollPane scrExtensions;

	/**
	 * What the entry form currently holds. It is created in {@link #onInitializeComponents()} and
	 * not in a field initializer, because the base panel initializes the components from its own
	 * constructor, before any field initializer of this class has run
	 */
	ExtensionsPanelModel extensionFormModel;

	/**
	 * Instantiates a new ExtensionsPanel
	 *
	 * @param model
	 *            the model
	 */
	public ExtensionsPanel(IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		super(model);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		extensionFormModel = new ExtensionsPanelModel();

		lblHeader = new JLabel("Extensions");

		tableModel = new DefaultTableModel(new Object[] { "Extension ID", "Critical", "Value" }, 0);
		tblExtensions = new JTable(tableModel);
		// JTable defaults preferredViewportSize to a fixed 450x400 regardless of row count - an
		// empty table would otherwise reserve that much room before a single extension is added
		tblExtensions.setPreferredScrollableViewportSize(
			new java.awt.Dimension(450, tblExtensions.getRowHeight() * 5));
		scrExtensions = new JScrollPane(tblExtensions);

		txtExtensionId = new JMTextField(20);
		txtExtensionValue = new JMTextField(20);
		chkCritical = new JMCheckBox("Critical");

		// the three extensions that actually come up can be picked by name; nobody should have to
		// know that "basic constraints" is 2.5.29.19
		List<String> kinds = new ArrayList<>(CertificateExtensionValues.understoodExtensionIds()
			.stream().map(CertificateExtensionValues::displayName).toList());
		kinds.add(OTHER_EXTENSION);
		extensionFormModel.setExtensionKind(kinds.get(0));
		cmbExtensionKind = new JMComboBox<>(kinds.toArray(new String[0]), LambdaModel
			.of(extensionFormModel::getExtensionKind, extensionFormModel::setExtensionKind));
		cmbExtensionKind.setName("cmbExtensionKind");
		cmbExtensionKind.addActionListener(e -> onChangeExtensionKind());
		lblValueHint = new JLabel(" ");
		lblValueHint.setName("lblValueHint");
		txtExtensionId.setName("txtExtensionId");
		txtExtensionValue.setName("txtExtensionValue");
		chkCritical.setName("chkCritical");
		tblExtensions.setName("tblExtensions");
		txtExtensionId.setPropertyModel(
			LambdaModel.of(extensionFormModel::getExtensionId, extensionFormModel::setExtensionId));
		txtExtensionValue.setPropertyModel(LambdaModel.of(extensionFormModel::getExtensionValue,
			extensionFormModel::setExtensionValue));
		chkCritical.setPropertyModel(
			LambdaModel.of(extensionFormModel::isCritical, extensionFormModel::setCritical));
		onChangeExtensionKind();

		btnAddExtension = new JButton("Add");
		btnEditExtension = new JButton("Edit");
		btnDeleteExtension = new JButton("Delete");

		btnAddExtension.setName("btnAddExtension");
		btnEditExtension.setName("btnEditExtension");
		btnDeleteExtension.setName("btnDeleteExtension");

		btnAddExtension.addActionListener(e -> onAddExtension());
		btnEditExtension.addActionListener(e -> onEditExtension());
		btnDeleteExtension.addActionListener(e -> onDeleteExtension());

		cmbExtensionKind
			.setToolTipText(Messages.getString("wizard.certificate.extensions.tooltip.kind",
				"which X.509 extension to add - picking one fills in its object id below"));
		txtExtensionId.setToolTipText(Messages.getString("wizard.certificate.extensions.tooltip.id",
			"the extension's object identifier (OID), filled in automatically for a known kind"));
		txtExtensionValue
			.setToolTipText(Messages.getString("wizard.certificate.extensions.tooltip.value",
				"the extension's value, in the format the hint below describes"));
		chkCritical.setToolTipText(Messages.getString(
			"wizard.certificate.extensions.tooltip.critical",
			"whether a system that does not understand this extension must reject the certificate"));
		btnAddExtension
			.setToolTipText(Messages.getString("wizard.certificate.extensions.tooltip.add",
				"adds the extension above to the certificate"));
		btnEditExtension
			.setToolTipText(Messages.getString("wizard.certificate.extensions.tooltip.edit",
				"applies the fields above to the selected row"));
		btnDeleteExtension.setToolTipText(Messages.getString(
			"wizard.certificate.extensions.tooltip.delete", "removes the selected extension"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		// one row template per row this panel actually has (header, kind, id, critical, value,
		// hint, buttons, table) - a shorter list has MigLayout repeat the last template for every
		// row beyond it, which put "[grow]" on the Extension ID row instead of the table (#106)
		setLayout(new MigLayout("wrap 2", "[grow,fill][grow,fill]", "[][][][][][][][grow]"));

		add(lblHeader, "span, align center, wrap 10");

		add(new JLabel("Extension:"));
		add(cmbExtensionKind, "wrap");

		add(new JLabel("Extension ID:"));
		add(txtExtensionId, "wrap");

		add(new JLabel("Critical:"));
		add(chkCritical, "wrap");

		add(new JLabel("Value:"));
		add(txtExtensionValue, "wrap");

		add(new JLabel(""));
		add(lblValueHint, "wrap");

		add(btnAddExtension, "split 3, align center");
		add(btnEditExtension);
		add(btnDeleteExtension, "wrap");

		add(scrExtensions, "span, grow");
	}

	/**
	 * Adds a new extension to the table if the inputs are valid
	 */
	protected void onAddExtension()
	{
		if (validateInputs())
		{
			tableModel.addRow(new Object[] { extensionFormModel.getExtensionId(),
					extensionFormModel.isCritical(), extensionFormModel.getExtensionValue() });
			clearForm();
		}
	}

	/**
	 * Edits the selected extension in the table if the inputs are valid
	 */
	protected void onEditExtension()
	{
		int selectedRow = tblExtensions.getSelectedRow();
		if (selectedRow != -1 && validateInputs())
		{
			tableModel.setValueAt(extensionFormModel.getExtensionId(), selectedRow, 0);
			tableModel.setValueAt(extensionFormModel.isCritical(), selectedRow, 1);
			tableModel.setValueAt(extensionFormModel.getExtensionValue(), selectedRow, 2);
			clearForm();
		}
	}

	/**
	 * Deletes the selected extension from the table
	 */
	protected void onDeleteExtension()
	{
		int selectedRow = tblExtensions.getSelectedRow();
		if (selectedRow != -1)
		{
			tableModel.removeRow(selectedRow);
			clearForm();
		}
	}

	/**
	 * Validates the inputs for Extension ID and Value
	 *
	 * @return true if the inputs are valid, false otherwise
	 */
	protected boolean validateInputs()
	{
		String extensionId = extensionFormModel.getExtensionId();
		String value = extensionFormModel.getExtensionValue();
		try
		{
			// building it is the only honest check: an extension whose value is not proper DER
			// produces a certificate that other tools reject, and the field it was meant to carry
			// is simply not there
			CertificateExtensionValues.toExtension(extensionId, extensionFormModel.isCritical(),
				value);
			return true;
		}
		catch (IllegalArgumentException exception)
		{
			showErrorDialog("This extension cannot be built",
				"<html><body width='420'>" + exception.getMessage() + "</body></html>");
			return false;
		}
	}

	/**
	 * Puts the object id of the chosen extension into the form and shows what its value looks like
	 */
	protected void onChangeExtensionKind()
	{
		String selected = extensionFormModel.getExtensionKind();
		if (OTHER_EXTENSION.equals(selected))
		{
			lblValueHint.setText(CertificateExtensionValues.valueHint("other"));
			return;
		}
		CertificateExtensionValues.understoodExtensionIds().stream()
			.filter(id -> CertificateExtensionValues.displayName(id).equals(selected)).findFirst()
			.ifPresent(id -> {
				txtExtensionId.setText(id);
				lblValueHint.setText(CertificateExtensionValues.valueHint(id));
			});
	}

	/**
	 * Shows an error dialog with the specified title and message
	 *
	 * @param title
	 *            the title of the error dialog
	 * @param message
	 *            the message of the error dialog
	 */
	protected void showErrorDialog(String title, String message)
	{
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Clears the form fields
	 */
	protected void clearForm()
	{
		txtExtensionId.setText("");
		chkCritical.setSelected(false);
		txtExtensionValue.setText("");
	}
}
