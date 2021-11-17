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
package io.github.astrapi69.mystic.crypt.actions;

import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.layout.ScreenSizeExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panels.checksum.ChecksumPanel;
import io.github.astrapi69.mystic.crypt.panels.dbtree.DatabaseTreePanel;
import io.github.astrapi69.swing.components.factories.JComponentFactory;
import io.github.astrapi69.swing.panels.tree.JXTreeElement;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;
import io.github.astrapi69.tree.TreeNode;

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		openDatabaseTreeFrame();
	}

	public static void openDatabaseTreeFrame()
	{
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Key database",
			true, true, true, true);
		TreeNode<JXTreeElement> rootTreeNode = MysticCryptApplicationFrame.getInstance().getModelObject().getRootTreeNode();
		if (rootTreeNode == null)
		{
			JXTreeElement parent = JXTreeElement.builder().name("root")
				.iconPath("io/github/astrapi69/silk/icons/book.png").withText(true).parent(null)
				.node(true).build();

			JXTreeElement firstChild = JXTreeElement.builder().name("mykeys").parent(parent)
				.iconPath("io/github/astrapi69/silk/icons/folder.png")
				.withText(true)
				.node(true).build();
			rootTreeNode = TreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null);
			TreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild, rootTreeNode);
		}
		final DatabaseTreePanel component = new DatabaseTreePanel(BaseModel.of(rootTreeNode));
		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		JDesktopPane mainComponent = MysticCryptApplicationFrame.getInstance().getMainComponent();
		int screenHeight = mainComponent.getHeight() - 50;
		int screenWidth = mainComponent.getWidth();
		internalFrame.setSize(screenWidth , screenHeight);
		internalFrame.setLocation(0, 0);
		internalFrame.setResizable(true);

		JInternalFrameExtensions.addJInternalFrame(mainComponent, internalFrame);
	}

}
