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

import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The copy "Duplicate node" places next to the node it copies: the whole subtree with its values,
 * entries and children, sharing no mutable state with the source.
 * <p>
 * Not {@code CloneQuietlyExtensions.clone(...)}: that resolves to a shallow {@code Object.clone()},
 * so original and duplicate shared the same {@link GenericTreeElement} - renaming the duplicate
 * renamed the original too, and both pointed at the same entry list. Kept apart from
 * {@link SecretKeyTreeWithContentPanel}, which asks for the name and hangs the copy into the tree,
 * so the copy can be tested without a display.
 */
public final class NodeDuplication
{

	private NodeDuplication()
	{
	}

	/**
	 * Deep-copies the given tree node with its value, entries and children (the node ids are kept
	 * and are reindexed by the caller)
	 *
	 * @param source
	 *            the tree node to copy
	 * @param parent
	 *            the parent of the new copy, or {@code null}
	 * @return the deep copy
	 */
	public static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copyOf(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> source,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> sourceValue = source.getValue();
		GenericTreeElement<List<MysticCryptEntryModelBean>> copiedValue = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(sourceValue.getName())
			.leaf(sourceValue.isLeaf()).withText(sourceValue.isWithText())
			.iconPath(sourceValue.getIconPath()).selectedIconPath(sourceValue.getSelectedIconPath())
			.build();
		sourceValue.getProperties().forEach((key, propertyValue) -> {
			if (!GenericTreeElement.DEFAULT_CONTENT_KEY.equals(key))
			{
				copiedValue.getProperties().put(key, propertyValue);
			}
		});
		List<MysticCryptEntryModelBean> sourceEntries = sourceValue.getDefaultContent();
		if (sourceEntries != null)
		{
			List<MysticCryptEntryModelBean> copiedEntries = new ArrayList<>();
			for (MysticCryptEntryModelBean entry : sourceEntries)
			{
				copiedEntries.add(entry.duplicate());
			}
			copiedValue.setDefaultContent(copiedEntries);
		}

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
			.id(source.getId()).value(copiedValue).parent(parent)
			.displayValue(source.getDisplayValue()).leaf(source.isLeaf()).build();
		if (source.getChildren() != null)
		{
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : source
				.getChildren())
			{
				copy.addChild(copyOf(child, copy));
			}
		}
		return copy;
	}
}
