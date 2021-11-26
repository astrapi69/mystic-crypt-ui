package io.github.astrapi69.mystic.crypt.panels.dbtree;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.panels.tree.JXTreeElement;
import io.github.astrapi69.tree.TreeNode;
import io.github.astrapi69.window.adapter.CloseWindow;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class TreeNodeJXTreeElementWithContentPanelTest
{

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(final String[] args)
	{
		final Frame frame = new Frame("JXTreeWithContentPanel");
		frame.addWindowListener(new CloseWindow());
		Model<TreeNode<JXTreeElement>> parentModel = BaseModel
			.of(TestTreeNodeFactory.initializeTestJXTreeNodeElement());
		frame.add(new TreeNodeJXTreeElementWithContentPanel(parentModel));
		frame.pack();
		frame.setVisible(true);
	}
}