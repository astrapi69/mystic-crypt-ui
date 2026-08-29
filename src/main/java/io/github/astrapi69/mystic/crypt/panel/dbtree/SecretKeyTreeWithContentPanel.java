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

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionComposerBuilder;
import org.kquiet.browser.ActionRunner;
import org.openqa.selenium.By;

import io.github.astrapi69.awt.extension.ClipboardExtensions;
import io.github.astrapi69.browser.BrowserControlExtensions;
import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.component.model.node.NodeModel;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.design.pattern.observer.event.EventSource;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.gen.tree.visitor.MaxIndexFinderTreeNodeVisitor;
import io.github.astrapi69.gen.tree.visitor.ReindexTreeNodeVisitor;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.mystic.crypt.panel.table.NewTableEntryModel;
import io.github.astrapi69.mystic.crypt.panel.table.NewTableEntryPanel;
import io.github.astrapi69.swing.dialog.DialogExtensions;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.listener.mouse.MouseDoubleClickListener;
import io.github.astrapi69.swing.menu.factory.JMenuItemFactory;
import io.github.astrapi69.swing.menu.factory.JPopupMenuFactory;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.label.LabelModel;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.swing.renderer.tree.renderer.state.NewGenericBaseTreeNodeCellRenderer;
import io.github.astrapi69.swing.table.GenericJTable;
import io.github.astrapi69.swing.table.model.GenericTableModel;
import io.github.astrapi69.swing.tree.extension.JTreeExtensions;
import io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory;
import io.github.astrapi69.swing.tree.panel.content.BaseTreeNodeGenericTreeElementWithContentPanel;
import io.github.astrapi69.swing.tree.panel.node.NodePanel;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

