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
import java.util.List;
import java.util.Optional;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jdesktop.swingx.JXTree;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.table.model.GenericTableModel;
import io.github.astrapi69.swing.table.model.dynamic.DynamicTableColumnsModel;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.JTreeExtensions;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.swing.tree.panel.TreeNodeGenericTreeElementWithContentPanel;
import io.github.astrapi69.tree.TreeNode;

public class SecretKeyTreeWithContentPanel
	extends
		TreeNodeGenericTreeElementWithContentPanel<List<MysticCryptEntryModelBean>>
{

	private static final long serialVersionUID = 1L;

	public SecretKeyTreeWithContentPanel()
	{
		this(BaseModel.of(new TreeNode<>()));
	}

	public SecretKeyTreeWithContentPanel(
		final Model<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> model)
	{
		super(model);
	}

	@Override
	protected JXTree newTree()
	{
		JXTree tree = super.newTree();
		tree.setCellRenderer(
			new JXTreeNodeRemixIconCellRenderer<List<MysticCryptEntryModelBean>>());
		return tree;
	}

	@Override
	protected GenericJXTable newJTable()
	{
		GenericTableModel<MysticCryptEntryModelBean> permissionsTableModel = new DynamicMysticCryptEntryTableModel(
			new DynamicTableColumnsModel<>(MysticCryptEntryModelBean.class));
		GenericJXTable<MysticCryptEntryModelBean> table = new GenericJXTable<MysticCryptEntryModelBean>(
			permissionsTableModel)
		{

			protected void onSingleLeftClick(MouseEvent event)
			{
				SecretKeyTreeWithContentPanel.this.onTableSingleLeftClick(event);
			}

			protected void onSingleMiddleClick(MouseEvent event)
			{
				SecretKeyTreeWithContentPanel.this.onTableSingleMiddleClick(event);
			}

			protected void onSingleRightClick(MouseEvent event)
			{
				SecretKeyTreeWithContentPanel.this.onTableSingleRightClick(event);
			}

			protected void onDoubleLeftClick(MouseEvent event)
			{
				SecretKeyTreeWithContentPanel.this.onTableDoubleLeftClick(event);
			}


			protected void onDoubleMiddleClick(MouseEvent event)
			{
				SecretKeyTreeWithContentPanel.this.onTableDoubleMiddleClick(event);
			}

			protected void onDoubleRightClick(MouseEvent event)
			{
				SecretKeyTreeWithContentPanel.this.onTableDoubleRightClick(event);
			}

		};
		return table;
	}

	@Override
	protected void onAfterInitializeComponents()
	{
		super.onAfterInitializeComponents();
		// set root
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> root = (TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>)getModelObject()
			.getRoot();
		getTblTreeEntryTable().setModel(newTableModel(root));
	}

	@Override
	protected TreeModel newTreeModel(
		final Model<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> model)
	{
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> parentTreeNode = model
			.getObject();
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
				TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> selectedTreeNode = (TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>)node
					.getUserObject();
				newTableModel(selectedTreeNode);
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
	protected GenericTableModel newTableModel(
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> model)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> parentTreeNode = model.getValue();
		List<MysticCryptEntryModelBean> defaultContent = parentTreeNode.getDefaultContent();
		List<MysticCryptEntryModelBean> permissions = (java.util.List<MysticCryptEntryModelBean>)defaultContent;
		// 2. Create a generic table model for the class Permission.
		getTblTreeEntryTable().getGenericTableModel().removeAll();
		getTblTreeEntryTable().getGenericTableModel().addList(permissions);
		return getTblTreeEntryTable().getGenericTableModel();
	}

	@Override
	protected void onTreeSingleLeftClick(MouseEvent mouseEvent)
	{
		DefaultMutableTreeNode selectedTreeNode = JTreeExtensions.getSelectedTreeNode(tree);
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> selectedTreeNodeElement = (TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>)selectedTreeNode
			.getUserObject();

		GenericTableModel tableModel = newTableModel(selectedTreeNodeElement);

		tableModel.fireTableDataChanged();
	}

	@Override
	protected void onTreeSingleRightClick(MouseEvent mouseEvent)
	{
		Optional<DefaultMutableTreeNode> selectedDefaultMutableTreeNode = JTreeExtensions
			.getSelectedDefaultMutableTreeNode(mouseEvent, tree);
		if (selectedDefaultMutableTreeNode.isPresent())
		{
			DefaultMutableTreeNode selectedTreeNode = selectedDefaultMutableTreeNode.get();
			TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> selectedTreeNodeElement
				= (TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>)selectedTreeNode
				.getUserObject();

			JPopupMenu popup = new JPopupMenu();
			if (selectedTreeNodeElement.isNode())
			{
				JMenuItem addChild = new JMenuItem("add node...");

				addChild.addActionListener(le -> {

					AddNodePanel addNodePanel = new AddNodePanel();

					JOptionPane pane = new JOptionPane(addNodePanel, JOptionPane.INFORMATION_MESSAGE,
						JOptionPane.OK_CANCEL_OPTION);
					JDialog dialog = pane.createDialog(null, "New node");
					dialog.addWindowFocusListener(new RequestFocusListener(addNodePanel.getTxtName()));
					dialog.pack();
					dialog.setLocationRelativeTo(null);
					dialog.setVisible(true);
					int option = JOptionPaneExtensions.getSelectedOption(pane);

					if (option == JOptionPane.OK_OPTION)
					{
						boolean allowsChildren = addNodePanel.getCbxNode().isSelected();
						String userObject = addNodePanel.getTxtName().getText();
						GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = GenericTreeElement
							.<List<MysticCryptEntryModelBean>> builder().name(userObject)
							.parent(selectedTreeNodeElement.getValue()).node(allowsChildren)
							.build();
						TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> newTreeNode = TreeNode
							.<GenericTreeElement<List<MysticCryptEntryModelBean>>> builder()
							.value(treeElement).parent(selectedTreeNodeElement)
							.displayValue(userObject).node(allowsChildren).build();

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
					((DefaultMutableTreeNode)selectedTreeNode.getParent())
						.remove(selectedNodeIndex);
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

	/**
	 * The callback method on the table single left click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableSingleLeftClick(MouseEvent event)
	{

	}

	/**
	 * The callback method on the table single middle click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableSingleMiddleClick(MouseEvent event)
	{
	}

	/**
	 * The callback method on the table single right click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableSingleRightClick(MouseEvent event)
	{
		System.out.println("Single Right Table clicked");
	}

	/**
	 * The callback method on the table double left click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableDoubleLeftClick(MouseEvent event)
	{
	}

	/**
	 * The callback method on the table double middle click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableDoubleMiddleClick(MouseEvent event)
	{
	}

	/**
	 * The callback method on the table double right click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableDoubleRightClick(MouseEvent event)
	{
	}

}
