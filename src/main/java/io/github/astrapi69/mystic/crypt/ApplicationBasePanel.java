package io.github.astrapi69.mystic.crypt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.tree.BaseTreeNodeFactory;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.tree.BaseTreeNode;
import io.github.astrapi69.tree.TreeIdNode;
import io.github.astrapi69.tree.convert.BaseTreeNodeTransformer;

public class ApplicationBasePanel extends BasePanel<ApplicationModelBean>
{

	SecretKeyTreeWithContentPanel secretKeyTreeWithContentPanel;

	public ApplicationBasePanel(final IModel<ApplicationModelBean> model)
	{
		super(model);
	}

	public ApplicationBasePanel()
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


	}
}