public class SecretKeyTreeWithContentPanel
	extends
		BaseTreeNodeGenericTreeElementWithContentPanel<List<MysticCryptEntryModelBean>, Long, MysticCryptEntryModelBean>
{
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Everything this panel holds outside the tree itself: the node the move dialog offers as the
	 * new parent. It is assigned after the components are built, which is what the base class does
	 * in its constructor - nothing in that cycle reads it
	 */
	private final transient SecretKeyTreeWithContentPanelModel panelModelObject;

	public SecretKeyTreeWithContentPanel(
		final IModel<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> model)
	{
		super(model);
		panelModelObject = new SecretKeyTreeWithContentPanelModel();
	}

	/**
	 * Gets the model of this panel, which holds the node the move dialog offers as the new parent
	 *
	 * @return the model of this panel
	 */
	public SecretKeyTreeWithContentPanelModel getPanelModelObject()
	{
		return panelModelObject;
	}

	@Override
	protected JTree newTree()
	{
		JTree tree = super.newTree();
		tree.setCellRenderer(
			new NewGenericBaseTreeNodeCellRenderer<List<MysticCryptEntryModelBean>, Long>());
		// the root is what the whole database hangs off, not something the user put there: there
		// can only ever be one of it, it cannot be renamed away, moved or deleted, and showing it
		// only makes every real node look like it sits one level too deep. Hidden, the nodes the
		// user created - and a subtree imported from KeePass - are the top level, which is what
		// they are meant to be
		tree.setRootVisible(false);
		// without the root row there would be nothing to click to open the top level, so the
		// handles have to be drawn for its children
		tree.setShowsRootHandles(true);
		return tree;
	}

	/**
	 * The root of the tree, which is not shown: everything the user sees is one of its children
	 *
	 * @return the root node
	 */
	@SuppressWarnings("unchecked")
	protected BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootTreeNode()
	{
		return (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)((DefaultMutableTreeNode)tree
			.getModel().getRoot()).getUserObject();
	}

	@Override
	protected JScrollPane newTreeScrollPane()
	{
		JScrollPane treeScrollPane = super.newTreeScrollPane();
		treeScrollPane.getViewport().setOpaque(false);
		treeScrollPane.setOpaque(false);
		return treeScrollPane;
	}

	@Override
	protected JScrollPane newTableScrollPane()
	{
		JScrollPane tableScrollPane = super.newTableScrollPane();
		tableScrollPane.getViewport().setOpaque(false);
		tableScrollPane.setOpaque(false);
		tableScrollPane.addMouseListener(new MouseDoubleClickListener()
		{
			@Override
			public void onSingleClick(MouseEvent mouseEvent)
			{
				List<MysticCryptEntryModelBean> data = getTblTreeEntryTable().getGenericTableModel()
					.getData();
				List<MysticCryptEntryModelBean> allSelectedRowData = getTblTreeEntryTable()
					.getAllSelectedRowData();

				boolean noRowSelected = allSelectedRowData.isEmpty();
				boolean emptyTable = data.isEmpty();
				if (emptyTable || noRowSelected)
				{
					if (mouseEvent.getButton() == MouseEvent.BUTTON1)
					{
						SecretKeyTreeWithContentPanel.this.onTableSingleLeftClick(mouseEvent);
					}
					if (mouseEvent.getButton() == MouseEvent.BUTTON2)
					{

						SecretKeyTreeWithContentPanel.this.onTableSingleMiddleClick(mouseEvent);
					}
					if (mouseEvent.getButton() == MouseEvent.BUTTON3)
					{
						SecretKeyTreeWithContentPanel.this.onTableSingleRightClick(mouseEvent);
					}
				}
			}

			@Override
			public void onDoubleClick(MouseEvent mouseEvent)
			{
				if (mouseEvent.getButton() == MouseEvent.BUTTON1)
				{
					// SecretKeyTreeWithContentPanel.this.onTableDoubleLeftClick(mouseEvent);
				}
				if (mouseEvent.getButton() == MouseEvent.BUTTON2)
				{

					// SecretKeyTreeWithContentPanel.this.onTableDoubleMiddleClick(mouseEvent);
				}
				if (mouseEvent.getButton() == MouseEvent.BUTTON3)
				{
					// SecretKeyTreeWithContentPanel.this.onTableDoubleRightClick(mouseEvent);
				}
			}
		});
		return tableScrollPane;
	}

	@Override
	protected GenericJTable<MysticCryptEntryModelBean> newJTable()
	{
		GenericTableModel<MysticCryptEntryModelBean> tableModel = new MysticCryptEntryTableModel();
		return new GenericJTable<>(tableModel)
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
		onInitializeKeyboardShortcuts();
	}

	/**
	 * The keyboard shortcuts of the tree and the entries table, the set a KeePass user expects.
	 * <p>
	 * A key press acts on the SELECTED node - the popup menu acts on the node under the click, and
	 * the two resolve differently on purpose: a click has coordinates, a key press has none. Both
	 * end in the same seam methods, so what a shortcut does and what the menu item does can never
	 * drift apart.
	 * <p>
	 * On the entries table, control C copies the entry's password and control B its user name, the
	 * way KeePass has it - which deliberately replaces the table's built-in copy of the visible
	 * cell text.
	 */
	protected void onInitializeKeyboardShortcuts()
	{
		installTreeShortcut("DELETE", "deleteSelectedNode", this::onDeleteSelectedTreeNode);
		installTreeShortcut("F2", "editSelectedNode", this::onEditSelectedTreeNode);
		installTreeShortcut("control K", "duplicateSelectedNode",
			this::onDuplicateSelectedTreeNode);
		installTreeShortcut("INSERT", "addChildToSelectedNode", this::addChildTreeNode);
		installTreeShortcut("alt UP", "moveSelectedNodeUp",
			selectedNode -> onMoveSelectedTreeNode(selectedNode, -1));
		installTreeShortcut("alt DOWN", "moveSelectedNodeDown",
			selectedNode -> onMoveSelectedTreeNode(selectedNode, 1));

		installTableShortcut("control B", "copyUsernameOfSelectedEntry",
			this::onCopyUsernameTableEntry);
		installTableShortcut("control C", "copyPasswordOfSelectedEntry",
			this::onCopyPasswordTableEntry);
		installTableShortcut("ENTER", "editSelectedEntry", this::onEditTableEntry);
		installTableShortcut("DELETE", "deleteSelectedEntries", this::onDeleteTableEntry);
		installTableShortcut("INSERT", "addEntryUnderSelectedNode", this::onAddTableEntry);
	}

	/**
	 * Binds the given key on the tree to the given action over the selected node. Without a
	 * selection, or with the hidden root selected, the key does nothing - a key press must never
	 * guess a node the way a click never has to.
	 *
	 * @param keyStroke
	 *            the key, in {@link KeyStroke#getKeyStroke(String)} form
	 * @param actionKey
	 *            the name the binding is registered under
	 * @param action
	 *            what to do with the selected node
	 */
	protected void installTreeShortcut(final String keyStroke, final String actionKey,
		final Consumer<DefaultMutableTreeNode> action)
	{
		tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyStroke), actionKey);
		tree.getActionMap().put(actionKey, new AbstractAction()
		{
			@Override
			public void actionPerformed(final ActionEvent actionEvent)
			{
				if (tree
					.getLastSelectedPathComponent()instanceof DefaultMutableTreeNode selectedNode
					&& !selectedNode.isRoot())
				{
					action.accept(selectedNode);
				}
			}
		});
	}

	/**
	 * Binds the given key on the entries table to the given action. The action only runs while the
	 * table has a selection AND a node is selected in the tree: the entry operations resolve the
	 * node the entries belong to, and without one they would have to guess.
	 *
	 * @param keyStroke
	 *            the key, in {@link KeyStroke#getKeyStroke(String)} form
	 * @param actionKey
	 *            the name the binding is registered under
	 * @param action
	 *            what to do
	 */
	protected void installTableShortcut(final String keyStroke, final String actionKey,
		final Runnable action)
	{
		getTblTreeEntryTable().getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(keyStroke), actionKey);
		getTblTreeEntryTable().getActionMap().put(actionKey, new AbstractAction()
		{
			@Override
			public void actionPerformed(final ActionEvent actionEvent)
			{
				boolean aNodeIsSelected = tree
					.getLastSelectedPathComponent()instanceof DefaultMutableTreeNode selectedNode
					&& !selectedNode.isRoot();
				if (aNodeIsSelected && 0 < getTblTreeEntryTable().getSelectedRows().length)
				{
					action.run();
				}
			}
		});
	}

	@Override
	protected TreeModel newTreeModel(
		final IModel<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> model)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parentTreeNode = model
			.getObject();
		DefaultMutableTreeNode rootNode = BaseTreeNodeFactory
			.newDefaultMutableTreeNode(parentTreeNode);
		letTheModelDecideWhatIsALeaf(rootNode);

		return new DefaultTreeModel(rootNode, true);
	}


	/**
	 * Makes the swing nodes agree with the leaf flag the application maintains.
	 * <p>
	 * The tree model is built with {@code asksAllowsChildren}, so swing decides leaf or not from
	 * {@link DefaultMutableTreeNode#getAllowsChildren()} and never looks at
	 * {@link BaseTreeNode#isLeaf()}. The factory that builds the swing nodes sets it to true for
	 * every one of them, which is why every row used to carry an expand handle, including the rows
	 * with nothing behind them.
	 *
	 * @param node
	 *            the root of the swing tree to walk
	 */
	public static void letTheModelDecideWhatIsALeaf(final DefaultMutableTreeNode node)
	{
		if (node == null)
		{
			return;
		}
		Enumeration<javax.swing.tree.TreeNode> nodes = node.preorderEnumeration();
		while (nodes.hasMoreElements())
		{
			javax.swing.tree.TreeNode next = nodes.nextElement();
			if (next instanceof DefaultMutableTreeNode swingNode
				&& swingNode.getUserObject()instanceof BaseTreeNode<?, ?> treeNode)
			{
				swingNode.setAllowsChildren(!treeNode.isLeaf());
			}
		}
	}

	/**
	 * {@inheritDoc}
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

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onTreeSingleRightClick(MouseEvent mouseEvent)
	{
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();
		Optional<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> optionalSelectedUserObject = JTreeExtensions
			.getSelectedUserObject(mouseEvent, tree);
		if (optionalSelectedUserObject.isEmpty())
		{
			// clicked next to the nodes: the root is hidden, so this is the only way left to put a
			// node on the top level
			JPopupMenu rootPopup = JPopupMenuFactory.newJPopupMenu();
			JMenuItem addTopLevelNode = JMenuItemFactory.newJMenuItem("add node...",
				actionEvent -> this.onAddNewTopLevelTreeNode());
			addTopLevelNode.setName("treeAddTopLevelNode");
			rootPopup.add(addTopLevelNode);
			rootPopup.show(tree, x, y);
			return;
		}
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

				popup.addSeparator();

				// moving is only offered where it can do something: the first child cannot go up,
				// the last cannot go down, and the root has no siblings at all
				JMenuItem moveUp = JMenuItemFactory.newJMenuItem("Move up",
					actionEvent -> this.onMoveSelectedTreeNode(mouseEvent, -1));
				moveUp.setName("treeMoveUp");
				moveUp.setEnabled(canMove(selectedTreeNodeElement, -1));
				popup.add(moveUp);

				JMenuItem moveDown = JMenuItemFactory.newJMenuItem("Move down",
					actionEvent -> this.onMoveSelectedTreeNode(mouseEvent, 1));
				moveDown.setName("treeMoveDown");
				moveDown.setEnabled(canMove(selectedTreeNodeElement, 1));
				popup.add(moveDown);

				JMenuItem moveTo = JMenuItemFactory.newJMenuItem("Move to node...",
					actionEvent -> this.onMoveSelectedTreeNodeToAnotherParent(mouseEvent));
				moveTo.setName("treeMoveTo");
				popup.add(moveTo);
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
	protected void onDuplicateSelectedTreeNode(final MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> onDuplicateSelectedTreeNode(
				selectedDefaultMutableTreeNode));
	}

	/**
	 * Duplicates the given node with its whole subtree, after asking for the copy's name. This is
	 * the seam the mouse path and the keyboard path share: the popup resolves its node by where the
	 * click landed, a key press resolves it from the selection, and both end here.
	 *
	 * @param selectedDefaultMutableTreeNode
	 *            the node to act on
	 */
	protected void onDuplicateSelectedTreeNode(
		final DefaultMutableTreeNode selectedDefaultMutableTreeNode)
	{
		// get the selected tree node from the DefaultMutableTreeNode
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)selectedDefaultMutableTreeNode
			.getUserObject();
		// declare a visitor for reindex the new tree nodes
		ReindexTreeNodeVisitor<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long, BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> reindexTreeNodeVisitor;

		// declare a visitor for find the maximum index
		MaxIndexFinderTreeNodeVisitor<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long, BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> maxIndexFinderTreeNodeVisitor;

		Long maxIndex;
		long nextId;
		// implement the visitor for find the max index
		maxIndexFinderTreeNodeVisitor = new MaxIndexFinderTreeNodeVisitor<>()
		{
			@Override
			public boolean isGreater(Long id)
			{
				return getMaxIndex() < id;
			}
		};
		// NOT CloneQuietlyExtensions.clone(...): that resolves to a SHALLOW
		// Object.clone(), so original and duplicate shared the same GenericTreeElement -
		// renaming the duplicate renamed the original too, and both pointed at the same
		// entry list. Deep-copy the subtree instead
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> clonedTreeNode = deepCopyTreeNode(
			selectedTreeNode, null);
		NodePanel panel = new NodePanel()
		{
			protected void onInitializeMigLayout()
			{
				MigLayout layout = new MigLayout(new LC().fillX().wrapAfter(2),
					new AC().align("left").gap("10").grow().fill(), new AC().fill().gap("10"));
				this.setLayout(layout);

				add(getLblName());
				add(getTxtName(), new CC().grow().width("120px"));
			}
		};
		String newName = clonedTreeNode.getDisplayValue() + "-Copy";
		panel.getModelObject().setName(newName);
		panel.getTxtName().setText(newName);
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null,
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
			MysticCryptApplicationFrame.getInstance().setIdGenerator(LongIdGenerator.of(nextId));
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

			try
			{
				TimeUnit.MILLISECONDS.sleep(200);
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}

			reload(parent);
		}
	}

	/**
	 * Deep-copies the given tree node with its value, entries and children, so the copy shares no
	 * mutable state with the source (ids are kept and are reindexed by the caller)
	 *
	 * @param source
	 *            the tree node to copy
	 * @param parent
	 *            the parent of the new copy, or {@code null}
	 * @return the deep copy
	 */
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> deepCopyTreeNode(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> source,
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> sourceValue = source.getValue();
		GenericTreeElement<List<MysticCryptEntryModelBean>> copiedValue = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(sourceValue.getName())
			.leaf(sourceValue.isLeaf()).withText(sourceValue.isWithText())
			.iconPath(sourceValue.getIconPath()).selectedIconPath(sourceValue.getSelectedIconPath())
			.build();
		sourceValue.getProperties().forEach((key, propertyValue) -> {
			if (!GenericTreeElement.DEFAULT_CONTENT_KEY.equals(key))
			{
				copiedValue.getProperties().put(key, propertyValue);
			}
		});
		List<MysticCryptEntryModelBean> sourceEntries = sourceValue.getDefaultContent();
		if (sourceEntries != null)
		{
			List<MysticCryptEntryModelBean> copiedEntries = new ArrayList<>();
			for (MysticCryptEntryModelBean entry : sourceEntries)
			{
				copiedEntries.add(entry.toBuilder().resources(new ArrayList<>(entry.getResources()))
					.properties(new ArrayList<>(entry.getProperties())).build());
			}
			copiedValue.setDefaultContent(copiedEntries);
		}

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
			.id(source.getId()).value(copiedValue).parent(parent)
			.displayValue(source.getDisplayValue()).leaf(source.isLeaf()).build();
		if (source.getChildren() != null)
		{
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : source
				.getChildren())
			{
				copy.addChild(deepCopyTreeNode(child, copy));
			}
		}
		return copy;
	}

	/**
	 * The siblings of a node in the order they are shown, as a list - the children are held in a
	 * set, which has an order but no positions
	 *
	 * @param treeNode
	 *            the node whose siblings are wanted
	 * @return the siblings including the node itself, empty for the root
	 */
	public static List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> siblingsOf(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent = treeNode
			.getParent();
		return parent == null ? new ArrayList<>() : new ArrayList<>(parent.getChildren());
	}

	/**
	 * The position of a node among its siblings, compared by identity - not by equals, which
	 * includes the name and therefore cannot tell two nodes of the same name apart
	 *
	 * @param treeNode
	 *            the node to look for
	 * @return the position, or -1 when the node has no parent
	 */
	public static int positionOf(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode)
	{
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> siblings = siblingsOf(
			treeNode);
		for (int position = 0; position < siblings.size(); position++)
		{
			if (siblings.get(position) == treeNode)
			{
				return position;
			}
		}
		return -1;
	}

	/**
	 * Whether a node can be moved by the given offset: the first child cannot go further up, the
	 * last cannot go further down, and the root has no siblings to move among
	 *
	 * @param treeNode
	 *            the node to move
	 * @param offset
	 *            -1 for up, 1 for down
	 * @return true if the move would change the order
	 */
	public static boolean canMove(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode,
		int offset)
	{
		int position = positionOf(treeNode);
		int target = position + offset;
		return 0 <= position && 0 <= target && target < siblingsOf(treeNode).size();
	}

	/**
	 * The callback method on moving the selected tree node one position up or down among its
	 * siblings
	 *
	 * @param mouseEvent
	 *            the mouse event that opened the context menu
	 * @param offset
	 *            -1 for up, 1 for down
	 */
	@SuppressWarnings("unchecked")
	protected void onMoveSelectedTreeNode(final MouseEvent mouseEvent, final int offset)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree).ifPresent(
			selectedDefaultMutableTreeNode -> onMoveSelectedTreeNode(selectedDefaultMutableTreeNode,
				offset));
	}

	/**
	 * Moves the given node by the given offset among its siblings. This is the seam the mouse path
	 * and the keyboard path share: the popup resolves its node by where the click landed, a key
	 * press resolves it from the selection, and both end here.
	 *
	 * @param selectedDefaultMutableTreeNode
	 *            the node to act on
	 */
	protected void onMoveSelectedTreeNode(
		final DefaultMutableTreeNode selectedDefaultMutableTreeNode, final int offset)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)selectedDefaultMutableTreeNode
			.getUserObject();
		if (!canMove(selectedTreeNode, offset))
		{
			return;
		}
		int position = positionOf(selectedTreeNode);
		int target = position + offset;
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> siblings = siblingsOf(
			selectedTreeNode);
		siblings.remove(position);
		siblings.add(target, selectedTreeNode);

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parentTreeNode = selectedTreeNode
			.getParent();
		Collection<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> reordered = new LinkedHashSet<>(
			siblings);
		if (reordered.size() != siblings.size())
		{
			// two siblings that are equal by name would collapse into one here, which would
			// lose a node; refuse the move instead of silently dropping it
			DialogExtensions.showMessageDialog(null, "Move not possible",
				"Two nodes of the same name are on this level. Rename one of them first.",
				JOptionPane.WARNING_MESSAGE);
			return;
		}
		parentTreeNode.setChildren(reordered);

		DefaultMutableTreeNode swingParent = (DefaultMutableTreeNode)selectedDefaultMutableTreeNode
			.getParent();
		swingParent.insert(selectedDefaultMutableTreeNode, target);
		reload(swingParent);
		// keep the moved node selected, so it can be moved again without picking it anew
		tree.setSelectionPath(new TreePath(selectedDefaultMutableTreeNode.getPath()));
	}

	/**
	 * The callback method on moving the selected tree node under another node. The move itself is
	 * the one the tree library already offers - it refuses a leaf as the new parent and refuses a
	 * node that is inside the moved subtree, which would cut the tree in two
	 *
	 * @param mouseEvent
	 *            the mouse event that opened the context menu
	 */
	@SuppressWarnings("unchecked")
	protected void onMoveSelectedTreeNodeToAnotherParent(MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> {
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)selectedDefaultMutableTreeNode
					.getUserObject();
				List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets = possibleMoveTargets(
					selectedTreeNode);
				if (targets.isEmpty())
				{
					DialogExtensions.showMessageDialog(null, "Move not possible",
						"There is no other node this one could be moved under.",
						JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				JMComboBox<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>, ?> targetChooser = newMoveTargetChooser(
					targets);
				JPanel panel = new JPanel();
				panel.add(new JLabel("Move under:"));
				panel.add(targetChooser);
				int option = JOptionPaneExtensions.getSelectedOption(panel,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
					Messages.getString("dialog.move.node.title", "Move node"), targetChooser);
				if (option != JOptionPane.OK_OPTION)
				{
					return;
				}
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> newParentTreeNode = panelModelObject
					.getMoveTarget();
				DefaultMutableTreeNode oldSwingParent = (DefaultMutableTreeNode)selectedDefaultMutableTreeNode
					.getParent();
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> oldParentTreeNode = selectedTreeNode
					.getParent();

				// the library guards against a leaf target and against a target inside the moved
				// subtree, but it detaches the node with a hash based remove, which a renamed node
				// survives - so the node is detached by identity here and only then handed over
				if (!newParentTreeNode.isNode() || newParentTreeNode.isDescendant(selectedTreeNode))
				{
					DialogExtensions.showMessageDialog(null, "Move not possible",
						"A node cannot be moved into a leaf or into one of its own children.",
						JOptionPane.WARNING_MESSAGE);
					return;
				}
				oldParentTreeNode.getChildren().removeIf(child -> child == selectedTreeNode);
				selectedTreeNode.setParent(newParentTreeNode);
				newParentTreeNode.addChild(selectedTreeNode);

				oldSwingParent.remove(selectedDefaultMutableTreeNode);
				DefaultMutableTreeNode newSwingParent = findSwingNodeOf(newParentTreeNode);
				if (newSwingParent != null)
				{
					newSwingParent.add(selectedDefaultMutableTreeNode);
					reload(newSwingParent);
					tree.setSelectionPath(new TreePath(selectedDefaultMutableTreeNode.getPath()));
				}
				reload(oldSwingParent);
			});
	}

	/**
	 * The combo box the move dialog offers the possible new parents in, bound to the model of this
	 * panel so that the move reads the chosen target from there and not out of the widget
	 *
	 * @param targets
	 *            the nodes the selected node could be moved under
	 * @return the combo box over the given targets, the first of them chosen
	 */
	@SuppressWarnings("unchecked")
	protected JMComboBox<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>, ?> newMoveTargetChooser(
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets)
	{
		// the combo box opens on its first entry, so the model carries that one before anything is
		// picked - a dialog that is confirmed unchanged moves the node where it says it will
		panelModelObject.setMoveTarget(targets.get(0));
		JMComboBox<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>, ?> targetChooser = new JMComboBox<>(
			targets.toArray(new BaseTreeNode[0]));
		targetChooser.setName("cmbMoveTarget");
		targetChooser.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
			{
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)value;
				return super.getListCellRendererComponent(list, node != null ? pathOf(node) : "",
					index, isSelected, cellHasFocus);
			}
		});
		targetChooser.setPropertyModel(
			LambdaModel.of(panelModelObject::getMoveTarget, panelModelObject::setMoveTarget));
		return targetChooser;
	}

	/**
	 * The nodes a given node could be moved under: every node that can hold children, except the
	 * node itself, its current parent and everything inside its own subtree
	 *
	 * @param treeNode
	 *            the node to move
	 * @return the possible new parents
	 */
	public static List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> possibleMoveTargets(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode)
	{
		return treeNode.getRoot().traverse().stream().filter(BaseTreeNode::isNode)
			.filter(candidate -> candidate != treeNode)
			.filter(candidate -> candidate != treeNode.getParent())
			.filter(candidate -> !treeNode.isDescendant(candidate)).toList();
	}

	/**
	 * The path of a node as text, so a target can be told apart from another one of the same name
	 */
	private static String pathOf(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode)
	{
		StringBuilder path = new StringBuilder(String.valueOf(treeNode.getDisplayValue()));
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent = treeNode
			.getParent();
		while (parent != null)
		{
			path.insert(0, parent.getDisplayValue() + " / ");
			parent = parent.getParent();
		}
		return path.toString();
	}

	/**
	 * Selects and shows the given node in the tree.
	 * <p>
	 * Scrolling to the path expands its ancestors by itself, so the tree is not expanded whole
	 * first - a search that jumps from match to match runs this per keystroke, and laying out the
	 * entire tree each time is a cost nobody ordered.
	 *
	 * @param treeNode
	 *            the node to select; nothing happens when it is not in the tree
	 */
	public void selectTreeNode(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode)
	{
		DefaultMutableTreeNode swingNode = findSwingNodeOf(treeNode);
		if (swingNode == null)
		{
			return;
		}
		TreePath path = new TreePath(swingNode.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
	}

	/** Finds the Swing node that carries the given tree node, comparing by identity */
	private DefaultMutableTreeNode findSwingNodeOf(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
		Enumeration<javax.swing.tree.TreeNode> nodes = root.depthFirstEnumeration();
		while (nodes.hasMoreElements())
		{
			javax.swing.tree.TreeNode candidate = nodes.nextElement();
			if (candidate instanceof DefaultMutableTreeNode mutableTreeNode
				&& mutableTreeNode.getUserObject() == treeNode)
			{
				return mutableTreeNode;
			}
		}
		return null;
	}

	/**
	 * The callback method on add a new node on the top level, which is what the children of the
	 * hidden root are
	 */
	protected void onAddNewTopLevelTreeNode()
	{
		addChildTreeNode((DefaultMutableTreeNode)tree.getModel().getRoot());
	}

	/**
	 * The callback method on add a new child tree node
	 */
	protected void onAddNewChildTreeNode(MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(this::addChildTreeNode);
	}

	/**
	 * Asks for a name and adds a new child under the given node
	 *
	 * @param parentSwingNode
	 *            the node the new child goes under
	 */
	@SuppressWarnings("unchecked")
	protected void addChildTreeNode(DefaultMutableTreeNode parentSwingNode)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parentTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)parentSwingNode
			.getUserObject();
		NodePanel panel = new NodePanel();
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null,
			Messages.getString("dialog.new.node.entry.title", "New node."), panel.getTxtName());
		if (option != JOptionPane.OK_OPTION)
		{
			return;
		}
		NodeModel modelObject = panel.getModelObject();
		boolean leaf = modelObject.isLeaf();
		String name = modelObject.getName();
		GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(leaf).build();
		LongIdGenerator idGenerator = MysticCryptApplicationFrame.getInstance().getIdGenerator();
		Long nextId = idGenerator.getNextId();
		MysticCryptApplicationFrame.getInstance().getModelObject().setLastId(nextId);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> newTreeNode = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(nextId)
			.value(treeElement).parent(parentTreeNode).displayValue(name).leaf(leaf).build();
		parentTreeNode.addChild(newTreeNode);

		// the second argument is allowsChildren, so a leaf must not allow them; passing 'leaf'
		// here gave a leaf a handle and told swing a folder had none, which hid its children
		DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(newTreeNode, !leaf);
		parentSwingNode.add(newChild);
		reload(parentSwingNode);
	}

	@SuppressWarnings("unchecked")
	protected void onEditSelectedTreeNode(final MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(selectedDefaultMutableTreeNode -> onEditSelectedTreeNode(
				selectedDefaultMutableTreeNode));
	}

	/**
	 * Opens the given node for editing. This is the seam the mouse path and the keyboard path
	 * share: the popup resolves its node by where the click landed, a key press resolves it from
	 * the selection, and both end here.
	 *
	 * @param selectedDefaultMutableTreeNode
	 *            the node to act on
	 */
	protected void onEditSelectedTreeNode(
		final DefaultMutableTreeNode selectedDefaultMutableTreeNode)
	{
		Object userObject = selectedDefaultMutableTreeNode.getUserObject();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;
		NodePanel panel = new NodePanel(
			BaseModel.of(NodeModel.builder().name(selectedTreeNode.getValue().getName())
				.leaf(selectedTreeNode.getValue().isLeaf()).build()));
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.INFORMATION_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null,
			Messages.getString("dialog.edit.node.entry.title", "Edit node."), panel.getTxtName());
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
	}

	/**
	 * The callback method on delete the selected tree node
	 */
	@SuppressWarnings("unchecked")
	protected void onDeleteSelectedTreeNode(final MouseEvent mouseEvent)
	{
		JTreeExtensions.getSelectedDefaultMutableTreeNode(mouseEvent, tree)
			.ifPresent(this::onDeleteSelectedTreeNode);
	}

	/**
	 * Deletes the given node with everything under it, after asking for confirmation. This is the
	 * seam the mouse path and the keyboard path share: the popup resolves its node by where the
	 * click landed, a key press resolves it from the selection, and both end here.
	 *
	 * @param selectedTreeNodeToDelete
	 *            the node to delete
	 */
	protected void onDeleteSelectedTreeNode(final DefaultMutableTreeNode selectedTreeNodeToDelete)
	{
		{
			int option = DialogExtensions.showConfirmDialog(null, "Confirm deletion",
				"<div width='450'>Are you sure<br></div>"
					+ "<div>The delete action is not recoverable</div>",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION)
			{
				DefaultMutableTreeNode selectedTreeNode = selectedTreeNodeToDelete;
				Object userObject = selectedTreeNode.getUserObject();
				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> currentSelectedTreeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;

				BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent = currentSelectedTreeNode
					.getParent();
				// NOT parent.removeChild(...): the children live in a LinkedHashSet and
				// BaseTreeNode's equals/hashCode include value and displayValue - after a rename
				// the node's hash no longer matches its bucket, contains() fails and removeChild
				// silently does nothing (a renamed node could never be deleted). Remove by
				// identity instead, which is immune to mutated hash codes
				parent.getChildren().removeIf(child -> child == currentSelectedTreeNode);
				currentSelectedTreeNode.setParent(null);
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
		MysticCryptEntryModelBean selectedRow;

		List<MysticCryptEntryModelBean> allSelectedRowData = getTblTreeEntryTable()
			.getAllSelectedRowData();

		boolean noRowSelected = allSelectedRowData.isEmpty();
		boolean singleSelectedRow = allSelectedRowData.size() == 1;
		boolean rowsSelected = !noRowSelected;
		boolean validUrl = false;
		if (singleSelectedRow)
		{
			selectedRow = allSelectedRowData.get(0);
			String urlString = selectedRow.getUrl();
			validUrl = validateUrlString(urlString);
		}

		JPopupMenu popup = JPopupMenuFactory.newJPopupMenu();

		JMenuItem copyUsername = JMenuItemFactory.newJMenuItem("Copy Username",
			actionEvent -> this.onCopyUsernameTableEntry());
		copyUsername.setEnabled(singleSelectedRow);
		popup.add(copyUsername);

		JMenuItem copyPassword = JMenuItemFactory.newJMenuItem("Copy Password",
			actionEvent -> this.onCopyPasswordTableEntry());
		copyPassword.setEnabled(singleSelectedRow);
		popup.add(copyPassword);

		JMenuItem openUrl = JMenuItemFactory.newJMenuItem("Open url",
			actionEvent -> this.onOpenUrlOfTableEntry());
		openUrl.setEnabled(validUrl);
		popup.add(openUrl);

		JMenuItem openUrlAndAutotype = JMenuItemFactory.newJMenuItem("Autotype",
			actionEvent -> this.onOpenUrlAndAutotypeOfTableEntry());
		openUrlAndAutotype.setEnabled(validUrl);
		popup.add(openUrlAndAutotype);

		// Separator
		popup.addSeparator();

		JMenuItem add = JMenuItemFactory.newJMenuItem("add...",
			actionEvent -> this.onAddTableEntry());
		popup.add(add);

		JMenuItem edit = JMenuItemFactory.newJMenuItem("edit...",
			actionEvent -> this.onEditTableEntry());
		edit.setEnabled(singleSelectedRow);
		popup.add(edit);

		JMenuItem duplicate = JMenuItemFactory.newJMenuItem("duplicate...",
			actionEvent -> this.onDuplicateTableEntry());
		duplicate.setEnabled(singleSelectedRow);
		popup.add(duplicate);

		JMenuItem delete = JMenuItemFactory.newJMenuItem("delete",
			actionEvent -> this.onDeleteTableEntry());
		delete.setEnabled(rowsSelected);
		popup.add(delete);
		// Separator
		popup.addSeparator();

		JMenuItem selectAll = JMenuItemFactory.newJMenuItem("select all",
			actionEvent -> this.onSelectAllTableEntries());
		selectAll.setEnabled(0 < getTblTreeEntryTable().getRowCount());

		popup.add(selectAll);

		JMenuItem clearSelection = JMenuItemFactory.newJMenuItem("clear selection",
			actionEvent -> this.onDeselectAllTableEntries());
		clearSelection.setEnabled(rowsSelected);

		popup.add(clearSelection);

		popup.show(getTblTreeEntryTable(), x, y);
	}

	private static boolean validateUrlString(String urlString)
	{
		try
		{
			new URL(urlString).toURI();
		}
		catch (MalformedURLException | URISyntaxException e)
		{
			return false;
		}
		return true;
	}

	protected void onOpenUrlAndAutotypeOfTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(selectedTableEntry -> {
			String url = selectedTableEntry.getUrl();
			try (ActionRunner actionRunner = new MyBasicActionRunner())
			{
				ActionComposer actionComposer = new ActionComposerBuilder().prepareActionSequence()
					.getUrl(url)
					.waitUntil(elementToBeClickable(By.xpath("//input[@id='UserName']")), 3000)
					.sendKey(By.xpath("//input[@id='UserName']"), selectedTableEntry.getUserName())
					.waitUntil(elementToBeClickable(By.xpath("//input[@id='Password']")), 3000)
					.sendKey(By.xpath("//input[@id='Password']"),
						String.valueOf(selectedTableEntry.getPassword()))
					.waitUntil(elementToBeClickable(By.cssSelector("input[type='submit']")), 3000)
					.prepareClick(By.cssSelector("input[type='submit']")).done()
					.returnToComposerBuilder().buildBasic().setCloseWindow(false)
					.onFail(
						ac -> System.err.println("an exception is thrown or is marked as failed "
							+ "when open and auto type the username and password"))
					.onDone(ac -> System.out
						.println("open and auto type the username and password done"));
				CompletableFuture<Void> voidCompletableFuture = actionRunner
					.executeComposer(actionComposer);
				voidCompletableFuture.join();
			}
		});
	}

	protected void onSelectAllTableEntries()
	{
		getTblTreeEntryTable().selectAll();
	}

	protected void onDeselectAllTableEntries()
	{
		getTblTreeEntryTable().clearSelection();
	}

	protected void onDuplicateTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData().ifPresent(selectedTableEntry -> {
			// NOT CloneQuietlyExtensions.clone(...): that resolves to a shallow copy, so original
			// and duplicate would share the same resources/properties/modification lists
			MysticCryptEntryModelBean clonedMysticCryptEntry = selectedTableEntry.toBuilder()
				.resources(new ArrayList<>(selectedTableEntry.getResources()))
				.properties(new ArrayList<>(selectedTableEntry.getProperties())).build();

			String newName = clonedMysticCryptEntry.getTitle() + "-Copy";
			NewTableEntryModel newTableEntryModel = NewTableEntryModel.builder().name(newName)
				.labelModelName(LabelModel.builder()
					.text(Messages.getString("dialog.duplicate.crypt.entry.new.title.name",
						"Name of title for duplicate"))
					.build())
				.build();

			NewTableEntryPanel panel = new NewTableEntryPanel(BaseModel.of(newTableEntryModel));

			int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION, null,
				Messages.getString("dialog.duplicate.crypt.entry.title", "New title for duplicate"),
				panel.getTxtName());
			if (option == JOptionPane.OK_OPTION)
			{
				String name = panel.getModelObject().getName();
				clonedMysticCryptEntry.setTitle(name);

				addNewTableEntryToModel(clonedMysticCryptEntry);
			}
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
			List<MysticCryptEntryModelBean> defaultContent = getSelectedBaseTreeNode().getValue()
				.getDefaultContent();
			getTblTreeEntryTable().getAllSelectedRowData().forEach(tableEntry -> {
				getTblTreeEntryTable().getGenericTableModel().remove(tableEntry);
				// the table model holds a COPY of the node's entry list (newTableModel does
				// removeAll+addList), so removing there alone leaves the entry in the node's
				// defaultContent - it would reappear on reselecting the node or after a reopen
				if (defaultContent != null)
				{
					defaultContent.removeIf(entry -> entry == tableEntry);
				}
			});

			getBaseTreeNodeModel();
		}
	}

	protected void onEditTableEntry()
	{
		getTblTreeEntryTable().getSingleSelectedRowData()
			.ifPresent(this::showEditMysticCryptEntryDialog);
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
			addNewTableEntryToModel(modelObject);
		}
	}

	private void addNewTableEntryToModel(MysticCryptEntryModelBean modelObject)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> selectedBaseTreeNode = getSelectedBaseTreeNode();

		GenericTreeElement<List<MysticCryptEntryModelBean>> value = selectedBaseTreeNode.getValue();
		if (value.getDefaultContent() == null)
		{
			value.setDefaultContent(new ArrayList<>());
		}
		value.getDefaultContent().add(modelObject);
		getTblTreeEntryTable().getGenericTableModel().add(modelObject);
		getBaseTreeNodeModel();
	}

	@SuppressWarnings("unchecked")
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> getSelectedBaseTreeNode()
	{
		DefaultMutableTreeNode selectedTreeNode = getSelectedTreeNode();
		if (selectedTreeNode == null)
		{
			return getModelObject().getRoot();
		}
		{
			Object userObject = selectedTreeNode.getUserObject();
			return (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)userObject;
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
