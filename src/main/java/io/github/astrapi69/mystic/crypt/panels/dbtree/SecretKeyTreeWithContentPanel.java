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

import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.tree.JXTreeElement;
import io.github.astrapi69.swing.table.model.dynamic.DynamicTableColumnsModel;
import io.github.astrapi69.swing.tree.panel.TreeNodeJXTreeElementWithContentPanel;
import org.jdesktop.swingx.JXTree;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.table.model.GenericTableModel;
import io.github.astrapi69.swing.tree.JTreeExtensions;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.tree.TreeNode;

public class SecretKeyTreeWithContentPanel extends TreeNodeJXTreeElementWithContentPanel
{

	private static final long serialVersionUID = 1L;

	public SecretKeyTreeWithContentPanel()
	{
		this(BaseModel.of(new TreeNode<>()));
	}

	public SecretKeyTreeWithContentPanel(final Model<TreeNode<JXTreeElement>> model)
	{
		super(model);
	}

	@Override
	protected JXTree newTree()
	{
		JXTree tree = super.newTree();
		tree.setCellRenderer(new JXTreeNodeRemixIconCellRenderer());
		return tree;
	}

	@Override
	protected GenericJXTable newJTable()
	{
		GenericTableModel<MysticCryptEntryModelBean> permissionsTableModel = new DynamicMysticCryptEntryTableModel(
			new DynamicTableColumnsModel<>(MysticCryptEntryModelBean.class));
		GenericJXTable<MysticCryptEntryModelBean> table = new GenericJXTable<>(permissionsTableModel);
		return table;
	}

	@Override
	protected void onAfterInitializeComponents()
	{
		super.onAfterInitializeComponents();
		// set root
		TreeNode<JXTreeElement> root = (TreeNode<JXTreeElement>)getModelObject().getRoot();
		getTblTreeEntryTable().setModel(getTableModel(root));
	}

	@Override
	protected TreeModel newTreeModel(final Model<TreeNode<JXTreeElement>> model)
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
				TreeNode<JXTreeElement> selectedTreeNode = (TreeNode<JXTreeElement>)node
					.getUserObject();
				getTableModel(selectedTreeNode);
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

	/**
	 * {@inheritDoc}
	 *
	 * @param model
	 */
	@Override
	protected GenericTableModel getTableModel(TreeNode<JXTreeElement> model)
	{
		JXTreeElement parentTreeNode = model.getValue();
		Object defaultContent = parentTreeNode.getDefaultContent();
		java.util.List<MysticCryptEntryModelBean> permissions = (java.util.List<MysticCryptEntryModelBean>)defaultContent;
		// 2. Create a generic table model for the class Permission.
		getTblTreeEntryTable().getGenericTableModel().removeAll();
		getTblTreeEntryTable().getGenericTableModel().addList(permissions);
		return getTblTreeEntryTable().getGenericTableModel();
	}

	@Override
	protected void onSingleLeftClick(MouseEvent mouseEvent)
	{
		DefaultMutableTreeNode selectedTreeNode = JTreeExtensions.getSelectedTreeNode(tree);
		TreeNode<JXTreeElement> selectedTreeNodeElement = (TreeNode<JXTreeElement>)selectedTreeNode
			.getUserObject();

		GenericTableModel tableModel = getTableModel(selectedTreeNodeElement);

		tableModel.fireTableDataChanged();
	}

	@Override
	protected void onSingleRightClick(MouseEvent mouseEvent)
	{
		DefaultMutableTreeNode selectedTreeNode = JTreeExtensions
			.getSelectedTreeNode(tree);
		TreeNode<JXTreeElement> selectedTreeNodeElement = (TreeNode<JXTreeElement>)selectedTreeNode
			.getUserObject();

		JPopupMenu popup = new JPopupMenu();
		if (selectedTreeNodeElement.isNode())
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
						.parent(selectedTreeNodeElement.getValue()).node(allowsChildren).build();
					TreeNode<JXTreeElement> newTreeNode = TreeNode.<JXTreeElement> builder()
						.value(treeElement).parent(selectedTreeNodeElement).displayValue(userObject)
						.node(allowsChildren).build();

					DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(newTreeNode,
						allowsChildren);
					selectedTreeNode.add(newChild);
					((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);
					tree.treeDidChange();
				}

			});
			popup.add(addChild);
		}

		if (!selectedTreeNodeElement.isRoot())
		{
			JMenuItem deleteNode = new JMenuItem("delete");
			deleteNode.addActionListener(le -> {
				int selectedNodeIndex = selectedTreeNode.getParent().getIndex(selectedTreeNode);
				selectedTreeNode.removeAllChildren();
				((DefaultMutableTreeNode)selectedTreeNode.getParent()).remove(selectedNodeIndex);
				((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);
				tree.treeDidChange();
				tree.treeDidChange();
				this.repaint();
			});
			popup.add(deleteNode);
		}
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();
		popup.show(tree, x, y);
	}
}
