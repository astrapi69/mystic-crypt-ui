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
import java.security.KeyStore;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

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

	private final JTextField txtKeyStoreFile = new JTextField(40);
	private final JComboBox<KeystoreType> cmbType = new JComboBox<>(
		KeyStoreSupport.USABLE_TYPES.toArray(new KeystoreType[0]));
	private final JPasswordField pwdStore = new JPasswordField(20);
	private final JTextField txtAlias = new JTextField(20);
	private final JTextField txtDistinguishedName = new JTextField(30);
	private final JComboBox<KeyPairGeneratorAlgorithm> cmbKeyAlgorithm = new JComboBox<>(
		KeyStoreSupport.KEY_ALGORITHMS.toArray(new KeyPairGeneratorAlgorithm[0]));
	private final JTextField txtCertificateFile = new JTextField(40);
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

	private transient KeyStore keyStore;

	private final transient Map<String, String> settings;

	public KeyStorePanel()
	{
		super(new GridBagLayout());

		// what the user configured in the settings dialog decides what this tool starts with
		settings = KeyStoreSettings.values();
		cmbType.setSelectedItem(KeyStoreSettings.type(settings));
		cmbKeyAlgorithm.setSelectedItem(KeyStoreSettings.algorithm(settings));
		txtDistinguishedName.setText(KeyStoreSettings.distinguishedName(settings));

		txtKeyStoreFile.setName("txtKeyStoreFile");
		cmbType.setName("cmbType");
		pwdStore.setName("pwdStore");
		txtAlias.setName("txtAlias");
		txtDistinguishedName.setName("txtDistinguishedName");
		cmbKeyAlgorithm.setName("cmbKeyAlgorithm");
		txtCertificateFile.setName("txtCertificateFile");
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
		add(buttonRow(btnImport, btnExport), at(1, row++, GridBagConstraints.WEST));
		add(lblResult, span(0, row, 3));
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
			keyStore = KeyStoreSupport.open(file(), type(), password());
			return "opened " + file().getName();
		});
	}

	private void onCreate()
	{
		run("created", () -> {
			keyStore = KeyStoreSupport.create(file(), type(), password());
			return "created " + file().getName();
		});
	}

	private void onAddKeyPair()
	{
		run("added", () -> {
			requireOpenKeyStore();
			String alias = requireAlias();
			KeyPairGeneratorAlgorithm algorithm = (KeyPairGeneratorAlgorithm)cmbKeyAlgorithm
				.getSelectedItem();
			long started = System.currentTimeMillis();
			KeyStoreSupport.addKeyPair(keyStore, file(), password(), alias,
				txtDistinguishedName.getText(), algorithm,
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
			keyStore = KeyStoreSupport.deleteAlias(file(), type(), password(), alias);
			return "deleted '" + alias + "'";
		});
	}

	private void onImport()
	{
		run("imported", () -> {
			requireOpenKeyStore();
			String alias = requireAlias();
			KeyStoreSupport.importCertificate(keyStore, file(), password(), alias,
				new File(txtCertificateFile.getText()));
			return "imported the certificate as '" + alias + "'";
		});
	}

	private void onExport()
	{
		run("exported", () -> {
			requireOpenKeyStore();
			String alias = selectedAlias();
			File target = txtCertificateFile.getText().isBlank()
				? new File(file().getParentFile(), alias + ".pem")
				: new File(txtCertificateFile.getText());
			KeyStoreSupport.exportCertificate(keyStore, alias, target);
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
			lblResult.setText(operation.execute());
		}
		catch (Exception exception)
		{
			lblResult.setText("not " + what + ": " + message(exception));
		}
		refreshEntries();
	}

	private void refreshEntries()
	{
		entryModel.setRowCount(0);
		if (keyStore == null)
		{
			return;
		}
		try
		{
			List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport.entries(keyStore);
			for (KeyStoreSupport.EntryInfo entry : entries)
			{
				entryModel.addRow(new Object[] { entry.alias(), entry.entryKind(),
						entry.algorithm(), entry.subject(), entry.validUntil(),
						entry.fingerprint() });
			}
		}
		catch (Exception exception)
		{
			lblResult.setText("the entries could not be read: " + message(exception));
		}
	}

	private void requireOpenKeyStore()
	{
		if (keyStore == null)
		{
			throw new IllegalStateException("open or create a key store first");
		}
	}

	private String requireAlias()
	{
		String alias = txtAlias.getText().trim();
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
		return new File(txtKeyStoreFile.getText().trim());
	}

	private KeystoreType type()
	{
		return (KeystoreType)cmbType.getSelectedItem();
	}

	private String password()
	{
		return new String(pwdStore.getPassword());
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
