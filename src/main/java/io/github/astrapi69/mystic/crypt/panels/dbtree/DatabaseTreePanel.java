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
package io.github.astrapi69.mystic.crypt.panels.dbtree;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTree;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.tree.JXTreeElement;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.swing.tree.panel.TreeNodeJXTreeElementPanel;
import io.github.astrapi69.tree.TreeNode;

public class DatabaseTreePanel extends TreeNodeJXTreeElementPanel
{

	private static final long serialVersionUID = 1L;

	public DatabaseTreePanel()
	{
		this(BaseModel.of(new TreeNode<>()));
	}

	public DatabaseTreePanel(final IModel<TreeNode<JXTreeElement>> model)
	{
		super(model);
	}

	public static int getOption(JOptionPane pane)
	{

		Object selectedValue = pane.getValue();

		if (selectedValue == null)
			return -1;
		Object[] options = pane.getOptions();
		if (options == null)
		{
			if (selectedValue instanceof Integer)
				return ((Integer)selectedValue).intValue();
			return -1;
		}
		for (int counter = 0, maxCounter = options.length; counter < maxCounter; counter++)
		{
			if (options[counter].equals(selectedValue))
				return counter;
		}
		return -1;
	}

	@Override
	protected JXTree newTree()
	{
		JXTree tree = super.newTree();
		tree.setCellRenderer(new JXTreeNodeRemixIconCellRenderer());
		return tree;
	}

	@Override
	protected TreeModel newTreeModel(final IModel<TreeNode<JXTreeElement>> model)
	{
		TreeNode<JXTreeElement> parentTreeNode = model.getObject();
		TreeModel treeModel;

		// treeModel = new TreeNodeModel(parentTreeNode);

		DefaultMutableTreeNode rootNode = TreeNodeFactory.newDefaultMutableTreeNode(parentTreeNode);

		treeModel = new DefaultTreeModel(rootNode, true);

		treeModel.addTreeModelListener(new TreeModelListener()
		{
			@Override
			public void treeNodesChanged(TreeModelEvent e)
			{
				Object lastPathComponent = e.getTreePath().getLastPathComponent();
				DefaultMutableTreeNode node;
				node = (DefaultMutableTreeNode)lastPathComponent;
				int index = e.getChildIndices()[0];
				node = (DefaultMutableTreeNode)(node.getChildAt(index));
			}

			@Override
			public void treeNodesInserted(TreeModelEvent e)
			{
				Object lastPathComponent = e.getTreePath().getLastPathComponent();
				DefaultMutableTreeNode node;
				node = (DefaultMutableTreeNode)lastPathComponent;
				System.err.println(node);
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent e)
			{
				Object lastPathComponent = e.getTreePath().getLastPathComponent();
				DefaultMutableTreeNode node;
				node = (DefaultMutableTreeNode)lastPathComponent;
				System.err.println(node);
			}

			@Override
			public void treeStructureChanged(TreeModelEvent e)
			{
				Object lastPathComponent = e.getTreePath().getLastPathComponent();
				DefaultMutableTreeNode node;
				node = (DefaultMutableTreeNode)lastPathComponent;
				System.err.println(node);
			}
		});
		return treeModel;
	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(scrTree, javax.swing.GroupLayout.PREFERRED_SIZE, 384,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(scrTree, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE)
				.addContainerGap()));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	@Override
	protected void onTreeSingleRightClick(MouseEvent e)
	{
		int x = e.getX();
		int y = e.getY();
		TreePath selectionPath = tree.getPathForLocation(e.getX(), e.getY());
		tree.getSelectionModel().setSelectionPath(selectionPath);

		Object lastPathComponent = selectionPath.getLastPathComponent();
		DefaultMutableTreeNode selectedTreeNode = (DefaultMutableTreeNode)lastPathComponent;
		TreeNode<JXTreeElement> parentTreeNode = (TreeNode<JXTreeElement>)selectedTreeNode
			.getUserObject();

		JPopupMenu popup = new JPopupMenu();
		if (parentTreeNode.isNode())
		{
			JMenuItem addChild = new JMenuItem("add node...");
			addChild.addActionListener(le -> {
				JTextField textField1 = new JTextField();
				final JCheckBox checkBox = new JCheckBox();

				checkBox.addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(ChangeEvent e)
					{
						if (e.getSource() == checkBox)
						{
							if (checkBox.isSelected())
							{

							}
							else
							{

							}
						}
					}
				});
				JPanel panel = new JPanel(new GridLayout(2, 2));
				panel.add(new JLabel("Enter name for node:"));
				panel.add(textField1);
				panel.add(new JLabel("Is leaf:"));
				panel.add(checkBox);

				JOptionPane pane = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
				JDialog dialog = pane.createDialog(null, "New node");
				dialog.addWindowFocusListener(new RequestFocusListener(textField1));
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
				int option = JOptionPaneExtensions.getSelectedOption(pane);

				if (option == JOptionPane.OK_OPTION)
				{
					boolean allowsChildren = !checkBox.isSelected();
					String userObject = textField1.getText();
					JXTreeElement treeElement = JXTreeElement.builder().name(userObject)
						.parent(parentTreeNode.getValue()).node(allowsChildren).build();
					TreeNode<JXTreeElement> newTreeNode = TreeNode.<JXTreeElement> builder()
						.value(treeElement).parent(parentTreeNode).displayValue(userObject)
						.node(allowsChildren).build();
					new TreeNode<>(treeElement);
					DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(newTreeNode,
						allowsChildren);
					selectedTreeNode.add(newChild);
					((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);
					tree.treeDidChange();
				}

			});
			popup.add(addChild);
		}

		if (!parentTreeNode.isRoot())
		{
			JMenuItem deleteNode = new JMenuItem("delete");
			deleteNode.addActionListener(le -> {
				if (!selectedTreeNode.isRoot())
				{
					int selectedNodeIndex = selectedTreeNode.getParent().getIndex(selectedTreeNode);
					selectedTreeNode.removeAllChildren();
					((DefaultMutableTreeNode)selectedTreeNode.getParent())
						.remove(selectedNodeIndex);
					((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);
					tree.treeDidChange();
				}
				else
				{
					DatabaseTreePanel.this.setModelObject(null);
				}
				tree.treeDidChange();
				this.repaint();
			});
			popup.add(deleteNode);
		}
		popup.show(tree, x, y);
	}
}
