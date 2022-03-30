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
package io.github.astrapi69.mystic.crypt.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.tree.BaseTreeNodeFactory;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.tree.BaseTreeNode;
import io.github.astrapi69.tree.TreeIdNode;
import io.github.astrapi69.tree.convert.TreeNodeTransformer;

/**
 * The class {@link OpenDatabaseTreeFrameAction}.
 */
public class OpenDatabaseTreeFrameAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new new action.
	 *
	 * @param name
	 *            the name
	 */
	public OpenDatabaseTreeFrameAction(final String name)
	{
		super(name);
	}

	public static void openDatabaseTreeFrame()
	{
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Key database",
			true, true, true, true);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootTreeNode;
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> rootTreeAsMap = MysticCryptApplicationFrame
			.getInstance().getModelObject().getRootTreeAsMap();
		if (rootTreeAsMap == null || rootTreeAsMap.isEmpty())
		{
			LongIdGenerator idGenerator = MysticCryptApplicationFrame
				.getInstance().getIdGenerator();
			GenericTreeElement<List<MysticCryptEntryModelBean>> parent = GenericTreeElement
				.<List<MysticCryptEntryModelBean>> builder().name("root")
				.iconPath("io/github/astrapi69/silk/icons/book.png").withText(true).parent(null)
				.node(true).build().setDefaultContent(new ArrayList<>());

			GenericTreeElement<List<MysticCryptEntryModelBean>> firstChild = GenericTreeElement
				.<List<MysticCryptEntryModelBean>> builder().name("mykeys").parent(parent)
				.iconPath("io/github/astrapi69/silk/icons/folder.png").withText(true).node(true)
				.build().setDefaultContent(new ArrayList<>());
			rootTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null,
				idGenerator);
			BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild, rootTreeNode,
				idGenerator);
			MysticCryptApplicationFrame.getInstance().getModelObject()
				.setRootTreeAsMap(TreeNodeTransformer.toKeyMap(rootTreeNode));
			rootTreeAsMap = MysticCryptApplicationFrame.getInstance().getModelObject()
				.getRootTreeAsMap();
		}
		rootTreeNode = TreeNodeTransformer.getRoot(rootTreeAsMap);
		final SecretKeyTreeWithContentPanel component = new SecretKeyTreeWithContentPanel(
			BaseModel.of(rootTreeNode));
		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		JDesktopPane mainComponent = MysticCryptApplicationFrame.getInstance().getMainComponent();
		int screenHeight = mainComponent.getHeight() - 50;
		int screenWidth = mainComponent.getWidth();
		internalFrame.setSize(screenWidth, screenHeight);
		internalFrame.setLocation(0, 0);
		internalFrame.setResizable(true);

		JInternalFrameExtensions.addJInternalFrame(mainComponent, internalFrame);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		openDatabaseTreeFrame();
	}

}
