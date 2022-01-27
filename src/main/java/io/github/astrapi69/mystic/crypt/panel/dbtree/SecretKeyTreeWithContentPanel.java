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

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jdesktop.swingx.JXTree;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.menu.MenuFactory;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.table.model.GenericTableModel;
import io.github.astrapi69.swing.table.model.dynamic.DynamicTableColumnsModel;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.JTreeExtensions;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.swing.tree.content.panel.TreeNodeGenericTreeElementWithContentPanel;
import io.github.astrapi69.swing.tree.panel.node.NodeModelBean;
import io.github.astrapi69.swing.tree.panel.node.NodePanel;
import io.github.astrapi69.tree.TreeNode;

public class SecretKeyTreeWithContentPanel
	extends
		TreeNodeGenericTreeElementWithContentPanel<List<MysticCryptEntryModelBean>, MysticCryptEntryModelBean>
{

	private static final long serialVersionUID = 1L;

	public SecretKeyTreeWithContentPanel()
	{
		this(BaseModel.of(new TreeNode<>()));
	}

	public SecretKeyTreeWithContentPanel(
		final IModel<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> model)
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
	protected JScrollPane newTreeScrollPane()
	{
		JScrollPane scroller = super.newTreeScrollPane();
		scroller.getViewport().setOpaque(false);
		scroller.setOpaque(false);
		return scroller;
	}

	@Override
	protected GenericJXTable<MysticCryptEntryModelBean> newJTable()
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
		final IModel<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> model)
	{
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> parentTreeNode = model
			.getObject();
		DefaultMutableTreeNode rootNode = TreeNodeFactory.newDefaultMutableTreeNode(parentTreeNode);
		return new DefaultTreeModel(rootNode, true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param model
	 */
	@Override
	protected GenericTableModel<MysticCryptEntryModelBean> newTableModel(
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> model)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> parentTreeNode = model.getValue();
		List<MysticCryptEntryModelBean> tableInfo = parentTreeNode.getDefaultContent();
		// 2. Create a generic table model for the class Permission.
		getTblTreeEntryTable().getGenericTableModel().removeAll();
		getTblTreeEntryTable().getGenericTableModel().addList(tableInfo);
		return getTblTreeEntryTable().getGenericTableModel();
	}

	@Override
	protected void onTreeSingleLeftClick(MouseEvent mouseEvent)
	{
		Optional<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> optionalSelectedUserObject = JTreeExtensions
			.getSelectedUserObject(mouseEvent, tree);
		if (optionalSelectedUserObject.isPresent())
		{
			TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> selectedTreeNodeElement = optionalSelectedUserObject
				.get();
			GenericTableModel tableModel = newTableModel(selectedTreeNodeElement);
			tableModel.fireTableDataChanged();
		}
	}

	@Override
	protected void onTreeSingleRightClick(MouseEvent mouseEvent)
	{
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();
		Optional<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> optionalSelectedUserObject = JTreeExtensions
			.getSelectedUserObject(mouseEvent, tree);
		optionalSelectedUserObject.ifPresent(selectedTreeNodeElement -> {
			DefaultMutableTreeNode selectedTreeNode = JTreeExtensions
				.getSelectedDefaultMutableTreeNode(mouseEvent, tree).get();

			JPopupMenu popup = MenuFactory.newJPopupMenu();
			if (selectedTreeNodeElement.isNode())
			{
				popup.add(MenuFactory.newJMenuItem("add node...",
					actionEvent -> this.onAddNewChildTreeNode(mouseEvent)));
			}

			if (!selectedTreeNodeElement.isRoot())
			{
				popup.add(MenuFactory.newJMenuItem("delete",
					actionEvent -> this.onDeleteSelectedTreeNode(mouseEvent)));
			}

			popup.add(MenuFactory.newJMenuItem("Edit node...",
				actionEvent -> this.onEditSelectedTreeNode(mouseEvent)));

			popup.add(MenuFactory.newJMenuItem("Collapse node",
				actionEvent -> this.onCollapseSelectedTreeNode(mouseEvent)));

			popup.add(MenuFactory.newJMenuItem("Expand node",
				actionEvent -> this.onExpandSelectedTreeNode(mouseEvent)));

			popup.show(tree, x, y);
		});
	}

	protected void onEditSelectedTreeNode(final MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> {

				TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> selectedTreeNode = (TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>)selectedDefaultMutableTreeNode
					.getUserObject();
				NodePanel nodePanel = new NodePanel(
					BaseModel.of(NodeModelBean.builder().name(selectedTreeNode.getValue().getName())
						.node(selectedTreeNode.getValue().isNode()).build()));
				JOptionPane pane = new JOptionPane(nodePanel, JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
				JDialog dialog = pane.createDialog(null, "Edit node");
				dialog.addWindowFocusListener(new RequestFocusListener(nodePanel.getTxtName()));
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
				int option = JOptionPaneExtensions.getSelectedOption(pane);

				if (option == JOptionPane.OK_OPTION)
				{
					NodeModelBean modelObject = nodePanel.getModelObject();
					boolean node = modelObject.isNode();
					String name = modelObject.getName();
					selectedTreeNode.setNode(node);
					selectedTreeNode.setDisplayValue(name);

					if (selectedTreeNode.getValue().isNode() != node)
					{
						// set to leaf only if the node has no children
						if ((node) || 0 == selectedDefaultMutableTreeNode.getChildCount())
						{
							selectedTreeNode.getValue().setNode(node);
						}
					}

					selectedTreeNode.getValue().setName(name);

					((DefaultTreeModel)tree.getModel()).reload(selectedDefaultMutableTreeNode);
					tree.treeDidChange();
				}
			});

	}

	/**
	 * The callback method on delete the selected tree node
	 */
	protected void onDeleteSelectedTreeNode(MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedTreeNode -> {
				int selectedNodeIndex = selectedTreeNode.getParent().getIndex(selectedTreeNode);
				selectedTreeNode.removeAllChildren();
				((DefaultMutableTreeNode)selectedTreeNode.getParent()).remove(selectedNodeIndex);
				((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);
				tree.treeDidChange();
				tree.treeDidChange();
				this.repaint();
			});
	}

	/**
	 * The callback method on add a new child tree node
	 */
	protected void onAddNewChildTreeNode(MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedTreeNode -> {
				TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> parentTreeNode = (TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>)selectedTreeNode
					.getUserObject();
				NodePanel nodePanel = new NodePanel();
				JOptionPane pane = new JOptionPane(nodePanel, JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
				JDialog dialog = pane.createDialog(null, "New node");
				dialog.addWindowFocusListener(new RequestFocusListener(nodePanel.getTxtName()));
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
				int option = JOptionPaneExtensions.getSelectedOption(pane);

				if (option == JOptionPane.OK_OPTION)
				{
					NodeModelBean modelObject = nodePanel.getModelObject();
					boolean node = modelObject.isNode();
					String name = modelObject.getName();
					GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = GenericTreeElement
						.<List<MysticCryptEntryModelBean>> builder().name(name)
						.parent(parentTreeNode.getValue()).node(node).build();
					TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> newTreeNode = TreeNode
						.<GenericTreeElement<List<MysticCryptEntryModelBean>>> builder()
						.value(treeElement).parent(parentTreeNode).displayValue(name).node(node)
						.build();

					DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(newTreeNode, node);
					selectedTreeNode.add(newChild);
					((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);
					tree.treeDidChange();
				}
			});
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
	 * @param mouseEvent
	 *            the mouse event
	 */
	protected void onTableSingleRightClick(MouseEvent mouseEvent)
	{
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();

		List<MysticCryptEntryModelBean> allSelectedRowData = getTblTreeEntryTable()
			.getAllSelectedRowData();

		JPopupMenu popup = MenuFactory.newJPopupMenu();

		popup.add(MenuFactory.newJMenuItem("add ...", actionEvent -> this.onAddTableEntry()));

		popup.show(getTblTreeEntryTable(), x, y);

	}

	protected void onAddTableEntry()
	{
		MysticCryptEntryPanel panel = new MysticCryptEntryPanel();
		JOptionPane pane = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = pane.createDialog(null, "New Crypt Entry");
		dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtEntryName()));
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		int option = JOptionPaneExtensions.getSelectedOption(pane);
		if (option == JOptionPane.OK_OPTION)
		{

		}
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
