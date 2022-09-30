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
import javax.swing.JInternalFrame;

import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.panel.desktoppane.JDesktopPanePanel;
import io.github.astrapi69.swing.tree.BaseTreeNodeFactory;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.tree.BaseTreeNode;
import io.github.astrapi69.tree.TreeIdNode;
import io.github.astrapi69.tree.convert.BaseTreeNodeTransformer;

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
		ApplicationModelBean modelObject = MysticCryptApplicationFrame.getInstance()
			.getModelObject();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootTreeNode;
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> rootTreeAsMap = modelObject
			.getRootTreeAsMap();
		if (rootTreeAsMap == null || rootTreeAsMap.isEmpty())
		{
			LongIdGenerator idGenerator = MysticCryptApplicationFrame.getInstance()
				.getIdGenerator();
			GenericTreeElement<List<MysticCryptEntryModelBean>> parent = GenericTreeElement
				.<List<MysticCryptEntryModelBean>> builder().name("root")
				.iconPath("io/github/astrapi69/silk/icons/book.png").withText(true).build()
				.setDefaultContent(new ArrayList<>());

			GenericTreeElement<List<MysticCryptEntryModelBean>> firstChild = GenericTreeElement
				.<List<MysticCryptEntryModelBean>> builder().name("mykeys")
				.iconPath("io/github/astrapi69/silk/icons/folder.png").withText(true).build()
				.setDefaultContent(new ArrayList<>());
			rootTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null,
				idGenerator);
			modelObject.setLastId(rootTreeNode.getId());
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> myKeysTreeNode = BaseTreeNodeFactory
				.initializeTreeNodeWithTreeElement(firstChild, rootTreeNode, idGenerator);
			modelObject.setLastId(myKeysTreeNode.getId());
			Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> longTreeIdNodeMap = BaseTreeNodeTransformer
				.toKeyMap(rootTreeNode);
			modelObject.setRootTreeAsMap(longTreeIdNodeMap);
			rootTreeAsMap = modelObject.getRootTreeAsMap();
		}
		rootTreeNode = BaseTreeNodeTransformer.getRoot(rootTreeAsMap);
		final SecretKeyTreeWithContentPanel component = new SecretKeyTreeWithContentPanel(
			BaseModel.of(rootTreeNode));
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Key database",
			true, true, true, true);
		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		BasePanel<ApplicationModelBean> mainComponent = MysticCryptApplicationFrame.getInstance()
			.getMainComponent();
		JDesktopPanePanel<ApplicationModelBean> desktopPanePanel = (JDesktopPanePanel<ApplicationModelBean>)mainComponent;
		int screenHeight = desktopPanePanel.getDesktopPane().getHeight() - 50;
		int screenWidth = desktopPanePanel.getDesktopPane().getWidth();
		internalFrame.setSize(screenWidth, screenHeight);
		internalFrame.setLocation(0, 0);
		internalFrame.setResizable(true);

		JInternalFrameExtensions.addJInternalFrame(desktopPanePanel.getDesktopPane(),
			internalFrame);
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
