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

import java.util.List;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The state of {@link SecretKeyTreeWithContentPanel} that does not live in the tree itself: the
 * node the move dialog offers as the new parent of the selected node.
 * <p>
 * The combo box of that dialog is bound to this object, so the move reads the chosen target from
 * here instead of asking the widget what is selected in it.
 */
public class SecretKeyTreeWithContentPanelModel
{

	/** The node the selected node is moved under, null as long as no move was started */
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> moveTarget;

	/**
	 * Gets the node the selected node is moved under
	 *
	 * @return the new parent, null as long as no move was started
	 */
	public BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> getMoveTarget()
	{
		return moveTarget;
	}

	/**
	 * Sets the node the selected node is moved under
	 *
	 * @param moveTarget
	 *            the new parent
	 */
	public void setMoveTarget(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> moveTarget)
	{
		this.moveTarget = moveTarget;
	}
}
