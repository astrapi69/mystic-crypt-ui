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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * What Ctrl+C on the database tree puts on the clipboard: the name of each selected node, one per
 * line, and nothing else.
 * <p>
 * Swing's own {@code TreeTransferHandler} exports {@code convertValueToText}, whose default is the
 * node's {@code toString} - and a node's {@code toString} carried every entry of the group with its
 * password, title and user name, 857 characters for one entry, measured in the running application
 * (#388). The node's name is what the tree shows, so the name is what the tree copies; a password
 * leaves the vault through "Copy Password" and no other way.
 * <p>
 * Nothing is dropped into the tree: this handler only exports.
 */
public class TreeNodeNameTransferHandler extends TransferHandler
{

	private static final long serialVersionUID = 1L;

	@Override
	public int getSourceActions(final JComponent component)
	{
		return COPY;
	}

	@Override
	protected Transferable createTransferable(final JComponent component)
	{
		if (!(component instanceof JTree tree))
		{
			return null;
		}
		TreePath[] selection = tree.getSelectionPaths();
		if (selection == null || selection.length == 0)
		{
			return null;
		}
		List<String> names = new ArrayList<>();
		for (TreePath path : selection)
		{
			names.add(nameOf(path.getLastPathComponent()));
		}
		return new StringSelection(String.join(System.lineSeparator(), names));
	}

	/**
	 * The text the tree shows for this node, which is the only text it copies
	 *
	 * @param lastPathComponent
	 *            the tree node
	 * @return its name
	 */
	static String nameOf(final Object lastPathComponent)
	{
		Object userObject = lastPathComponent instanceof DefaultMutableTreeNode treeNode
			? treeNode.getUserObject()
			: lastPathComponent;
		if (userObject instanceof BaseTreeNode<?, ?> node)
		{
			if (node.getDisplayValue() != null)
			{
				return node.getDisplayValue();
			}
			if (node.getValue()instanceof GenericTreeElement<?> element
				&& element.getName() != null)
			{
				return element.getName();
			}
		}
		// a node without a name is copied as nothing rather than as its toString, which is the
		// leak this class exists to close
		return "";
	}
}
