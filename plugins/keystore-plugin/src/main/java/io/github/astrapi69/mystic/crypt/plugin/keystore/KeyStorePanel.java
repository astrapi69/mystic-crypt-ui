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

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
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

	/** The height the certificate view keeps even in the minimum layout */
	private static final int MINIMUM_CERTIFICATE_HEIGHT = 120;

	/** The size the table of entries asks for, so the tool window opens wide enough to read a row */
	private static final Dimension PREFERRED_ENTRY_TABLE_SIZE = new Dimension(760, 200);

	/** The height the table of entries keeps when the window has nothing left to give */
	private static final int MINIMUM_ENTRY_TABLE_HEIGHT = 48;

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
		// one layout for every tool window, so this one looks like the one next to it: labels in a
		// narrow right aligned column, fields taking the width, the table of entries taking the
		// height that is left, buttons under what they act on
		super(ToolForm.newLayout());

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

		txtKeyStoreFile.setToolTipText(KeyStoreMessages.getString("keystore.tooltip.file",
			"the .jks or .p12 file this tool works on"));
		cmbType.setToolTipText(KeyStoreMessages.getString("keystore.tooltip.type",
			"the key store format - JKS or PKCS12"));
		pwdStore.setToolTipText(KeyStoreMessages.getString("keystore.tooltip.password",
			"the password that opens and protects this key store"));
		txtAlias.setToolTipText(KeyStoreMessages.getString("keystore.tooltip.alias",
			"the name an entry is stored and looked up under"));
		txtDistinguishedName.setToolTipText(
			KeyStoreMessages.getString("keystore.tooltip.distinguished.name",
				"the subject of the self-signed certificate generated with a new key pair, "
					+ "for example CN=example.com"));
		cmbKeyAlgorithm.setToolTipText(KeyStoreMessages.getString("keystore.tooltip.key.algorithm",
			"the algorithm a newly generated key pair uses"));
		txtCertificateFile.setToolTipText(KeyStoreMessages.getString(
			"keystore.tooltip.certificate.file", "the certificate file used to import or export"));
		txtPrivateKeyFile.setToolTipText(
			KeyStoreMessages.getString("keystore.tooltip.private.key.file",
				"the private key file used together with the certificate to import a key pair"));

		JButton btnBrowse = button("btnBrowse", "...",
			event -> onBrowse(txtKeyStoreFile, keyStoreFilePathModel()),
			KeyStoreMessages.getString("keystore.tooltip.browse", "choose the key store file"));
		JButton btnOpen = button("btnOpen", "Open", event -> onOpen(),
			KeyStoreMessages.getString("keystore.tooltip.open", "opens the key store file above"));
		JButton btnCreate = button("btnCreate", "Create", event -> onCreate(),
			KeyStoreMessages.getString("keystore.tooltip.create",
				"creates a new, empty key store at the file above - refuses to overwrite an "
					+ "existing one"));
		JButton btnAddKeyPair = button("btnAddKeyPair", "Add key pair", event -> onAddKeyPair(),
			KeyStoreMessages.getString("keystore.tooltip.add.key.pair",
				"generates a new key pair with a self-signed certificate and adds it under the "
					+ "alias above"));
		JButton btnDelete = button("btnDelete", "Delete alias", event -> onDelete(),
			KeyStoreMessages.getString("keystore.tooltip.delete",
				"removes the selected alias from the key store"));
		JButton btnBrowseCertificate = button("btnBrowseCertificate", "...",
			event -> onBrowse(txtCertificateFile, certificateFilePathModel()), KeyStoreMessages
				.getString("keystore.tooltip.browse.certificate", "choose the certificate file"));
		JButton btnImport = button("btnImport", "Import certificate", event -> onImport(),
			KeyStoreMessages.getString("keystore.tooltip.import",
				"imports the certificate file above under the alias"));
		JButton btnExport = button("btnExport", "Export certificate as PEM", event -> onExport(),
			KeyStoreMessages.getString("keystore.tooltip.export",
				"exports the selected alias's certificate as PEM to the certificate file above, "
					+ "or next to the key store file if left blank"));
		JButton btnImportKeyPair = button("btnImportKeyPair", "Import key + certificate",
			event -> onImportKeyPair(),
			KeyStoreMessages.getString("keystore.tooltip.import.key.pair",
				"imports the private key and certificate files above together, under the alias"));
		JButton btnAddSecretKey = button("btnAddSecretKey", "Add secret key",
			event -> onAddSecretKey(), KeyStoreMessages.getString("keystore.tooltip.add.secret.key",
				"adds a new AES secret key under the alias above"));
		JButton btnDetails = button("btnDetails", "Details...", event -> onShowDetails(),
			KeyStoreMessages.getString("keystore.tooltip.details",
				"shows the full details of the selected alias's certificate"));

		ToolForm.sized(txtKeyStoreFile);
		ToolForm.sized(pwdStore);
		ToolForm.sized(txtAlias);
		ToolForm.sized(txtDistinguishedName);
		ToolForm.sized(txtCertificateFile);
		ToolForm.sized(txtPrivateKeyFile);

		add(new JLabel("Key store file:"));
		add(txtKeyStoreFile, ToolForm.FIELD + ", split 2");
		add(btnBrowse);
		add(new JLabel("Type:"));
		add(cmbType, ToolForm.FIELD);
		add(new JLabel("Store password:"));
		add(pwdStore, ToolForm.FIELD);
		add(ToolForm.buttons(btnOpen, btnCreate), ToolForm.BUTTON_ROW);

		JScrollPane entryScrollPane = new JScrollPane(tblEntries);
		// a table asks for a viewport of its own whatever it holds, which would make the table the
		// one thing in this window that cannot give way; the window decides its height, and the
		// floor below only keeps a row visible when there is nothing left to give
		entryScrollPane.setPreferredSize(PREFERRED_ENTRY_TABLE_SIZE);
		entryScrollPane.setMinimumSize(
			new Dimension(ToolForm.MINIMUM_FIELD_WIDTH, MINIMUM_ENTRY_TABLE_HEIGHT));
		add(entryScrollPane, ToolForm.GROWING);

		add(new JLabel("Alias:"));
		add(txtAlias, ToolForm.FIELD);
		add(new JLabel("Distinguished name:"));
		add(txtDistinguishedName, ToolForm.FIELD);
		add(new JLabel("Key algorithm:"));
		add(cmbKeyAlgorithm, ToolForm.FIELD);
		add(ToolForm.buttons(btnAddKeyPair, btnDelete), ToolForm.BUTTON_ROW);

		add(new JLabel("Certificate file:"));
		add(txtCertificateFile, ToolForm.FIELD + ", split 2");
		add(btnBrowseCertificate);
		add(new JLabel("Private key file:"));
		add(txtPrivateKeyFile, ToolForm.FIELD + ", split 2");
		add(button("btnBrowsePrivateKey", "...",
			event -> onBrowse(txtPrivateKeyFile, privateKeyFilePathModel()),
			KeyStoreMessages.getString("keystore.tooltip.browse.private.key",
				"choose the private key file")));
		add(ToolForm.buttons(btnImport, btnExport), ToolForm.BUTTON_ROW);
		add(ToolForm.buttons(btnImportKeyPair, btnAddSecretKey, btnDetails), ToolForm.BUTTON_ROW);
		add(lblResult, ToolForm.RESULT_LINE);
	}

	/**
	 * Binds every component to the model, so that each edit lands in the model and the model is
	 * what the buttons read - the components carry the values the model already holds
	 */
	private void bindComponents()
	{
		txtKeyStoreFile.setPropertyModel(keyStoreFilePathModel());
		cmbType.setPropertyModel(
			LambdaModel.of(modelObject::getKeystoreType, modelObject::setKeystoreType));
		pwdStore.setPropertyModel(
			LambdaModel.of(modelObject::getStorePassword, modelObject::setStorePassword));
		txtAlias.setPropertyModel(LambdaModel.of(modelObject::getAlias, modelObject::setAlias));
		txtDistinguishedName.setPropertyModel(
			LambdaModel.of(modelObject::getDistinguishedName, modelObject::setDistinguishedName));
		cmbKeyAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getKeyAlgorithm, modelObject::setKeyAlgorithm));
		txtCertificateFile.setPropertyModel(certificateFilePathModel());
		txtPrivateKeyFile.setPropertyModel(privateKeyFilePathModel());
	}

	/** The path of the key store file, bound to its field and read by its browse button */
	private IModel<String> keyStoreFilePathModel()
	{
		return LambdaModel.of(modelObject::getKeyStoreFilePath, modelObject::setKeyStoreFilePath);
	}

	/** The path of the certificate file, bound to its field and read by its browse button */
	private IModel<String> certificateFilePathModel()
	{
		return LambdaModel.of(modelObject::getCertificateFilePath,
			modelObject::setCertificateFilePath);
	}

	/** The path of the private key file, bound to its field and read by its browse button */
	private IModel<String> privateKeyFilePathModel()
	{
		return LambdaModel.of(modelObject::getPrivateKeyFilePath,
			modelObject::setPrivateKeyFilePath);
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

	private void onBrowse(JMTextField field, IModel<String> chosenFile)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!chosenFile.getObject().isBlank())
		{
			fileChooser.setSelectedFile(new File(chosenFile.getObject()));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			chosenFile.setObject(fileChooser.getSelectedFile().getAbsolutePath());
			field.setText(chosenFile.getObject());
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
			String alias = requireFreeAlias();
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
			String alias = requireFreeAlias();
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
			modelObject.setCertificateDetails(
				KeyStoreSupport.details(modelObject.getKeyStore(), alias));
			JComponent detailsPanel = newDetailsPanel(modelObject.getCertificateDetails());
			JOptionPane.showMessageDialog(this, detailsPanel, "Certificate of '" + alias + "'",
				JOptionPane.PLAIN_MESSAGE);
			return "showed the details of '" + alias + "'";
		});
	}

	/**
	 * The details of one certificate, laid out as a form with the certificate itself underneath, so
	 * it can be selected and copied out
	 */
	protected JComponent newDetailsPanel(KeyStoreSupport.CertificateDetails details)
	{
		JPanel panel = new JPanel(ToolForm.newLayout());
		addDetail(panel, "Issued to", details.subject());
		addDetail(panel, "Issued by", details.issuer());
		addDetail(panel, "Valid from", details.validFrom());
		addDetail(panel, "Valid until",
			details.validUntil() + (details.expired() ? "  (expired)" : ""));
		addDetail(panel, "Key", details.keyAlgorithm());
		addDetail(panel, "Signed with", details.signatureAlgorithm());
		addDetail(panel, "Serial number", details.serialNumber());
		addDetail(panel, "X.509 version", String.valueOf(details.version()));
		addDetail(panel, "SHA-256", details.fingerprint());

		// a snapshot of what the store holds, not state the user edits: a model backed
		// component here would carry a property model that nothing ever reads
		JTextArea pem = new JTextArea(details.pem(), 8, 64);
		pem.setName("txtCertificatePem");
		pem.setEditable(false);
		pem.setFont(new Font("monospaced", Font.PLAIN, 11));
		pem.setCaretPosition(0);
		JScrollPane pemScrollPane = ToolForm.scrolled(pem);
		// the minimum belongs here and not on the text area: a text area asks for the size of its
		// content, which leaves the pane around it free to collapse
		pemScrollPane.setMinimumSize(
			new Dimension(ToolForm.MINIMUM_FIELD_WIDTH, MINIMUM_CERTIFICATE_HEIGHT));
		panel.add(pemScrollPane, ToolForm.GROWING);
		return panel;
	}

	/**
	 * Adds one labelled line of the certificate to the details form, in a field that can be
	 * selected and copied out but not edited
	 *
	 * @param panel
	 *            the form the line is added to
	 * @param label
	 *            the label in front of the value
	 * @param value
	 *            the value the certificate holds
	 */
	private void addDetail(JPanel panel, String label, String value)
	{
		panel.add(new JLabel(label + ":"));
		// the same: this shows one line of the certificate that was just read
		JTextField field = new JTextField(value, 52);
		field.setName("txtDetail" + label.replace(" ", ""));
		field.setEditable(false);
		panel.add(ToolForm.sized(field), ToolForm.FIELD);
	}

	private void onCreate()
	{
		run("created", () -> {
			requireFreeFile();
			modelObject.setKeyStore(KeyStoreSupport.create(file(), type(), password()));
			return "created " + file().getName();
		});
	}

	private void onAddKeyPair()
	{
		run("added", () -> {
			requireOpenKeyStore();
			String alias = requireFreeAlias();
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
			String alias = requireFreeAlias();
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
			// where the certificate went is part of what this panel holds, so it goes into the
			// model and the field shows what the model says
			modelObject.setCertificateFilePath(target.getAbsolutePath());
			txtCertificateFile.setText(modelObject.getCertificateFilePath());
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

	/**
	 * The alias of an entry that is about to be written, refused when the store already holds one
	 * under that name.
	 * <p>
	 * A key store replaces an entry silently, which for a private key means it is gone. The
	 * command line refuses the same thing, so the two halves of this tool now agree.
	 *
	 * @return the alias, which is free
	 * @throws Exception
	 *             if the store cannot be asked
	 */
	private String requireFreeAlias() throws Exception
	{
		String alias = requireAlias();
		if (modelObject.getKeyStore().containsAlias(alias))
		{
			throw new IllegalStateException("'" + alias + "' already exists in "
				+ file().getName() + "; delete it first or choose another alias");
		}
		return alias;
	}

	/**
	 * Refuses a path that already holds something, because creating a key store there writes an
	 * empty one over whatever was in it
	 */
	private void requireFreeFile()
	{
		File keyStoreFile = file();
		if (keyStoreFile.exists() && keyStoreFile.length() > 0)
		{
			throw new IllegalStateException("'" + keyStoreFile.getName()
				+ "' already exists; open it instead, or choose another file");
		}
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

	private static JButton button(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}

}
