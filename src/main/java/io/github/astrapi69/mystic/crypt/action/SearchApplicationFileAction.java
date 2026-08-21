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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Searches the open database: asks for a term, finds the first tree node whose name - or one of
 * whose entries' title, user name, URL or notes - contains the term (case-insensitive) and selects
 * that node in the tree. Reports when nothing matches.
 */
public class SearchApplicationFileAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	public SearchApplicationFileAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		ApplicationPanel applicationPanel = frame.getApplicationPanel();
		if (applicationPanel == null)
		{
			JOptionPane.showMessageDialog(frame, "Please sign in first.", "Search",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		JTextField searchField = new JTextField(20);
		searchField.setName("txtSearch");
		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(
			new JLabel("Search for a node name or an entry's title, user name, URL or notes:"));
		panel.add(searchField);
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, frame, "Search", searchField);
		if (option != JOptionPane.OK_OPTION)
		{
			return;
		}
		String term = searchField.getText().trim();
		if (term.isEmpty())
		{
			return;
		}

		SecretKeyTreeWithContentPanel treePanel = applicationPanel
			.getSecretKeyTreeWithContentPanel();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> match = findFirstMatch(
			treePanel.getModelObject(), term.toLowerCase());
		if (match == null)
		{
			JOptionPane.showMessageDialog(frame, "No match found for \"" + term + "\".", "No match",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		selectNodeInTree(treePanel.getTree(), match);
	}

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> findFirstMatch(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root,
		String lowerTerm)
	{
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : root
			.traverse())
		{
			if (nodeMatches(node, lowerTerm))
			{
				return node;
			}
		}
		return null;
	}

	private static boolean nodeMatches(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
		String lowerTerm)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> value = node.getValue();
		if (value == null)
		{
			return false;
		}
		if (contains(value.getName(), lowerTerm))
		{
			return true;
		}
		List<MysticCryptEntryModelBean> entries = value.getDefaultContent();
		if (entries != null)
		{
			for (MysticCryptEntryModelBean entry : entries)
			{
				if (contains(entry.getTitle(), lowerTerm)
					|| contains(entry.getUserName(), lowerTerm)
					|| contains(entry.getUrl(), lowerTerm) || contains(entry.getNotes(), lowerTerm))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean contains(String value, String lowerTerm)
	{
		return value != null && value.toLowerCase().contains(lowerTerm);
	}

	private static void selectNodeInTree(JTree tree,
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> match)
	{
		// fully expand the tree so the matching row is laid out before it is resolved
		int previousRowCount = -1;
		while (tree.getRowCount() != previousRowCount)
		{
			previousRowCount = tree.getRowCount();
			for (int row = 0; row < tree.getRowCount(); row++)
			{
				tree.expandRow(row);
			}
		}
		for (int row = 0; row < tree.getRowCount(); row++)
		{
			TreePath path = tree.getPathForRow(row);
			Object lastComponent = path.getLastPathComponent();
			if (lastComponent instanceof DefaultMutableTreeNode treeNode
				&& treeNode.getUserObject()instanceof BaseTreeNode<?, ?> baseTreeNode
				&& baseTreeNode.getId() != null && baseTreeNode.getId().equals(match.getId()))
			{
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				return;
			}
		}
	}
}
