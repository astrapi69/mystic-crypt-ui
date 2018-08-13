package de.alpharogroup.mystic.crypt.panels.dbtree;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;

public class DatabaseTreePanel extends BasePanel<DatabaseTreeModelBean>
{

	private static final long serialVersionUID = 1L;

	private javax.swing.JTree tree;
	private javax.swing.JScrollPane scrDbTree;

	public DatabaseTreePanel()
	{

		this(BaseModel.<DatabaseTreeModelBean> of(DatabaseTreeModelBean.builder().build()));
	}

	public DatabaseTreePanel(final Model<DatabaseTreeModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		scrDbTree = new javax.swing.JScrollPane();
		tree = new javax.swing.JTree();

		setPreferredSize(new java.awt.Dimension(420, 560));

		scrDbTree.setViewportView(tree);
		// ===== custom ====
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("database", true),
			node1 = new DefaultMutableTreeNode("node 1", true),
			node2 = new DefaultMutableTreeNode("node 2", true),
			node3 = new DefaultMutableTreeNode("node 3", true);
		rootNode.add(node1);
		node1.add(node2);
		rootNode.add(node3);

		TreeModel treeModel = new DefaultTreeModel(rootNode, true);
		tree.setModel(treeModel);
		tree.setEditable(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		treeModel.addTreeModelListener(new TreeModelListener()
		{
			public void treeNodesChanged(TreeModelEvent e)
			{
				DefaultMutableTreeNode node;
				node = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());

				/*
				 * If the event lists children, then the changed node is the child of the node we
				 * have already gotten. Otherwise, the changed node and the specified node are the
				 * same.
				 */
				try
				{
					int index = e.getChildIndices()[0];
					node = (DefaultMutableTreeNode)(node.getChildAt(index));
				}
				catch (NullPointerException exc)
				{
				}

				System.out.println("The user has finished editing the node.");
				System.out.println("New value: " + node.getUserObject());
			}

			public void treeNodesInserted(TreeModelEvent e)
			{
			}

			public void treeNodesRemoved(TreeModelEvent e)
			{
			}

			public void treeStructureChanged(TreeModelEvent e)
			{
			}
		});

		tree.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					if (e.getClickCount() == 1)
					{
						onSingleClick(e);
					}
					else if (e.getClickCount() == 2)
					{
						onDoubleClick(selRow, selPath);
					}
				}
			}
		});

	}

	protected void onSingleClick(MouseEvent e)
	{
		int x = e.getX();
		int y = e.getY();
		TreePath selectionPath = tree.getPathForLocation(e.getX(), e.getY());

		JPopupMenu popup = new JPopupMenu();
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
			Object[] inputFields = { "Enter name for node", textField1, "Is leaf", checkBox };

			int option = JOptionPane.showConfirmDialog(this, inputFields, "Multiple Inputs",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

			if (option == JOptionPane.OK_OPTION)
			{
				boolean allowsChildren = !checkBox.isSelected();
				String userObject = textField1.getText();
				DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(userObject,
					allowsChildren);
				DefaultMutableTreeNode selectedPathComponent = (DefaultMutableTreeNode)selectionPath
					.getLastPathComponent();
				selectedPathComponent.add(newChild);
				((DefaultTreeModel)tree.getModel()).reload((TreeNode)selectedPathComponent);
				tree.treeDidChange();
			}

		});
		popup.add(addChild);

		JMenuItem deleteNode = new JMenuItem("delete");
		deleteNode.addActionListener(le -> {
			DefaultMutableTreeNode selectedPathComponent = (DefaultMutableTreeNode)selectionPath
				.getLastPathComponent();

			if (selectedPathComponent.getParent() != null)
			{
				int selectedNodeIndex = selectedPathComponent.getParent()
					.getIndex(selectedPathComponent);
				selectedPathComponent.removeAllChildren();
				((DefaultMutableTreeNode)selectedPathComponent.getParent())
					.remove(selectedNodeIndex);
				((DefaultTreeModel)tree.getModel()).reload((TreeNode)selectedPathComponent);
				tree.treeDidChange();
			}

		});
		popup.add(deleteNode);
		popup.show(tree, x, y);
	}

	protected void onDoubleClick(int selRow, TreePath selPath)
	{

	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(scrDbTree, javax.swing.GroupLayout.PREFERRED_SIZE, 384,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(scrDbTree, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE)
				.addContainerGap()));
	}
}
