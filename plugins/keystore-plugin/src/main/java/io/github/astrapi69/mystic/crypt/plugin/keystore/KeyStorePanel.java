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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Tool panel for working with a Java key store: open or create one, see what it holds, add a
 * generated key pair with a self-signed certificate, import a certificate, export one as PEM, and
 * remove an alias.
 * <p>
 * Every operation writes the store back to disk immediately, so what the table shows is always what
 * the file contains - there is no separate "save" step that could be forgotten.
 */
public class KeyStorePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMNS = { "alias", "kind", "algorithm", "subject",
			"valid until", "SHA-256 fingerprint" };

	/** Everything the user typed or chose; every component below writes into it */
	private final transient KeyStorePanelModel modelObject = new KeyStorePanelModel();

	private final JMTextField txtKeyStoreFile = new JMTextField(40);
	private final JMComboBox<KeystoreType, ?> cmbType = new JMComboBox<>(
		offeredValues(KeystoreType.class, KeyStoreSupport.USABLE_TYPES));
	private final JMPasswordField pwdStore = new JMPasswordField(20);
	private final JMTextField txtAlias = new JMTextField(20);
	private final JMTextField txtDistinguishedName = new JMTextField(30);
	private final JMComboBox<KeyPairGeneratorAlgorithm, ?> cmbKeyAlgorithm = new JMComboBox<>(
		offeredValues(KeyPairGeneratorAlgorithm.class, KeyStoreSupport.KEY_ALGORITHMS));
	private final JMTextField txtCertificateFile = new JMTextField(40);
	private final JMTextField txtPrivateKeyFile = new JMTextField(40);
	private final DefaultTableModel entryModel = new DefaultTableModel(COLUMNS, 0)
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column)
		{
			// the table shows what the store holds; changes go through the buttons, never through
			// an edited cell
			return false;
		}
	};
	private final JTable tblEntries = new JTable(entryModel);
	private final JLabel lblResult = new JLabel(" ");

	private final transient Map<String, String> settings;

	public KeyStorePanel()
	{
		super(new GridBagLayout());

		// what the user configured in the settings dialog decides what this tool starts with; the
		// components take it from the model when they are bound to it
		settings = KeyStoreSettings.values();
		modelObject.setKeystoreType(KeyStoreSettings.type(settings));
		modelObject.setKeyAlgorithm(KeyStoreSettings.algorithm(settings));
		modelObject.setDistinguishedName(KeyStoreSettings.distinguishedName(settings));
		bindComponents();

		txtKeyStoreFile.setName("txtKeyStoreFile");
		cmbType.setName("cmbType");
		pwdStore.setName("pwdStore");
		txtAlias.setName("txtAlias");
		txtDistinguishedName.setName("txtDistinguishedName");
		cmbKeyAlgorithm.setName("cmbKeyAlgorithm");
		txtCertificateFile.setName("txtCertificateFile");
		txtPrivateKeyFile.setName("txtPrivateKeyFile");
		tblEntries.setName("tblEntries");
		tblEntries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		JButton btnBrowse = button("btnBrowse", "...", event -> onBrowse(txtKeyStoreFile));
		JButton btnOpen = button("btnOpen", "Open", event -> onOpen());
		JButton btnCreate = button("btnCreate", "Create", event -> onCreate());
		JButton btnAddKeyPair = button("btnAddKeyPair", "Add key pair", event -> onAddKeyPair());
		JButton btnDelete = button("btnDelete", "Delete alias", event -> onDelete());
		JButton btnBrowseCertificate = button("btnBrowseCertificate", "...",
			event -> onBrowse(txtCertificateFile));
		JButton btnImport = button("btnImport", "Import certificate", event -> onImport());
		JButton btnExport = button("btnExport", "Export certificate as PEM", event -> onExport());
		JButton btnImportKeyPair = button("btnImportKeyPair", "Import key + certificate",
			event -> onImportKeyPair());
		JButton btnAddSecretKey = button("btnAddSecretKey", "Add secret key",
			event -> onAddSecretKey());
		JButton btnDetails = button("btnDetails", "Details...", event -> onShowDetails());

		int row = 0;
		add(new JLabel("Key store file:"), at(0, row, GridBagConstraints.EAST));
		add(txtKeyStoreFile, at(1, row, GridBagConstraints.WEST));
		add(btnBrowse, at(2, row++, GridBagConstraints.WEST));
		add(new JLabel("Type:"), at(0, row, GridBagConstraints.EAST));
		add(cmbType, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Store password:"), at(0, row, GridBagConstraints.EAST));
		add(pwdStore, at(1, row++, GridBagConstraints.WEST));
		add(buttonRow(btnOpen, btnCreate), at(1, row++, GridBagConstraints.WEST));

		JScrollPane entryScrollPane = new JScrollPane(tblEntries);
		entryScrollPane.setPreferredSize(new java.awt.Dimension(760, 200));
		add(entryScrollPane, span(0, row++, 3));

		add(new JLabel("Alias:"), at(0, row, GridBagConstraints.EAST));
		add(txtAlias, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Distinguished name:"), at(0, row, GridBagConstraints.EAST));
		add(txtDistinguishedName, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Key algorithm:"), at(0, row, GridBagConstraints.EAST));
		add(cmbKeyAlgorithm, at(1, row++, GridBagConstraints.WEST));
		add(buttonRow(btnAddKeyPair, btnDelete), at(1, row++, GridBagConstraints.WEST));

		add(new JLabel("Certificate file:"), at(0, row, GridBagConstraints.EAST));
		add(txtCertificateFile, at(1, row, GridBagConstraints.WEST));
		add(btnBrowseCertificate, at(2, row++, GridBagConstraints.WEST));
		add(new JLabel("Private key file:"), at(0, row, GridBagConstraints.EAST));
		add(txtPrivateKeyFile, at(1, row, GridBagConstraints.WEST));
		add(button("btnBrowsePrivateKey", "...", event -> onBrowse(txtPrivateKeyFile)),
			at(2, row++, GridBagConstraints.WEST));
		add(buttonRow(btnImport, btnExport), at(1, row++, GridBagConstraints.WEST));
		add(buttonRow(btnImportKeyPair, btnAddSecretKey, btnDetails),
			at(1, row++, GridBagConstraints.WEST));
		add(lblResult, span(0, row, 3));
	}

	/**
	 * Binds every component to the model, so that each edit lands in the model and the model is
	 * what the buttons read - the components carry the values the model already holds
	 */
	private void bindComponents()
	{
		txtKeyStoreFile.setPropertyModel(
			LambdaModel.of(modelObject::getKeyStoreFilePath, modelObject::setKeyStoreFilePath));
		cmbType.setPropertyModel(
			LambdaModel.of(modelObject::getKeystoreType, modelObject::setKeystoreType));
		pwdStore.setPropertyModel(
			LambdaModel.of(modelObject::getStorePassword, modelObject::setStorePassword));
		txtAlias.setPropertyModel(LambdaModel.of(modelObject::getAlias, modelObject::setAlias));
		txtDistinguishedName.setPropertyModel(
			LambdaModel.of(modelObject::getDistinguishedName, modelObject::setDistinguishedName));
		cmbKeyAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getKeyAlgorithm, modelObject::setKeyAlgorithm));
		txtCertificateFile.setPropertyModel(LambdaModel.of(modelObject::getCertificateFilePath,
			modelObject::setCertificateFilePath));
		txtPrivateKeyFile.setPropertyModel(
			LambdaModel.of(modelObject::getPrivateKeyFilePath, modelObject::setPrivateKeyFilePath));
	}

	/**
	 * A combo box model over an enum, holding only the values this tool offers and holding them in
	 * the order they are offered in - the exclude set of {@link EnumComboBoxModel} is a hash set
	 * and would leave the order to the hash codes
	 *
	 * @param <E>
	 *            the type of the enum
	 * @param enumClass
	 *            the enum class the values come from
	 * @param offered
	 *            the values the tool offers, in the order they are offered in
	 * @return the combo box model over the offered values
	 */
	private static <E extends Enum<E>> EnumComboBoxModel<E> offeredValues(final Class<E> enumClass,
		final List<E> offered)
	{
		EnumComboBoxModel<E> comboBoxModel = new EnumComboBoxModel<>(enumClass, offered.get(0));
		comboBoxModel.setComboList(new ArrayList<>(offered));
		return comboBoxModel;
	}

	private void onBrowse(JTextField target)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!target.getText().isBlank())
		{
			fileChooser.setSelectedFile(new File(target.getText()));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			target.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void onOpen()
	{
		run("opened", () -> {
			// the file says what kind of store it is; the combo only decides when it does not
			KeystoreType detected = KeyStoreSupport.detectType(file(), password());
			KeystoreType type = detected != null ? detected : type();
			modelObject.setKeyStore(KeyStoreSupport.open(file(), type, password()));
			if (detected != null && detected != type())
			{
				cmbType.setSelectedItem(detected);
				return "opened " + file().getName() + " - it is a " + detected + " store";
			}
			return "opened " + file().getName();
		});
	}

	private void onImportKeyPair()
	{
		run("imported", () -> {
			requireOpenKeyStore();
			String alias = requireAlias();
			KeyStoreSupport.importKeyPair(modelObject.getKeyStore(), file(), password(), alias,
				new File(modelObject.getPrivateKeyFilePath().trim()),
				new File(modelObject.getCertificateFilePath().trim()));
			return "imported the key and its certificate as '" + alias + "'";
		});
	}

	private void onAddSecretKey()
	{
		run("added", () -> {
			requireOpenKeyStore();
			String alias = requireAlias();
			KeyStoreSupport.addSecretKey(modelObject.getKeyStore(), file(), password(), alias,
				"AES", KeyStoreSettings.secretKeySize());
			return "added the " + KeyStoreSettings.secretKeySize() + " bit AES key '" + alias + "'";
		});
	}

	private void onShowDetails()
	{
		run("shown", () -> {
			requireOpenKeyStore();
			String alias = selectedAlias();
			KeyStoreSupport.CertificateDetails details = KeyStoreSupport
				.details(modelObject.getKeyStore(), alias);
			JOptionPane.showMessageDialog(this, newDetailsPanel(details),
				"Certificate of '" + alias + "'", JOptionPane.PLAIN_MESSAGE);
			return "showed the details of '" + alias + "'";
		});
	}

	/**
	 * The details of one certificate, laid out as a form with the certificate itself underneath, so
	 * it can be selected and copied out
	 */
	protected JComponent newDetailsPanel(KeyStoreSupport.CertificateDetails details)
	{
		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		row = addDetail(panel, row, "Issued to", details.subject());
		row = addDetail(panel, row, "Issued by", details.issuer());
		row = addDetail(panel, row, "Valid from", details.validFrom());
		row = addDetail(panel, row, "Valid until",
			details.validUntil() + (details.expired() ? "  (expired)" : ""));
		row = addDetail(panel, row, "Key", details.keyAlgorithm());
		row = addDetail(panel, row, "Signed with", details.signatureAlgorithm());
		row = addDetail(panel, row, "Serial number", details.serialNumber());
		row = addDetail(panel, row, "X.509 version", String.valueOf(details.version()));
		row = addDetail(panel, row, "SHA-256", details.fingerprint());

		JTextArea pem = new JTextArea(details.pem(), 8, 64);
		pem.setName("txtCertificatePem");
		pem.setEditable(false);
		pem.setFont(new java.awt.Font("monospaced", java.awt.Font.PLAIN, 11));
		pem.setCaretPosition(0);
		GridBagConstraints constraints = at(0, row, GridBagConstraints.WEST);
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.BOTH;
		panel.add(new JScrollPane(pem), constraints);
		return panel;
	}

	private int addDetail(JPanel panel, int row, String label, String value)
	{
		panel.add(new JLabel(label + ":"), at(0, row, GridBagConstraints.EAST));
		JTextField field = new JTextField(value, 52);
		field.setName("txtDetail" + label.replace(" ", ""));
		field.setEditable(false);
		panel.add(field, at(1, row, GridBagConstraints.WEST));
		return row + 1;
	}

	private void onCreate()
	{
		run("created", () -> {
			modelObject.setKeyStore(KeyStoreSupport.create(file(), type(), password()));
			return "created " + file().getName();
		});
	}

	private void onAddKeyPair()
	{
		run("added", () -> {
			requireOpenKeyStore();
			String alias = requireAlias();
			KeyPairGeneratorAlgorithm algorithm = modelObject.getKeyAlgorithm();
			long started = System.currentTimeMillis();
			KeyStoreSupport.addKeyPair(modelObject.getKeyStore(), file(), password(), alias,
				modelObject.getDistinguishedName(), algorithm,
				PluginSettings.asInt(settings, KeyStoreSettingsContribution.KEY_KEY_SIZE,
					KeyStoreSupport.DEFAULT_KEY_SIZE),
				KeyStoreSupport.signatureAlgorithmFor(algorithm),
				PluginSettings.asInt(settings, KeyStoreSettingsContribution.KEY_DAYS_VALID,
					KeyStoreSupport.DEFAULT_DAYS_VALID));
			return "added '" + alias + "' (" + algorithm + ") in "
				+ (System.currentTimeMillis() - started) + " ms";
		});
	}

	private void onDelete()
	{
		run("deleted", () -> {
			requireOpenKeyStore();
			String alias = selectedAlias();
			modelObject.setKeyStore(KeyStoreSupport.deleteAlias(file(), type(), password(), alias));
			return "deleted '" + alias + "'";
		});
	}

	private void onImport()
	{
		run("imported", () -> {
			requireOpenKeyStore();
			String alias = requireAlias();
			KeyStoreSupport.importCertificate(modelObject.getKeyStore(), file(), password(), alias,
				new File(modelObject.getCertificateFilePath()));
			return "imported the certificate as '" + alias + "'";
		});
	}

	private void onExport()
	{
		run("exported", () -> {
			requireOpenKeyStore();
			String alias = selectedAlias();
			File target = modelObject.getCertificateFilePath().isBlank()
				? new File(file().getParentFile(), alias + ".pem")
				: new File(modelObject.getCertificateFilePath());
			KeyStoreSupport.exportCertificate(modelObject.getKeyStore(), alias, target);
			// setting the field is what puts the path into the model as well
			txtCertificateFile.setText(target.getAbsolutePath());
			return "exported '" + alias + "' to " + target.getName();
		});
	}

	/**
	 * Runs an operation, refreshes the table from the store afterwards and reports either the
	 * message the operation returned or why it failed. Keeping this in one place is what keeps every
	 * button from having to repeat the same try-catch and the same refresh.
	 */
	private void run(String what, KeyStoreOperation operation)
	{
		try
		{
			showResult(operation.execute());
		}
		catch (Exception exception)
		{
			showResult("not " + what + ": " + message(exception));
		}
		refreshEntries();
	}

	/**
	 * Keeps the message of the last operation in the model and shows it, so what the label says and
	 * what the model holds cannot drift apart
	 */
	private void showResult(String resultMessage)
	{
		modelObject.setResultMessage(resultMessage);
		lblResult.setText(resultMessage);
	}

	private void refreshEntries()
	{
		entryModel.setRowCount(0);
		if (modelObject.getKeyStore() == null)
		{
			return;
		}
		try
		{
			List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport
				.entries(modelObject.getKeyStore());
			for (KeyStoreSupport.EntryInfo entry : entries)
			{
				entryModel.addRow(new Object[] { entry.alias(), entry.entryKind(),
						entry.algorithm(), entry.subject(), entry.validUntil(),
						entry.fingerprint() });
			}
		}
		catch (Exception exception)
		{
			showResult("the entries could not be read: " + message(exception));
		}
	}

	private void requireOpenKeyStore()
	{
		if (modelObject.getKeyStore() == null)
		{
			throw new IllegalStateException("open or create a key store first");
		}
	}

	private String requireAlias()
	{
		String alias = modelObject.getAlias().trim();
		if (alias.isEmpty())
		{
			throw new IllegalStateException("enter an alias");
		}
		return alias;
	}

	private String selectedAlias()
	{
		int row = tblEntries.getSelectedRow();
		if (row < 0)
		{
			// falling back to the alias field keeps the tool usable with the keyboard alone
			return requireAlias();
		}
		return String.valueOf(entryModel.getValueAt(tblEntries.convertRowIndexToModel(row), 0));
	}

	private File file()
	{
		return new File(modelObject.getKeyStoreFilePath().trim());
	}

	private KeystoreType type()
	{
		return modelObject.getKeystoreType();
	}

	private String password()
	{
		return new String(modelObject.getStorePassword());
	}

	private static String message(Exception exception)
	{
		// a wrong password surfaces as an exception without a message often enough that the class
		// name is the only thing left to show
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface KeyStoreOperation
	{
		String execute() throws Exception;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}

	private static JPanel buttonRow(JButton... buttons)
	{
		JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		for (JButton button : buttons)
		{
			panel.add(button);
		}
		return panel;
	}

	private static GridBagConstraints at(int column, int row, int anchor)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.anchor = anchor;
		constraints.insets = new Insets(4, 4, 4, 4);
		return constraints;
	}

	private static GridBagConstraints span(int column, int row, int width)
	{
		GridBagConstraints constraints = at(column, row, GridBagConstraints.WEST);
		constraints.gridwidth = width;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		return constraints;
	}
}
