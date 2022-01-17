package io.github.astrapi69.mystic.crypt.panels.dbtree;

import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.tree.TreeNode;
import io.github.astrapi69.window.adapter.CloseWindow;

import java.awt.Frame;
import java.util.List;

class TestSecretKeyTreeWithContentPanel
{

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(final String[] args)
	{
		final Frame frame = new Frame("SecretKeyTreeWithContentPanel");
		frame.addWindowListener(new CloseWindow());
		List<MysticCryptEntryModelBean> first = ListFactory.newArrayList();
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> genericTreeElementTreeNode =
			TestTreeNodeFactory
				.initializeTestGenericTreeNodeElement(first, first);
		Model<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> parentModel = BaseModel
			.of(genericTreeElementTreeNode);
		SecretKeyTreeWithContentPanel secretKeyTreeWithContentPanel =
			new SecretKeyTreeWithContentPanel(parentModel);
		frame.add(secretKeyTreeWithContentPanel);
		frame.pack();
		frame.setVisible(true);
	}
}
