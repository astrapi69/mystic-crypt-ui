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
package io.github.astrapi69.mystic.crypt.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter;
import io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.mystic.crypt.panel.keepass.ExportKeePassPanel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Exports the currently open application database to a KeePass {@code .kdbx} file
 */
public class ExportKeePassDatabaseAction extends AbstractAction
{

	private static final long serialVersionUID = 1L;

	public ExportKeePassDatabaseAction(final String name)
	{
		super(name);
	}

	@Override
	public void actionPerformed(final ActionEvent e)
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		ApplicationPanel applicationPanel = instance.getApplicationPanel();
		if (applicationPanel == null)
		{
			JOptionPane.showMessageDialog(instance, "Please sign in first.", "Export failed",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		MemoizedKeePassModelBean memoized = MemoizedKeePassModelBean
			.load(instance.getConfigurationDirectory());
		ExportKeePassPanel panel = new ExportKeePassPanel(memoized.getLastExportFilePath(),
			memoized.getLastExportKeyFilePath());
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, instance, "Export to KeePass", panel.getTxtFile());
		if (option != JOptionPane.OK_OPTION)
		{
			return;
		}
		File file = panel.getSelectedFile();
		if (file == null)
		{
			JOptionPane.showMessageDialog(instance, "No destination file selected.",
				"Export failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// remember the chosen paths regardless of whether the export below succeeds, so a wrong
		// password/key-file during development doesn't lose the already-correct file selection
		memoized.setLastExportFilePath(file.getAbsolutePath());
		memoized.setLastExportKeyFilePath(panel.getSelectedKeyFile() != null
			? panel.getSelectedKeyFile().getAbsolutePath()
			: null);
		memoized.save(instance.getConfigurationDirectory());
		try
		{
			KdbxCreds credentials = newCredentials(panel);
			SecretKeyTreeWithContentPanel treePanel = applicationPanel
				.getSecretKeyTreeWithContentPanel();
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = treePanel
				.getModelObject();

			SimpleDatabase database = new SimpleDatabase();
			SimpleGroup rootGroup = database.getRootGroup();
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : root
				.getChildren())
			{
				KeePassTreeConverter.toSimpleGroup(database, child, rootGroup);
			}

			try (OutputStream outputStream = new FileOutputStream(file))
			{
				database.save(credentials, outputStream);
			}
			JOptionPane.showMessageDialog(instance, "KeePass database exported successfully.",
				"Export successful", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(instance,
				"Could not export the KeePass file: " + exception.getMessage(), "Export failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	private KdbxCreds newCredentials(ExportKeePassPanel panel) throws Exception
	{
		char[] password = panel.getPassword();
		File keyFile = panel.getSelectedKeyFile();
		if (password.length > 0 && keyFile != null)
		{
			try (InputStream keyFileStream = new FileInputStream(keyFile))
			{
				return new KdbxCreds(new String(password).getBytes(StandardCharsets.UTF_8),
					keyFileStream);
			}
		}
		if (keyFile != null)
		{
			try (InputStream keyFileStream = new FileInputStream(keyFile))
			{
				return new KdbxCreds(keyFileStream);
			}
		}
		return new KdbxCreds(new String(password).getBytes(StandardCharsets.UTF_8));
	}

}
