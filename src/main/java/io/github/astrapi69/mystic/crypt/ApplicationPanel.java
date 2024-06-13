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
package io.github.astrapi69.mystic.crypt;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory;

public class ApplicationPanel extends BasePanel<ApplicationModelBean>
{

	SecretKeyTreeWithContentPanel secretKeyTreeWithContentPanel;

	public ApplicationPanel(final IModel<ApplicationModelBean> model)
	{
		super(model);
	}

	public ApplicationPanel()
	{
		this(BaseModel.of(ApplicationModelBean.builder().build()));
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		ApplicationModelBean modelObject = getModelObject();
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
		secretKeyTreeWithContentPanel = new SecretKeyTreeWithContentPanel(
			BaseModel.of(rootTreeNode));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		this.setLayout(new BorderLayout());
		this.add(this.secretKeyTreeWithContentPanel, "Center");
	}

}
