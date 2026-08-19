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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.base.BasePanel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link ExtensionsPanel} provides a user interface for managing extensions in a
 * certificate wizard. Users can add, edit, and delete extensions, as well as mark them as critical.
 * The inputs for extension ID and value are validated to ensure they conform to ASN.1 standards
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtensionsPanel extends BasePanel<BaseWizardStateMachineModel<CertificateInfoModel>>
{

	JLabel lblHeader;
	JTable tblExtensions;
	DefaultTableModel tableModel;
	JTextField txtExtensionId;
	JTextField txtExtensionValue;
	JCheckBox chkCritical;
	JButton btnAddExtension;
	JButton btnEditExtension;
	JButton btnDeleteExtension;
	JScrollPane scrExtensions;

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

		lblHeader = new JLabel("Extensions");

		tableModel = new DefaultTableModel(new Object[] { "Extension ID", "Critical", "Value" }, 0);
		tblExtensions = new JTable(tableModel);
		scrExtensions = new JScrollPane(tblExtensions);

		scrExtensions.setViewportView(tblExtensions);

		txtExtensionId = new JTextField(20);
		txtExtensionValue = new JTextField(20);
		chkCritical = new JCheckBox("Critical");

		btnAddExtension = new JButton("Add");
		btnEditExtension = new JButton("Edit");
		btnDeleteExtension = new JButton("Delete");

		btnAddExtension.addActionListener(e -> onAddExtension());
		btnEditExtension.addActionListener(e -> onEditExtension());
		btnDeleteExtension.addActionListener(e -> onDeleteExtension());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		setLayout(new MigLayout("wrap 2", "[grow,fill][grow,fill]", "[][][grow][][]"));

		add(lblHeader, "span, align center, wrap 10");

		add(new JLabel("Extension ID:"));
		add(txtExtensionId, "wrap");

		add(new JLabel("Critical:"));
		add(chkCritical, "wrap");

		add(new JLabel("Value:"));
		add(txtExtensionValue, "wrap");

		add(btnAddExtension, "split 3, align center");
		add(btnEditExtension);
		add(btnDeleteExtension, "wrap");

		add(new JScrollPane(tblExtensions), "span, grow");
	}

	/**
	 * Adds a new extension to the table if the inputs are valid
	 */
	protected void onAddExtension()
	{
		if (validateInputs())
		{
			String extensionId = txtExtensionId.getText();
			boolean critical = chkCritical.isSelected();
			String value = txtExtensionValue.getText();

			tableModel.addRow(new Object[] { extensionId, critical, value });
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
			tableModel.setValueAt(txtExtensionId.getText(), selectedRow, 0);
			tableModel.setValueAt(chkCritical.isSelected(), selectedRow, 1);
			tableModel.setValueAt(txtExtensionValue.getText(), selectedRow, 2);
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
		String extensionId = txtExtensionId.getText();
		String value = txtExtensionValue.getText();

		try
		{
			new ASN1ObjectIdentifier(extensionId);
		}
		catch (IllegalArgumentException e)
		{
			showErrorDialog("Invalid Extension ID",
				"<html>The provided Extension ID is invalid. Please ensure it is a valid ASN.1 OID.<br>"
					+ "Refer to the following resource for more details: <a href='https://en.wikipedia.org/wiki/Object_identifier'>Object Identifier</a>"
					+ " <br>https://en.wikipedia.org/wiki/Object_identifier</html>");
			return false;
		}

		try
		{
			new DEROctetString(value.getBytes());
		}
		catch (IllegalArgumentException e)
		{
			showErrorDialog("Invalid Value",
				"<html>The provided Value is invalid. Please ensure it is a valid ASN.1 Octet String.<br>"
					+ "Refer to the following resource for more details: <a href='https://en.wikipedia.org/wiki/ASN.1'>ASN.1</a></html>"
					+ " <br>https://en.wikipedia.org/wiki/ASN.1</html>");
			return false;
		}

		return true;
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
