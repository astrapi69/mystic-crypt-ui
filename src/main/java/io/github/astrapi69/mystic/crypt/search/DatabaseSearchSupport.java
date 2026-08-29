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
package io.github.astrapi69.mystic.crypt.search;

import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Searches the open database: a node matches when its name - or the title, user name, URL or notes
 * of one of its entries - contains the term, case-insensitively. Passwords are never searched.
 * <p>
 * The matches come back in the order the tree displays, top down: a search that jumps to "the
 * first" match must mean the first one the user sees, and the tree's own traversal is post-order,
 * which would make it the deepest one instead.
 * <p>
 * This is the one place that decides what matches. The menu search and the toolbar field both ask
 * here, so the two can never drift apart.
 */
public final class DatabaseSearchSupport
{

	private DatabaseSearchSupport()
	{
	}

	/**
	 * Every node that matches the given term, in the order the tree displays them
	 *
	 * @param root
	 *            the root of the database tree
	 * @param term
	 *            what to search for; blank finds nothing
	 * @return the matching nodes, top down; empty when nothing matches
	 */
	public static List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> findMatches(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root,
		final String term)
	{
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> matches = new ArrayList<>();
		if (root == null || term == null || term.isBlank())
		{
			return matches;
		}
		collectMatches(root, term.toLowerCase(), matches);
		return matches;
	}

	/**
	 * The first matching node the user would see, or null when nothing matches
	 *
	 * @param root
	 *            the root of the database tree
	 * @param term
	 *            what to search for
	 * @return the topmost matching node, or null
	 */
	public static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> findFirstMatch(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root,
		final String term)
	{
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> matches = findMatches(
			root, term);
		return matches.isEmpty() ? null : matches.get(0);
	}

	/**
	 * Whether the given node matches the given term: by its name, or by the title, user name, URL
	 * or notes of one of its entries. Passwords are deliberately not searched.
	 *
	 * @param node
	 *            the node to ask
	 * @param lowerTerm
	 *            the term, already lower case
	 * @return true when the node matches
	 */
	public static boolean matches(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
		final String lowerTerm)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> value = node.getValue();
		if (value == null)
		{
			return false;
		}
		if (contains(value.getName(), lowerTerm))
		{
			return true;
		}
		List<MysticCryptEntryModelBean> entries = value.getDefaultContent();
		if (entries != null)
		{
			for (MysticCryptEntryModelBean entry : entries)
			{
				if (contains(entry.getTitle(), lowerTerm)
					|| contains(entry.getUserName(), lowerTerm)
					|| contains(entry.getUrl(), lowerTerm) || contains(entry.getNotes(), lowerTerm))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static void collectMatches(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
		final String lowerTerm,
		final List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> matches)
	{
		if (matches(node, lowerTerm))
		{
			matches.add(node);
		}
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : node
			.getChildren())
		{
			collectMatches(child, lowerTerm, matches);
		}
	}

	private static boolean contains(final String value, final String lowerTerm)
	{
		return value != null && value.toLowerCase().contains(lowerTerm);
	}

}
