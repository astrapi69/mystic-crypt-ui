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
package io.github.astrapi69.mystic.crypt.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.linguafranca.pwdb.kdbx.jackson.KdbxEntry;
import org.linguafranca.pwdb.kdbx.jackson.KdbxGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.keepass.KeePassEntryConverter;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The model side of a KeePass import, {@link ImportKeePassDatabaseAction#importUnder}: what it
 * leaves in the vault tree, the tree map and the dirty flag when the file goes in, and when the
 * conversion of an entry is refused half way (#1208).
 * <p>
 * The file is built at runtime: a root group with two subgroups, the first with a subgroup of its
 * own, and one entry in every group. No file is known whose entry conversion fails (the runs
 * 45992648533 and 45993393891 in the issue), so the refusal comes from the conversion this test
 * passes in. It refuses the entry of one named group with a {@link NumberFormatException}, the
 * exception of the one source found in the library's entry conversion, and converts every other
 * entry the way the import does.
 */
class ImportKeePassDatabaseActionTest
{

	private static final String VAULT_ROOT = "Vault root";
	private static final String EXISTING_GROUP = "Existing group";
	private static final String KEEPASS_ROOT = "KeePass root";
	private static final String FIRST_SUBGROUP = "First subgroup";
	private static final String NESTED_SUBGROUP = "Nested subgroup";
	private static final String LAST_SUBGROUP = "Last subgroup";

	@Test
	@DisplayName("an import hangs the whole file under the vault root, records the tree and marks the vault changed")
	void importUnder_hangsTheWholeFileUnderTheVaultRoot_andMarksTheVaultDirty()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = vaultRootWithOneGroup();
		ApplicationModelBean model = modelOf(root);

		ImportKeePassDatabaseAction.importUnder(root, keePassFile(), model, LongIdGenerator.of(2L),
			KeePassEntryConverter::toEntryModelBean);

		assertEquals(
			List.of(EXISTING_GROUP, KEEPASS_ROOT, FIRST_SUBGROUP, NESTED_SUBGROUP, LAST_SUBGROUP),
			namesUnder(root));
		assertEquals(idsOf(root), model.getRootTreeAsMap().keySet(),
			"the tree map holds every node of the tree, the imported ones included");
		assertEquals(Collections.max(idsOf(root)), model.getLastId(),
			"the model remembers the last id the import gave");
		assertTrue(model.isDirty(), "an import marks the vault changed");
	}

	@ParameterizedTest(name = "a refused entry in the group {0}")
	@MethodSource("refusalsAndTheGroupsTheyLeaveToday")
	@DisplayName("a refused import leaves the groups converted before the refusal under the vault root, today")
	void importUnder_refusedHalfWay_leavesTheGroupsConvertedSoFar_andTheVaultNotDirty(
		final String refusedGroup, final List<String> groupsLeftUnderTheVaultRoot)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = vaultRootWithOneGroup();
		ApplicationModelBean model = modelOf(root);
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeMapBefore = model
			.getRootTreeAsMap();

		assertThrows(NumberFormatException.class,
			() -> ImportKeePassDatabaseAction.importUnder(root, keePassFile(), model,
				LongIdGenerator.of(2L), refusingTheEntryOf(refusedGroup)));

		assertEquals(groupsLeftUnderTheVaultRoot, namesUnder(root),
			"today the groups converted before the refusal stay under the vault root (#1208)");
		assertSame(treeMapBefore, model.getRootTreeAsMap(), "a refused import records no tree");
		assertFalse(model.isDirty(), "a refused import does not mark the vault changed");
	}

	private static Stream<Arguments> refusalsAndTheGroupsTheyLeaveToday()
	{
		return Stream.of(Arguments.of(KEEPASS_ROOT, List.of(EXISTING_GROUP)),
			Arguments.of(NESTED_SUBGROUP, List.of(EXISTING_GROUP, KEEPASS_ROOT, FIRST_SUBGROUP)),
			Arguments.of(LAST_SUBGROUP,
				List.of(EXISTING_GROUP, KEEPASS_ROOT, FIRST_SUBGROUP, NESTED_SUBGROUP)));
	}

	/**
	 * The file to import: its root group with the first subgroup, which holds the nested one, and
	 * the last subgroup after it; one entry in every group
	 */
	private static KdbxGroup keePassFile()
	{
		KdbxDatabase database = new KdbxDatabase();
		KdbxGroup keePassRoot = database.getRootGroup();
		keePassRoot.setName(KEEPASS_ROOT);
		keePassRoot.addEntry(database.newEntry(entryTitleOf(KEEPASS_ROOT)));
		KdbxGroup first = groupWithAnEntry(database, FIRST_SUBGROUP);
		first.addGroup(groupWithAnEntry(database, NESTED_SUBGROUP));
		keePassRoot.addGroup(first);
		keePassRoot.addGroup(groupWithAnEntry(database, LAST_SUBGROUP));
		return keePassRoot;
	}

	private static KdbxGroup groupWithAnEntry(final KdbxDatabase database, final String name)
	{
		KdbxGroup group = (KdbxGroup)database.newGroup(name);
		group.addEntry(database.newEntry(entryTitleOf(name)));
		return group;
	}

	private static String entryTitleOf(final String groupName)
	{
		return "the entry of " + groupName;
	}

	/**
	 * The entry conversion of the import, refusing the entry of the given group
	 */
	private static Function<KdbxEntry, MysticCryptEntryModelBean> refusingTheEntryOf(
		final String groupName)
	{
		return entry -> {
			if (entryTitleOf(groupName).equals(entry.getTitle()))
			{
				throw new NumberFormatException("refused by the test: " + entry.getTitle());
			}
			return KeePassEntryConverter.toEntryModelBean(entry);
		};
	}

	/**
	 * A vault root that already holds one group, as an open vault does
	 */
	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> vaultRootWithOneGroup()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = node(0L,
			VAULT_ROOT, false);
		root.addChild(node(1L, EXISTING_GROUP, true));
		return root;
	}

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node(
		final long id, final String name, final boolean leaf)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(leaf).build();
		element.setDefaultContent(new ArrayList<>());
		return BaseTreeNode.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
			.id(id).value(element).displayValue(name).leaf(leaf).build();
	}

	/**
	 * The model of a vault whose tree map was taken from the given root, as signing in leaves it
	 */
	private static ApplicationModelBean modelOf(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root)
	{
		ApplicationModelBean model = new ApplicationModelBean();
		model.setRootTreeAsMap(BaseTreeNodeTransformer.toKeyMap(root));
		model.setLastId(Collections.max(idsOf(root)));
		return model;
	}

	/**
	 * The display values of every node under the given one, depth first, each before its children
	 */
	private static List<String> namesUnder(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node)
	{
		List<String> names = new ArrayList<>();
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : node
			.getChildren())
		{
			names.add(child.getDisplayValue());
			names.addAll(namesUnder(child));
		}
		return names;
	}

	private static Set<Long> idsOf(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node)
	{
		Set<Long> ids = new HashSet<>();
		ids.add(node.getId());
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : node
			.getChildren())
		{
			ids.addAll(idsOf(child));
		}
		return ids;
	}

}
