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
package io.github.astrapi69.mystic.crypt.panels.dbtree;

import java.awt.*;
import java.util.List;

import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.tree.TreeNode;
import io.github.astrapi69.window.adapter.CloseWindow;

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
		first.add(MysticCryptEntryModelBean.builder().userName("foo").build());
		TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>> genericTreeElementTreeNode = TestTreeNodeFactory
			.initializeTestGenericTreeNodeElement(first, first);
		Model<TreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>>> parentModel = BaseModel
			.of(genericTreeElementTreeNode);
		SecretKeyTreeWithContentPanel secretKeyTreeWithContentPanel = new SecretKeyTreeWithContentPanel(
			parentModel);
		frame.add(secretKeyTreeWithContentPanel);
		frame.pack();
		frame.setVisible(true);
	}
}
