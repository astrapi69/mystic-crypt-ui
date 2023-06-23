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
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTree;

import io.github.astrapi69.browser.BrowserControlExtensions;
import io.github.astrapi69.clone.CloneQuietlyExtensions;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.design.pattern.observer.event.EventSource;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.gen.tree.visitor.MaxIndexFinderTreeNodeVisitor;
import io.github.astrapi69.gen.tree.visitor.ReindexTreeNodeVisitor;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.model.enumtype.visibity.RenderMode;
import io.github.astrapi69.model.node.NodeModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.swing.dialog.DialogExtensions;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.menu.factory.JMenuItemFactory;
import io.github.astrapi69.swing.menu.factory.JPopupMenuFactory;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.table.model.GenericTableModel;
import io.github.astrapi69.swing.tree.BaseTreeNodeFactory;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.JTreeExtensions;
import io.github.astrapi69.swing.tree.panel.content.BaseTreeNodeGenericTreeElementWithContentPanel;
import io.github.astrapi69.swing.tree.panel.node.NodePanel;
import io.github.astrapi69.swing.tree.renderer.state.NewGenericBaseTreeNodeCellRenderer;
import io.github.astrapi69.swing.util.ClipboardExtensions;

public class SecretKeyTreeWithContentPanel
	extends
		BaseTreeNodeGenericTreeElementWithContentPanel<List<MysticCryptEntryModelBean>, Long, MysticCryptEntryModelBean>
{
	@Serial
	private static final long serialVersionUID = 1L;

	public SecretKeyTreeWithContentPanel(
		final IModel<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> model)
	{
		super(model);
	}

	@Override
	protected JXTree newTree()
	{
		JXTree tree = super.newTree();
		tree.setCellRenderer(
			new NewGenericBaseTreeNodeCellRenderer<List<MysticCryptEntryModelBean>, Long>());
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
		GenericTableModel<MysticCryptEntryModelBean> tableModel = new MysticCryptEntryTableModel();
		return new GenericJXTable<>(tableModel)
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
	}

	@Override
	protected void onAfterInitializeComponents()
	{
		super.onAfterInitializeComponents();
		// set root
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = getModelObject()
			.getRoot();
		getTblTreeEntryTable().setModel(newTableModel(root));
	}

	@Override
	protected TreeModel newTreeModel(
		final IModel<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> model)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parentTreeNode = model
			.getObject();
		DefaultMutableTreeNode rootNode = BaseTreeNodeFactory
			.newDefaultMutableTreeNode(parentTreeNode);

		return new DefaultTreeModel(rootNode, true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param model
	 */
	@Override
	protected GenericTableModel<MysticCryptEntryModelBean> newTableModel(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> model)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> parentTreeNode = model.getValue();
		List<MysticCryptEntryModelBean> tableInfo = parentTreeNode.getDefaultContent();
		if (tableInfo == null)
		{
			tableInfo = new ArrayList<>();
		}
		// 2. Create a generic table model
		getTblTreeEntryTable().getGenericTableModel().removeAll();
		getTblTreeEntryTable().getGenericTableModel().addList(tableInfo);
		return getTblTreeEntryTable().getGenericTableModel();
	}

	@Override
	protected void onTreeSingleLeftClick(MouseEvent mouseEvent)
	{
		Optional<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> optionalSelectedUserObject = JTreeExtensions
			.getSelectedUserObject(mouseEvent, tree);
		if (optionalSelectedUserObject.isPresent())
		{
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNodeElement = optionalSelectedUserObject
				.get();
			GenericTableModel<MysticCryptEntryModelBean> tableModel = newTableModel(
				selectedTreeNodeElement);
			tableModel.fireTableDataChanged();
		}
	}

	@Override
	protected void onTreeSingleRightClick(MouseEvent mouseEvent)
	{
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();
		Optional<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> optionalSelectedUserObject = JTreeExtensions
			.getSelectedUserObject(mouseEvent, tree);
		optionalSelectedUserObject.ifPresent(selectedTreeNodeElement -> {
			JPopupMenu popup = JPopupMenuFactory.newJPopupMenu();
			if (selectedTreeNodeElement.isNode())
			{
				popup.add(JMenuItemFactory.newJMenuItem("add node...",
					actionEvent -> this.onAddNewChildTreeNode(mouseEvent)));
			}

			popup.add(JMenuItemFactory.newJMenuItem("Edit node...",
				actionEvent -> this.onEditSelectedTreeNode(mouseEvent)));


			if (!selectedTreeNodeElement.isRoot())
			{
				popup.add(JMenuItemFactory.newJMenuItem("Duplicate node...",
					actionEvent -> this.onDuplicateSelectedTreeNode(mouseEvent)));

				popup.add(JMenuItemFactory.newJMenuItem("delete",
					actionEvent -> this.onDeleteSelectedTreeNode(mouseEvent)));
			}

			popup.add(JMenuItemFactory.newJMenuItem("Collapse node",
				actionEvent -> this.onCollapseSelectedTreeNode(mouseEvent)));

			popup.add(JMenuItemFactory.newJMenuItem("Expand node",
				actionEvent -> this.onExpandSelectedTreeNode(mouseEvent)));

			popup.show(tree, x, y);
		});
	}

	/**
	 * The callback method on duplicate a tree node
	 */
	@SuppressWarnings("unchecked")
	protected void onDuplicateSelectedTreeNode(MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> {
				// get the selected tree node from the DefaultMutableTreeNode
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)selectedDefaultMutableTreeNode
					.getUserObject();
				// declare a visitor for reindex the new tree nodes
				ReindexTreeNodeVisitor<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long, BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> reindexTreeNodeVisitor;

				// declare a visitor for find the maximum index
				MaxIndexFinderTreeNodeVisitor<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long, BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> maxIndexFinderTreeNodeVisitor;

				Long maxIndex;
				Long nextId;
				// implement the visitor for find the max index
				maxIndexFinderTreeNodeVisitor = new MaxIndexFinderTreeNodeVisitor<>()
				{
					@Override
					public boolean isGreater(Long id)
					{
						return getMaxIndex() < id;
					}
				};
				// clone the tree structure
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> clonedTreeNode = CloneQuietlyExtensions
					.clone(selectedTreeNode);
				NodePanel panel = new NodePanel()
				{
					protected void onInitializeMigLayout()
					{
						MigLayout layout = new MigLayout(new LC().fillX().wrapAfter(2),
							new AC().align("left").gap("10").grow().fill(),
							new AC().fill().gap("10"));
						this.setLayout(layout);

						add(getLblName());
						add(getTxtName(), new CC().grow().width("120px"));
					}
				};
				String newName = clonedTreeNode.getDisplayValue() + "-Copy";
				panel.getModelObject().setName(newName);
				panel.getTxtName().setText(newName);
				int option = JOptionPaneExtensions.getSelectedOption(panel,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
					Messages.getString("dialog.duplicate.node.entry.title", "Name for duplicate"),
					panel.getTxtName());
				if (option == JOptionPane.OK_OPTION)
				{
					// get parent
					BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parentTreeNode = selectedTreeNode
						.getParent();
					NodeModel modelObject = panel.getModelObject();
					newName = modelObject.getName();
					// set new name ...
					clonedTreeNode.getValue().setName(newName);
					clonedTreeNode.setDisplayValue(newName);
					clonedTreeNode.setParent(parentTreeNode);

					parentTreeNode.addChild(clonedTreeNode);

					selectedTreeNode.getRoot().accept(maxIndexFinderTreeNodeVisitor);
					maxIndex = maxIndexFinderTreeNodeVisitor.getMaxIndex();

					nextId = maxIndex + 1;
					MysticCryptApplicationFrame.getInstance()
						.setIdGenerator(LongIdGenerator.of(nextId));
					LongIdGenerator idGenerator = MysticCryptApplicationFrame.getInstance()
						.getIdGenerator();
					reindexTreeNodeVisitor = new ReindexTreeNodeVisitor<>(idGenerator);
					clonedTreeNode.accept(reindexTreeNodeVisitor);

					BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootTreeNode = selectedTreeNode
						.getRoot();
					Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> clonedKeyMap = BaseTreeNodeTransformer
						.toKeyMap(clonedTreeNode);
					Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> longTreeIdNodeMap = BaseTreeNodeTransformer
						.toKeyMap(rootTreeNode);
					longTreeIdNodeMap.putAll(clonedKeyMap);
					MysticCryptApplicationFrame.getInstance().getModelObject()
						.setRootTreeAsMap(longTreeIdNodeMap);

					DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selectedDefaultMutableTreeNode
						.getParent();

					BaseTreeNodeFactory.newDefaultMutableTreeNode(clonedTreeNode, parent, false);

					reload(parent);
				}
			});
	}

	/**
	 * The callback method on add a new child tree node
	 */
	@SuppressWarnings("unchecked")
	protected void onAddNewChildTreeNode(MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> {
				Object userObject = selectedDefaultMutableTreeNode.getUserObject();
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parentTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;
				NodePanel panel = new NodePanel();
				int option = JOptionPaneExtensions.getSelectedOption(panel,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
					Messages.getString("dialog.new.node.entry.title", "New node."),
					panel.getTxtName());

				if (option == JOptionPane.OK_OPTION)
				{
					NodeModel modelObject = panel.getModelObject();
					boolean leaf = modelObject.isLeaf();
					String name = modelObject.getName();
					GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = GenericTreeElement
						.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(leaf).build();
					LongIdGenerator idGenerator = MysticCryptApplicationFrame.getInstance()
						.getIdGenerator();
					Long nextId = idGenerator.getNextId();
					MysticCryptApplicationFrame.getInstance().getModelObject().setLastId(nextId);
					BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> newTreeNode = BaseTreeNode
						.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
						.id(nextId).value(treeElement).parent(parentTreeNode).displayValue(name)
						.leaf(leaf).build();
					parentTreeNode.addChild(newTreeNode);

					DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(newTreeNode, leaf);
					selectedDefaultMutableTreeNode.add(newChild);
					reload(selectedDefaultMutableTreeNode);
				}
			});
	}

	@SuppressWarnings("unchecked")
	protected void onEditSelectedTreeNode(final MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> {
				Object userObject = selectedDefaultMutableTreeNode.getUserObject();
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;
				NodePanel panel = new NodePanel(
					BaseModel.of(NodeModel.builder().name(selectedTreeNode.getValue().getName())
						.leaf(selectedTreeNode.getValue().isLeaf()).build()));
				int option = JOptionPaneExtensions.getSelectedOption(panel,
					JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
					Messages.getString("dialog.edit.node.entry.title", "Edit node."),
					panel.getTxtName());
				if (option == JOptionPane.OK_OPTION)
				{
					NodeModel modelObject = panel.getModelObject();
					boolean leaf = modelObject.isLeaf();
					String name = modelObject.getName();
					selectedTreeNode.setLeaf(leaf);
					selectedTreeNode.setDisplayValue(name);

					if (selectedTreeNode.getValue().isLeaf() != leaf)
					{
						// set to leaf only if the node has no children
						if ((leaf) || 0 == selectedDefaultMutableTreeNode.getChildCount())
						{
							selectedTreeNode.getValue().setLeaf(!leaf);
						}
					}

					selectedTreeNode.getValue().setName(name);

					reload(selectedDefaultMutableTreeNode);
				}
			});

	}

	/**
	 * The callback method on delete the selected tree node
	 */
	@SuppressWarnings("unchecked")
	protected void onDeleteSelectedTreeNode(MouseEvent mouseEvent)
	{
		Optional<DefaultMutableTreeNode> selectedDefaultMutableTreeNode = JTreeExtensions
			.getSelectedDefaultMutableTreeNode(mouseEvent, tree);
		if (selectedDefaultMutableTreeNode.isPresent())
		{
			int option = DialogExtensions.showConfirmDialog(null, "Confirm deletion",
				"<div width='450'>Are you sure<br></div>"
					+ "<div>The delete action is not recoverable</div>",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION)
			{
				DefaultMutableTreeNode selectedTreeNode = selectedDefaultMutableTreeNode.get();
				Object userObject = selectedTreeNode.getUserObject();
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> currentSelectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;

				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent = currentSelectedTreeNode
					.getParent();
				parent.removeChild(currentSelectedTreeNode);
				int selectedNodeIndex = selectedTreeNode.getParent().getIndex(selectedTreeNode);
				selectedTreeNode.removeAllChildren();
				DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)selectedTreeNode
					.getParent();
				defaultMutableTreeNode.remove(selectedNodeIndex);
				reload(defaultMutableTreeNode);
			}
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
	 * @param mouseEvent
	 *            the mouse event
	 */
	protected void onTableSingleRightClick(MouseEvent mouseEvent)
	{
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();

		List<MysticCryptEntryModelBean> allSelectedRowData = getTblTreeEntryTable()
			.getAllSelectedRowData();

		JPopupMenu popup = JPopupMenuFactory.newJPopupMenu();

		JMenuItem copyUsername = JMenuItemFactory.newJMenuItem("Copy Password",
			actionEvent -> this.onCopyUsernameTableEntry());
		copyUsername.setEnabled(allSelectedRowData.size() == 1);
		popup.add(copyUsername);

		JMenuItem copyPassword = JMenuItemFactory.newJMenuItem("Copy Password",
			actionEvent -> this.onCopyPasswordTableEntry());
		copyPassword.setEnabled(allSelectedRowData.size() == 1);
		popup.add(copyPassword);

		JMenuItem openUrl = JMenuItemFactory.newJMenuItem("Open url",
			actionEvent -> this.onOpenUrlOfTableEntry());
		openUrl.setEnabled(
			allSelectedRowData.size() == 1 && allSelectedRowData.get(0).getUrl() != null);
		popup.add(openUrl);

		// Separator
		popup.addSeparator();

		JMenuItem add = JMenuItemFactory.newJMenuItem("add ...",
			actionEvent -> this.onAddTableEntry());
		popup.add(add);

		JMenuItem edit = JMenuItemFactory.newJMenuItem("edit",
			actionEvent -> this.onEditTableEntry());
		edit.setEnabled(allSelectedRowData.size() == 1);
		popup.add(edit);

		JMenuItem duplicate = JMenuItemFactory.newJMenuItem("duplicate",
			actionEvent -> this.onDuplicateTableEntry());
		duplicate.setEnabled(allSelectedRowData.size() == 1);
		popup.add(duplicate);

		JMenuItem delete = JMenuItemFactory.newJMenuItem("delete",
			actionEvent -> this.onDeleteTableEntry());
		delete.setEnabled(!allSelectedRowData.isEmpty());
		popup.add(delete);
		// Separator
		popup.addSeparator();

		JMenuItem selectAll = JMenuItemFactory.newJMenuItem("select all",
			actionEvent -> this.onSelectAllTableEntries());
		selectAll.setEnabled(0 < getTblTreeEntryTable().getRowCount());
		popup.add(selectAll);

		popup.show(getTblTreeEntryTable(), x, y);
	}

	protected void onSelectAllTableEntries()
	{
		getTblTreeEntryTable().selectAll();
	}

	protected void onDuplicateTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(tableEntry -> {

			// TODO...
		});
	}

	protected void onCopyUsernameTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(tableEntry -> {
			String userName = tableEntry.getUserName();
			ClipboardExtensions.copyToClipboard(userName);
		});
	}

	protected void onCopyPasswordTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(tableEntry -> {
			char[] password = tableEntry.getPassword();
			ClipboardExtensions.copyToClipboard(String.valueOf(password));
		});
	}

	protected void onDeleteTableEntry()
	{
		int option = DialogExtensions.showConfirmDialog(null, "Confirm deletion",
			"<div width='450'>Are you sure<br></div>"
				+ "<div>The delete action is not recoverable</div>",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null);
		if (option == JOptionPane.OK_OPTION)
		{
			getTblTreeEntryTable().getAllSelectedRowData().forEach(tableEntry -> {
				getTblTreeEntryTable().getGenericTableModel().remove(tableEntry);
			});

			getBaseTreeNodeModel();
		}
	}

	protected void onEditTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(tableEntry -> {
			showEditMysticCryptEntryDialog(tableEntry);
		});
	}

	protected void onOpenUrlOfTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(tableEntry -> {
			String urlString = tableEntry.getUrl();
			try
			{
				URL url = new URL(urlString);
				url.toExternalForm();
				BrowserControlExtensions.displayURLonStandardBrowser(this, urlString);
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected void onAddTableEntry()
	{
		MysticCryptEntryTabbedPanel panel = new MysticCryptEntryTabbedPanel();

		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, this,
			Messages.getString("dialog.new.crypt.entry.title", "New Crypt Entry."),
			panel.getMysticCryptEntryPanel().getTxtEntryName());
		if (option == JOptionPane.OK_OPTION)
		{
			MysticCryptEntryModelBean modelObject = panel.getModelObject();
			if (modelObject.isExpirable()
				&& panel.getMysticCryptEntryPanel().getTxtExpires().getSelectedDate() != null)
			{
				modelObject
					.setExpires(panel.getMysticCryptEntryPanel().getTxtExpires().getSelectedDate());
			}
			DefaultMutableTreeNode selectedTreeNode = getSelectedTreeNode();
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedBaseTreeNode;
			if (selectedTreeNode == null)
			{
				selectedBaseTreeNode = getModelObject().getRoot();
			}
			else
			{
				Object userObject = selectedTreeNode.getUserObject();
				selectedBaseTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;
			}
			GenericTreeElement<List<MysticCryptEntryModelBean>> value = selectedBaseTreeNode
				.getValue();
			if (value.getDefaultContent() == null)
			{
				value.setDefaultContent(new ArrayList<>());
			}
			value.getDefaultContent().add(modelObject);
			getTblTreeEntryTable().getGenericTableModel().add(modelObject);
			getBaseTreeNodeModel();
		}
	}

	private void reloadApplicationTreeModel()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootTreeNode = getModelObject();
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> longTreeIdNodeMap = BaseTreeNodeTransformer
			.toKeyMap(rootTreeNode);
		MysticCryptApplicationFrame.getInstance().getModelObject()
			.setRootTreeAsMap(longTreeIdNodeMap);
		MysticCryptApplicationFrame.getInstance().getModelObject().setDirty(true);

		final EventSource<EventObject<RenderMode>> eventSource = ApplicationEventBus.getSaveState();
		eventSource.fireEvent(new EventObject<>(RenderMode.EDITABLE));
	}

	private void getBaseTreeNodeModel()
	{
		reloadApplicationTreeModel();
		getTblTreeEntryTable().getGenericTableModel().fireTableDataChanged();
	}

	/**
	 * The callback method on the table double left click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onTableDoubleLeftClick(MouseEvent event)
	{
		Optional<MysticCryptEntryModelBean> singleSelectedRow = getTblTreeEntryTable()
			.getSingleSelectedRowData();
		if (singleSelectedRow.isPresent())
		{
			MysticCryptEntryModelBean tableEntry = singleSelectedRow.get();
			showEditMysticCryptEntryDialog(tableEntry);
		}
	}

	private void showEditMysticCryptEntryDialog(MysticCryptEntryModelBean tableEntry)
	{
		MysticCryptEntryTabbedPanel panel = new MysticCryptEntryTabbedPanel(
			BaseModel.of(tableEntry));
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null,
			Messages.getString("dialog.edit.crypt.entry.title", "Edit Crypt Entry."),
			panel.getMysticCryptEntryPanel().getTxtEntryName());

		if (option == JOptionPane.OK_OPTION)
		{
			List<MysticCryptEntryModelBean> data = getTblTreeEntryTable().getGenericTableModel()
				.getData();
			int index = data.indexOf(tableEntry);
			data.remove(tableEntry);
			MysticCryptEntryModelBean modelObject = panel.getModelObject();
			if (modelObject.isExpirable()
				&& panel.getMysticCryptEntryPanel().getTxtExpires().getSelectedDate() != null)
			{
				modelObject
					.setExpires(panel.getMysticCryptEntryPanel().getTxtExpires().getSelectedDate());
			}
			data.add(index, modelObject);

			getBaseTreeNodeModel();
		}
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

	private void reload(DefaultMutableTreeNode selectedTreeNode)
	{
		((DefaultTreeModel)tree.getModel()).reload(selectedTreeNode);

		reloadApplicationTreeModel();
		tree.treeDidChange();
		this.repaint();
	}

}
