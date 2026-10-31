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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.vault.VaultCloseSupport;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The vault state over the host's model: entries live in the groups of the tree, a vault is open
 * while its master password file model is set, and read-only when a newer build wrote it (#405).
 */
public final class ModelVaultState implements VaultState
{

	private final Supplier<ApplicationModelBean> model;

	private final Runnable afterChange;

	/**
	 * The state over one model, for a caller that does nothing beyond marking it changed
	 *
	 * @param model
	 *            the model
	 */
	public ModelVaultState(final ApplicationModelBean model)
	{
		this(() -> model, () -> {
		});
	}

	/**
	 * The state over the model the host currently holds
	 *
	 * @param model
	 *            gives the host's current model
	 * @param afterChange
	 *            what the host does after a change besides marking the model changed, such as
	 *            refreshing the window
	 */
	public ModelVaultState(final Supplier<ApplicationModelBean> model, final Runnable afterChange)
	{
		this.model = model;
		this.afterChange = afterChange;
	}

	@Override
	public boolean isOpen()
	{
		return VaultCloseSupport.aVaultIsOpen(model.get());
	}

	@Override
	public boolean isUnlocked()
	{
		return model.get().isSignedIn();
	}

	@Override
	public String readOnlyReason()
	{
		ApplicationModelBean current = model.get();
		return VaultXmlCodec.isNewerThanThisBuild(current)
			? VaultXmlCodec.whyItIsReadOnly(current)
			: null;
	}

	@Override
	public List<List<MysticCryptEntryModelBean>> entryLists()
	{
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> tree = model
			.get().getRootTreeAsMap();
		List<List<MysticCryptEntryModelBean>> lists = new ArrayList<>();
		if (tree == null)
		{
			return lists;
		}
		for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : tree
			.values())
		{
			if (node.getValue() != null && node.getValue().getDefaultContent() != null)
			{
				lists.add(node.getValue().getDefaultContent());
			}
		}
		return lists;
	}

	@Override
	public List<MysticCryptEntryModelBean> rootEntries()
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> root = BaseTreeNodeTransformer
			.getRoot(model.get().getRootTreeAsMap()).getValue();
		if (root.getDefaultContent() == null)
		{
			root.setDefaultContent(new ArrayList<>());
		}
		return root.getDefaultContent();
	}

	@Override
	public void markChanged()
	{
		model.get().setDirty(true);
		afterChange.run();
	}
}
