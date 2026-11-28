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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.Group;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A duplicated group read back from the KDBX file the export writes: the copy is a group of its
 * own, so its KeePass group UUID is not the original's. The group property {@code keepass.uuid},
 * which the export writes as the group's UUID, is the identity of the original and does not go into
 * the copy, in the sub-groups of the copy neither.
 */
class NodeDuplicationKdbxExportTest
{

	private static final String COPY_NAME = "group-Copy";

	private final String password = TestPasswords.throwaway();

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> newNode(
		final long id, final String name,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).build();
		element.setDefaultContent(new ArrayList<>());
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(id)
			.value(element).displayValue(name).leaf(false).parent(parent).build();
		if (parent != null)
		{
			parent.addChild(node);
		}
		return node;
	}

	/**
	 * Duplicates the group the way the panel does after the name dialog: renamed, hung next to the
	 * original
	 */
	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> duplicate(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> group)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = NodeDuplication
			.copyOf(group, null);
		copy.getValue().setName(COPY_NAME);
		copy.setDisplayValue(COPY_NAME);
		copy.setParent(root);
		root.addChild(copy);
		assertEquals(2, root.getChildren().size(), "the copy hangs next to the original");
		return copy;
	}

	private KdbxDatabase exportedAndReadBack(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root)
		throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		KeePassTreeConverter.fillDatabase(database, root);
		KdbxCredentials credentials = new KdbxCredentials(
			password.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		database.write(credentials, file);
		return KdbxDatabase.read(credentials, new ByteArrayInputStream(file.toByteArray()));
	}

	private static void collectUuids(final Group group, final List<UUID> into)
	{
		for (Object subGroup : group.getGroups())
		{
			Group child = (Group)subGroup;
			into.add(child.getUuid());
			collectUuids(child, into);
		}
	}

	@Test
	@DisplayName("pin: a duplicated group and its original share their UUIDs in the exported file")
	void export_ofADuplicatedGroup_sharesTheGroupUuids() throws Exception
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = newNode(0,
			"root", null);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> group = newNode(1,
			"group", root);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> subGroup = newNode(
			2, "sub-group", group);
		UUID groupUuid = UUID.randomUUID();
		UUID subGroupUuid = UUID.randomUUID();
		group.getValue().getProperties().put(KeePassTreeConverter.KEEPASS_UUID_PROPERTY, groupUuid);
		subGroup.getValue().getProperties().put(KeePassTreeConverter.KEEPASS_UUID_PROPERTY,
			subGroupUuid);
		duplicate(root, group);

		KdbxDatabase read = exportedAndReadBack(root);

		List<UUID> uuids = new ArrayList<>();
		collectUuids(read.getRootGroup(), uuids);
		Set<UUID> distinct = new HashSet<>(uuids);
		assertEquals(4, uuids.size(), "two groups with a sub-group each: " + uuids);
		assertEquals(2, distinct.size(),
			"today the copy and the original share their UUIDs: " + uuids);
		assertEquals(Set.of(groupUuid, subGroupUuid), distinct);
	}

	@Test
	@DisplayName("pin: the copy of a group carries the KeePass group UUID of the original")
	void copyOf_carriesTheKeePassGroupUuid()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = newNode(0,
			"root", null);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> group = newNode(1,
			"group", root);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> subGroup = newNode(
			2, "sub-group", group);
		UUID groupUuid = UUID.randomUUID();
		group.getValue().getProperties().put(KeePassTreeConverter.KEEPASS_UUID_PROPERTY, groupUuid);
		subGroup.getValue().getProperties().put(KeePassTreeConverter.KEEPASS_UUID_PROPERTY,
			UUID.randomUUID());

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = duplicate(
			root, group);

		assertEquals(groupUuid,
			copy.getValue().getProperties().get(KeePassTreeConverter.KEEPASS_UUID_PROPERTY),
			"today the copy carries the UUID of its original");
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copiedSubGroup = copy
			.getChildren().iterator().next();
		assertEquals(
			subGroup.getValue().getProperties().get(KeePassTreeConverter.KEEPASS_UUID_PROPERTY),
			copiedSubGroup.getValue().getProperties()
				.get(KeePassTreeConverter.KEEPASS_UUID_PROPERTY),
			"today the copied sub-group carries the UUID of its original");
		assertEquals(groupUuid,
			group.getValue().getProperties().get(KeePassTreeConverter.KEEPASS_UUID_PROPERTY),
			"copying does not change the original");
	}
}
