package de.alpharogroup.mystic.crypt.panels.dbtree;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;

import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;


/**
 * The abstract class {@link JTreePanel} provides a {@link JTree} that is already embedded in a
 * {@link JScrollPane}. Additionally it provides factory methods that can be overwritten to provide
 * specific behavior.
 *
 * @param <T>
 *            the generic type of the model object
 * @deprecated use instead the same name class from new release swing-components project
 */
@Getter
public abstract class JTreePanel<T> extends BasePanel<T>
{

	/** The serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The {@link JScrollPane} for the {@link JTree}. */
	protected JScrollPane scrTree;

	/** The {@link JTree}. */
	protected JTree tree;

	/**
	 * Instantiates a new {@link JTreePanel} object.
	 */
	public JTreePanel()
	{
		super();
	}

	/**
	 * Instantiates a new new {@link JTreePanel} object.
	 *
	 * @param model
	 *            the model
	 */
	public JTreePanel(final Model<T> model)
	{
		super(model);
	}

	/**
	 * New tree.
	 *
	 * @return the j tree
	 */
	protected JTree newTree()
	{
		JTree tree = new JTree();

		tree.setModel(newTreeModel(getModel()));
		tree.setEditable(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);


		tree.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					if (e.getClickCount() == 1)
					{
						onSingleClick(e);
					}
					else if (e.getClickCount() == 2)
					{
						onDoubleClick(e);
					}
				}
			}
		});

		return tree;
	}

	/**
	 * Abstract factory callback method that have to be overwritten to provide the specific
	 * {@link TreeModel} for the {@link JTree}
	 *
	 * @param model
	 *            the model
	 * @return the tree model
	 */
	protected abstract TreeModel newTreeModel(final Model<T> model);

	/**
	 * Factory method for creating the new {@link JScrollPane}. This method is invoked in the
	 * constructor from the derived classes and can be overridden so users can provide their own
	 * version of a {@link JScrollPane}
	 *
	 * @return the new {@link JScrollPane}
	 */
	protected JScrollPane newTreeScrollPane()
	{
		JScrollPane scrDbTree = new JScrollPane();
		return scrDbTree;
	}

	/**
	 * The callback method on double click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onDoubleClick(MouseEvent event)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		scrTree = newTreeScrollPane();
		tree = newTree();

		setPreferredSize(newPreferredSize(420, 560));
		scrTree.setViewportView(tree);
	}

	/**
	 * 
	 * Factory method for creating the new {@link Dimension}. This method is invoked in the
	 * constructor from the derived classes and can be overridden so users can provide their own
	 * version of a {@link Dimension}
	 *
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * 
	 * @return the new {@link Dimension}
	 */
	protected Dimension newPreferredSize(int width, int height)
	{
		return new Dimension(width, height);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
	}

	/**
	 * The callback method on single click.
	 *
	 * @param event
	 *            the mouse event
	 */
	protected void onSingleClick(MouseEvent event)
	{
	}

}
