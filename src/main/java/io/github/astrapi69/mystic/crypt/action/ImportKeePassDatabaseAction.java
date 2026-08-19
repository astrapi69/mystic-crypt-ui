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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter;
import io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.mystic.crypt.panel.keepass.ImportKeePassPanel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory;

/**
 * Imports a KeePass {@code .kdbx} database into the currently open application database, under a
 * new top-level group named after the imported file
 */
public class ImportKeePassDatabaseAction extends AbstractAction
{

	private static final long serialVersionUID = 1L;

	public ImportKeePassDatabaseAction(final String name)
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
			JOptionPane.showMessageDialog(instance, "Please sign in first.", "Import failed",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		MemoizedKeePassModelBean memoized = MemoizedKeePassModelBean
			.load(instance.getConfigurationDirectory());
		ImportKeePassPanel panel = new ImportKeePassPanel(memoized.getLastImportFilePath(),
			memoized.getLastImportKeyFilePath());
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, instance, "Import from KeePass", panel.getTxtFile());
		if (option != JOptionPane.OK_OPTION)
		{
			return;
		}
		File file = panel.getSelectedFile();
		if (file == null)
		{
			JOptionPane.showMessageDialog(instance, "No KeePass file selected.", "Import failed",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		// remember the chosen paths regardless of whether the import below succeeds, so a wrong
		// password/key-file during development doesn't lose the already-correct file selection
		memoized.setLastImportFilePath(file.getAbsolutePath());
		memoized.setLastImportKeyFilePath(panel.getSelectedKeyFile() != null
			? panel.getSelectedKeyFile().getAbsolutePath()
			: null);
		memoized.save(instance.getConfigurationDirectory());
		try
		{
			KdbxCreds credentials = newCredentials(panel);
			try (InputStream inputStream = new FileInputStream(file))
			{
				SimpleDatabase database = SimpleDatabase.load(credentials, inputStream);
				importDatabase(instance, applicationPanel, database, file.getName());
			}
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(instance,
				"Could not import the KeePass file: " + exception.getMessage(), "Import failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	private KdbxCreds newCredentials(ImportKeePassPanel panel) throws Exception
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

	private void importDatabase(MysticCryptApplicationFrame instance,
		ApplicationPanel applicationPanel, SimpleDatabase database, String sourceFileName)
	{
		ApplicationModelBean applicationModelBean = instance.getModelObject();
		SecretKeyTreeWithContentPanel treePanel = applicationPanel
			.getSecretKeyTreeWithContentPanel();
		// the live tree node currently displayed by the already-open tree panel - mutating this
		// (instead of a fresh copy derived from applicationModelBean.getRootTreeAsMap()) is what
		// makes the import show up without needing to reopen the database view
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = treePanel
			.getModelObject();
		LongIdGenerator idGenerator = instance.getIdGenerator();

		SimpleGroup keePassRootGroup = database.getRootGroup();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> importedNode = KeePassTreeConverter
			.toTreeNode(keePassRootGroup, root, () -> {
				Long nextId = idGenerator.getNextId();
				applicationModelBean.setLastId(nextId);
				return nextId;
			});
		importedNode.getValue().setName("Imported from " + sourceFileName);

		DefaultMutableTreeNode newRootNode = BaseTreeNodeFactory.newDefaultMutableTreeNode(root);
		treePanel.getTree().setModel(new DefaultTreeModel(newRootNode, true));

		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> updatedTreeAsMap = BaseTreeNodeTransformer
			.toKeyMap(root);
		applicationModelBean.setRootTreeAsMap(updatedTreeAsMap);
		applicationModelBean.setDirty(true);

		JOptionPane.showMessageDialog(instance, "KeePass database imported successfully.",
			"Import successful", JOptionPane.INFORMATION_MESSAGE);
	}

}
