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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.TestBaseTreeNodeFactory;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * What the search finds and in which order. Both search surfaces - the menu dialog and the toolbar
 * field - ask this one class, so what is pinned here is pinned for both.
 */
class DatabaseSearchSupportTest
{

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> aTree()
	{
		List<MysticCryptEntryModelBean> parentEntries = List.of(
			MysticCryptEntryModelBean.builder().title("bank account").userName("alice@example.org")
				.url("https://bank.example.org").notes("the shared notes").build());
		List<MysticCryptEntryModelBean> childEntries = List
			.of(MysticCryptEntryModelBean.builder().title("mail account").userName("bob")
				.password("secret-not-found".toCharArray()).build());
		return TestBaseTreeNodeFactory.initializeTestGenericTreeNodeElement(parentEntries,
			childEntries, LongIdGenerator.of(0L));
	}

	@Test
	@DisplayName("the first match is the topmost one the user sees, not the deepest")
	void theFirstMatchIsTheTopmostOne()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = aTree();

		// "Child" is in firstChild/search, firstGrandChild, firstGrandGrandChild, secondChild and
		// secondGrandChild - the tree's own post-order traversal would hand back a grandchild first
		assertEquals("firstChild/search",
			DatabaseSearchSupport.findFirstMatch(root, "Child").getValue().getName(),
			"the first match has to be the one nearest the top of the tree");
	}

	@Test
	@DisplayName("the matches come back in display order, parents before their children")
	void theMatchesComeBackInDisplayOrder()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = aTree();

		List<String> names = DatabaseSearchSupport.findMatches(root, "grand").stream()
			.map(match -> match.getValue().getName()).toList();

		assertEquals(List.of("firstGrandChild", "firstGrandGrandChild", "secondGrandChild"), names,
			"the matches are not in the order the tree displays");
	}

	@Test
	@DisplayName("an entry matches by title, user name, url and notes, case-insensitively")
	void anEntryMatchesByItsFields()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = aTree();

		for (String term : List.of("BANK ACC", "alice@", "bank.example", "SHARED NOTES"))
		{
			assertEquals("parent",
				DatabaseSearchSupport.findFirstMatch(root, term).getValue().getName(),
				"the term '" + term + "' did not find the entry that carries it");
		}
	}

	@Test
	@DisplayName("passwords are not searched")
	void passwordsAreNotSearched()
	{
		assertNull(DatabaseSearchSupport.findFirstMatch(aTree(), "secret-not-found"),
			"a password was found by the search, which must never happen");
	}

	@Test
	@DisplayName("no match and a blank term both come back empty instead of failing")
	void noMatchAndABlankTermBothComeBackEmpty()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = aTree();

		assertNull(DatabaseSearchSupport.findFirstMatch(root, "nothing-carries-this"));
		assertTrue(DatabaseSearchSupport.findMatches(root, "  ").isEmpty(),
			"a blank term has to find nothing, not everything");
		assertTrue(DatabaseSearchSupport.findMatches(null, "anything").isEmpty(),
			"no tree yet has to find nothing, not fail");
	}

}
